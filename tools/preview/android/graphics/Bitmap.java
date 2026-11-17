package android.graphics;

import java.awt.image.BufferedImage;

/** Desktop stand-in for android.graphics.Bitmap backed by a BufferedImage. */
public class Bitmap {

    public enum Config { ARGB_8888, RGB_565 }

    public final BufferedImage image;

    private Bitmap(BufferedImage image) {
        this.image = image;
    }

    public static Bitmap wrap(BufferedImage image) {
        return new Bitmap(image);
    }

    public static Bitmap createBitmap(int w, int h, Config config) {
        return new Bitmap(new BufferedImage(Math.max(1, w), Math.max(1, h),
                BufferedImage.TYPE_INT_ARGB));
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }
}
