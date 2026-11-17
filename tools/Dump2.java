import com.nativebirds.game.*;

public class Dump2 {
    public static void main(String[] a) {
        Game g = new Game(new Sfx() { public void play(String n, float v, float r) {} },
                new Store() {
                    public int getInt(String k, int d) { return d; }
                    public void putInt(String k, int v) {}
                });
        g.setViewport(1088, 538);
        g.startLevel(0);
        for (int i = 0; i < 72; i++) g.update(1f / 60f);
        System.out.printf("idle  camY=%.4f gy=%.2f pouch=(%.2f,%.2f)%n",
                g.camY, g.toScreenY(0f), g.pouchX, g.pouchY);
        g.touchDown(Game.ANCHOR_X - 0.2f, Game.ANCHOR_Y + 0.6f);
        g.touchMove(Game.ANCHOR_X - 2.1f, Game.ANCHOR_Y + 1.7f);
        for (int i = 0; i < 15; i++) g.update(1f / 60f);
        System.out.printf("drag  camY=%.4f gy=%.2f pouch=(%.2f,%.2f) dragging=%b%n",
                g.camY, g.toScreenY(0f), g.pouchX, g.pouchY, g.dragging);
    }
}
