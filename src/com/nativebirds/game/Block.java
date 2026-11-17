package com.nativebirds.game;

/** A destructible wooden / stone / ice block. */
public class Block extends Entity {
    public static final int WOOD = 0;
    public static final int STONE = 1;
    public static final int ICE = 2;

    public int material;
    public float w, h;
    public float hp, maxHp;
    /** 90 degrees when the sprite is stored in its horizontal orientation. */
    public float spriteAngle;
    public float flash;

    public String spriteName() {
        int lo = (int) Math.min(w, h);
        int hi = (int) Math.max(w, h);
        String mat = material == WOOD ? "wood" : material == STONE ? "stone" : "ice";
        float frac = maxHp > 0 ? hp / maxHp : 1f;
        int state = frac > 0.75f ? 1 : frac > 0.5f ? 2 : frac > 0.25f ? 3 : 4;
        return "blk_" + mat + "_" + hi + "x" + lo + "_" + state;
    }

    public float area() {
        return w * h;
    }
}
