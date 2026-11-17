import com.nativebirds.game.*;

/** Prints where key world features land on screen, in pixels and as a fraction. */
public class Dump6 {
    public static void main(String[] a) {
        int W = 1520, H = 720;
        Game g = new Game(new Sfx() { public void play(String n, float v, float r) {} },
                new Store() {
                    public int getInt(String k, int d) { return d; }
                    public void putInt(String k, int v) {}
                });
        g.setViewport(W, H);
        g.startLevel(0);

        System.out.printf("scale = %.2f px/unit   visible world = %.1f x %.1f units%n",
                g.scale, W / g.scale, H / g.scale);

        float gy = g.toScreenY(0f);
        System.out.printf("ground line      y=%6.1f  (%.1f%% down the screen)%n", gy, 100 * gy / H);

        float sx = g.toScreenX(Game.ANCHOR_X);
        float sTop = g.toScreenY(Game.SLING_H);
        System.out.printf("slingshot        x=%6.1f (%.1f%% across)   top y=%.1f  height %.0f px (%.1f%% of screen)%n",
                sx, 100 * sx / W, sTop, gy - sTop, 100 * (gy - sTop) / H);

        Level lv = g.levels.get(0);
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE, top = Float.MAX_VALUE;
        for (Level.BlockSpec b : lv.blocks) {
            lo = Math.min(lo, b.x);
            hi = Math.max(hi, b.x + b.w);
            top = Math.min(top, g.toScreenY(b.y + b.h));
        }
        System.out.printf("level 1 fort     x=%.1f..%.1f px  (%.1f%%..%.1f%% across)  top y=%.1f (%.1f%% down)%n",
                g.toScreenX(lo), g.toScreenX(hi), 100 * g.toScreenX(lo) / W, 100 * g.toScreenX(hi) / W,
                top, 100 * top / H);
        System.out.printf("gap fork->fort   %.1f world units  (%.1f%% of the screen width)%n",
                lo - Game.ANCHOR_X, 100 * (g.toScreenX(lo) - sx) / W);
    }
}
