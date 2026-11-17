package android.graphics;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

/** Desktop stand-in for Typeface. */
public class Typeface {

    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;

    public final Font font;

    private Typeface(Font font) {
        this.font = font;
    }

    public static Typeface create(String family, int style) {
        boolean bold = (style & BOLD) != 0;
        boolean italic = (style & ITALIC) != 0;
        Map<TextAttribute, Object> attrs = new HashMap<TextAttribute, Object>();
        attrs.put(TextAttribute.FAMILY, "SansSerif");
        attrs.put(TextAttribute.WEIGHT, bold ? TextAttribute.WEIGHT_BOLD
                : TextAttribute.WEIGHT_REGULAR);
        attrs.put(TextAttribute.POSTURE, italic ? TextAttribute.POSTURE_OBLIQUE
                : TextAttribute.POSTURE_REGULAR);
        return new Typeface(new Font(attrs));
    }

    public static final Typeface DEFAULT = create("sans-serif", NORMAL);
    public static final Typeface DEFAULT_BOLD = create("sans-serif", BOLD);
}
