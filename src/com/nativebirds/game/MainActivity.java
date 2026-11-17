package com.nativebirds.game;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/** Fullscreen, portrait-locked host for the game surface. */
public class MainActivity extends Activity {

    private GameView view;
    private Audio audio;
    private Game game;

    private static class PrefsStore implements Store {
        private final SharedPreferences prefs;

        PrefsStore(Activity a) {
            prefs = a.getSharedPreferences("nativebirds", MODE_PRIVATE);
        }

        @Override
        public int getInt(String key, int def) {
            return prefs.getInt(key, def);
        }

        @Override
        public void putInt(String key, int value) {
            prefs.edit().putInt(key, value).apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // draw into the camera-cutout strip instead of letterboxing it black
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        audio = new Audio(this);
        game = new Game(audio, new PrefsStore(this));
        view = new GameView(this, game, new Renderer(new SpriteBank(this)));
        setContentView(view);
        hideSystemUi();
    }

    private void hideSystemUi() {
        View decor = getWindow().getDecorView();
        // legacy flags are safe on every API level, even before the decor is attached
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // the modern path needs an attached decor view, otherwise getInsetsController() NPEs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && decor.isAttachedToWindow()) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (view != null) {
            view.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (view != null) {
            view.pause();
        }
        if (game != null && game.state == Game.PLAY) {
            game.state = Game.PAUSED;
        }
    }

    @Override
    public void onBackPressed() {
        if (game == null) {
            super.onBackPressed();
            return;
        }
        if (game.state == Game.PLAY || game.state == Game.PAUSED) {
            game.state = Game.PAUSED;
        } else if (game.state == Game.LEVELS || game.state == Game.WIN || game.state == Game.LOSE) {
            game.state = Game.MENU;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audio != null) {
            audio.release();
        }
    }
}
