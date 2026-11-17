import com.nativebirds.game.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Static geometry checks for every level, independent of the physics solver.
 *
 * Catches the mistakes that produce a level which is technically "stable" but
 * looks wrong or explodes on the first frame: overlapping blocks, floating
 * blocks, pigs wedged inside a beam, and levels that run off the camera.
 *
 * Run:  java -cp build/test LevelTest
 */
public class LevelTest {

    static int failures = 0;

    public static void main(String[] a) {
        List<Level> levels = Level.all();
        System.out.println("checking " + levels.size() + " levels");

        for (int i = 0; i < levels.size(); i++) {
            Level lv = levels.get(i);
            System.out.println("\n== level " + (i + 1) + "  " + lv.name
                    + "  blocks=" + lv.blocks.size() + " pigs=" + lv.pigs.size());

            List<float[]> boxes = new ArrayList<float[]>();
            for (int b = 0; b < lv.blocks.size(); b++) {
                Level.BlockSpec s = lv.blocks.get(b);
                boxes.add(new float[]{s.x, s.y, s.x + s.w, s.y + s.h});
            }

            checkOverlaps(lv);
            checkSupport(lv);
            checkPigs(lv);
            checkBounds(lv);
            checkSpriteSizes(lv);
        }

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL LEVEL CHECKS PASSED");
        } else {
            System.out.println(failures + " LEVEL CHECK(S) FAILED");
            System.exit(1);
        }
    }

    static void fail(String msg) {
        System.out.println("  FAIL " + msg);
        failures++;
    }

    /** Blocks must not interpenetrate: the solver would blast them apart. */
    static void checkOverlaps(Level lv) {
        List<Level.BlockSpec> bs = lv.blocks;
        for (int i = 0; i < bs.size(); i++) {
            for (int j = i + 1; j < bs.size(); j++) {
                Level.BlockSpec a = bs.get(i), b = bs.get(j);
                float ox = Math.min(a.x + a.w, b.x + b.w) - Math.max(a.x, b.x);
                float oy = Math.min(a.y + a.h, b.y + b.h) - Math.max(a.y, b.y);
                if (ox > 0.02f && oy > 0.02f) {
                    fail(String.format("blocks %d and %d overlap by %.2f x %.2f",
                            i, j, ox, oy));
                }
            }
        }
    }

    /** Every block rests on the ground or on top of another block. */
    static void checkSupport(Level lv) {
        for (int i = 0; i < lv.blocks.size(); i++) {
            Level.BlockSpec s = lv.blocks.get(i);
            if (s.y <= 0.02f) continue;
            boolean held = false;
            for (int j = 0; j < lv.blocks.size() && !held; j++) {
                if (i == j) continue;
                Level.BlockSpec o = lv.blocks.get(j);
                float overlapX = Math.min(s.x + s.w, o.x + o.w) - Math.max(s.x, o.x);
                boolean topMatches = Math.abs((o.y + o.h) - s.y) <= 0.02f;
                if (overlapX > 0.05f && topMatches) held = true;
            }
            if (!held) {
                fail(String.format("block %d at (%.2f,%.2f) %.0fx%.0f is floating",
                        i, s.x, s.y, s.w, s.h));
            }
        }
    }

    /** Pigs rest on a real surface and never intersect a block. */
    static void checkPigs(Level lv) {
        for (int i = 0; i < lv.pigs.size(); i++) {
            Level.PigSpec p = lv.pigs.get(i);
            float cy = p.surfaceY + p.radius;

            for (int j = 0; j < lv.blocks.size(); j++) {
                Level.BlockSpec s = lv.blocks.get(j);
                // closest point on the block to the pig centre
                float cx = Math.max(s.x, Math.min(p.x, s.x + s.w));
                float yy = Math.max(s.y, Math.min(cy, s.y + s.h));
                float dx = p.x - cx, dy = cy - yy;
                float d2 = dx * dx + dy * dy;
                if (d2 < (p.radius - 0.02f) * (p.radius - 0.02f)) {
                    fail(String.format("pig %d at (%.2f,%.2f) r=%.2f intersects block %d",
                            i, p.x, cy, p.radius, j));
                }
            }

            if (p.surfaceY > 0.02f) {
                boolean held = false;
                for (int j = 0; j < lv.blocks.size() && !held; j++) {
                    Level.BlockSpec s = lv.blocks.get(j);
                    boolean underX = p.x > s.x - 0.05f && p.x < s.x + s.w + 0.05f;
                    if (underX && Math.abs((s.y + s.h) - p.surfaceY) <= 0.02f) held = true;
                }
                if (!held) {
                    fail(String.format("pig %d at (%.2f,%.2f) floats above %.2f",
                            i, p.x, p.surfaceY, p.surfaceY));
                }
            }
        }
    }

    /** Keep everything inside the camera so the player can actually see it. */
    static void checkBounds(Level lv) {
        for (Level.BlockSpec s : lv.blocks) {
            if (s.x < 16.0f || s.x + s.w > 30.0f) {
                fail(String.format("block at x %.2f..%.2f is outside the camera",
                        s.x, s.x + s.w));
            }
            if (s.y + s.h > 8.0f) {
                fail(String.format("block at y up to %.2f is too tall", s.y + s.h));
            }
        }
        for (Level.PigSpec p : lv.pigs) {
            if (p.x < 16.0f || p.x > 30.0f) fail("pig at x " + p.x + " is off camera");
            if (p.surfaceY + p.radius * 2 > 8.2f) fail("pig at y " + p.surfaceY + " too high");
        }
    }

    /** Every block size must have a sprite, or it draws as a fallback. */
    static void checkSpriteSizes(Level lv) {
        String[] ok = {"1x1", "2x1", "2x2", "4x1", "4x2", "8x1", "10x1"};
        for (Level.BlockSpec s : lv.blocks) {
            int hi = (int) Math.max(s.w, s.h);
            int lo = (int) Math.min(s.w, s.h);
            String key = hi + "x" + lo;
            boolean found = false;
            for (String k : ok) if (k.equals(key)) found = true;
            if (!found) {
                fail("block size " + key + " has no sprite");
            }
        }
    }
}
