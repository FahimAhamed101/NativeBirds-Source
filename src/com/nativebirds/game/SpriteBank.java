package com.nativebirds.game;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Lazily loads and caches PNG sprites from assets/sprites. */
public class SpriteBank {
    private final AssetManager assets;
    private final Map<String, Bitmap> cache = new HashMap<String, Bitmap>();
    private final BitmapFactory.Options opts = new BitmapFactory.Options();

    public SpriteBank(Context context) {
        this.assets = context.getAssets();
        opts.inScaled = false;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    public Bitmap get(String name) {
        Bitmap b = cache.get(name);
        if (b != null) {
            return b;
        }
        InputStream in = null;
        try {
            in = assets.open("sprites/" + name + ".png");
            b = BitmapFactory.decodeStream(in, null, opts);
        } catch (IOException e) {
            b = null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (b == null) {
            b = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        }
        cache.put(name, b);
        return b;
    }
}
