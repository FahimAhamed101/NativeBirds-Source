package android.graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Desktop stand-in for android.graphics.Canvas, implemented on Java2D.
 * Enough of the API surface for Renderer to draw real frames off-device.
 */
public class Canvas {

    private final Graphics2D g;
    private final Deque<AffineTransform> stack = new ArrayDeque<AffineTransform>();
    private final Map<String, Font> fontCache = new HashMap<String, Font>();
    private final FontRenderContext frc =
            new FontRenderContext(null, true, true);

    public Canvas(Bitmap bitmap) {
        BufferedImage img = bitmap.image;
        g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
    }

    public int save() {
        stack.push(g.getTransform());
        return stack.size();
    }

    public void restore() {
        if (!stack.isEmpty()) {
            g.setTransform(stack.pop());
        }
    }

    public void translate(float dx, float dy) {
        g.translate(dx, dy);
    }

    public void rotate(float degrees) {
        g.rotate(Math.toRadians(degrees));
    }

    public void scale(float sx, float sy) {
        g.scale(sx, sy);
    }

    // ------------------------------------------------------------------ shapes

    public void drawRect(float l, float t, float r, float b, Paint p) {
        draw(new Rectangle2D.Float(Math.min(l, r), Math.min(t, b),
                Math.abs(r - l), Math.abs(b - t)), p);
    }

    public void drawRect(RectF rc, Paint p) {
        drawRect(rc.left, rc.top, rc.right, rc.bottom, p);
    }

    public void drawRoundRect(RectF rc, float rx, float ry, Paint p) {
        float w = Math.abs(rc.width());
        float h = Math.abs(rc.height());
        float ax = Math.min(Math.abs(rx), w / 2f);
        float ay = Math.min(Math.abs(ry), h / 2f);
        draw(new RoundRectangle2D.Float(Math.min(rc.left, rc.right),
                Math.min(rc.top, rc.bottom), w, h, ax * 2f, ay * 2f), p);
    }

    public void drawCircle(float cx, float cy, float r, Paint p) {
        draw(new Ellipse2D.Float(cx - r, cy - r, r * 2f, r * 2f), p);
    }

    public void drawOval(RectF rc, Paint p) {
        draw(new Ellipse2D.Float(Math.min(rc.left, rc.right), Math.min(rc.top, rc.bottom),
                Math.abs(rc.width()), Math.abs(rc.height())), p);
    }

    public void drawLine(float x1, float y1, float x2, float y2, Paint p) {
        applyPaint(p);
        g.draw(new Line2D.Float(x1, y1, x2, y2));
    }

    public void drawPath(Path path, Paint p) {
        draw(path.p, p);
    }

    private void draw(Shape s, Paint p) {
        applyPaint(p);
        g.draw(s);
        if (p.getStyle() != Paint.Style.STROKE) {
            g.fill(s);
        }
    }

    // ----------------------------------------------------------------- bitmaps

    public void drawBitmap(Bitmap b, Rect src, RectF dst, Paint p) {
        if (b == null) {
            return;
        }
        Composite old = g.getComposite();
        int alpha = p == null ? 255 : p.getAlpha();
        if (alpha < 255) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    Math.max(0f, alpha / 255f)));
        }
        BufferedImage img = b.image;
        float l = Math.min(dst.left, dst.right);
        float t = Math.min(dst.top, dst.bottom);
        float w = Math.abs(dst.width());
        float h = Math.abs(dst.height());
        int sx = src == null ? 0 : (int) src.left;
        int sy = src == null ? 0 : (int) src.top;
        int sw = src == null ? img.getWidth() : (int) src.width();
        int sh = src == null ? img.getHeight() : (int) src.height();
        if (sw <= 0 || sh <= 0) {
            g.setComposite(old);
            return;
        }
        g.drawImage(img, Math.round(l), Math.round(t), Math.round(l + w), Math.round(t + h),
                sx, sy, sx + sw, sy + sh, null);
        g.setComposite(old);
    }

    public void drawBitmap(Bitmap b, float left, float top, Paint p) {
        RectF r = new RectF(left, top, left + b.getWidth(), top + b.getHeight());
        drawBitmap(b, null, r, p);
    }

    // -------------------------------------------------------------------- text

    public void drawText(String s, float x, float y, Paint p) {
        if (s == null || s.isEmpty()) {
            return;
        }
        Font f = font(p);
        GlyphVector gv = f.createGlyphVector(frc, s);
        // Android centres on the advance width, not the ink bounds.
        float adv = (float) f.getStringBounds(s, frc).getWidth();
        float dx = x;
        if (p.getTextAlign() == Paint.Align.CENTER) {
            dx = x - adv / 2f;
        } else if (p.getTextAlign() == Paint.Align.RIGHT) {
            dx = x - adv;
        }
        Shape outline = gv.getOutline(dx, y);
        applyPaint(p);
        if (p.getStyle() == Paint.Style.STROKE) {
            g.draw(outline);
        } else {
            g.fill(outline);
        }
    }

    private Font font(Paint p) {
        Typeface tf = p.getTypeface() == null ? Typeface.DEFAULT : p.getTypeface();
        String key = tf.font.getAttributes().hashCode() + "/" + p.getTextSize();
        Font f = fontCache.get(key);
        if (f == null) {
            f = tf.font.deriveFont(p.getTextSize());
            fontCache.put(key, f);
        }
        return f;
    }

    // ------------------------------------------------------------------- paint

    private void applyPaint(Paint p) {
        if (p.getShader() != null) {
            g.setPaint(p.getShader().toAwt(p.getAlpha() / 255f));
        } else {
            g.setPaint(new java.awt.Color(p.effectiveColor(), true));
        }
        if (p.getStyle() == Paint.Style.STROKE || p.getStyle() == Paint.Style.FILL_AND_STROKE) {
            int cap = p.getStrokeCap() == Paint.Cap.ROUND ? BasicStroke.CAP_ROUND
                    : p.getStrokeCap() == Paint.Cap.SQUARE ? BasicStroke.CAP_SQUARE
                    : BasicStroke.CAP_BUTT;
            g.setStroke(new BasicStroke(Math.max(0.01f, p.getStrokeWidth()), cap,
                    BasicStroke.JOIN_ROUND));
        } else {
            g.setStroke(new BasicStroke(1f));
        }
    }
}
