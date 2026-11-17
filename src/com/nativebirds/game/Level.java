package com.nativebirds.game;

import java.util.ArrayList;
import java.util.List;

/** Level definition plus a builder that turns it into physics bodies. */
public class Level {

    public static final float GROUND_Y = 0f;

    public static class BlockSpec {
        public final int material;
        public final float x, y, w, h;

        BlockSpec(int material, float x, float y, float w, float h) {
            this.material = material;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    public static class PigSpec {
        public final float x, surfaceY, radius;
        public final int size;
        public final boolean helmet;

        PigSpec(float x, float surfaceY, int size, boolean helmet) {
            this.x = x;
            this.surfaceY = surfaceY;
            this.size = size;
            this.helmet = helmet;
            /** Pig radii scaled alongside the birds (x0.68) so a medium pig reads as
         *  slightly bigger than a bird rather than towering over the whole hut. */
        this.radius = size == Pig.SMALL ? 0.62f : size == Pig.BIG ? 1.18f : 0.85f;
        }
    }

    public final int index;
    public final String name;
    public final int[] birds;
    public final List<BlockSpec> blocks = new ArrayList<BlockSpec>();
    public final List<PigSpec> pigs = new ArrayList<PigSpec>();
    public final int star2;
    public final int star3;

    private Level(int index, String name, int[] birds, int star2, int star3) {
        this.index = index;
        this.name = name;
        this.birds = birds;
        this.star2 = star2;
        this.star3 = star3;
    }

    private Level b(int material, float x, float y, float w, float h) {
        blocks.add(new BlockSpec(material, x, y, w, h));
        return this;
    }

    private Level p(float x, float surfaceY, int size) {
        pigs.add(new PigSpec(x, surfaceY, size, false));
        return this;
    }

    private Level ph(float x, float surfaceY) {
        pigs.add(new PigSpec(x, surfaceY, Pig.MEDIUM, true));
        return this;
    }

    public int pigCount() {
        return pigs.size();
    }

    // ------------------------------------------------------------------ build

    public static final float WOOD_DENSITY = 1.35f;
    public static final float STONE_DENSITY = 2.4f;
    public static final float ICE_DENSITY = 1.0f;

    public void spawn(World world, List<Block> outBlocks, List<Pig> outPigs) {
        for (BlockSpec s : blocks) {
            Block blk = new Block();
            blk.material = s.material;
            blk.w = s.w;
            blk.h = s.h;
            blk.spriteAngle = s.h > s.w ? (float) (Math.PI / 2) : 0f;

            float density = s.material == Block.WOOD ? WOOD_DENSITY
                    : s.material == Block.STONE ? STONE_DENSITY : ICE_DENSITY;
            Body body = new Body().setBox(s.w, s.h, density);
            body.set(s.x + s.w / 2f, s.y + s.h / 2f, 0f);
            body.friction = s.material == Block.ICE ? 0.34f : 0.62f;
            body.restitution = s.material == Block.ICE ? 0.12f : 0.03f;
            body.linearDamping = 0.06f;
            body.angularDamping = 0.10f;
            body.userData = blk;
            blk.body = body;

            float perArea = s.material == Block.WOOD ? 34f
                    : s.material == Block.STONE ? 62f : 16f;
            blk.maxHp = s.w * s.h * perArea;
            blk.hp = blk.maxHp;

            world.add(body);
            outBlocks.add(blk);
        }

        for (PigSpec s : pigs) {
            Pig pig = new Pig();
            pig.size = s.size;
            pig.radius = s.radius;
            pig.helmet = s.helmet;

            Body body = new Body().setCircle(s.radius, 1.15f);
            body.set(s.x, s.surfaceY + s.radius, 0f);
            body.friction = 0.55f;
            body.restitution = 0.22f;
            body.linearDamping = 0.05f;
            body.angularDamping = 0.4f;
            body.userData = pig;
            pig.body = body;

            pig.maxHp = s.helmet ? 210f : s.size == Pig.SMALL ? 46f
                    : s.size == Pig.BIG ? 165f : 92f;
            pig.hp = pig.maxHp;

            world.add(body);
            outPigs.add(pig);
        }
    }

    // ----------------------------------------------------------------- levels

    public static List<Level> all() {
        List<Level> l = new ArrayList<Level>();
        l.add(level1());
        l.add(level2());
        l.add(level3());
        l.add(level4());
        l.add(level5());
        l.add(level6());
        l.add(level7());
        l.add(level8());
        l.add(level9());
        l.add(level10());
        l.add(level11());
        l.add(level12());
        return l;
    }

    // ---------------------------------------------------------------- levels
    //
    // Geometry rules these layouts obey (checked by tools/LevelTest):
    //  * block sizes are limited to those with artwork: 1x1 2x1 2x2 2x4 4x1 4x2 8x1 10x1
    //  * a pig's diameter must fit the bay it stands in: 1-wide columns 3 apart
    //    leave a 2.0 bay (small pig), 8-wide spans leave 6.0 (any pig)
    //  * nothing is taller than 7 and no pig sits higher than 7.7, so the whole
    //    structure stays inside the camera with the HUD clear above it

    // ---------------------------------------------------------------- levels
    //
    // Geometry rules these layouts obey (checked by tools/LevelTest):
    //  * block sizes are limited to those with artwork: 1x1 2x1 2x2 2x4 4x1 4x2 8x1 10x1
    //  * a pig's diameter must fit the bay it stands in: 1-wide columns 3 apart
    //    leave a 2.0 bay (small pig), 8-wide spans leave 6.0 (any pig)
    //  * nothing is taller than 7 and no pig sits higher than 7.7, so the whole
    //    structure stays inside the camera with the HUD clear above it

    // ---------------------------------------------------------------- levels
    //
    // Geometry rules these layouts obey (checked by tools/LevelTest):
    //  * block sizes are limited to those with artwork: 1x1 2x1 2x2 2x4 4x1 4x2 8x1 10x1
    //  * a pig's diameter must fit the bay it stands in: 1-wide columns 3 apart
    //    leave a 2.0 bay (small pig), 8-wide spans leave 6.0 (any pig)
    //  * nothing is taller than 7 and no pig sits higher than 7.7, so the whole
    //    structure stays inside the camera with the HUD clear above it

    private static Level level1() {
        // Hut: 1x4 legs 3 apart leave a 2.0 bay, 4x1 lintel across them.
        Level lv = new Level(0, "GREEN HILLS", new int[]{Bird.RED, Bird.RED, Bird.RED}, 12000, 17000);
        lv.b(Block.WOOD, 19.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 22.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 19.5f, 4f, 4f, 1f);
        lv.p(21.5f, 0f, Pig.SMALL);
        lv.p(21.5f, 5f, Pig.MEDIUM);
        return lv;
    }

    private static Level level2() {
        // Two huts sharing one stone plinth.
        Level lv = new Level(1, "TIMBER YARD", new int[]{Bird.RED, Bird.YELLOW, Bird.RED}, 16000, 23000);
        lv.b(Block.STONE, 17.5f, 0f, 8f, 1f);
        lv.b(Block.WOOD, 17.5f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 20.5f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 17.5f, 5f, 4f, 1f);
        lv.b(Block.WOOD, 21.5f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 24.5f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 21.5f, 5f, 4f, 1f);
        lv.p(19.5f, 1f, Pig.SMALL);
        lv.p(23.5f, 1f, Pig.SMALL);
        lv.p(21.5f, 6f, Pig.SMALL);
        return lv;
    }

    private static Level level3() {
        // Glass hall: one 8-wide span, three pigs along the floor.
        Level lv = new Level(2, "ICE FORTRESS", new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK}, 18000, 25000);
        lv.b(Block.ICE, 18.5f, 0f, 1f, 4f);
        lv.b(Block.ICE, 25.5f, 0f, 1f, 4f);
        lv.b(Block.ICE, 18.5f, 4f, 8f, 1f);
        lv.p(20.5f, 0f, Pig.SMALL);
        lv.p(23.5f, 0f, Pig.SMALL);
        lv.p(22f, 5f, Pig.SMALL);
        return lv;
    }

    private static Level level4() {
        // Stone plinth carrying a wide timber span; big pig underneath.
        Level lv = new Level(3, "STONE TOWER",
                new int[]{Bird.YELLOW, Bird.BLACK, Bird.RED, Bird.WHITE}, 21000, 29000);
        lv.b(Block.STONE, 17.5f, 0f, 8f, 1f);
        lv.b(Block.WOOD, 17.5f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 24.5f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 17.5f, 5f, 8f, 1f);
        lv.p(21.5f, 1f, Pig.BIG);
        lv.p(21.5f, 6f, Pig.MEDIUM);
        return lv;
    }

    private static Level level5() {
        // Two stone plinths, one long deck, timber legs on top.
        Level lv = new Level(4, "SKY TOWER",
                new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK, Bird.RED}, 24000, 32000);
        lv.b(Block.STONE, 18.5f, 0f, 2f, 2f);
        lv.b(Block.STONE, 23.5f, 0f, 2f, 2f);
        lv.b(Block.WOOD, 17.5f, 2f, 10f, 1f);
        lv.b(Block.WOOD, 19.5f, 3f, 1f, 4f);
        lv.b(Block.WOOD, 23.5f, 3f, 1f, 4f);
        lv.p(22f, 0f, Pig.SMALL);
        lv.p(22f, 3f, Pig.SMALL);
        return lv;
    }

    private static Level level6() {
        // The king on his roof, flanked by a guard.
        Level lv = new Level(5, "THE KING",
                new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK, Bird.WHITE, Bird.RED}, 30000, 40000);
        lv.b(Block.STONE, 17.5f, 0f, 8f, 1f);
        lv.b(Block.WOOD, 17.5f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 24.5f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 17.5f, 5f, 8f, 1f);
        lv.p(21.5f, 1f, Pig.BIG);
        lv.ph(22f, 6f);
        lv.p(19f, 6f, Pig.SMALL);
        return lv;
    }

    private static Level level7() {
        // Two separate huts; you have to spend a bird on each.
        Level lv = new Level(6, "HILLSIDE HUT",
                new int[]{Bird.RED, Bird.RED, Bird.YELLOW}, 20000, 27000);
        lv.b(Block.WOOD, 18.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 21.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 18.5f, 4f, 4f, 1f);
        lv.b(Block.WOOD, 22.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 25.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 22.5f, 4f, 4f, 1f);
        lv.p(20.5f, 0f, Pig.SMALL);
        lv.p(24.5f, 0f, Pig.SMALL);
        lv.p(22f, 5f, Pig.SMALL);
        return lv;
    }

    private static Level level8() {
        // Stone hall with a helmeted pig on the roof.
        Level lv = new Level(7, "STONE YARD",
                new int[]{Bird.YELLOW, Bird.BLACK, Bird.RED}, 22000, 30000);
        lv.b(Block.STONE, 18.5f, 0f, 1f, 4f);
        lv.b(Block.STONE, 25.5f, 0f, 1f, 4f);
        lv.b(Block.STONE, 18.5f, 4f, 8f, 1f);
        lv.p(22f, 0f, Pig.BIG);
        lv.ph(22f, 5f);
        return lv;
    }

    private static Level level9() {
        // Glass hut next to a timber hut.
        Level lv = new Level(8, "ICE BRIDGE",
                new int[]{Bird.RED, Bird.YELLOW, Bird.RED}, 20000, 27000);
        lv.b(Block.ICE, 18.5f, 0f, 1f, 4f);
        lv.b(Block.ICE, 21.5f, 0f, 1f, 4f);
        lv.b(Block.ICE, 18.5f, 4f, 4f, 1f);
        lv.b(Block.WOOD, 22.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 25.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 22.5f, 4f, 4f, 1f);
        lv.p(20.5f, 0f, Pig.SMALL);
        lv.p(24.5f, 0f, Pig.SMALL);
        lv.p(20.5f, 5f, Pig.MEDIUM);
        return lv;
    }

    private static Level level10() {
        // Stone deck on two piers, timber legs above, big pig on the deck.
        Level lv = new Level(9, "STONE HALL",
                new int[]{Bird.YELLOW, Bird.BLACK, Bird.WHITE}, 24000, 32000);
        lv.b(Block.STONE, 17.5f, 0f, 2f, 2f);
        lv.b(Block.STONE, 24.5f, 0f, 2f, 2f);
        lv.b(Block.STONE, 17.5f, 2f, 10f, 1f);
        lv.b(Block.WOOD, 18.5f, 3f, 1f, 4f);
        lv.b(Block.WOOD, 24.5f, 3f, 1f, 4f);
        lv.p(22f, 0f, Pig.SMALL);
        lv.p(21.5f, 3f, Pig.BIG);
        return lv;
    }

    private static Level level11() {
        // Long hall split into two bays, glass legs on the roof.
        Level lv = new Level(10, "DOUBLE DECK",
                new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK, Bird.RED}, 26000, 34000);
        lv.b(Block.WOOD, 17.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 24.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 20.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 21.5f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 17.5f, 4f, 8f, 1f);
        lv.b(Block.ICE, 18.5f, 5f, 1f, 2f);
        lv.b(Block.ICE, 23.5f, 5f, 1f, 2f);
        lv.p(19.5f, 0f, Pig.SMALL);
        lv.p(23.5f, 0f, Pig.SMALL);
        lv.p(21.5f, 5f, Pig.MEDIUM);
        return lv;
    }

    private static Level level12() {
        // The finale: stone shell, one big pig and two helmeted guards.
        Level lv = new Level(11, "THE FORTRESS",
                new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK, Bird.WHITE, Bird.RED}, 34000, 46000);
        lv.b(Block.STONE, 17.5f, 0f, 8f, 1f);
        lv.b(Block.STONE, 17.5f, 1f, 1f, 4f);
        lv.b(Block.STONE, 24.5f, 1f, 1f, 4f);
        lv.b(Block.STONE, 17.5f, 5f, 8f, 1f);
        lv.p(21.5f, 1f, Pig.BIG);
        lv.ph(19f, 6f);
        lv.ph(24f, 6f);
        return lv;
    }
}
