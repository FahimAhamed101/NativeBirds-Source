package com.nativebirds.game;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

/** View that drives a fixed-step simulation and renders it every frame. */
public class GameView extends View implements Choreographer.FrameCallback {

    private static final float STEP = 1f / 120f;

    private final Game game;
    private final Renderer renderer;

    private long lastNanos;
    private float accumulator;
    private float clock;
    private boolean running;

    public GameView(Context context, Game game, Renderer renderer) {
        super(context);
        this.game = game;
        this.renderer = renderer;
        setFocusable(true);
        setKeepScreenOn(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        game.setViewport(w, h);
    }

    public void resume() {
        if (!running) {
            running = true;
            lastNanos = 0;
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    public void pause() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }
        if (lastNanos == 0) {
            lastNanos = frameTimeNanos;
        }
        float dt = (frameTimeNanos - lastNanos) / 1e9f;
        lastNanos = frameTimeNanos;
        if (dt > 0.12f) {
            dt = 0.12f;      // never let a stall explode the simulation
        }
        clock += dt;
        accumulator += dt;
        int guard = 0;
        while (accumulator >= STEP && guard++ < 12) {
            game.update(STEP);
            accumulator -= STEP;
        }
        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        renderer.draw(canvas, game, clock);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float px = e.getX(), py = e.getY();
        float nx = px / Math.max(1f, getWidth());
        float ny = py / Math.max(1f, getHeight());

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                // UI is handled press-on-down / fire-on-up, so a player can slide off
                // a button to cancel it. Gameplay drag still starts immediately.
                String hit = game.uiHitAt(nx, ny);
                if (hit != null) {
                    game.pressUi(hit);
                } else if (game.state == Game.PLAY) {
                    game.touchDown(game.toWorldX(px), game.toWorldY(py));
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (game.pressedId != null && game.uiHitAt(nx, ny) == null) {
                    game.cancelUiPress();
                }
                if (game.dragging) {
                    game.touchMove(game.toWorldX(px), game.toWorldY(py));
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (game.pressedId != null) {
                    game.releaseUi(nx, ny);
                }
                game.touchUp(game.toWorldX(px), game.toWorldY(py));
                return true;
            case MotionEvent.ACTION_CANCEL:
                game.cancelUiPress();
                game.touchUp(game.toWorldX(px), game.toWorldY(py));
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }
}
