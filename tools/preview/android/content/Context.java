package android.content;

import android.content.res.AssetManager;

/** Desktop stand-in for Context: only getAssets() is needed by SpriteBank. */
public class Context {

    private final AssetManager assets;

    public Context(AssetManager assets) {
        this.assets = assets;
    }

    public AssetManager getAssets() {
        return assets;
    }
}
