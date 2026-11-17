import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.nativebirds.game.Game;
import com.nativebirds.game.Renderer;
import com.nativebirds.game.Sfx;
import com.nativebirds.game.SpriteBank;
import com.nativebirds.game.Store;

import java.io.File;

import javax.imageio.ImageIO;

/**
 * Renders real game frames to PNG on the desktop JVM, using the same Renderer
 * the device uses. Lets the play screen be inspected and tuned without adb.
 *
 * usage: Preview <assetsDir> <outDir> <scene> [width] [height]
 * scenes: menu, levels, idle, drag, fly, hit, win, lose, pause
 */
public final class Preview {

    private static final Sfx SILENT = new Sfx() {
        @Override
        public void play(String name, float volume, float rate) {
        }
    };

    private static final class MemStore implements Store {
        private final java.util.Map<String, Integer> map = new java.util.HashMap<String, Integer>();

        @Override
        public int getInt(String key, int def) {
            Integer v = map.get(key);
            return v == null ? def : v;
        }

        @Override
        public void putInt(String key, int value) {
            map.put(key, value);
        }
    }

    public static void main(String[] args) throws Exception {
        File assets = new File(args[0]);
        File outDir = new File(args[1]);
        String scene = args[2];
        int w = args.length > 3 ? Integer.parseInt(args[3]) : 1088;
        int h = args.length > 4 ? Integer.parseInt(args[4]) : 538;

        SpriteBank bank = new SpriteBank(new Context(new AssetManager(assets)));
        Game game = new Game(SILENT, new MemStore());
        game.setViewport(w, h);
        Renderer renderer = new Renderer(bank);

        float time = 1.6f;

        if ("menu".equals(scene)) {
            game.state = Game.MENU;
        } else if ("levels".equals(scene)) {
            game.state = Game.LEVELS;
        } else {
            // scene "lvl<N>" renders level N at rest, so every level can be checked.
            int lvl = 0;
            if (scene.startsWith("lvl")) {
                lvl = Integer.parseInt(scene.substring(3));
            }
            game.startLevel(lvl);
            settle(game, 1.2f);

            if ("drag".equals(scene)) {
                game.touchDown(Game.ANCHOR_X - 0.2f, Game.ANCHOR_Y + 0.6f);
                game.touchMove(Game.ANCHOR_X - 2.3f, Game.ANCHOR_Y - 0.85f);
                settle(game, 0.25f);
            } else if ("fly".equals(scene) || "hit".equals(scene)) {
                game.touchDown(Game.ANCHOR_X - 0.2f, Game.ANCHOR_Y + 0.6f);
                game.touchMove(Game.ANCHOR_X - 2.6f, Game.ANCHOR_Y - 0.6f);
                game.touchUp(0, 0);
                settle(game, "fly".equals(scene) ? 0.30f : 0.75f);
            } else if ("win".equals(scene)) {
                for (int i = 0; i < game.pigs.size(); i++) {
                    game.pigs.get(i).alive = false;
                }
                game.score = 32500;
                game.starsEarned = 3;
                settle(game, 0.9f);
            } else if ("lose".equals(scene)) {
                game.state = Game.LOSE;
                settle(game, 0.2f);
            } else if ("pause".equals(scene)) {
                game.state = Game.PAUSED;
            }
        }

        Bitmap frame = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        renderer.draw(new Canvas(frame), game, time);

        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IllegalStateException("cannot create " + outDir);
        }
        File out = new File(outDir, "play-" + scene + ".png");
        ImageIO.write(frame.image, "png", out);
        System.out.println("wrote " + out.getAbsolutePath()
                + "  state=" + game.state + " score=" + game.score);
    }

    private static void settle(Game game, float seconds) {
        float dt = 1f / 60f;
        for (float t = 0; t < seconds; t += dt) {
            game.update(dt);
        }
    }
}
