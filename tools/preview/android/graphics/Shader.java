package android.graphics;

/** Desktop stand-in for Shader; converts to a Java2D paint. */
public abstract class Shader {

    public enum TileMode { CLAMP, REPEAT, MIRROR }

    /** Builds a Java2D paint. {@code alphaMul} scales every colour's alpha (paint alpha). */
    public abstract java.awt.Paint toAwt(float alphaMul);

    static int[] scaleAlpha(int[] colors, float mul) {
        int[] out = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            int c = colors[i];
            int a = Math.round(((c >>> 24) & 0xFF) * mul);
            a = Math.max(0, Math.min(255, a));
            out[i] = (a << 24) | (c & 0x00FFFFFF);
        }
        return out;
    }
}
