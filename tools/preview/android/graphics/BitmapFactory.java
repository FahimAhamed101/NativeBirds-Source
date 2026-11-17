package android.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/** Desktop stand-in for BitmapFactory. */
public class BitmapFactory {

    public static class Options {
        public boolean inScaled = true;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    public static Bitmap decodeStream(InputStream in, Object padding, Options opts) {
        if (in == null) {
            return null;
        }
        try {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                return null;
            }
            if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                BufferedImage conv = new BufferedImage(img.getWidth(), img.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                conv.getGraphics().drawImage(img, 0, 0, null);
                img = conv;
            }
            return Bitmap.wrap(img);
        } catch (IOException e) {
            return null;
        }
    }
}
