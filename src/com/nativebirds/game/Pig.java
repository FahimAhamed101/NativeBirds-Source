package com.nativebirds.game;

/** A pig target. */
public class Pig extends Entity {
    public static final int SMALL = 0;
    public static final int MEDIUM = 1;
    public static final int BIG = 2;

    public int size = MEDIUM;
    public float radius = 1.2f;
    public float hp, maxHp;
    public boolean helmet;
    public float blinkTimer;
    public boolean blinking;
    public float flash;

    public String spriteName() {
        String base;
        if (helmet) {
            base = "pig_helmet";
        } else {
            base = size == SMALL ? "pig_small" : size == BIG ? "pig_big" : "pig_medium";
        }
        return blinking ? base + "_blink" : base;
    }
}
