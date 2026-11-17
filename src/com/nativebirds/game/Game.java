package com.nativebirds.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * All gameplay, in pure Java: no Android types. The renderer reads state from here
 * and the view feeds it touches, so a whole level can be simulated headlessly.
 */
public class Game {

    public static final int MENU = 0, LEVELS = 1, PLAY = 2, PAUSED = 3, WIN = 4, LOSE = 5;

    /** World units visible vertically; width follows the device aspect ratio. */
    public static final float VIEW_H = 17.0f;
    public static final float CAM_X = 14.0f;
    public static final float WORLD_W = 40f;
    public static final float SLING_X = 2.5f;
    /** Height of the whole slingshot sprite, ground to fork tips.
     *  The artwork's fork opening is 0.242 * height wide, so this must be tall
     *  enough that the opening clears a bird's diameter (1.24 units) with margin. */
    public static final float SLING_H = 7.0f;
    /** Pouch height: the bird hangs in the fork opening, above the crotch (4.04). */
    public static final float SLING_TOP = 5.35f;
    /** Sprite is centred on SLING_X, and its fork opens on that same axis. */
    public static final float ANCHOR_X = SLING_X;
    public static final float ANCHOR_Y = SLING_TOP;
    public static final float MAX_DRAG = 3.0f;
    /** Max pull (3 units) -> 19.8 u/s, ~17.8 units of range: enough to reach the far tower. */
    public static final float POWER = 7.8f;
    /**
     * Bird density. Radii were scaled by 0.62 to make the birds fit the slingshot
     * fork, so density is scaled by 1/0.62^2 to keep mass - and therefore the
     * momentum each bird delivers - exactly as before.
     */
    public static final float BIRD_DENSITY = 6.76f;

    public static final int SCORE_BLOCK = 500;
    public static final int SCORE_PIG = 5000;
    public static final int SCORE_BIRD = 10000;

    private final Sfx sfx;
    private final Store store;
    private final Random rnd = new Random(7);

    public final World world = new World();
    public final List<Block> blocks = new ArrayList<Block>();
    public final List<Pig> pigs = new ArrayList<Pig>();
    public final List<Bird> active = new ArrayList<Bird>();
    public final List<Bird> queue = new ArrayList<Bird>();
    public final List<Particle> particles = new ArrayList<Particle>();
    public final List<Body> eggs = new ArrayList<Body>();

    public final List<Level> levels = Level.all();
    /** Per-level progress, sized from the level list so adding levels cannot
     *  overflow these arrays. */
    public final int[] stars = new int[levels.size()];
    public final int[] best = new int[levels.size()];

    public int state = MENU;
    public int levelIndex;
    public Level level;
    public Bird current;
    public boolean dragging;
    public float pouchX = ANCHOR_X, pouchY = ANCHOR_Y;

    public int score;
    public float damageDealt;
    public int birdsLeft;
    public float time;
    public float winTimer, loseTimer, nextBirdDelay;
    public float shake;
    public boolean soundOn = true;
    public int starsEarned;

    // camera / viewport (landscape)
    public int screenW = 1520, screenH = 720;
    public float scale = 62.6f;
    public float camX = CAM_X, camY = 4.4f;
    private float camTargetY = 4.4f;

    private float sfxCooldown;

    public Game(Sfx sfx, Store store) {
        this.sfx = sfx;
        this.store = store;
        loadProgress();
    }

    // --------------------------------------------------------------- progress

    private void loadProgress() {
        for (int i = 0; i < stars.length; i++) {
            stars[i] = store.getInt("stars" + i, 0);
            best[i] = store.getInt("best" + i, 0);
        }
        soundOn = store.getInt("sound", 1) == 1;
    }

    public boolean isUnlocked(int i) {
        return i == 0 || stars[i - 1] > 0;
    }

    public int totalStars() {
        int s = 0;
        for (int v : stars) s += v;
        return s;
    }

    // ------------------------------------------------------------- level flow

    public void startLevel(int index) {
        levelIndex = Math.max(0, Math.min(levels.size() - 1, index));
        level = levels.get(levelIndex);
        world.clear();
        blocks.clear();
        pigs.clear();
        active.clear();
        queue.clear();
        particles.clear();
        eggs.clear();
        current = null;
        dragging = false;
        score = 0;
        damageDealt = 0f;
        starsEarned = 0;
        winTimer = loseTimer = 0f;
        nextBirdDelay = 0.6f;
        shake = 0f;
        pouchX = ANCHOR_X;
        pouchY = ANCHOR_Y;
        camY = camTargetY = baseCamY();

        Body ground = new Body().setBox(70f, 4f, 1f).set(7f, -2f, 0f);
        ground.setStatic();
        ground.friction = 0.9f;
        ground.restitution = 0f;
        world.add(ground);

        level.spawn(world, blocks, pigs);

        for (int t : level.birds) {
            Bird b = new Bird();
            b.type = t;
            b.radius = Bird.radiusFor(t);
            queue.add(b);
        }
        birdsLeft = queue.size();
        state = PLAY;
        loadNextBird();
        if (sfx != null && soundOn) sfx.play("start", 0.7f, 1f);
    }

    private void loadNextBird() {
        if (queue.isEmpty()) {
            return;
        }
        current = queue.remove(0);
        current.launched = false;
        current.abilityUsed = false;
        current.body = null;
        pouchX = ANCHOR_X;
        pouchY = ANCHOR_Y;
        birdsLeft = queue.size() + 1;
        if (sfx != null && soundOn) sfx.play("bird_select", 0.55f, 1f);
    }

    // ---------------------------------------------------------------- updates

    public void update(float dt) {
        time += dt;
        if (pressGlow > 0f) {
            pressGlow = Math.max(0f, pressGlow - dt / 0.22f);
            if (pressGlow == 0f) glowId = null;
        }
        if (state == PLAY || state == WIN || state == LOSE) {
            world.step(dt);
            processDamage();
            updateEggs(dt);
            updateEntities(dt);
            updateCamera(dt);
        }
        updateParticles(dt);
        if (shake > 0f) shake = Math.max(0f, shake - dt * 2.2f);
        if (sfxCooldown > 0f) sfxCooldown -= dt;
    }

    private void updateEntities(float dt) {
        for (Pig p : pigs) {
            if (!p.alive) continue;
            p.flash = Math.max(0f, p.flash - dt * 4f);
            p.blinkTimer -= dt;
            if (p.blinkTimer <= 0f) {
                p.blinking = !p.blinking;
                p.blinkTimer = p.blinking ? 0.12f : 1.6f + rnd.nextFloat() * 2.5f;
            }
        }
        for (Block b : blocks) {
            if (b.alive) b.flash = Math.max(0f, b.flash - dt * 5f);
        }

        for (int i = active.size() - 1; i >= 0; i--) {
            Bird b = active.get(i);
            b.airborneTime += dt;
            b.spin += dt * (b.body != null ? b.body.angularVelocity : 0f);

            if (b.fuse > 0f) {
                b.fuse -= dt;
                if (b.fuse <= 0f && b.body != null) {
                    explode(b.body.position.x, b.body.position.y, 5.2f, 13f, 380f);
                    b.exploding = true;
                    world.remove(b.body);
                    active.remove(i);
                    continue;
                }
            }

            if (b.body == null) {
                active.remove(i);
                continue;
            }
            boolean slow = b.body.speed() < 0.9f && b.airborneTime > 1.1f;
            boolean gone = b.body.position.x > WORLD_W + 5f || b.body.position.y < -6f
                    || b.body.position.x < -8f;
            if (slow || gone || b.airborneTime > 18f) {
                world.remove(b.body);
                active.remove(i);
                if (nextBirdDelay <= 0f) nextBirdDelay = 0.75f;
            }
        }

        // cull fallen debris
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Block b = blocks.get(i);
            if (b.alive && (b.body.position.y < -8f || b.body.position.x > WORLD_W + 8f)) {
                b.alive = false;
                world.remove(b.body);
                blocks.remove(i);
            }
        }
        for (int i = pigs.size() - 1; i >= 0; i--) {
            Pig p = pigs.get(i);
            if (p.alive && (p.body.position.y < -8f || p.body.position.x > WORLD_W + 8f)) {
                p.alive = false;
                world.remove(p.body);
            }
        }

        if (state == PLAY) {
            if (current == null && active.isEmpty()) {
                if (!queue.isEmpty()) {
                    nextBirdDelay -= dt;
                    if (nextBirdDelay <= 0f) loadNextBird();
                }
            }
            if (allPigsDead()) {
                winTimer += dt;
                if (winTimer > 0.9f) finishWin();
            } else {
                winTimer = 0f;
            }

            boolean noBirds = queue.isEmpty() && current == null && active.isEmpty();
            if (!noBirds) {
                loseTimer = 0f;
            } else if (world.isSettled(0.03f)) {
                loseTimer += dt;
                if (loseTimer > 1.8f) finishLose();
            }
        }
    }

    private void updateEggs(float dt) {
        for (int i = eggs.size() - 1; i >= 0; i--) {
            Body e = eggs.get(i);
            float dv = e.mass > 0 ? e.impact / e.mass : 0f;
            if (dv > 2.5f || e.position.y < 0.8f || e.dead) {
                if (!e.dead) explode(e.position.x, e.position.y, 3.0f, 7f, 200f);
                world.remove(e);
                eggs.remove(i);
            }
        }
    }

    private void updateCamera(float dt) {
        float halfH = screenH / (2f * scale);
        float base = baseCamY();
        // Only a bird in flight can push the camera up. The pouch never can: it is
        // always framed, so following it made the whole world jump on touch-down.
        float focus = 0f;
        for (Bird b : active) {
            if (b.body != null) focus = Math.max(focus, b.body.position.y);
        }
        float target = base;
        if (focus > base + halfH * 0.80f) {
            target = focus - halfH * 0.55f;
        }
        camTargetY = target;
        camY += (camTargetY - camY) * Math.min(1f, dt * 4.5f);
        camX = CAM_X;
    }

    private float baseCamY() {
        // Ground sits ~24% up from the bottom of the screen: enough soil to plant the
        // slingshot and towers in, without wasting the frame on empty sky.
        return screenH / (2f * scale) - VIEW_H * 0.21f;
    }

    public void setViewport(int w, int h) {
        screenW = w;
        screenH = h;
        scale = h / VIEW_H;
        camX = CAM_X;
        if (camY <= 0f) camY = baseCamY();
    }

    public float toScreenX(float wx) {
        return (wx - camX) * scale + screenW * 0.5f;
    }

    public float toScreenY(float wy) {
        return screenH * 0.5f - (wy - camY) * scale;
    }

    public float toWorldX(float sx) {
        return (sx - screenW * 0.5f) / scale + camX;
    }

    public float toWorldY(float sy) {
        return camY - (sy - screenH * 0.5f) / scale;
    }

    // ------------------------------------------------------------ damage model

    private void processDamage() {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Block b = blocks.get(i);
            if (!b.alive) continue;
            float dv = b.body.mass > 0 ? b.body.impact / b.body.mass : 0f;
            if (dv > 2.2f) {
                float factor = b.material == Block.ICE ? 15f
                        : b.material == Block.WOOD ? 9f : 7f;
                float dmg = (dv - 2.2f) * factor;
                if (dmg > 3f) {
                    damageBlock(b, dmg, false);
                }
            }
        }
        for (int i = pigs.size() - 1; i >= 0; i--) {
            Pig p = pigs.get(i);
            if (!p.alive) continue;
            float dv = p.body.mass > 0 ? p.body.impact / p.body.mass : 0f;
            if (dv > 1.6f) {
                float dmg = (dv - 1.6f) * 14f;
                if (dmg > 2f) damagePig(p, dmg, false);
            }
        }
    }

    private void damageBlock(Block b, float dmg, boolean silent) {
        if (!b.alive) return;
        b.hp -= dmg;
        damageDealt += dmg;
        b.flash = Math.min(1f, b.flash + dmg / 60f);
        if (b.hp <= 0f) {
            destroyBlock(b);
        } else if (!silent && sfxCooldown <= 0f) {
            sfxCooldown = 0.045f;
            if (sfx != null && soundOn) {
                String s = b.material == Block.WOOD ? "wood_hit"
                        : b.material == Block.STONE ? "stone_hit" : "ice_hit";
                sfx.play(s, 0.42f, 0.9f + rnd.nextFloat() * 0.3f);
            }
        }
    }

    private void destroyBlock(Block b) {
        if (!b.alive) return;
        b.alive = false;
        world.remove(b.body);
        blocks.remove(b);
        score += SCORE_BLOCK;
        addScorePop(b.body.position.x, b.body.position.y, SCORE_BLOCK);
        spawnDebris(b);
        if (sfx != null && soundOn && sfxCooldown <= 0f) {
            sfxCooldown = 0.03f;
            String s = b.material == Block.WOOD ? "wood_break"
                    : b.material == Block.STONE ? "stone_break" : "ice_break";
            sfx.play(s, 0.7f, 0.9f + rnd.nextFloat() * 0.25f);
        }
    }

    private void damagePig(Pig p, float dmg, boolean silent) {
        if (!p.alive) return;
        p.hp -= dmg;
        damageDealt += dmg;
        p.flash = Math.min(1f, p.flash + dmg / 40f);
        if (p.hp <= 0f) {
            killPig(p);
        } else if (!silent && sfxCooldown <= 0f) {
            sfxCooldown = 0.06f;
            if (sfx != null && soundOn) {
                sfx.play(rnd.nextBoolean() ? "pig_hit" : "pig_damage", 0.5f,
                        0.9f + rnd.nextFloat() * 0.25f);
            }
        }
    }

    private void killPig(Pig p) {
        if (!p.alive) return;
        p.alive = false;
        world.remove(p.body);
        score += SCORE_PIG;
        addScorePop(p.body.position.x, p.body.position.y, SCORE_PIG);
        for (int i = 0; i < 12; i++) {
            Particle pt = new Particle();
            pt.type = Particle.DUST;
            pt.x = p.body.position.x;
            pt.y = p.body.position.y;
            float a = rnd.nextFloat() * 6.283f;
            float sp = 2f + rnd.nextFloat() * 5f;
            pt.vx = (float) Math.cos(a) * sp;
            pt.vy = (float) Math.sin(a) * sp;
            pt.life = pt.maxLife = 0.55f + rnd.nextFloat() * 0.3f;
            pt.size = 0.18f + rnd.nextFloat() * 0.22f;
            pt.color = 0xFF8ED44A;
            particles.add(pt);
        }
        if (sfx != null && soundOn) sfx.play("pig_pop", 0.75f, 0.95f + rnd.nextFloat() * 0.2f);
    }

    public void explode(float x, float y, float radius, float strength, float damage) {
        world.applyRadialImpulse(x, y, radius, strength);
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Block b = blocks.get(i);
            if (!b.alive) continue;
            float d = dist(x, y, b.body.position.x, b.body.position.y);
            if (d < radius) damageBlock(b, damage * (1f - d / radius), true);
        }
        for (int i = pigs.size() - 1; i >= 0; i--) {
            Pig p = pigs.get(i);
            if (!p.alive) continue;
            float d = dist(x, y, p.body.position.x, p.body.position.y);
            if (d < radius) damagePig(p, damage * 1.15f * (1f - d / radius), true);
        }
        for (int i = 0; i < 26; i++) {
            Particle pt = new Particle();
            pt.type = Particle.DUST;
            pt.x = x;
            pt.y = y;
            float a = rnd.nextFloat() * 6.283f;
            float sp = 3f + rnd.nextFloat() * 11f;
            pt.vx = (float) Math.cos(a) * sp;
            pt.vy = (float) Math.sin(a) * sp;
            pt.life = pt.maxLife = 0.35f + rnd.nextFloat() * 0.5f;
            pt.size = 0.2f + rnd.nextFloat() * 0.45f;
            pt.color = rnd.nextBoolean() ? 0xFFFFD54A : 0xFFFF8A3D;
            particles.add(pt);
        }
        shake = Math.min(1.2f, shake + 0.85f);
        if (sfx != null && soundOn) sfx.play("black_bomb", 1f, 1f);
    }

    private void spawnDebris(Block b) {
        int color = b.material == Block.WOOD ? 0xFFC08840
                : b.material == Block.STONE ? 0xFF9BA3AB : 0xFFB6E8F7;
        int n = 8 + (int) (b.area() * 2);
        for (int i = 0; i < n; i++) {
            Particle pt = new Particle();
            pt.type = Particle.DEBRIS;
            pt.x = b.body.position.x + (rnd.nextFloat() - 0.5f) * b.w;
            pt.y = b.body.position.y + (rnd.nextFloat() - 0.5f) * b.h;
            pt.vx = b.body.velocity.x * 0.35f + (rnd.nextFloat() - 0.5f) * 7f;
            pt.vy = b.body.velocity.y * 0.35f + rnd.nextFloat() * 6f + 1f;
            pt.life = pt.maxLife = 0.6f + rnd.nextFloat() * 0.6f;
            pt.size = 0.1f + rnd.nextFloat() * 0.16f;
            pt.rot = rnd.nextFloat() * 6.28f;
            pt.spin = (rnd.nextFloat() - 0.5f) * 14f;
            pt.color = color;
            particles.add(pt);
        }
    }

    private void addScorePop(float x, float y, int amount) {
        Particle pt = new Particle();
        pt.type = Particle.TEXT;
        pt.x = x;
        pt.y = y + 0.6f;
        pt.vy = 2.4f;
        pt.life = pt.maxLife = 1.1f;
        pt.text = "+" + amount;
        pt.color = 0xFFFFFFFF;
        particles.add(pt);
    }

    private void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                particles.remove(i);
                continue;
            }
            if (p.type == Particle.TEXT) {
                p.y += p.vy * dt;
                p.vy *= 0.96f;
            } else {
                p.vy -= 20f * dt;
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                p.rot += p.spin * dt;
                if (p.y < 0.05f && p.vy < 0f) {
                    p.y = 0.05f;
                    p.vy *= -0.32f;
                    p.vx *= 0.72f;
                }
            }
        }
    }

    // ---------------------------------------------------------------- abilities

    public void useAbility(Bird b) {
        if (b.abilityUsed || b.body == null) return;
        b.abilityUsed = true;
        switch (b.type) {
            case Bird.YELLOW: {
                float sp = b.body.velocity.length();
                float target = Math.min(36f, Math.max(sp * 2.1f, 18f));
                if (sp > 0.01f) b.body.velocity.mul(target / sp);
                b.body.velocity.y += 1.5f;
                if (sfx != null && soundOn) sfx.play("yellow_dash", 0.9f, 1f);
                break;
            }
            case Bird.BLACK:
                b.fuse = 0.16f;
                if (sfx != null && soundOn) sfx.play("black_bomb", 0.8f, 1.1f);
                break;
            case Bird.BLUE:
                splitBlue(b);
                if (sfx != null && soundOn) sfx.play("blue_split", 0.9f, 1f);
                break;
            case Bird.WHITE:
                dropEgg(b);
                if (sfx != null && soundOn) sfx.play("white_egg", 0.9f, 1f);
                break;
            default:
                if (sfx != null && soundOn) sfx.play("bird_hit", 0.4f, 1.25f);
                break;
        }
    }

    private void splitBlue(Bird parent) {
        float sp = parent.body.velocity.length();
        float ang = (float) Math.atan2(parent.body.velocity.y, parent.body.velocity.x);
        for (int i = -1; i <= 1; i += 2) {
            Bird c = new Bird();
            c.type = Bird.BLUE;
            c.radius = 0.55f;
            c.launched = true;
            c.abilityUsed = true;
            c.splitChild = true;
            Body body = new Body().setCircle(0.55f, 2.6f);
            body.set(parent.body.position.x, parent.body.position.y + i * 0.55f, 0f);
            float a = ang + i * 0.30f;
            body.velocity.set((float) Math.cos(a) * sp, (float) Math.sin(a) * sp);
            body.restitution = 0.3f;
            body.friction = 0.5f;
            body.linearDamping = 0.01f;
            body.userData = c;
            c.body = body;
            world.add(body);
            active.add(c);
        }
    }

    private void dropEgg(Bird b) {
        Body egg = new Body().setCircle(0.5f, 3.2f);
        egg.set(b.body.position.x, b.body.position.y - b.radius - 0.5f, 0f);
        egg.velocity.set(b.body.velocity.x * 0.25f, -13f);
        egg.restitution = 0.05f;
        egg.userData = "egg";
        world.add(egg);
        eggs.add(egg);
        b.body.velocity.y += 8.5f;
        b.body.velocity.x *= 0.45f;
    }

    // ------------------------------------------------------------------ input

    public void touchDown(float wx, float wy) {
        if (state != PLAY) return;
        if (current != null && !current.launched) {
            float dx = wx - pouchX, dy = wy - pouchY;
            if (dx * dx + dy * dy < 7f) {
                dragging = true;
                moveDrag(wx, wy);
                return;
            }
        }
        Bird b = active.isEmpty() ? null : active.get(active.size() - 1);
        if (b != null && !b.abilityUsed && b.launched) {
            useAbility(b);
        }
    }

    public void touchMove(float wx, float wy) {
        if (state != PLAY || !dragging) return;
        moveDrag(wx, wy);
    }

    private void moveDrag(float wx, float wy) {
        float dx = wx - ANCHOR_X;
        float dy = wy - ANCHOR_Y;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > MAX_DRAG) {
            dx *= MAX_DRAG / d;
            dy *= MAX_DRAG / d;
        }
        if (dx > 0.45f) dx = 0.45f;
        pouchX = ANCHOR_X + dx;
        pouchY = ANCHOR_Y + dy;
        if (pouchY < 0.6f) pouchY = 0.6f;
    }

    public void touchUp(float wx, float wy) {
        if (state != PLAY) return;
        if (!dragging) return;
        dragging = false;
        float dx = ANCHOR_X - pouchX;
        float dy = ANCHOR_Y - pouchY;
        float pull = (float) Math.sqrt(dx * dx + dy * dy);
        if (pull < 0.55f || current == null) {
            pouchX = ANCHOR_X;
            pouchY = ANCHOR_Y;
            return;
        }
        launch(dx, dy);
    }

    private void launch(float dx, float dy) {
        Bird b = current;
        current = null;
        b.launched = true;
        b.airborneTime = 0f;
        b.abilityUsed = false;

        Body body = new Body().setCircle(b.radius, BIRD_DENSITY);
        body.set(pouchX, pouchY, 0f);
        body.velocity.set(dx * POWER, dy * POWER);
        body.restitution = 0.32f;
        body.friction = 0.55f;
        body.linearDamping = 0.012f;
        body.angularDamping = 0.5f;
        body.userData = b;
        b.body = body;
        world.add(body);
        active.add(b);

        birdsLeft = queue.size();
        pouchX = ANCHOR_X;
        pouchY = ANCHOR_Y;
        if (sfx != null && soundOn) sfx.play("bird_launch", 0.9f, 1f);
    }

    public float launchSpeed() {
        float dx = ANCHOR_X - pouchX, dy = ANCHOR_Y - pouchY;
        return (float) Math.sqrt(dx * dx + dy * dy) * POWER;
    }

    // -------------------------------------------------------------- ui / state

    public static class Rect {
        public final float l, t, r, b;

        public Rect(float l, float t, float r, float b) {
            this.l = l;
            this.t = t;
            this.r = r;
            this.b = b;
        }

        public boolean contains(float x, float y) {
            return x >= l && x <= r && y >= t && y <= b;
        }
    }

    public static class UiButton {
        public final String id, label;
        public final Rect rect;
        public final int style;

        public UiButton(String id, String label, Rect rect, int style) {
            this.id = id;
            this.label = label;
            this.rect = rect;
            this.style = style;
        }
    }

    private final List<UiButton> ui = new ArrayList<UiButton>();

    /** Button currently held down, or null. Drives the pressed visual and lets the
     *  player slide off to cancel before releasing. */
    public String pressedId;
    /** 0..1 highlight that decays after a press, so a tap still reads as a tap. */
    public float pressGlow;
    private String glowId;

    /** Button under a normalised point, or null. */
    public String uiHitAt(float nx, float ny) {
        for (UiButton b : uiButtons()) {
            if (b.rect.contains(nx, ny)) return b.id;
        }
        return null;
    }

    /** Called on touch-down over a button. */
    public void pressUi(String id) {
        pressedId = id;
    }

    /** Called when the finger lifts. Fires the action only if it is still on the
     *  same button, which is the behaviour people expect from a button. */
    public void releaseUi(float nx, float ny) {
        String under = uiHitAt(nx, ny);
        if (pressedId != null && pressedId.equals(under)) {
            glowId = pressedId;
            pressGlow = 1f;
            pressedId = null;
            action(under);
            return;
        }
        pressedId = null;
    }

    public void cancelUiPress() {
        pressedId = null;
    }

    /** Id of the button that should render as pressed this frame. */
    public String activePressId() {
        return pressedId;
    }

    /** Id of the button still showing its release flash, or null. */
    public String glowPressId() {
        return pressGlow > 0f ? glowId : null;
    }

    public List<UiButton> uiButtons() {
        ui.clear();
        switch (state) {
            case MENU:
                // One height and one left edge for both, aligned with the title.
                ui.add(new UiButton("play", "PLAY", new Rect(0.085f, 0.600f, 0.420f, 0.715f), 0));
                ui.add(new UiButton("sound", soundOn ? "SOUND ON" : "SOUND OFF",
                        new Rect(0.085f, 0.740f, 0.420f, 0.855f), 1));
                break;
            case LEVELS:
                ui.add(new UiButton("menu", "BACK", new Rect(0.045f, 0.045f, 0.145f, 0.160f), 1));
                for (int i = 0; i < levels.size(); i++) {
                    int col = i % 4, row = i / 4;
                    float l = 0.10f + col * 0.21f;
                    float t = 0.285f + row * 0.225f;
                    ui.add(new UiButton("level" + i, String.valueOf(i + 1),
                            new Rect(l, t, l + 0.18f, t + 0.185f), 2));
                }
                break;
            case PLAY:
                // 158x81 px on a 720p screen = 94x48 dp, a full-size touch target.
                ui.add(new UiButton("pause", "", new Rect(0.868f, 0.028f, 0.972f, 0.140f), 3));
                break;
            case PAUSED:
                ui.add(new UiButton("resume", "RESUME", new Rect(0.350f, 0.360f, 0.650f, 0.475f), 0));
                ui.add(new UiButton("restart", "RESTART", new Rect(0.350f, 0.495f, 0.650f, 0.610f), 1));
                ui.add(new UiButton("menu", "MENU", new Rect(0.350f, 0.630f, 0.650f, 0.745f), 1));
                break;
            case WIN: {
                boolean last = levelIndex >= levels.size() - 1;
                ui.add(new UiButton(last ? "menu" : "next", last ? "MENU" : "NEXT",
                        new Rect(0.340f, 0.590f, 0.660f, 0.705f), 0));
                ui.add(new UiButton("restart", "REPLAY", new Rect(0.340f, 0.715f, 0.660f, 0.830f), 1));
                ui.add(new UiButton("menu", "LEVELS", new Rect(0.340f, 0.840f, 0.660f, 0.955f), 1));
                break;
            }
            case LOSE:
                ui.add(new UiButton("restart", "TRY AGAIN", new Rect(0.350f, 0.575f, 0.650f, 0.690f), 0));
                ui.add(new UiButton("menu", "LEVELS", new Rect(0.350f, 0.705f, 0.650f, 0.820f), 1));
                break;
            default:
                break;
        }
        return ui;
    }

    /** Returns true when the tap was consumed by a button. */
    public boolean handleTap(float nx, float ny) {
        for (UiButton b : uiButtons()) {
            if (b.rect.contains(nx, ny)) {
                action(b.id);
                return true;
            }
        }
        return false;
    }

    public void action(String id) {
        if (sfx != null && soundOn) sfx.play("bird_select", 0.3f, 1.35f);
        if (id.equals("play")) {
            state = LEVELS;
        } else if (id.equals("menu")) {
            state = MENU;
        } else if (id.equals("pause")) {
            state = PAUSED;
        } else if (id.equals("resume")) {
            state = PLAY;
        } else if (id.equals("restart")) {
            startLevel(levelIndex);
        } else if (id.equals("next")) {
            startLevel(levelIndex + 1);
        } else if (id.equals("sound")) {
            soundOn = !soundOn;
            store.putInt("sound", soundOn ? 1 : 0);
        } else if (id.startsWith("level")) {
            int i = Integer.parseInt(id.substring(5));
            if (isUnlocked(i)) startLevel(i);
        }
    }

    // ------------------------------------------------------------ win / lose

    public boolean allPigsDead() {
        for (Pig p : pigs) if (p.alive) return false;
        return true;
    }

    private void finishWin() {
        score += (queue.size() + (current != null ? 1 : 0)) * SCORE_BIRD;
        starsEarned = score >= level.star3 ? 3 : score >= level.star2 ? 2 : 1;
        if (score > best[levelIndex]) {
            best[levelIndex] = score;
            store.putInt("best" + levelIndex, score);
        }
        if (starsEarned > stars[levelIndex]) {
            stars[levelIndex] = starsEarned;
            store.putInt("stars" + levelIndex, starsEarned);
        }
        state = WIN;
        if (sfx != null && soundOn) sfx.play("win", 1f, 1f);
    }

    private void finishLose() {
        state = LOSE;
        if (sfx != null && soundOn) sfx.play("lose", 1f, 1f);
    }

    public void retry() {
        startLevel(levelIndex);
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // -------------------------------------------------------------- particles

    public static class Particle {
        public static final int DEBRIS = 0, DUST = 1, TEXT = 2;
        public int type = DEBRIS;
        public float x, y, vx, vy, life, maxLife, size, rot, spin;
        public int color = 0xFFFFFFFF;
        public String text;
    }
}
