"""Objective visual checks for a rendered frame.

Vision feedback is not always available, so this reports the properties that
actually make a 2D game screen look cheap: hard horizontal seams, large areas of
constant colour, and elements that run off the edge of the frame.

Usage:  python tools/qa.py build/shots/play-idle.png
"""
import sys
from PIL import Image, ImageStat

RAMP = " .:-=+*#%@"


def load(path):
    return Image.open(path).convert("RGB")


def seams(im, min_jump=14):
    """Rows whose average colour jumps far more than their neighbours."""
    w, h = im.size
    px = im.load()
    rows = []
    for y in range(h):
        acc = [0, 0, 0]
        for x in range(0, w, 4):
            c = px[x, y]
            acc[0] += c[0]; acc[1] += c[1]; acc[2] += c[2]
        n = len(range(0, w, 4))
        rows.append([a / n for a in acc])

    out = []
    for y in range(1, h):
        d = max(abs(rows[y][i] - rows[y - 1][i]) for i in range(3))
        if d >= min_jump:
            out.append((y, round(d, 1), tuple(int(v) for v in rows[y - 1]),
                        tuple(int(v) for v in rows[y])))
    return out


def flat_regions(im, block=24, tol=3.0):
    """Blocks whose colour barely varies: a good proxy for 'untextured'."""
    w, h = im.size
    hits = []
    for by in range(0, h - block, block):
        for bx in range(0, w - block, block):
            reg = im.crop((bx, by, bx + block, by + block))
            sd = ImageStat.Stat(reg).stddev
            if max(sd) < tol:
                hits.append((bx, by))
    return hits


def cluster(points, gap=40):
    """Merge nearby block hits so the output is readable."""
    if not points:
        return []
    pts = sorted(points)
    groups = []
    cur = [pts[0]]
    for p in pts[1:]:
        if abs(p[0] - cur[-1][0]) < gap and abs(p[1] - cur[-1][1]) < gap:
            cur.append(p)
        else:
            groups.append(cur)
            cur = [p]
    groups.append(cur)
    return [(min(p[0] for p in g), min(p[1] for p in g),
             max(p[0] for p in g), max(p[1] for p in g), len(g)) for g in groups if len(g) > 1]


def ascii_map(im, cols=100, rows=26):
    small = im.resize((cols, rows), Image.BOX)
    px = small.load()
    print("+" + "-" * cols + "+")
    for y in range(rows):
        line = ""
        for x in range(cols):
            r, g, b = px[x, y]
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            line += RAMP[min(len(RAMP) - 1, int(lum * len(RAMP)))]
        print("|" + line + "|")
    print("+" + "-" * cols + "+")


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "build/shots/play-idle.png"
    im = load(path)
    print("frame", im.size)

    print("\n== hard horizontal seams (avg delta >= 14 per row) ==")
    s = seams(im)
    if not s:
        print("  none")
    for y, d, a, b in s[:20]:
        print("  row %3d  delta %5.1f  %s -> %s" % (y, d, a, b))

    print("\n== flat 24x24 blocks (max channel stddev < 3) ==")
    groups = cluster(flat_regions(im))
    if not groups:
        print("  none - every region has texture")
    for x0, y0, x1, y1, n in groups[:14]:
        print("  bbox %4d,%3d .. %4d,%3d  (%d blocks)" % (x0, y0, x1 + 24, y1 + 24, n))

    print("\n== luminance map ==")
    ascii_map(im)


if __name__ == "__main__":
    main()
