package com.nativebirds.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.List;
import java.util.Random;

/** Draws the whole game with Canvas 2D. All geometry is derived from Game's camera. */
public class Renderer {

    private final SpriteBank sprites;
    private final Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dst = new RectF();
    private final RectF tmp = new RectF();
    private final Path path = new Path();
    private final Random rnd = new Random(3);

    private final Typeface heavy = Typeface.create("sans-serif-black", Typeface.BOLD);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);

    /** Shared ground tones so hills, grass and slab never disagree at a seam. */
    private static final int GRASS_TOP = 0xFF93D45A;

    /** Slingshot geometry derived from sling.png (80x194), used unsquashed so the
     *  drawn prong tips land exactly where the artwork has them: the fork opening
     *  is then 1.66 units wide, clearing the 1.24-unit bird with a 0.2 margin a side. */
    private static final float SLING_W = Game.SLING_H * 80f / 194f;
    private static final float PRONG_L = Game.SLING_X + (16f - 40f) / 80f * SLING_W;
    private static final float PRONG_R = Game.SLING_X + (62f - 40f) / 80f * SLING_W;
    private static final float PRONG_L_Y = Game.SLING_H * (1f - 5f / 194f);
    private static final float PRONG_R_Y = Game.SLING_H;

    private float W, H;
    private Game g;
    private float t;

    public Renderer(SpriteBank sprites) {
        this.sprites = sprites;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        txt.setTextAlign(Paint.Align.CENTER);
    }

    public void draw(Canvas c, Game game, float time) {
        g = game;
        W = game.screenW;
        H = game.screenH;
        t = time;

        drawSky(c);
        drawClouds(c);
        drawHills(c);

        c.save();
        if (g.shake > 0.001f) {
            float m = g.shake * 14f;
            c.translate((rnd.nextFloat() - 0.5f) * m, (rnd.nextFloat() - 0.5f) * m);
        }
        drawGround(c);
        drawShadows(c);

        boolean inLevel = g.state != Game.MENU && g.state != Game.LEVELS;
        if (inLevel) {
            drawSlingBack(c);
            // Bands and pouch sit behind the bird and both end at its underside, so
            // the visible elastic hugs the silhouette instead of crossing the face.
            drawBands(c);
            drawPouch(c);
        }
        drawBlocks(c);
        drawPigs(c);
        drawBirds(c);
        drawTrajectory(c);
        drawParticles(c);
        c.restore();

        drawVignette(c);
        drawHud(c);
        drawUi(c);
    }

    // ------------------------------------------------------------------ world

    private void drawSky(Canvas c) {
        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0, 0, 0, H,
                new int[]{0xFF1E8FCC, 0xFF54B8E0, 0xFFA8DFF2, 0xFFD9F1FA},
                new float[]{0f, 0.42f, 0.72f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, fill);
        fill.setShader(null);

        // sun: a tight glowing core rather than a flat disc
        float sx = W * 0.87f, sy = H * 0.12f, r = W * 0.105f;
        fill.setAlpha(255);
        fill.setShader(new RadialGradient(sx, sy, r,
                new int[]{0x59FFF6CC, 0x24FFEDAE, 0x00FFF0B0},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(sx, sy, r, fill);
        fill.setShader(new RadialGradient(sx, sy, r * 0.44f,
                new int[]{0xFFFFFDF2, 0xE6FFF4C6, 0x00FFF4C6},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(sx, sy, r * 0.44f, fill);
        fill.setShader(null);
    }

    private void drawClouds(Canvas c) {
        float drift = t * 3.2f;
        float py = -(g.camY - baseCamY()) * g.scale * 0.18f;
        cloud(c, "cloud1", 0.10f * W + drift, 0.17f * H + py, 0.30f * W, 1f);
        cloud(c, "cloud2", 0.48f * W + drift * 0.7f, 0.30f * H + py, 0.24f * W, 0.96f);
        cloud(c, "cloud3", 0.71f * W + drift * 1.2f, 0.09f * H + py, 0.20f * W, 1f);
        cloud(c, "cloud2", 0.22f * W + drift * 0.5f, 0.42f * H + py, 0.17f * W, 0.70f);
    }

    private void cloud(Canvas c, String name, float x, float y, float w, float alpha) {
        Bitmap b = sprites.get(name);
        if (b.getWidth() < 4) return;
        x = wrap(x, W + 2f * w) - w;
        float h = w * b.getHeight() / b.getWidth();
        bmp.setAlpha((int) (alpha * 235));
        dst.set(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
        c.drawBitmap(b, null, dst, bmp);
        bmp.setAlpha(255);
    }

    private static float wrap(float v, float span) {
        float m = v % span;
        return m < 0 ? m + span : m;
    }

    private float baseCamY() {
        return H / (2f * g.scale) - Game.VIEW_H * 0.24f;
    }

    private void drawHills(Canvas c) {
        float gy = g.toScreenY(0f);
        float u = g.scale;

        // Three ridges, furthest first, each closer in tone and parallax than the last.
        // The palest one is blue-shifted so it dissolves into the sky, and the nearest
        // ends on GRASS_TOP so nothing shows a seam where the hills meet the field.

        // far: hazy, almost part of the sky
        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0, gy - 4.6f * u, 0, gy,
                new int[]{0xFFCDE8DF, 0xFFBCE2C6, 0xFFAEDCAC},
                new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
        hillRow(c, gy, u, 0.13f, 0.0f, 3.8f, 6.4f);

        // mid
        fill.setShader(new LinearGradient(0, gy - 3.1f * u, 0, gy,
                new int[]{0xFFB6E4B4, 0xFFA8DB97, 0xFF9CD682},
                new float[]{0f, 0.54f, 1f}, Shader.TileMode.CLAMP));
        hillRow(c, gy, u, 0.40f, 1.4f, 2.8f, 4.9f);

        // near: shares the field's colour so the join is invisible
        fill.setShader(new LinearGradient(0, gy - 1.9f * u, 0, gy,
                new int[]{0xFF9FD97C, 0xFF98D571, GRASS_TOP},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        hillRow(c, gy, u, 0.70f, 3.2f, 2.0f, 3.5f);
        fill.setShader(null);

        // Hedge on the ground line: breaks up the meeting point between the parallax
        // hills and the static field, and gives the empty middle some depth.
        bushRow(c, gy, u);
    }

    /** Shrub band straddling the ground line. Two staggered rows of irregularly sized
     *  bushes so it reads as a treeline, not a scalloped border. */
    private void bushRow(Canvas c, float gy, float u) {
        // far row: smaller, paler, sits a touch higher
        shrubRow(c, gy, u, 0xB9, 0.70f, 0.30f, 0.62f, 0xFF8FCE6A, 0xFFA3DA82);
        // near row: bigger, darker, so the two rows overlap into a hedge
        shrubRow(c, gy, u, 0x2D, 0.86f, 0.40f, 1.00f, 0xFF4E8F27, 0xFF6FBA3C);
    }

    private void shrubRow(Canvas c, float gy, float u, int seed, float parallax,
                          float minR, float maxR, int dark, int lit) {
        float off = (7f - g.camX) * u * parallax;
        float step = (minR + maxR) * 0.72f * u;
        int n = (int) (W / step) + 4;
        for (int i = -1; i < n; i++) {
            float jag = hash(i + seed, 21) - 0.5f;
            float cx = i * step + jag * step * 0.9f + off;
            float r = (minR + hash(i + seed, 22) * (maxR - minR)) * u;
            float lift = (hash(i + seed, 23) - 0.5f) * 0.16f * u;

            fill.setColor(dark);
            c.drawOval(new RectF(cx - r, gy + lift - r * 0.94f,
                    cx + r, gy + lift + r * 0.36f), fill);
            fill.setColor(lit);
            c.drawOval(new RectF(cx - r * 0.80f, gy + lift - r * 1.04f,
                    cx + r * 0.56f, gy + lift + r * 0.10f), fill);
        }
    }

    private void hillRow(Canvas c, float gy, float u, float alpha, float phase,
                         float minR, float maxR) {
        float off = (7f - g.camX) * u * alpha;
        float step = maxR * u * 1.15f;
        int n = (int) (W / step) + 3;
        for (int i = -1; i < n; i++) {
            float cx = i * step + phase * u + off;
            float rx = (minR + ((i * 37) % 10) / 10f * (maxR - minR)) * u;
            float ry = rx * 0.62f;
            c.drawOval(new RectF(cx - rx, gy - ry, cx + rx, gy + ry * 0.35f), fill);
        }
    }

    private void drawGround(Canvas c) {
        float gy = g.toScreenY(0f);
        float u = g.scale;

        // Earth first, filling everything below the grass; the grass then overlaps it.
        // Its first colour is the grass slab's last colour, darkened slightly, so the
        // join is a tonal shift rather than a hard brown line across the frame.
        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0, gy, 0, gy + 3.4f * u,
                // 1.62u of grass covers this rect, i.e. 0.476 of its height: the stop
                // sits exactly there and repeats the grass slab's last colour, so the
                // two surfaces are continuous and only then fade to earth.
                new int[]{0xFF5D9F2D, 0xFF5D9F2D, 0xFF4C3614, 0xFF3B2209},
                new float[]{0f, 0.476f, 0.63f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, gy, W, H, fill);
        fill.setShader(null);

        // Grass slab: its first row is GRASS_TOP so it continues the hills exactly,
        // and it is deep enough that the earth below reads as a thin edge rather
        // than a brown plinth across the bottom of the screen. The gradient stays
        // shallow - a strong ramp made the field look airbrushed.
        float gh = 1.62f * u;
        fill.setShader(new LinearGradient(0, gy, 0, gy + gh,
                new int[]{GRASS_TOP, 0xFF85C948, 0xFF72B83A, 0xFF5D9F2D},
                new float[]{0f, 0.36f, 0.70f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, gy, W, gy + gh, fill);
        fill.setShader(null);

        // No drawn shadow at the grass/earth join: the gradients already meet on the
        // same colour, and any hard band here shows up as a seam right across the
        // frame (a 44-level row jump, by measurement).

        // mottling across the slab: many small, low-alpha patches rather than a few big
        // soft ovals, which was reading as a blurry green smear
        for (int i = 0; i < 340; i++) {
            float x = hash(i, 7) * W;
            float y = gy + 0.14f * u + hash(i, 8) * (gh - 0.24f * u);
            float rx = (0.035f + hash(i, 9) * 0.13f) * u;
            float ry = rx * 0.46f;
            boolean dark = (i & 1) == 0;
            fill.setColor(dark ? 0x0E1F5A0E : 0x10D6F5A0);
            c.drawOval(new RectF(x - rx, y - ry, x + rx, y + ry), fill);
        }

        // scattered tufts over the whole field, denser toward the camera, so the
        // surface has grain instead of being one flat wash of green. Kept wide and
        // short: hair-thin vertical slivers read as rain streaks, not grass.
        for (int i = 0; i < 240; i++) {
            float x = hash(i, 31) * W;
            float d = hash(i, 32);
            float y = gy + (0.20f + d * d * (gh - 0.30f * u));
            float tall = (0.045f + hash(i, 33) * 0.085f) * (0.55f + d * 0.7f) * u;
            float half = (0.035f + hash(i, 34) * 0.045f) * u;
            float lean = (hash(i, 35) - 0.5f) * 0.16f * u;
            int shade = i % 3;
            fill.setColor(shade == 0 ? 0x333F7A16 : shade == 1 ? 0x2AA8DE72 : 0x226BA82C);
            path.reset();
            path.moveTo(x - half, y + 0.04f * u);
            path.lineTo(x + lean, y - tall);
            path.lineTo(x + half, y + 0.04f * u);
            path.close();
            c.drawPath(path, fill);
        }

        // clods and pebbles in the exposed earth, with a lit top edge so they read 3D
        for (int i = 0; i < 46; i++) {
            float x = hash(i, 1) * W;
            float y = gy + gh + (0.02f + hash(i, 2) * 1.2f) * u;
            if (y > H) continue;
            float r = (0.035f + hash(i, 3) * 0.075f + (hash(i, 10) > 0.86f ? 0.08f : 0f)) * u;
            fill.setColor(0x44000000);
            c.drawOval(new RectF(x - r, y - r * 0.55f, x + r, y + r * 0.55f), fill);
            fill.setColor(0x1EFFE0B0);
            c.drawOval(new RectF(x - r * 0.6f, y - r * 0.5f, x + r * 0.4f, y - r * 0.02f), fill);
        }

        // grass blades along the lip: sparse enough that the gaps stay visible,
        // otherwise they merge into a dark band
        for (int i = 0; i < 250; i++) {
            float x = hash(i, 4) * W;
            float tall = 0.05f + hash(i, 5) * 0.26f;
            float lean = (hash(i, 6) - 0.5f) * 0.26f;
            int shade = i % 4;
            fill.setColor(shade == 0 ? 0xFF6FB534 : shade == 1 ? 0xFF7FC044
                    : shade == 2 ? 0xFF5A9A2B : 0xFF8ACB52);
            path.reset();
            path.moveTo(x, gy + 0.05f * u);
            path.lineTo(x + lean * u, gy - tall * u);
            path.lineTo(x + (0.055f + lean * 0.4f) * u, gy + 0.05f * u);
            path.close();
            c.drawPath(path, fill);
        }
    }

    /** Deterministic value in [0,1) for (i,k): stable across frames, no shimmer. */
    private static float hash(int i, int k) {
        int h = i * 374761393 + k * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h = h ^ (h >>> 16);
        return (h & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }

    private void drawShadows(Canvas c) {
        float gy = g.toScreenY(0.03f);
        for (Block b : g.blocks) {
            shadow(c, b.body.position.x, b.w * 0.5f, b.body.position.y, gy);
        }
        for (Pig p : g.pigs) {
            if (p.alive) shadow(c, p.body.position.x, p.radius * 0.85f, p.body.position.y, gy);
        }
        for (Bird b : g.active) {
            if (b.body != null) shadow(c, b.body.position.x, b.radius * 0.8f, b.body.position.y, gy);
        }
        if (g.current != null && !g.current.launched) {
            shadow(c, g.pouchX, g.current.radius * 0.8f, g.pouchY, gy);
        }
    }

    private void shadow(Canvas c, float wx, float halfW, float wy, float gy) {
        // Shadows shrink and fade as the body rises, so nothing floats on a full-size blob.
        float lift = Math.max(0f, wy - 0.15f);
        float fade = 1f - Math.min(1f, lift / 11f);
        if (fade <= 0.05f) return;
        float shrink = Math.max(0.42f, 1f - lift / 16f);
        float sx = g.toScreenX(wx);
        float rx = Math.max(2f, halfW * 0.95f * g.scale * shrink);
        float ry = Math.max(2f, 0.16f * g.scale * shrink);
        fill.setColor(((int) (fade * fade * 88) << 24));
        tmp.set(sx - rx, gy - ry, sx + rx, gy + ry);
        c.drawOval(tmp, fill);
    }

    private void drawSlingBack(Canvas c) {
        // contact shadow so the trunk is planted, not pasted on the horizon
        float trunkX = Game.SLING_X + (66f - 40f) / 80f * SLING_W;
        fill.setColor(0x4D000000);
        tmp.set(g.toScreenX(trunkX) - 0.62f * g.scale, g.toScreenY(0f) - 0.10f * g.scale,
                g.toScreenX(trunkX) + 0.62f * g.scale, g.toScreenY(0f) + 0.10f * g.scale);
        c.drawOval(tmp, fill);

        Bitmap b = sprites.get("sling");
        drawSprite(c, b, Game.SLING_X, Game.SLING_H * 0.5f, SLING_W, Game.SLING_H, 0f, 1f);
    }

    private void drawSlingFront(Canvas c) {
        // single-piece slingshot: the fork is part of sling.png, nothing to overlay
    }

    private float pouchX() {
        return g.current != null && !g.current.launched ? g.pouchX : Game.ANCHOR_X;
    }

    private float pouchY() {
        return g.current != null && !g.current.launched ? g.pouchY : Game.ANCHOR_Y;
    }

    /** How far below the bird's centre the elastic grips, as a fraction of its radius. */
    private static final float GRIP_DROP = 0.80f;

    /** Visual radius of the bird in the pouch, or a sensible default when empty. */
    private float birdScale() {
        Bird b = g.current;
        return b != null && !b.launched ? b.radius * 1.025f : 0.62f;
    }

    /** World point the elastics converge on: the underside of the bird. */
    private float gripY() {
        return pouchY() - GRIP_DROP * birdScale();
    }

    private void drawBands(Canvas c) {
        // Only while a bird is actually in the pouch. After a launch `current` is
        // null, and drawing anyway left the elastic hanging with nothing in it.
        if (g.current == null || g.current.launched) return;
        float bx = g.toScreenX(pouchX());
        float by = g.toScreenY(gripY());
        band(c, g.toScreenX(PRONG_L), g.toScreenY(PRONG_L_Y), bx + 0.30f * g.scale, by);
        band(c, g.toScreenX(PRONG_R), g.toScreenY(PRONG_R_Y), bx - 0.30f * g.scale, by);
    }

    /** One elastic: a dark rubber core with a thin lit edge, so it reads as a band
     *  rather than a scratch in the wood. */
    private void band(Canvas c, float ax, float ay, float bx, float by) {
        stroke.setStrokeWidth(0.125f * g.scale);
        stroke.setColor(0xFF2A1607);
        c.drawLine(ax, ay, bx, by, stroke);
        stroke.setStrokeWidth(0.045f * g.scale);
        stroke.setColor(0x59B07A45);
        c.drawLine(ax, ay - 0.025f * g.scale, bx, by - 0.025f * g.scale, stroke);
    }

    private void drawPouch(Canvas c) {
        if (g.current == null || g.current.launched) return;
        float s = birdScale();
        float cx = g.toScreenX(pouchX());
        float y = g.toScreenY(gripY() + 0.06f * s);
        float r = 0.66f * s * g.scale;
        float hgt = 0.24f * s * g.scale;

        // leather cup, only its lower half ends up visible below the bird
        tmp.set(cx - r, y - hgt, cx + r, y + hgt);
        fill.setColor(0xFF462909);
        c.drawRoundRect(tmp, hgt * 0.8f, hgt * 0.8f, fill);
        tmp.set(cx - r * 0.82f, y - hgt * 0.55f, cx + r * 0.82f, y + hgt * 0.62f);
        fill.setColor(0xFF67401B);
        c.drawRoundRect(tmp, hgt * 0.6f, hgt * 0.6f, fill);
    }

    private void drawBlocks(Canvas c) {
        for (Block b : g.blocks) {
            Bitmap sprite = sprites.get(b.spriteName());
            float hi = Math.max(b.w, b.h), lo = Math.min(b.w, b.h);
            drawSprite(c, sprite, b.body.position.x, b.body.position.y, hi, lo,
                    b.body.rotation + b.spriteAngle, 1f);
            if (b.flash > 0.01f) {
                fill.setColor(((int) (b.flash * 110) << 24) | 0xFFFFFF);
                drawRotRect(c, b.body.position.x, b.body.position.y, b.w, b.h,
                        b.body.rotation, fill);
            }
        }
    }

    private void drawPigs(Canvas c) {
        for (Pig p : g.pigs) {
            if (!p.alive) continue;
            Bitmap sprite = sprites.get(p.spriteName());
            // sprites carry a couple of transparent rows at the bottom: draw slightly
            // larger and a touch lower so the pig's belly rests on the surface.
            float s = p.radius * 2.08f;
            drawSprite(c, sprite, p.body.position.x, p.body.position.y - p.radius * 0.07f,
                    s, s, p.body.rotation * 0.35f, 1f);
            if (p.flash > 0.01f) {
                fill.setColor(((int) (p.flash * 120) << 24) | 0xFFFFFF);
                c.drawCircle(g.toScreenX(p.body.position.x), g.toScreenY(p.body.position.y),
                        p.radius * g.scale, fill);
            }
        }
    }

    private void drawBirds(Canvas c) {
        if (g.current != null && !g.current.launched) {
            drawBird(c, g.current, g.pouchX, g.pouchY, 0f);
        }
        for (Bird b : g.active) {
            if (b.body == null) continue;
            float ang = b.body.rotation;
            if (b.body.speed() > 1.5f) {
                ang = (float) Math.atan2(b.body.velocity.y, b.body.velocity.x);
            }
            drawBird(c, b, b.body.position.x, b.body.position.y, ang);
        }
    }

    private void drawBird(Canvas c, Bird b, float wx, float wy, float ang) {
        Bitmap sprite = sprites.get(b.spriteName());
        float h = b.radius * 2.05f;
        float w = h * sprite.getWidth() / sprite.getHeight();
        drawSprite(c, sprite, wx, wy, w, h, ang, 1f);
    }

    /** Draws a bitmap centred at a world point, sized in world units, rotated. */
    private void drawSprite(Canvas c, Bitmap b, float wx, float wy, float wu, float hu,
                            float angle, float alpha) {
        if (b.getWidth() < 4) return;
        float sx = g.toScreenX(wx);
        float sy = g.toScreenY(wy);
        float sw = wu * g.scale;
        float sh = hu * g.scale;
        c.save();
        c.translate(sx, sy);
        if (angle != 0f) {
            c.rotate((float) -Math.toDegrees(angle));
        }
        bmp.setAlpha((int) (alpha * 255));
        dst.set(-sw / 2, -sh / 2, sw / 2, sh / 2);
        c.drawBitmap(b, null, dst, bmp);
        bmp.setAlpha(255);
        c.restore();
    }

    private void drawRotRect(Canvas c, float wx, float wy, float wu, float hu,
                             float angle, Paint p) {
        c.save();
        c.translate(g.toScreenX(wx), g.toScreenY(wy));
        c.rotate((float) -Math.toDegrees(angle));
        dst.set(-wu * g.scale / 2, -hu * g.scale / 2, wu * g.scale / 2, hu * g.scale / 2);
        c.drawRoundRect(dst, 0.12f * g.scale, 0.12f * g.scale, p);
        c.restore();
    }

    private void drawTrajectory(Canvas c) {
        if (g.state != Game.PLAY || !g.dragging) return;
        float vx = (Game.ANCHOR_X - g.pouchX) * Game.POWER;
        float vy = (Game.ANCHOR_Y - g.pouchY) * Game.POWER;
        float x = g.pouchX, y = g.pouchY;
        float dt = 0.055f;
        for (int i = 0; i < 90; i++) {
            x += vx * dt;
            y += vy * dt;
            vy -= 22f * dt;
            if (y < 0.2f || x > 24f) break;
            if (i % 4 == 0) {
                float r = Math.max(1.6f, (0.09f - i * 0.0006f) * g.scale);
                float sx = g.toScreenX(x), sy = g.toScreenY(y);
                fill.setColor(0x33000000);
                c.drawCircle(sx, sy + 1.5f, r, fill);
                fill.setColor(0xE6FFFFFF);
                c.drawCircle(sx, sy, r, fill);
            }
        }
    }

    private void drawParticles(Canvas c) {
        for (Game.Particle p : g.particles) {
            float a = Math.min(1f, p.life / (p.maxLife * 0.55f));
            float sx = g.toScreenX(p.x), sy = g.toScreenY(p.y);
            if (p.type == Game.Particle.TEXT) {
                txt.setTypeface(heavy);
                txt.setTextSize(0.62f * g.scale);
                txt.setTextAlign(Paint.Align.CENTER);
                txt.setColor((((int) (a * 255)) << 24) | 0xFFFFFF);
                c.drawText(p.text, sx, sy, txt);
            } else if (p.type == Game.Particle.DUST) {
                fill.setColor(((int) (a * 200) << 24) | (p.color & 0xFFFFFF));
                c.drawCircle(sx, sy, p.size * g.scale, fill);
            } else {
                fill.setColor(((int) (a * 255) << 24) | (p.color & 0xFFFFFF));
                c.save();
                c.translate(sx, sy);
                c.rotate((float) Math.toDegrees(p.rot));
                float s = p.size * g.scale;
                c.drawRect(-s / 2, -s / 2, s / 2, s / 2, fill);
                c.restore();
            }
        }
    }

    private void drawVignette(Canvas c) {
        fill.setAlpha(255);
        fill.setShader(new RadialGradient(W * 0.5f, H * 0.52f, Math.max(W, H) * 0.78f,
                new int[]{0x00000000, 0x00000000, 0x3D000000},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, fill);
        fill.setShader(null);
    }

    // -------------------------------------------------------------------- hud

    private void drawHud(Canvas c) {
        if (g.state == Game.MENU || g.state == Game.LEVELS) return;

        // One height for every top control so the row lines up exactly, and tall
        // enough to stay a 48dp touch target on a 720p phone.
        float pillH = H * 0.112f;
        float top = H * 0.028f;
        float pad = H * 0.028f;

        // ---- score, top-left
        RectF score = new RectF(pad, top, pad + W * 0.150f, top + pillH);
        plate(c, score, pillH * 0.5f);
        star(c, score.left + pillH * 0.48f, score.centerY(), pillH * 0.235f, 0xFFFFC107);
        txt.setStyle(Paint.Style.FILL);
        txt.setTypeface(heavy);
        txt.setTextSize(pillH * 0.50f);
        txt.setTextAlign(Paint.Align.RIGHT);
        txt.setColor(0xFFFFFFFF);
        c.drawText(String.valueOf(g.score), score.right - pillH * 0.34f,
                score.centerY() + pillH * 0.18f, txt);

        // ---- level, centred on the same row
        float lw = Math.max(W * 0.105f, pillH * 2.1f);
        RectF lvl = new RectF(W * 0.5f - lw * 0.5f, top, W * 0.5f + lw * 0.5f, top + pillH);
        plate(c, lvl, pillH * 0.5f);
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setTextSize(pillH * 0.42f);
        txt.setColor(0xFFFFFFFF);
        c.drawText("LEVEL " + (g.levelIndex + 1), lvl.centerX(),
                lvl.centerY() + pillH * 0.15f, txt);

        // ---- first-shot hint: only while nothing has been launched and nothing scored
        if (g.state == Game.PLAY && g.current != null && !g.current.launched
                && g.active.isEmpty() && g.score == 0) {
            float pulse = 0.72f + 0.28f * (float) Math.sin(t * 3.4f);
            float hw = W * 0.150f;
            float hh = pillH * 0.80f;
            float hx = W * 0.5f - hw * 0.5f;
            float hy = Math.min(H - pad - hh, g.toScreenY(0f) + pad * 1.9f);
            RectF tip = new RectF(hx, hy, hx + hw, hy + hh);
            fill.setColor(0xA6162836);
            c.drawRoundRect(tip, hh * 0.5f, hh * 0.5f, fill);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(1.5f, hh * 0.05f));
            stroke.setColor((((int) (150 * pulse)) << 24) | 0xFFFFFF);
            c.drawRoundRect(tip, hh * 0.5f, hh * 0.5f, stroke);
            stroke.setStyle(Paint.Style.FILL);
            text(c, "DRAG THE BIRD BACK TO AIM", tip.centerX(),
                    tip.centerY() + hh * 0.20f, hh * 0.36f, 0xFFFFFFFF, true);
        }

        // ---- birds left: sits in the grass band, clear of the slingshot trunk
        int n = g.queue.size() + (g.current != null ? 1 : 0);
        if (n > 0) {
            int shown = Math.min(n, 6);
            float icon = pillH * 0.62f;
            float step = icon * 1.02f;
            float pw = step * (shown - 1) + icon * 1.5f;
            float bh = pillH * 0.92f;
            float by = Math.min(H - pad - bh, g.toScreenY(0f) + pad * 0.55f);
            RectF bp = new RectF(pad, by, pad + pw, by + bh);
            plate(c, bp, bh * 0.5f);
            Bitmap iconBmp = sprites.get("bird_red_idle");
            for (int i = 0; i < shown; i++) {
                float x = bp.left + icon * 0.75f + i * step;
                float y = bp.centerY();
                dst.set(x - icon / 2, y - icon / 2, x + icon / 2, y + icon / 2);
                c.drawBitmap(iconBmp, null, dst, bmp);
            }
        }
    }

    /** Dark translucent plate with a light top edge, shared by every HUD pill. */
    private void plate(Canvas c, RectF r, float radius) {
        fill.setColor(0xA6162836);
        c.drawRoundRect(r, radius, radius, fill);
        fill.setColor(0x33FFFFFF);
        c.drawRoundRect(new RectF(r.left + radius * 0.5f, r.top + Math.max(1f, r.height() * 0.055f),
                r.right - radius * 0.5f, r.top + Math.max(2f, r.height() * 0.12f)),
                radius * 0.4f, radius * 0.4f, fill);
    }

    // --------------------------------------------------------------------- ui

    private void drawUi(Canvas c) {
        List<Game.UiButton> buttons = g.uiButtons();

        if (g.state == Game.MENU) {
            drawMenu(c);
        } else if (g.state == Game.LEVELS) {
            drawLevels(c);
        } else if (g.state == Game.PAUSED) {
            dim(c, 0.58f);
            panel(c, 0.32f, 0.16f, 0.68f, 0.84f);
            textDisplay(c, "PAUSED", W * 0.5f, H * 0.290f, H * 0.070f,
                    0xFFFFFFFF, Paint.Align.CENTER);
        } else if (g.state == Game.WIN) {
            dim(c, 0.55f);
            panel(c, 0.255f, 0.065f, 0.745f, 0.975f);
            textDisplay(c, "LEVEL CLEARED", W * 0.5f, H * 0.195f, H * 0.070f,
                    0xFFFFD54F, Paint.Align.CENTER);
            drawStarsRow(c, W * 0.5f, H * 0.325f, H * 0.060f, g.starsEarned, 1.0f);
            text(c, String.valueOf(g.score), W * 0.5f, H * 0.455f, H * 0.082f,
                    0xFFFFFFFF, true);
            text(c, "BEST " + g.best[g.levelIndex], W * 0.5f, H * 0.525f, H * 0.038f,
                    0xB3FFFFFF, false);
        } else if (g.state == Game.LOSE) {
            dim(c, 0.55f);
            panel(c, 0.31f, 0.15f, 0.69f, 0.87f);
            textDisplay(c, "LEVEL FAILED", W * 0.5f, H * 0.275f, H * 0.070f,
                    0xFFFF8A80, Paint.Align.CENTER);
            text(c, "OUT OF BIRDS", W * 0.5f, H * 0.375f, H * 0.040f, 0xCCFFFFFF, false);
            drawStarsRow(c, W * 0.5f, H * 0.485f, H * 0.052f, 0, 0.85f);
        }

        for (Game.UiButton b : buttons) {
            drawButton(c, b);
        }
    }

    private void drawMenu(Canvas c) {
        // Soft scrim behind the left column: the title sits over a bright sky and a
        // pale hilltop, and without this the lower line loses contrast.
        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0, 0, W * 0.62f, 0,
                new int[]{0x7A06304A, 0x4006304A, 0x0006304A},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W * 0.62f, H, fill);
        fill.setShader(null);

        float left = W * 0.085f;
        textDisplay(c, "ANGRY", left, H * 0.300f, H * 0.125f, 0xFFE53935, Paint.Align.LEFT);
        textDisplay(c, "BIRDS", left, H * 0.425f, H * 0.125f, 0xFFFFC107, Paint.Align.LEFT);
        textLeft(c, "NATIVE EDITION", left + H * 0.006f, H * 0.495f, H * 0.038f,
                0xD9FFFFFF, false);

        Bitmap bird = sprites.get("bird_red_idle");
        float s = H * 0.34f;
        float bob = (float) Math.sin(t * 2.2f) * H * 0.010f;
        float bx = W * 0.775f, by = H * 0.415f + bob;

        // glow disc so the bird separates from the sky
        fill.setAlpha(255);
        fill.setShader(new RadialGradient(bx, by, s * 0.80f,
                new int[]{0x33FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawCircle(bx, by, s * 0.80f, fill);
        fill.setShader(null);

        dst.set(bx - s / 2, by - s / 2, bx + s / 2, by + s / 2);
        c.drawBitmap(bird, null, dst, bmp);

        drawStarsRow(c, bx, H * 0.655f, H * 0.030f, 0, 1.0f);
        text(c, g.totalStars() + " / " + (g.levels.size() * 3), bx, H * 0.715f, H * 0.040f,
                0xE6FFFFFF, true);
    }

    private void drawLevels(Canvas c) {
        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0, 0, 0, H,
                new int[]{0x99061C2E, 0x66061C2E}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, fill);
        fill.setShader(null);

        textDisplay(c, "SELECT LEVEL", W * 0.5f, H * 0.145f, H * 0.072f,
                0xFFFFFFFF, Paint.Align.CENTER);
        text(c, "TAP A LEVEL TO PLAY", W * 0.5f, H * 0.215f, H * 0.036f, 0xCCFFFFFF, false);

        for (int i = 0; i < g.levels.size(); i++) {
            // 4 x 3 grid fits twelve levels without shrinking the touch targets.
            int col = i % 4, row = i / 4;
            float l = (0.10f + col * 0.21f) * W;
            float tp = (0.285f + row * 0.225f) * H;
            float r = l + 0.18f * W;
            float bt = tp + 0.185f * H;
            boolean unlocked = g.isUnlocked(i);
            boolean pressed = ("level" + i).equals(g.activePressId());

            if (pressed) {
                float in = 0.008f * H;
                l += in; tp += in; r -= in; bt -= in;
            }

            float rad = 0.028f * H;
            // drop shadow keeps the cards off the busy background
            fill.setColor(0x55000000);
            c.drawRoundRect(new RectF(l, tp + 0.012f * H, r, bt + 0.012f * H), rad, rad, fill);

            fill.setColor(unlocked ? 0xF2FFFFFF : 0x4DFFFFFF);
            c.drawRoundRect(new RectF(l, tp, r, bt), rad, rad, fill);

            // accent bar: green when playable, grey when locked
            fill.setColor(unlocked ? 0xFF8BC34A : 0xFF9E9E9E);
            c.drawRoundRect(new RectF(l, tp, r, tp + 0.020f * H), rad * 0.7f, rad * 0.7f, fill);

            text(c, String.valueOf(i + 1), (l + r) / 2, tp + 0.108f * H, H * 0.092f,
                    unlocked ? 0xFF2E3B45 : 0xFF9E9E9E, true);
            if (unlocked) {
                drawStarsRow(c, (l + r) / 2, tp + 0.170f * H, H * 0.027f, g.stars[i], 1f);
            } else {
                text(c, "LOCKED", (l + r) / 2, tp + 0.175f * H, H * 0.032f, 0xFF8A8A8A, false);
            }
        }
    }

    private void drawButton(Canvas c, Game.UiButton b) {
        if (b.style == 2) return; // level cards draw themselves

        float l = b.rect.l * W, tp = b.rect.t * H, r = b.rect.r * W, bt = b.rect.b * H;
        boolean pressed = b.id.equals(g.activePressId());

        // Press shrinks the face slightly; the plate behind stays put, which reads
        // as the button being pushed in.
        float rad = (bt - tp) * 0.5f;

        if (b.style == 3) {
            drawPauseButton(c, l, tp, r, bt, pressed);
            return;
        }

        // resting plate
        fill.setColor(0x55000000);
        c.drawRoundRect(new RectF(l, tp + (bt - tp) * 0.10f, r, bt + (bt - tp) * 0.10f),
                rad, rad, fill);

        if (pressed) {
            float in = (bt - tp) * 0.055f;
            l += in; tp += in * 1.6f; r -= in; bt -= in;
            rad = (bt - tp) * 0.5f;
        }

        int top = b.style == 0 ? 0xFF9CD35A : 0xFF8AA0AD;
        int bottom = b.style == 0 ? 0xFF4E8B27 : 0xFF3D5461;
        if (pressed) {
            top = b.style == 0 ? 0xFF79B03C : 0xFF6C8290;
            bottom = b.style == 0 ? 0xFF356417 : 0xFF2A3D48;
        }
        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0, tp, 0, bt, top, bottom, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(l, tp, r, bt), rad, rad, fill);
        fill.setShader(null);

        if (!pressed) {
            fill.setColor(0x33FFFFFF);
            c.drawRoundRect(new RectF(l + rad * 0.35f, tp + (bt - tp) * 0.09f,
                    r - rad * 0.35f, tp + (bt - tp) * 0.42f), rad * 0.6f, rad * 0.6f, fill);
        }

        text(c, b.label, (l + r) / 2, (tp + bt) / 2 + (bt - tp) * 0.155f,
                (bt - tp) * 0.40f, 0xFFFFFFFF, true);
    }

    /** Pause: a square plate with two bars, sized as a real 48dp touch target. */
    private void drawPauseButton(Canvas c, float l, float tp, float r, float bt, boolean pressed) {
        float rad = (bt - tp) * 0.30f;
        fill.setColor(pressed ? 0xCC16283A : 0x8C16283A);
        c.drawRoundRect(new RectF(l, tp, r, bt), rad, rad, fill);
        fill.setColor(0x33FFFFFF);
        c.drawRoundRect(new RectF(l + rad * 0.5f, tp + (bt - tp) * 0.09f,
                r - rad * 0.5f, tp + (bt - tp) * 0.15f), rad * 0.4f, rad * 0.4f, fill);

        float bw = (r - l) * 0.13f;
        float bh = (bt - tp) * 0.42f;
        float cy = (tp + bt) / 2;
        fill.setColor(0xFFFFFFFF);
        c.drawRoundRect(new RectF((l + r) / 2 - bw * 1.55f, cy - bh / 2,
                (l + r) / 2 - bw * 0.45f, cy + bh / 2), bw * 0.3f, bw * 0.3f, fill);
        c.drawRoundRect(new RectF((l + r) / 2 + bw * 0.45f, cy - bh / 2,
                (l + r) / 2 + bw * 1.55f, cy + bh / 2), bw * 0.3f, bw * 0.3f, fill);
    }

    private void dim(Canvas c, float a) {
        fill.setColor(((int) (a * 255) << 24));
        c.drawRect(0, 0, W, H, fill);
    }

    private void panel(Canvas c, float l, float tp, float r, float bt) {
        RectF box = new RectF(l * W, tp * H, r * W, bt * H);
        fill.setColor(0xE6151E28);
        c.drawRoundRect(box, 0.045f * H, 0.045f * H, fill);
        stroke.setStrokeWidth(0.005f * H);
        stroke.setColor(0x66FFFFFF);
        c.drawRoundRect(box, 0.045f * H, 0.045f * H, stroke);
    }

    private void drawStarsRow(Canvas c, float cx, float cy, float r, int earned, float scale) {
        for (int i = 0; i < 3; i++) {
            float x = cx + (i - 1) * r * 2.9f;
            float rr = r * (i == 1 ? 1.18f : 1f) * scale;
            star(c, x, cy, rr, i < earned ? 0xFFFFC107 : 0x55FFFFFF);
        }
    }

    private void star(Canvas c, float cx, float cy, float r, int color) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float rr = (i % 2 == 0) ? r : r * 0.45f;
            float px = cx + (float) Math.cos(a) * rr;
            float py = cy + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        fill.setColor(color);
        c.drawPath(path, fill);
        if (color != 0x55FFFFFF) {
            stroke.setStrokeWidth(Math.max(1f, r * 0.12f));
            stroke.setColor(0x88FF8F00);
            c.drawPath(path, stroke);
        }
    }

    // ------------------------------------------------------------------- text

    private void text(Canvas c, String s, float x, float y, float size, int color, boolean heavyFont) {
        textAt(c, s, x, y, size, color, Paint.Align.CENTER, heavyFont);
    }

    private void textLeft(Canvas c, String s, float x, float y, float size, int color, boolean heavyFont) {
        textAt(c, s, x, y, size, color, Paint.Align.LEFT, heavyFont);
    }

    private void textAt(Canvas c, String s, float x, float y, float size, int color,
                        Paint.Align align, boolean heavyFont) {
        txt.setStyle(Paint.Style.FILL);
        txt.setTypeface(heavyFont ? heavy : bold);
        txt.setTextSize(size);
        txt.setTextAlign(align);
        txt.setColor(0x66000000);
        c.drawText(s, x, y + size * 0.06f, txt);
        txt.setColor(color);
        c.drawText(s, x, y, txt);
    }

    /** Display type: layered soft shadows behind the fill. Deliberately avoids
     *  stroke-outlined text, which renders differently across devices. */
    private void textDisplay(Canvas c, String s, float x, float y, float size, int color,
                             Paint.Align align) {
        txt.setStyle(Paint.Style.FILL);
        txt.setTypeface(heavy);
        txt.setTextSize(size);
        txt.setTextAlign(align);
        txt.setColor(0x5C000000);
        c.drawText(s, x + size * 0.035f, y + size * 0.085f, txt);
        txt.setColor(0x2E000000);
        c.drawText(s, x + size * 0.014f, y + size * 0.034f, txt);
        txt.setColor(color);
        c.drawText(s, x, y, txt);
    }
}
