package android.graphics;

/** Desktop stand-in for RectF. */
public class RectF {

    public float left, top, right, bottom;

    public RectF() {
    }

    public RectF(float l, float t, float r, float b) {
        set(l, t, r, b);
    }

    public void set(float l, float t, float r, float b) {
        left = l;
        top = t;
        right = r;
        bottom = b;
    }

    public void set(RectF o) {
        set(o.left, o.top, o.right, o.bottom);
    }

    public float width() {
        return right - left;
    }

    public float height() {
        return bottom - top;
    }

    public float centerX() {
        return (left + right) * 0.5f;
    }

    public float centerY() {
        return (top + bottom) * 0.5f;
    }
}
