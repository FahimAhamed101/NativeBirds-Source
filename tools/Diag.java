import com.nativebirds.game.*;

/** Prints per-block motion over time to locate the source of settling drift. */
public class Diag {
    public static void main(String[] a) {
        Game g = new Game(null, new GameTest.MemStore());
        for (int lv = 0; lv < 3; lv++) {
            g.startLevel(lv);
            System.out.println("=== level " + (lv + 1) + " ===");
            int n = g.blocks.size();
            float[][] p0 = new float[n][2];
            for (int i = 0; i < n; i++) {
                p0[i][0] = g.blocks.get(i).body.position.x;
                p0[i][1] = g.blocks.get(i).body.position.y;
            }
            for (int t = 1; t <= 6; t++) {
                for (int s = 0; s < 120; s++) g.update(1f / 120f);
                StringBuilder sb = new StringBuilder("  t=" + t + "s ");
                float worst = 0;
                int wi = 0;
                for (int i = 0; i < n; i++) {
                    Block b = g.blocks.get(i);
                    float d = Math.max(Math.abs(b.body.position.x - p0[i][0]),
                            Math.abs(b.body.position.y - p0[i][1]));
                    if (d > worst) { worst = d; wi = i; }
                }
                Block w = g.blocks.get(wi);
                sb.append(String.format("worst=%.3f (%.0fx%.0f at %.1f,%.1f) rot=%.3f v=%.3f w=%.3f",
                        worst, w.w, w.h, p0[wi][0], p0[wi][1], w.body.rotation,
                        w.body.velocity.length(), w.body.angularVelocity));
                System.out.println(sb);
            }
        }
    }
}
