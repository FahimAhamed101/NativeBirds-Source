import com.nativebirds.game.*;

import java.util.HashMap;
import java.util.Map;

/** Headless gameplay validation: level stability, launch/damage, win + lose flow. */
public class GameTest {

    static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name + "   " + detail);
        if (!ok) failures++;
    }

    static class MemStore implements Store {
        final Map<String, Integer> m = new HashMap<>();

        public int getInt(String k, int d) {
            return m.containsKey(k) ? m.get(k) : d;
        }

        public void putInt(String k, int v) {
            m.put(k, v);
        }
    }

    static void run(Game g, float seconds) {
        int steps = (int) (seconds * 120f);
        for (int i = 0; i < steps; i++) g.update(1f / 120f);
    }

    public static void main(String[] args) {
        MemStore store = new MemStore();
        Game g = new Game(null, store);

        // ---- every level must be stable at rest -----------------------------
        for (int i = 0; i < g.levels.size(); i++) {
            g.startLevel(i);
            int nBlocks = g.blocks.size();
            int nPigs = g.pigs.size();
            float[][] start = new float[nBlocks][2];
            for (int b = 0; b < nBlocks; b++) {
                start[b][0] = g.blocks.get(b).body.position.x;
                start[b][1] = g.blocks.get(b).body.position.y;
            }
            run(g, 4f);

            float maxDrift = 0f;
            for (int b = 0; b < g.blocks.size(); b++) {
                maxDrift = Math.max(maxDrift, Math.abs(g.blocks.get(b).body.position.x - start[b][0]));
                maxDrift = Math.max(maxDrift, Math.abs(g.blocks.get(b).body.position.y - start[b][1]));
            }
            int alive = 0;
            for (Pig p : g.pigs) if (p.alive) alive++;
            check("L" + (i + 1) + " structure is stable", maxDrift < 0.12f && alive == nPigs,
                    "drift=" + fmt(maxDrift) + " pigs=" + alive + "/" + nPigs + " blocks=" + g.blocks.size() + "/" + nBlocks);
            check("L" + (i + 1) + " stays in PLAY at rest", g.state == Game.PLAY, "state=" + g.state);
        }

        // ---- a solid hit must destroy things and score ----------------------
        g.startLevel(0);
        run(g, 0.5f);
        check("bird is loaded on slingshot", g.current != null, "");
        // pull straight back and slightly down -> shallow arc into the tower
        g.touchDown(Game.ANCHOR_X - 0.1f, Game.ANCHOR_Y - 0.1f);
        g.touchMove(Game.ANCHOR_X - 1.9f, Game.ANCHOR_Y - 0.75f);
        float speed = g.launchSpeed();
        g.touchUp(0, 0);
        check("bird launched with sensible speed", speed > 6f && speed < 18f, "speed=" + fmt(speed));
        run(g, 6f);
        check("impact damaged the level", g.damageDealt > 20f, "damage=" + fmt(g.damageDealt)
                + " score=" + g.score);
        check("no NaN in world", finite(g), "");

        // ---- clearing all pigs wins -----------------------------------------
        g.startLevel(0);
        run(g, 0.2f);
        for (Pig p : new java.util.ArrayList<Pig>(g.pigs)) {
            g.explode(p.body.position.x, p.body.position.y, 6f, 4f, 5000f);
        }
        run(g, 2f);
        check("clearing pigs reaches WIN", g.state == Game.WIN, "state=" + g.state);
        check("win awards stars", g.starsEarned >= 1, "stars=" + g.starsEarned);
        check("progress persisted", store.getInt("stars0", 0) >= 1, "stars0=" + store.getInt("stars0", 0));

        // ---- running out of birds loses --------------------------------------
        g.startLevel(1);
        run(g, 0.2f);
        int guard = 0;
        while (g.state == Game.PLAY && guard++ < 80) {
            if (g.current != null) {
                g.touchDown(Game.ANCHOR_X - 0.1f, Game.ANCHOR_Y - 0.1f);
                g.touchMove(Game.ANCHOR_X - 0.3f, Game.ANCHOR_Y - 3.0f); // straight up, misses
                g.touchUp(0, 0);
            }
            run(g, 3f);
        }
        run(g, 5f);
        check("missing everything reaches LOSE", g.state == Game.LOSE, "state=" + g.state);

        // ---- restart from win keeps state consistent -------------------------
        g.startLevel(2);
        check("restart resets score", g.score == 0 && g.state == Game.PLAY, "score=" + g.score);

        System.out.println(failures == 0 ? "\nALL GAME TESTS PASSED" : "\n" + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    static boolean finite(Game g) {
        for (Block b : g.blocks) {
            if (Float.isNaN(b.body.position.x) || Float.isNaN(b.body.position.y)) return false;
        }
        for (Pig p : g.pigs) {
            if (Float.isNaN(p.body.position.x) || Float.isNaN(p.body.position.y)) return false;
        }
        return true;
    }

    static String fmt(float f) {
        return String.format("%.2f", f);
    }
}
