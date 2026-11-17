package com.nativebirds.game;

/** Audio sink, implemented on Android with SoundPool (no-op in headless tests). */
public interface Sfx {
    void play(String name, float volume, float rate);
}
