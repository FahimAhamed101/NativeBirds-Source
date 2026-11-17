package com.nativebirds.game;

/** Tiny key/value store, implemented on Android with SharedPreferences. */
public interface Store {
    int getInt(String key, int def);

    void putInt(String key, int value);
}
