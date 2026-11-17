"""Replace the level set in Level.java with twelve validated layouts.

Written as a script rather than many hand edits so the whole block is replaced
atomically and `all()` cannot drift out of sync with the factory methods.

Only block sizes that have sprites are used: 1x1, 2x1, 2x2, 2x4, 4x1, 4x2,
8x1, 10x1 (in either orientation).
"""
import io
import re
import sys

PATH = "src/com/nativebirds/game/Level.java"

LEVELS = r'''
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
        lv.b(Block.WOOD, 10f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 13f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 10f, 4f, 4f, 1f);
        lv.p(12f, 0f, Pig.SMALL);
        lv.p(12f, 5f, Pig.MEDIUM);
        return lv;
    }

    private static Level level2() {
        // Two huts sharing one stone plinth.
        Level lv = new Level(1, "TIMBER YARD", new int[]{Bird.RED, Bird.YELLOW, Bird.RED}, 16000, 23000);
        lv.b(Block.STONE, 8f, 0f, 8f, 1f);
        lv.b(Block.WOOD, 8f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 11f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 8f, 5f, 4f, 1f);
        lv.b(Block.WOOD, 12f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 15f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 12f, 5f, 4f, 1f);
        lv.p(10f, 1f, Pig.SMALL);
        lv.p(14f, 1f, Pig.SMALL);
        lv.p(12f, 6f, Pig.SMALL);
        return lv;
    }

    private static Level level3() {
        // Glass hall: one 8-wide span, three pigs along the floor.
        Level lv = new Level(2, "ICE FORTRESS", new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK}, 18000, 25000);
        lv.b(Block.ICE, 9f, 0f, 1f, 4f);
        lv.b(Block.ICE, 16f, 0f, 1f, 4f);
        lv.b(Block.ICE, 9f, 4f, 8f, 1f);
        lv.p(11f, 0f, Pig.SMALL);
        lv.p(14f, 0f, Pig.SMALL);
        lv.p(12.5f, 5f, Pig.SMALL);
        return lv;
    }

    private static Level level4() {
        // Stone plinth carrying a wide timber span; big pig underneath.
        Level lv = new Level(3, "STONE TOWER",
                new int[]{Bird.YELLOW, Bird.BLACK, Bird.RED, Bird.WHITE}, 21000, 29000);
        lv.b(Block.STONE, 8f, 0f, 8f, 1f);
        lv.b(Block.WOOD, 8f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 15f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 8f, 5f, 8f, 1f);
        lv.p(12f, 1f, Pig.BIG);
        lv.p(12f, 6f, Pig.MEDIUM);
        return lv;
    }

    private static Level level5() {
        // Two stone plinths, one long deck, timber legs on top.
        Level lv = new Level(4, "SKY TOWER",
                new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK, Bird.RED}, 24000, 32000);
        lv.b(Block.STONE, 9f, 0f, 2f, 2f);
        lv.b(Block.STONE, 14f, 0f, 2f, 2f);
        lv.b(Block.WOOD, 8f, 2f, 10f, 1f);
        lv.b(Block.WOOD, 10f, 3f, 1f, 4f);
        lv.b(Block.WOOD, 14f, 3f, 1f, 4f);
        lv.p(12.5f, 0f, Pig.SMALL);
        lv.p(12.5f, 3f, Pig.SMALL);
        return lv;
    }

    private static Level level6() {
        // The king on his roof, flanked by a guard.
        Level lv = new Level(5, "THE KING",
                new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK, Bird.WHITE, Bird.RED}, 30000, 40000);
        lv.b(Block.STONE, 8f, 0f, 8f, 1f);
        lv.b(Block.WOOD, 8f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 15f, 1f, 1f, 4f);
        lv.b(Block.WOOD, 8f, 5f, 8f, 1f);
        lv.p(12f, 1f, Pig.BIG);
        lv.ph(12.5f, 6f);
        lv.p(9.5f, 6f, Pig.SMALL);
        return lv;
    }

    private static Level level7() {
        // Two separate huts; you have to spend a bird on each.
        Level lv = new Level(6, "HILLSIDE HUT",
                new int[]{Bird.RED, Bird.RED, Bird.YELLOW}, 20000, 27000);
        lv.b(Block.WOOD, 9f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 12f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 9f, 4f, 4f, 1f);
        lv.b(Block.WOOD, 13f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 16f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 13f, 4f, 4f, 1f);
        lv.p(11f, 0f, Pig.SMALL);
        lv.p(15f, 0f, Pig.SMALL);
        lv.p(12.5f, 5f, Pig.SMALL);
        return lv;
    }

    private static Level level8() {
        // Stone hall with a helmeted pig on the roof.
        Level lv = new Level(7, "STONE YARD",
                new int[]{Bird.YELLOW, Bird.BLACK, Bird.RED}, 22000, 30000);
        lv.b(Block.STONE, 9f, 0f, 1f, 4f);
        lv.b(Block.STONE, 16f, 0f, 1f, 4f);
        lv.b(Block.STONE, 9f, 4f, 8f, 1f);
        lv.p(12.5f, 0f, Pig.BIG);
        lv.ph(12.5f, 5f);
        return lv;
    }

    private static Level level9() {
        // Glass hut next to a timber hut.
        Level lv = new Level(8, "ICE BRIDGE",
                new int[]{Bird.RED, Bird.YELLOW, Bird.RED}, 20000, 27000);
        lv.b(Block.ICE, 9f, 0f, 1f, 4f);
        lv.b(Block.ICE, 12f, 0f, 1f, 4f);
        lv.b(Block.ICE, 9f, 4f, 4f, 1f);
        lv.b(Block.WOOD, 13f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 16f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 13f, 4f, 4f, 1f);
        lv.p(11f, 0f, Pig.SMALL);
        lv.p(15f, 0f, Pig.SMALL);
        lv.p(11f, 5f, Pig.MEDIUM);
        return lv;
    }

    private static Level level10() {
        // Stone deck on two piers, timber legs above, big pig on the deck.
        Level lv = new Level(9, "STONE HALL",
                new int[]{Bird.YELLOW, Bird.BLACK, Bird.WHITE}, 24000, 32000);
        lv.b(Block.STONE, 8f, 0f, 2f, 2f);
        lv.b(Block.STONE, 15f, 0f, 2f, 2f);
        lv.b(Block.STONE, 8f, 2f, 10f, 1f);
        lv.b(Block.WOOD, 9f, 3f, 1f, 4f);
        lv.b(Block.WOOD, 15f, 3f, 1f, 4f);
        lv.p(12.5f, 0f, Pig.SMALL);
        lv.p(12f, 3f, Pig.BIG);
        return lv;
    }

    private static Level level11() {
        // Long hall split into two bays, glass legs on the roof.
        Level lv = new Level(10, "DOUBLE DECK",
                new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK, Bird.RED}, 26000, 34000);
        lv.b(Block.WOOD, 8f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 15f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 11f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 12f, 0f, 1f, 4f);
        lv.b(Block.WOOD, 8f, 4f, 8f, 1f);
        lv.b(Block.ICE, 9f, 5f, 1f, 2f);
        lv.b(Block.ICE, 14f, 5f, 1f, 2f);
        lv.p(10f, 0f, Pig.SMALL);
        lv.p(14f, 0f, Pig.SMALL);
        lv.p(12f, 5f, Pig.MEDIUM);
        return lv;
    }

    private static Level level12() {
        // The finale: stone shell, one big pig and two helmeted guards.
        Level lv = new Level(11, "THE FORTRESS",
                new int[]{Bird.RED, Bird.YELLOW, Bird.BLACK, Bird.WHITE, Bird.RED}, 34000, 46000);
        lv.b(Block.STONE, 8f, 0f, 8f, 1f);
        lv.b(Block.STONE, 8f, 1f, 1f, 4f);
        lv.b(Block.STONE, 15f, 1f, 1f, 4f);
        lv.b(Block.STONE, 8f, 5f, 8f, 1f);
        lv.p(12f, 1f, Pig.BIG);
        lv.ph(9.5f, 6f);
        lv.ph(14.5f, 6f);
        return lv;
    }
'''


# Distance from the slingshot to the structures. Real Angry Birds puts the fort
# roughly 18-25 units from the fork; the original 10 units made every shot a
# short lob. Applied to the generated text so the Java literals stay accurate.
SHIFT = 9.5


def _fmt(v):
    v = round(v, 1)
    return ("%d" % int(v)) if v == int(v) else ("%.1f" % v)


def shift(text):
    def b(m):
        return m.group(1) + _fmt(float(m.group(2)) + SHIFT) + "f,"
    text = re.sub(r"(lv\.b\(Block\.\w+,\s*)(-?\d+(?:\.\d+)?)f,", b, text)

    def q(m):
        return m.group(1) + _fmt(float(m.group(2)) + SHIFT) + "f,"
    text = re.sub(r"(lv\.p\()(-?\d+(?:\.\d+)?)f,", q, text)
    text = re.sub(r"(lv\.ph\()(-?\d+(?:\.\d+)?)f,", q, text)
    return text


def main():
    src = io.open(PATH, encoding="utf-8").read()

    start = src.index("    private static Level level1() {")
    end = src.rindex("}")                 # class closing brace
    new = src[:start] + shift(LEVELS.strip("\n")) + "\n}\n"

    body = "    public static List<Level> all() {\n        List<Level> l = new ArrayList<Level>();\n"
    for i in range(1, 13):
        body += "        l.add(level%d());\n" % i
    body += "        return l;\n    }"

    new = re.sub(r"    public static List<Level> all\(\) \{.*?\n    \}", body, new,
                 count=1, flags=re.S)

    io.open(PATH, "w", encoding="utf-8").write(new)
    print("wrote %d levels" % 12)


if __name__ == "__main__":
    main()
