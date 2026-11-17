package android.content.res;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Desktop stand-in for AssetManager, rooted at the project's assets folder. */
public class AssetManager {

    private final File root;

    public AssetManager(File root) {
        this.root = root;
    }

    public InputStream open(String path) throws IOException {
        File f = new File(root, path.replace('/', File.separatorChar));
        if (!f.isFile()) {
            throw new IOException("missing asset: " + path);
        }
        return new FileInputStream(f);
    }
}
