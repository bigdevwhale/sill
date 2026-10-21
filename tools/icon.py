"""Draws the README logo, the launcher icon (a terracotta pot with a sprout on a morning sill), as docs/logo.png.

python tools/icon.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
S = 1024
K = 4  # supersampling


def main():
    n = S * K
    u = n / 108
    im = Image.new("RGBA", (n, n), (0xF1, 0xE9, 0xDA, 255))
    d = ImageDraw.Draw(im)

    def rect(x0, y0, x1, y1, fill):
        d.rectangle([x0 * u, y0 * u, x1 * u, y1 * u], fill=fill)

    def poly(points, fill):
        d.polygon([(x * u, y * u) for x, y in points], fill=fill)

    rect(20, 8, 88, 66, (0xF7, 0xF3, 0xEA))
    for x0, y0, x1, y1 in ((24, 12, 52, 32), (56, 12, 84, 32), (24, 36, 52, 64), (56, 36, 84, 64)):
        rect(x0, y0, x1, y1, (0x9C, 0xC9, 0xE6))
    poly([(24, 52), (38, 47), (52, 50), (52, 64), (24, 64)], (0x9D, 0xBB, 0x8C))
    poly([(56, 50), (70, 46), (84, 50), (84, 64), (56, 64)], (0x9D, 0xBB, 0x8C))
    rect(0, 64, 108, 73, (0xED, 0xE6, 0xD8))
    rect(0, 73, 108, 78, (0xD9, 0xCF, 0xBD))
    d.ellipse([41 * u, 83.5 * u, 67 * u, 87.5 * u], fill=(0xC9, 0xBF, 0xAE))
    poly([(40.5, 64), (67.5, 64), (64.5, 85), (43.5, 85)], (0xBF, 0x66, 0x40))
    poly([(58, 64), (67.5, 64), (64.5, 85), (56.1, 85)], (0xA9, 0x55, 0x2F))
    rect(38, 57.5, 70, 64.5, (0xD0, 0x7A, 0x52))
    rect(62, 57.5, 70, 64.5, (0xB8, 0x5E, 0x39))
    d.ellipse([40 * u, 55.2 * u, 68 * u, 60.4 * u], fill=(0x4A, 0x34, 0x26))
    d.line([(54 * u, 57.5 * u), (54 * u, 38 * u)], fill=(0x4C, 0x7A, 0x35), width=int(2.4 * u))
    poly([(53.6, 48.5), (47, 47.5), (41.5, 44), (39, 39.5), (45.5, 39), (51, 42.5)], (0x5E, 0x8F, 0x40))
    poly([(54.2, 44), (59, 42), (63.5, 37.5), (66, 31.5), (60, 32.5), (55.5, 37)], (0x6F, 0xA3, 0x4A))
    poly([(54, 38.5), (52.2, 34.5), (53, 30.5), (55, 28), (56.6, 32), (55.8, 35.8)], (0x7D, 0xB2, 0x57))

    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.22), fill=255)
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    out = out.resize((S, S), Image.LANCZOS)
    (ROOT / "docs").mkdir(exist_ok=True)
    out.save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()
