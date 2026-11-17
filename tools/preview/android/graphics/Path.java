package android.graphics;

import java.awt.geom.Path2D;

/** Desktop stand-in for Path (float precision, winding fill). */
public class Path {

    public final Path2D.Float p = new Path2D.Float(Path2D.WIND_NON_ZERO);

    public void reset() {
        p.reset();
    }

    public void moveTo(float x, float y) {
        p.moveTo(x, y);
    }

    public void lineTo(float x, float y) {
        p.lineTo(x, y);
    }

    public void quadTo(float x1, float y1, float x2, float y2) {
        p.quadTo(x1, y1, x2, y2);
    }

    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        p.curveTo(x1, y1, x2, y2, x3, y3);
    }

    public void close() {
        p.closePath();
    }
}
