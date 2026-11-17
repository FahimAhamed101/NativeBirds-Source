"""Build launcher icons from the red bird sprite."""
import os
from PIL import Image, ImageDraw

SRC = r"C:\Users\Admin\Desktop\Angry-Birds\NativeBirds\assets\sprites\bird_red_idle.png"
RES = r"C:\Users\Admin\Desktop\Angry-Birds\NativeBirds\res"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

bird = Image.open(SRC).convert("RGBA")

for dens, n in SIZES.items():
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))

    # vertical green gradient clipped to a rounded square
    grad = Image.new("RGBA", (n, n))
    gd = ImageDraw.Draw(grad)
    for y in range(n):
        f = y / max(1, n - 1)
        r = int(0x8B + (0x4C - 0x8B) * f)
        g = int(0xD4 + (0x7A - 0xD4) * f)
        b = int(0x3A + (0x2A - 0x3A) * f)
        gd.line([(0, y), (n, y)], fill=(r, g, b, 255))

    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.24), fill=255)
    out.paste(grad, (0, 0), mask)

    # inner highlight
    hi = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    ImageDraw.Draw(hi).rounded_rectangle(
        [int(n * 0.06), int(n * 0.06), int(n * 0.94), int(n * 0.5)],
        radius=int(n * 0.2), fill=(255, 255, 255, 38))
    out.alpha_composite(hi)

    bs = int(n * 0.74)
    b = bird.copy()
    b.thumbnail((bs, bs), Image.LANCZOS)
    out.alpha_composite(b, ((n - b.width) // 2, int(n * 0.52) - b.height // 2))

    d = os.path.join(RES, "mipmap-" + dens)
    os.makedirs(d, exist_ok=True)
    out.save(os.path.join(d, "ic_launcher.png"), optimize=True)

print("icons:", ", ".join("%s=%d" % (k, v) for k, v in SIZES.items()))
