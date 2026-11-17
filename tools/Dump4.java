import com.nativebirds.game.*;

public class Dump4 {
    public static void main(String[] a) {
        Game g = new Game(new Sfx() { public void play(String n, float v, float r) {} },
                new Store() {
                    public int getInt(String k, int d) { return d; }
                    public void putInt(String k, int v) {}
                });
        g.setViewport(1088, 538);
        g.startLevel(0);
        for (int i = 0; i < 60; i++) g.update(1f / 60f);
        System.out.printf("SLING_H=%.2f SLING_X=%.2f ANCHOR=(%.2f,%.2f)%n",
                Game.SLING_H, Game.SLING_X, Game.ANCHOR_X, Game.ANCHOR_Y);
        System.out.printf("fork tips: L=(%.2f,%.2f) R=(%.2f,%.2f) gap=%.2f%n",
                Game.SLING_X + (16f - 40f) / 80f * (Game.SLING_H * 80f / 194f),
                Game.SLING_H * (1f - 5f / 194f),
                Game.SLING_X + (62f - 40f) / 80f * (Game.SLING_H * 80f / 194f),
                Game.SLING_H,
                (62f - 16f) / 80f * (Game.SLING_H * 80f / 194f));
        Bird b = g.current;
        System.out.printf("bird r=%.2f diam=%.2f  mass=%.3f%n", b.radius, b.radius * 2,
                Math.PI * b.radius * b.radius * Game.BIRD_DENSITY);
        System.out.printf("diam vs gap: %.2f vs %.2f  -> clearance each side %.3f%n",
                b.radius * 2, (62f - 16f) / 80f * (Game.SLING_H * 80f / 194f),
                ((62f - 16f) / 80f * (Game.SLING_H * 80f / 194f) - b.radius * 2) / 2);

        // drag far and check the pouch stays reachable
        g.touchDown(Game.ANCHOR_X, Game.ANCHOR_Y);
        g.touchMove(Game.ANCHOR_X - 2.2f, Game.ANCHOR_Y + 1.2f);
        for (int i = 0; i < 6; i++) g.update(1f / 60f);
        System.out.printf("drag: pouch=(%.2f,%.2f) dragging=%b launchSpeed=%.2f%n",
                g.pouchX, g.pouchY, g.dragging, g.launchSpeed());
        float dx = Game.ANCHOR_X - g.pouchX, dy = Game.ANCHOR_Y - g.pouchY;
        System.out.printf("pull=%.2f (max %.2f)  dir=(%.2f,%.2f)%n",
                (float) Math.sqrt(dx * dx + dy * dy), Game.MAX_DRAG, dx, dy);
    }
}
