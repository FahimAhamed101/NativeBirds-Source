import com.nativebirds.game.*;

public class Dump3 {
    public static void main(String[] a) {
        Game g = new Game(new Sfx() { public void play(String n, float v, float r) {} },
                new Store() {
                    public int getInt(String k, int d) { return d; }
                    public void putInt(String k, int v) {}
                });
        g.setViewport(1088, 538);
        g.startLevel(0);
        for (int i = 0; i < 72; i++) g.update(1f / 60f);
        System.out.printf("scale=%.4f camY=%.4f camX=%.4f groundY=%.2f%n",
                g.scale, g.camY, g.camX, g.toScreenY(0f));
        System.out.printf("pouch=(%.3f,%.3f) SLING_H=%.2f SLING_TOP=%.2f%n",
                g.pouchX, g.pouchY, Game.SLING_H, Game.SLING_TOP);
        if (g.current != null) {
            Bird b = g.current;
            System.out.printf("current bird type=%s radius=%.3f diam=%.2f  (px=%.1f)%n",
                    b.type, b.radius, b.radius * 2, b.radius * 2 * g.scale);
            System.out.printf("  drawn top=%.2f bottom=%.2f%n",
                    g.pouchY + b.radius, g.pouchY - b.radius);
        }
        for (Pig p : g.pigs) {
            System.out.printf("pig size=%d r=%.3f diam=%.2f pos=(%.2f,%.2f)%n",
                    p.size, p.radius, p.radius * 2, p.body.position.x, p.body.position.y);
        }
        for (Block b : g.blocks) {
            System.out.printf("blk %.2fx%.2f pos=(%.2f,%.2f) px=%.0fx%.0f%n",
                    b.w, b.h, b.body.position.x, b.body.position.y,
                    b.w * g.scale, b.h * g.scale);
        }
    }
}
