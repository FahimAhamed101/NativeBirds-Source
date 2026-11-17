"""Insert classes.dex into the APK produced by aapt2 (no zip CLI on this machine)."""
import sys
import zipfile


def main():
    apk, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
    with zipfile.ZipFile(apk, "r") as zin:
        items = [(i, zin.read(i.filename)) for i in zin.infolist()]
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zout:
        for info, data in items:
            if info.filename == "classes.dex":
                continue
            zout.writestr(info, data)
        with open(dex, "rb") as f:
            data = f.read()
        info = zipfile.ZipInfo("classes.dex")
        info.compress_type = zipfile.ZIP_DEFLATED
        zout.writestr(info, data)
    print("packed %d entries + classes.dex (%.1f KB)" % (len(items), len(data) / 1024.0))


main()
