import com.nativebirds.game.*;

public class Dump {
    public static void main(String[] a) {
        Game g = new Game(new Sfx() { public void play(String n, float v, float r) {} },
                new Store() {
                    public int getInt(String k, int d) { return d; }
                    public void putInt(String k, int v) {}
                });
        g.setViewport(1088, 538);
        g.startLevel(0);
        for (int i = 0; i < 90; i++) g.update(1f / 60f);
        System.out.println("camY=" + g.camY + " scale=" + g.scale);
        for (Pig p : g.pigs) {
            System.out.printf("pig size=%d r=%.3f pos=(%.3f,%.3f) top=%.3f bottom=%.3f alive=%b%n",
                    p.size, p.radius, p.body.position.x, p.body.position.y,
                    p.body.position.y + p.radius, p.body.position.y - p.radius, p.alive);
        }
        for (Block b : g.blocks) {
            System.out.printf("blk w=%.2f h=%.2f pos=(%.3f,%.3f) rot=%.2f sprite=%s%n",
                    b.w, b.h, b.body.position.x, b.body.position.y, b.body.rotation, b.spriteName());
        }
    }
}
