import com.nativebirds.game.*;
import java.util.List;

public class Dump5 {
    static Game g;
    public static void main(String[] a) {
        g = new Game(new Sfx() { public void play(String n, float v, float r) {} },
                new Store() {
                    public int getInt(String k, int d) { return d; }
                    public void putInt(String k, int v) {}
                });
        g.setViewport(1520, 720);
        String[] names = {"MENU", "LEVELS", "PLAY", "PAUSED", "WIN", "LOSE"};
        int[] states = {Game.MENU, Game.LEVELS, Game.PLAY, Game.PAUSED, Game.WIN, Game.LOSE};
        for (int i = 0; i < states.length; i++) {
            setState(states[i]);
            System.out.println("== " + names[i]);
            for (Game.UiButton b : g.uiButtons()) {
                float w = b.rect.r - b.rect.l, h = b.rect.b - b.rect.t;
                boolean inside = b.rect.l >= 0 && b.rect.t >= 0 && b.rect.r <= 1 && b.rect.b <= 1;
                System.out.printf("   %-8s x %.3f..%.3f y %.3f..%.3f  %.0fx%.0f px  %.0fx%.0f dp  %s%n",
                        b.id, b.rect.l, b.rect.r, b.rect.t, b.rect.b,
                        w * 1520, h * 720, w * 1520 / 1.6875f, h * 720 / 1.6875f,
                        inside ? "ok" : "OFF-FRAME");
            }
        }
    }

    static void setState(int s) {
        try {
            java.lang.reflect.Field f = Game.class.getField("state");
            f.setInt(g, s);
        } catch (Exception e) { System.out.println("reflect failed: " + e); }
    }
}
