package com.nativebirds.game;

/** A slingshot bird. */
public class Bird extends Entity {
    public static final int RED = 0;
    public static final int YELLOW = 1;
    public static final int BLACK = 2;
    public static final int BLUE = 3;
    public static final int WHITE = 4;

    public int type = RED;
    public float radius = 1f;

    public boolean launched;
    public boolean abilityUsed;
    public boolean splitChild;
    public float airborneTime;
    public float spin;
    public float fuse = -1f;
    public boolean exploding;

    public static String name(int type) {
        switch (type) {
            case YELLOW:
                return "yellow";
            case BLACK:
                return "black";
            case BLUE:
                return "blue";
            case WHITE:
                return "white";
            default:
                return "red";
        }
    }

    /**
     * Visual + collision radius, in world units.
     *
     * <p>Sized so a bird fits inside the slingshot fork (whose opening is fixed by
     * the artwork at {@code 0.242 * slingshotHeight}). A 1.0-unit bird would need a
     * fork 11 units tall, i.e. taller than the screen, so the birds are kept small
     * and their <em>density</em> is raised instead: mass is unchanged, only the
     * silhouette shrinks.
     */
    public static float radiusFor(int type) {
        switch (type) {
            case YELLOW:
                return 0.60f;
            case BLACK:
                return 0.66f;
            case BLUE:
                return 0.36f;
            case WHITE:
                return 0.70f;
            default:
                return 0.62f;
        }
    }

    public String spriteName() {
        return "bird_" + name(type) + (launched && !abilityUsed ? "_fly" : "_idle");
    }
}
