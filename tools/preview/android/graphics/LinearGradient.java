package android.graphics;

import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

/** Desktop stand-in for LinearGradient. */
public class LinearGradient extends Shader {

    private final float x0, y0, x1, y1;
    private final int[] colors;
    private final float[] positions;

    public LinearGradient(float x0, float y0, float x1, float y1,
                          int[] colors, float[] positions, TileMode tile) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.colors = colors.clone();
        this.positions = positions == null ? null : positions.clone();
    }

    public LinearGradient(float x0, float y0, float x1, float y1,
                          int color0, int color1, TileMode tile) {
        this(x0, y0, x1, y1, new int[]{color0, color1}, new float[]{0f, 1f}, tile);
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
        // Java2D needs strictly increasing fractions starting at 0.
        frac = frac.clone();
        frac[0] = 0f;
        if (frac.length > 1) {
            frac[frac.length - 1] = 1f;
        }
        java.awt.Color[] cc = toColors(Shader.scaleAlpha(colors, alphaMul));
        if (x0 == x1 && y0 == y1) {
            return cc[0];
        }
        return new java.awt.LinearGradientPaint(new Point2D.Float(x0, y0),
                new Point2D.Float(x1, y1), frac, cc,
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    static java.awt.Color[] toColors(int[] argb) {
        java.awt.Color[] out = new java.awt.Color[argb.length];
        for (int i = 0; i < argb.length; i++) {
            out[i] = new java.awt.Color(argb[i], true);
        }
        return out;
    }
}
