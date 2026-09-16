#!/usr/bin/env python3
"""Build the bundled picture set from Microsoft Fluent Emoji.

Reads subjects.txt, downloads each subject's `Color` vector from the upstream
repository, renders it at full resolution, and writes a WebP plus a backdrop
colour into app/src/main/assets/pictures/.

The vectors are rendered here rather than on the phone on purpose: they are
Figma exports carrying SVG filters for their soft inner shading, and the SVG
renderer available on Android does not implement filters, so the art would
arrive flattened. Headless Chromium draws them correctly.

Run it by hand after editing subjects.txt; the output is committed, so the app
build itself never needs the network:

    python3 tools/pictures/build-pictures.py

Needs Pillow (`pip install Pillow`) and a Chromium binary (--chromium, or the
usual names on PATH).
"""

from __future__ import annotations

import argparse
import colorsys
import math
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parents[2]
TOOL_DIR = Path(__file__).resolve().parent
SUBJECTS = TOOL_DIR / "subjects.txt"
CACHE = TOOL_DIR / ".cache"
OUT_DIR = REPO / "app" / "src" / "main" / "assets" / "pictures"
INDEX = OUT_DIR / "index.txt"
SOURCES = TOOL_DIR / "SOURCES.md"

UPSTREAM = "https://github.com/microsoft/fluentui-emoji"
RAW = "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets"

# The canvas every picture is drawn on. 1280 is comfortably above the width of
# a 1440p phone screen, so the art is never upscaled in use.
CANVAS = 1280

# The art's longest edge inside that canvas. The margin keeps every subject the
# same visual size and stops anything touching the edge of the screen.
ART = 1120

# Lossy WebP with alpha. 85 is where the soft shading stops gaining anything
# visible from a higher setting.
WEBP_QUALITY = 85

# Pale enough to stay a background rather than a second subject.
BACKDROP_SATURATION = 0.20
BACKDROP_VALUE = 0.95

CHROMIUM_NAMES = [
    "/opt/pw-browsers/chromium-1194/chrome-linux/chrome",
    "chromium",
    "chromium-browser",
    "google-chrome",
]


def find_chromium(override: str | None) -> str:
    for name in ([override] if override else []) + CHROMIUM_NAMES:
        found = shutil.which(name) or (name if Path(name).is_file() else None)
        if found:
            return found
    sys.exit("No Chromium found. Pass --chromium /path/to/chrome.")


def read_subjects() -> list[str]:
    names = []
    for line in SUBJECTS.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            names.append(line)
    duplicates = {n for n in names if names.count(n) > 1}
    if duplicates:
        sys.exit(f"Duplicate subjects in subjects.txt: {', '.join(sorted(duplicates))}")
    return names


def slug_of(name: str) -> str:
    return name.lower().replace(" ", "_").replace("-", "_")


def svg_url(name: str) -> str:
    return f"{RAW}/{urllib.parse.quote(name)}/Color/{slug_of(name)}_color.svg"


def fetch(name: str) -> Path:
    """Download a subject's vector, cached so re-runs cost nothing."""
    path = CACHE / f"{slug_of(name)}.svg"
    if path.exists() and path.stat().st_size > 0:
        return path
    CACHE.mkdir(parents=True, exist_ok=True)
    try:
        with urllib.request.urlopen(svg_url(name), timeout=60) as response:
            path.write_bytes(response.read())
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"{name}: {error.code} for {svg_url(name)}") from error
    return path


def render(chromium: str, svg: Path, out: Path) -> None:
    """Rasterise one vector on transparency, at CANVAS x CANVAS."""
    with tempfile.TemporaryDirectory() as work:
        page = Path(work) / "page.html"
        page.write_text(
            "<html><style>html,body{margin:0;padding:0;background:transparent}"
            f"img{{display:block;width:{CANVAS}px;height:{CANVAS}px}}</style>"
            f'<body><img src="{svg.resolve().as_uri()}"></body></html>',
            encoding="utf-8",
        )
        subprocess.run(
            [
                chromium,
                "--headless=new",
                "--no-sandbox",
                "--disable-gpu",
                "--hide-scrollbars",
                "--force-device-scale-factor=1",
                "--default-background-color=00000000",
                f"--user-data-dir={work}/profile",
                f"--window-size={CANVAS},{CANVAS}",
                f"--screenshot={out}",
                page.as_uri(),
            ],
            check=True,
            capture_output=True,
            timeout=120,
        )
    if not out.exists():
        raise RuntimeError(f"Chromium wrote nothing for {svg.name}")


def backdrop_of(art: Image.Image) -> str:
    """A pale backdrop for the subject to sit on, opposite it on the colour wheel.

    The subject's own hue, paled out, was the first thing tried and it washes
    out exactly the pictures that need help: a pink pig on pink, a white sheep
    on lilac. The complement keeps the same soft, unsaturated feel while giving
    every subject a clear edge against it.

    Hues are averaged as angles -- a plain mean would put a red subject, whose
    hues straddle 0, somewhere around cyan.
    """
    small = art.resize((64, 64), Image.LANCZOS).convert("RGBA")
    pixels = small.tobytes()
    x = y = weight = 0.0
    saturations = []
    for i in range(0, len(pixels), 4):
        r, g, b, a = pixels[i], pixels[i + 1], pixels[i + 2], pixels[i + 3]
        if a < 128:
            continue
        h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
        saturations.append(s)
        # Weight by how colourful and how bright the pixel is, so highlights and
        # near-grey shading do not drag the hue around.
        w = s * v
        x += math.cos(2 * math.pi * h) * w
        y += math.sin(2 * math.pi * h) * w
        weight += w

    mean_saturation = sum(saturations) / len(saturations) if saturations else 0.0
    if weight <= 0.0 or mean_saturation < 0.12:
        # A grey or white subject (a snowman, a cloud) has no hue worth reading,
        # so give it a fixed one and let the complement below turn it pale blue.
        hue = 35 / 360
    else:
        hue = (math.atan2(y, x) / (2 * math.pi)) % 1.0

    r, g, b = colorsys.hsv_to_rgb((hue + 0.5) % 1.0, BACKDROP_SATURATION, BACKDROP_VALUE)
    return f"{round(r * 255):02X}{round(g * 255):02X}{round(b * 255):02X}"


def compose(raw_png: Path, out_webp: Path) -> str:
    """Trim, centre and encode one rendered subject. Returns its backdrop."""
    image = Image.open(raw_png).convert("RGBA")
    box = image.getbbox()
    if box is None:
        raise RuntimeError(f"{raw_png.name} rendered empty")
    art = image.crop(box)

    scale = ART / max(art.size)
    art = art.resize(
        (max(1, round(art.width * scale)), max(1, round(art.height * scale))),
        Image.LANCZOS,
    )

    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    canvas.paste(art, ((CANVAS - art.width) // 2, (CANVAS - art.height) // 2))
    canvas.save(out_webp, "WEBP", quality=WEBP_QUALITY, method=6)
    return backdrop_of(art)


def build_one(chromium: str, name: str, work: Path) -> tuple[str, str]:
    svg = fetch(name)
    png = work / f"{slug_of(name)}.png"
    render(chromium, svg, png)
    backdrop = compose(png, OUT_DIR / f"{slug_of(name)}.webp")
    png.unlink(missing_ok=True)
    return slug_of(name), backdrop


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--chromium", help="path to a Chromium binary")
    parser.add_argument(
        "--check",
        action="store_true",
        help="only verify every subject name resolves upstream",
    )
    parser.add_argument("--jobs", type=int, default=4, help="parallel renders")
    args = parser.parse_args()

    names = read_subjects()
    print(f"{len(names)} subjects in {SUBJECTS.relative_to(REPO)}")

    if args.check:
        missing = []
        with ThreadPoolExecutor(max_workers=8) as pool:
            for name, ok in zip(names, pool.map(name_resolves, names)):
                if not ok:
                    missing.append(name)
        if missing:
            print("Not found upstream:")
            for name in missing:
                print(f"  {name}  ->  {svg_url(name)}")
            return 1
        print("every subject resolves")
        return 0

    chromium = find_chromium(args.chromium)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for stale in OUT_DIR.glob("*.webp"):
        stale.unlink()

    results: dict[str, str] = {}
    failures: list[str] = []
    with tempfile.TemporaryDirectory() as tmp:
        work = Path(tmp)
        with ThreadPoolExecutor(max_workers=args.jobs) as pool:
            futures = {pool.submit(build_one, chromium, n, work): n for n in names}
            for done, (future, name) in enumerate(futures.items(), start=1):
                try:
                    slug, backdrop = future.result()
                    results[slug] = backdrop
                    print(f"  [{done:3}/{len(names)}] {slug}  #{backdrop}")
                except Exception as error:  # noqa: BLE001 - reported, then fatal
                    failures.append(f"{name}: {error}")

    if failures:
        print("\nFailed:")
        for failure in failures:
            print(f"  {failure}")
        return 1

    INDEX.write_text(
        "# <file> <backdrop RRGGBB>. Generated by tools/pictures/build-pictures.py.\n"
        + "".join(f"{slug}.webp {results[slug]}\n" for slug in sorted(results)),
        encoding="utf-8",
    )
    SOURCES.write_text(
        "# Picture sources\n\n"
        f"Every picture is rendered from the `Color` vector of [Microsoft Fluent\n"
        f"Emoji]({UPSTREAM}), MIT licensed. See `THIRD_PARTY_NOTICES.md`.\n\n"
        "| Picture | Subject | Upstream |\n| --- | --- | --- |\n"
        + "".join(
            f"| `{slug_of(n)}.webp` | {n} | [`assets/{n}/Color`]"
            f"({UPSTREAM}/tree/main/assets/{urllib.parse.quote(n)}/Color) |\n"
            for n in sorted(names)
        ),
        encoding="utf-8",
    )

    total = sum(f.stat().st_size for f in OUT_DIR.glob("*.webp"))
    print(f"\n{len(results)} pictures, {total / 1024 / 1024:.1f} MB in {OUT_DIR.relative_to(REPO)}")
    return 0


def name_resolves(name: str) -> bool:
    request = urllib.request.Request(svg_url(name), method="HEAD")
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status == 200
    except urllib.error.URLError:
        return False


if __name__ == "__main__":
    sys.exit(main())
