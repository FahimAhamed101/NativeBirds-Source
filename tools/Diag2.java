import com.nativebirds.game.*;

/** Per-block dx/dy breakdown for level 1. */
public class Diag2 {
    public static void main(String[] a) {
        Game g = new Game(null, new GameTest.MemStore());
        g.startLevel(0);
        System.out.println("spawn:");
        for (Block b : g.blocks) {
            System.out.printf("  %.0fx%.0f  x=%.3f y=%.3f%n", b.w, b.h, b.body.position.x, b.body.position.y);
        }
        float[][] p0 = new float[g.blocks.size()][2];
        for (int i = 0; i < g.blocks.size(); i++) {
            p0[i][0] = g.blocks.get(i).body.position.x;
            p0[i][1] = g.blocks.get(i).body.position.y;
        }
        for (int s = 0; s < 120; s++) g.update(1f / 120f);
        System.out.println("after 1s:");
        for (int i = 0; i < g.blocks.size(); i++) {
            Block b = g.blocks.get(i);
            System.out.printf("  %.0fx%.0f  dx=%+.3f dy=%+.3f rot=%+.4f  (x=%.3f y=%.3f)%n",
                    b.w, b.h, b.body.position.x - p0[i][0], b.body.position.y - p0[i][1],
                    b.body.rotation, b.body.position.x, b.body.position.y);
        }
        for (Pig p : g.pigs) {
            System.out.printf("  pig r=%.2f x=%.3f y=%.3f%n", p.radius, p.body.position.x, p.body.position.y);
        }
    }
}
