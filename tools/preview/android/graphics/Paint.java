package android.graphics;

/** Desktop stand-in for Paint: carries the attributes Renderer sets. */
public class Paint {

    public static final int ANTI_ALIAS_FLAG = 0x01;
    public static final int FILTER_BITMAP_FLAG = 0x02;

    public enum Style { FILL, STROKE, FILL_AND_STROKE }

    public enum Cap { BUTT, ROUND, SQUARE }

    public enum Align { LEFT, CENTER, RIGHT }

    private int color = 0xFF000000;
    private int alpha = 255;
    private Style style = Style.FILL;
    private Cap cap = Cap.BUTT;
    private float strokeWidth = 0f;
    private float textSize = 12f;
    private Align align = Align.LEFT;
    private Typeface typeface = null;
    private Shader shader = null;

    public Paint() {
    }

    public Paint(int flags) {
    }

    public void setColor(int c) {
        color = c;
        alpha = (c >>> 24) & 0xFF;
    }

    public int getColor() {
        return color;
    }

    /** Android's setAlpha replaces the alpha channel of the current colour. */
    public void setAlpha(int a) {
        alpha = a & 0xFF;
        color = (color & 0x00FFFFFF) | (alpha << 24);
    }

    public int getAlpha() {
        return alpha;
    }

    public void setStyle(Style s) {
        style = s;
    }

    public Style getStyle() {
        return style;
    }

    public void setStrokeWidth(float w) {
        strokeWidth = w;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeCap(Cap c) {
        cap = c;
    }

    public Cap getStrokeCap() {
        return cap;
    }

    public void setTextSize(float s) {
        textSize = s;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextAlign(Align a) {
        align = a;
    }

    public Align getTextAlign() {
        return align;
    }

    public void setTypeface(Typeface tf) {
        typeface = tf;
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public void setShader(Shader s) {
        shader = s;
    }

    public Shader getShader() {
        return shader;
    }

    /** Effective ARGB, alpha taken from the current alpha field. */
    public int effectiveColor() {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
