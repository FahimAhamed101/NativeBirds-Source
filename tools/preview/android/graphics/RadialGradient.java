package android.graphics;

import java.awt.MultipleGradientPaint;

/** Desktop stand-in for RadialGradient. */
public class RadialGradient extends Shader {

    private final float cx, cy, radius;
    private final int[] colors;
    private final float[] positions;

    public RadialGradient(float cx, float cy, float radius,
                          int[] colors, float[] positions, TileMode tile) {
        this.cx = cx;
        this.cy = cy;
        this.radius = Math.max(0.01f, radius);
        this.colors = colors.clone();
        this.positions = positions == null ? null : positions.clone();
    }

    public RadialGradient(float cx, float cy, float radius, int color0, int color1, TileMode tile) {
        this(cx, cy, radius, new int[]{color0, color1}, new float[]{0f, 1f}, tile);
    }

    @Override
    public java.awt.Paint toAwt(float alphaMul) {
        float[] frac = positions;
        if (frac == null) {
            frac = new float[colors.length];
            for (int i = 0; i < frac.length; i++) {
                frac[i] = frac.length == 1 ? 0f : i / (float) (frac.length - 1);
            }
        }
        frac = frac.clone();
        frac[0] = 0f;
        if (frac.length > 1) {
            frac[frac.length - 1] = 1f;
        }
        java.awt.Color[] cc = LinearGradient.toColors(Shader.scaleAlpha(colors, alphaMul));
        return new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Float(cx, cy), radius, frac, cc,
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }
}
