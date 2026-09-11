#!/usr/bin/env python3
"""Render launcher icons for Towfik Music from art/icon_source.png.

Legacy (pre-API 26) launchers need bitmaps in mipmap-<dpi>. This script derives
every density from the single 1024px source artwork so the icon stays in sync.
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "art" / "icon_source.png"
RES = ROOT / "app" / "src" / "main" / "res"

LEGACY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def rounded_square(img: Image.Image, size: int, radius_ratio: float = 0.22) -> Image.Image:
    """Resize to `size` and clip to a rounded square (legacy launcher shape)."""
    art = img.convert("RGBA").resize((size * 4, size * 4), Image.LANCZOS)
    art = art.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * radius_ratio), fill=255
    )
    mask = mask.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(art, (0, 0), mask)
    return out


def main() -> None:
    src = Image.open(SRC).convert("RGBA")
    print(f"source: {src.size}")

    for dpi, size in LEGACY.items():
        target = RES / f"mipmap-{dpi}"
        target.mkdir(parents=True, exist_ok=True)
        icon = rounded_square(src, size)
        icon.save(target / "ic_launcher.png")
        icon.save(target / "ic_launcher_round.png")
        print(f"mipmap-{dpi}: {size}x{size} -> ic_launcher.png + ic_launcher_round.png")

    # Verify every written file really is the size we asked for.
    for dpi, size in LEGACY.items():
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            path = RES / f"mipmap-{dpi}" / name
            actual = Image.open(path).size
            assert actual == (size, size), f"{path} is {actual}, expected {(size, size)}"
    print("verified: all densities match their expected pixel size")


if __name__ == "__main__":
    main()
