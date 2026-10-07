"""Generates the simple 16x16 placeholder textures for the R0 bombs.

Run from the repo root:  python3 tools/make_textures.py
The kids are welcome to redraw these in any pixel editor later.
"""
from pathlib import Path

from PIL import Image

OUT = Path("src/main/resources/assets/tafelboem/textures/block")

RED = (200, 40, 40)
DARK_RED = (140, 20, 20)
WHITE = (240, 240, 240)
BLACK = (25, 25, 25)
YELLOW = (250, 210, 40)
ORANGE = (240, 140, 30)
BROWN = (110, 70, 40)
GREY = (90, 90, 90)
FEATHER = (250, 250, 245)
FEATHER_SHADE = (215, 215, 205)
COMB = (220, 30, 40)


def blank(color):
    return Image.new("RGBA", (16, 16), color + (255,))


def put(img, pixels, color):
    for x, y in pixels:
        img.putpixel((x, y), color + (255,))


def cross(cx, cy, size):
    """An X (the times sign) centred on (cx, cy)."""
    pixels = []
    for i in range(-size, size + 1):
        pixels += [(cx + i, cy + i), (cx + i, cy - i)]
    return pixels


def ring_top(base, rim):
    img = blank(base)
    for i in range(16):
        put(img, [(i, 0), (i, 15), (0, i), (15, i)], rim)
    return img


def fuse_top(base, rim):
    img = ring_top(base, rim)
    put(img, [(7, 7), (8, 7), (7, 8), (8, 8)], BLACK)
    put(img, [(8, 6), (9, 5)], GREY)
    return img


def knalletje():
    side = blank(RED)
    for x in range(16):
        for y in range(16):
            if (x // 3) % 2 == 1:
                put(side, [(x, y)], YELLOW)
    put(side, [(x, y) for x in range(16) for y in (0, 15)], DARK_RED)
    top = fuse_top(RED, DARK_RED)
    bottom = blank(BROWN)
    return side, top, bottom


def gewone_tnt():
    side = blank(RED)
    for x in range(16):
        for y in range(5, 11):
            put(side, [(x, y)], WHITE)
    put(side, cross(7, 7, 2) + cross(8, 7, 2), BLACK)
    put(side, [(x, y) for x in range(16) for y in (0, 15)], DARK_RED)
    top = fuse_top(RED, DARK_RED)
    bottom = blank(DARK_RED)
    return side, top, bottom


def kippenbom():
    side = blank(FEATHER)
    for x in range(16):
        for y in range(16):
            if (x + 2 * y) % 5 == 0:
                put(side, [(x, y)], FEATHER_SHADE)
    put(side, [(5, 6), (6, 6), (9, 6), (10, 6)], BLACK)  # eyes
    put(side, [(7, 8), (8, 8), (7, 9), (8, 9), (7, 10)], ORANGE)  # beak
    put(side, [(7, 11), (8, 11)], COMB)  # wattle
    put(side, cross(13, 13, 1), RED)  # little times sign
    top = blank(FEATHER)
    put(top, [(6, y) for y in range(4, 12)] + [(7, y) for y in range(3, 12)] + [(8, y) for y in range(4, 12)], COMB)
    bottom = blank(FEATHER_SHADE)
    return side, top, bottom


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, make in (("knalletje", knalletje), ("gewone_tnt", gewone_tnt), ("kippenbom", kippenbom)):
        side, top, bottom = make()
        side.save(OUT / f"{name}_side.png")
        top.save(OUT / f"{name}_top.png")
        bottom.save(OUT / f"{name}_bottom.png")
        print("wrote", name)


if __name__ == "__main__":
    main()


# --- Graaf Fout -------------------------------------------------------------------------------
# Usage: python3 tools/make_textures.py --graaf-fout <path to vanilla evoker.png>
# The skin is the vanilla evoker texture recoloured: purple robes and a big red times sign.

ENTITY_OUT = Path("src/main/resources/assets/tafelboem/textures/entity")
ITEM_OUT = Path("src/main/resources/assets/tafelboem/textures/item")
PURPLE = (120, 40, 160)
GOLD = (230, 190, 60)

# UV regions (x0, y0, x1, y1) of the illager texture that are clothing, not skin.
CLOTHING = [(16, 20, 40, 38), (0, 38, 28, 64), (40, 20, 64, 48)]
CHEST_FRONT = (22, 26)  # top-left of the 8x12 body front
ROBE_FRONT = (6, 44)  # top-left of the 8x20 robe front


def in_clothing(x, y):
    return any(x0 <= x < x1 and y0 <= y < y1 for x0, y0, x1, y1 in CLOTHING)


def graaf_fout(evoker_path):
    src = Image.open(evoker_path).convert("RGBA")
    out = src.copy()
    for x in range(64):
        for y in range(64):
            r, g, b, a = src.getpixel((x, y))
            if a == 0 or not in_clothing(x, y):
                continue
            light = (r + g + b) / (3 * 255)
            if r > 150 and g > 120 and b < 90:  # keep the gold trim, just a little warmer
                out.putpixel((x, y), GOLD + (a,))
            else:
                shade = 0.45 + light * 0.9
                out.putpixel((x, y), tuple(min(255, int(c * shade)) for c in PURPLE) + (a,))
    for ox, oy in (CHEST_FRONT, ROBE_FRONT):
        put(out, [(ox + 1 + px, oy + 2 + py) for px, py in cross(3, 3, 3)], RED)
    ENTITY_OUT.mkdir(parents=True, exist_ok=True)
    out.save(ENTITY_OUT / "graaf_fout.png")

    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 49:
                icon.putpixel((x, y), PURPLE + (255,))
    put(icon, cross(7, 7, 3) + cross(8, 7, 3), RED)
    ITEM_OUT.mkdir(parents=True, exist_ok=True)
    icon.save(ITEM_OUT / "summon_graaf_fout.png")
    print("wrote graaf_fout")


if __name__ == "__main__":
    import sys

    if len(sys.argv) == 3 and sys.argv[1] == "--graaf-fout":
        graaf_fout(sys.argv[2])
