#!/usr/bin/env python3
"""WCAG and palette-role checks for the shipped Deep Oceanic Slate theme."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
THEME = (ROOT / "app/src/main/java/com/example/mydailyroutine/core/designsystem/theme/DesignSystem.kt").read_text()
XML = (ROOT / "app/src/main/res/values/colors.xml").read_text()
DIRECT = {name: "#" + value.upper() for name, value in re.findall(r"val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", THEME)}
COLORS = {
    **DIRECT,
    "Surface4": DIRECT["Surface3"], "SheetSurface": DIRECT["SurfaceLow"], "GlassTint": DIRECT["Surface1"],
    "GlassRim": DIRECT["TextPrimary"], "Amber": DIRECT["Primary"], "Cobalt": DIRECT["Timer"],
    "Sage": DIRECT["Success"], "Crimson": DIRECT["Error"], "Violet": DIRECT["FocusAccent"],
    "Teal": DIRECT["Timer"], "Indigo": DIRECT["Timer"], "Neutral": DIRECT["TextSecondary"],
    "InkOnPrimary": DIRECT["Background"], "InkOnSecondary": DIRECT["Background"],
    "InkOnTertiary": DIRECT["Background"], "InkOnError": DIRECT["Background"],
}
ALPHAS = {name: float(value) for name, value in re.findall(r"val (\w+Alpha) = ([0-9.]+)f", THEME)}


def channel(value: float) -> float:
    return value / 12.92 if value <= 0.03928 else ((value + 0.055) / 1.055) ** 2.4


def luminance(value: str) -> float:
    value = value.lstrip("#")[-6:]
    rgb = [int(value[index:index + 2], 16) / 255 for index in (0, 2, 4)]
    return sum(weight * channel(component) for weight, component in zip((0.2126, 0.7152, 0.0722), rgb))


def ratio(one: str, other: str) -> float:
    high, low = sorted((luminance(one), luminance(other)), reverse=True)
    return (high + 0.05) / (low + 0.05)


def composite(foreground: str, alpha: float, background: str) -> str:
    top, bottom = foreground.lstrip("#")[-6:], background.lstrip("#")[-6:]
    return "#" + "".join(f"{round(int(top[i:i + 2], 16) * alpha + int(bottom[i:i + 2], 16) * (1 - alpha)):02X}" for i in (0, 2, 4))


def check(rows: list[tuple[str, float, float]], label: str, foreground: str, background: str, minimum: float) -> None:
    value = ratio(foreground, background)
    rows.append((label, value, minimum))


SURFACES = ["Background", "SurfaceLow", "Surface1", "Surface2", "Surface3"]
TEXTS = {"TextPrimary": 11.0, "TextSecondary": 5.0, "TextMuted": 3.0}
ACCENTS = ["Primary", "Timer", "FocusAccent", "Success", "Warning", "Error"]
rows: list[tuple[str, float, float]] = []
failures: list[str] = []

for text, minimum in TEXTS.items():
    for surface in SURFACES:
        check(rows, f"{text} on {surface}", COLORS[text], COLORS[surface], minimum)
for accent in ACCENTS:
    for surface in SURFACES:
        check(rows, f"{accent} on {surface}", COLORS[accent], COLORS[surface], 4.5)

# Tertiary text is intentionally a metadata role; it is held to the WCAG large/decorative floor,
# while primary and secondary carry reading content at stronger levels.
for role, accent in (("InkOnPrimary", "Primary"), ("InkOnSecondary", "Timer"),
                     ("InkOnTertiary", "FocusAccent"), ("InkOnError", "Error")):
    check(rows, f"{role} on {accent}", COLORS[role], COLORS[accent], 4.5)
check(rows, "Warning on WarningContainer", COLORS["Warning"], COLORS["WarningContainer"], 4.5)

# Role-specific glass worst cases. The standard/compact surface is Surface1 at 0.52/0.58 alpha;
# the sheet uses the low slate surface at 0.68. Text is measured after compositing, not against an
# opaque sample, so this gate catches the common mistake of making glass beautiful but unreadable.
page = [COLORS[name] for name in ("Background", "SurfaceLow", "Surface1", "Surface2", "Surface3")]
for role, tint, alpha, minimum_secondary in (
    ("standard", COLORS["GlassTint"], ALPHAS["GlassTintAlpha"], 4.5),
    ("compact", COLORS["GlassTint"], ALPHAS["GlassTintCompactAlpha"], 4.5),
    ("sheet", COLORS["SheetSurface"], ALPHAS["GlassTintStrongAlpha"], 4.5),
):
    for behind in page:
        panel = composite(tint, alpha, behind)
        check(rows, f"glass {role} primary", COLORS["TextPrimary"], panel, 7.0)
        check(rows, f"glass {role} secondary", COLORS["TextSecondary"], panel, minimum_secondary)

# Category chips and cards use a shared raised slate container; the accent is the non-text indicator.
for name, accent in (("School", "Timer"), ("Focus", "FocusAccent"), ("Recovery", "Success"),
                     ("Exam", "Error"), ("Project", "FocusAccent"), ("Personal", "TextSecondary")):
    check(rows, f"{name} content", COLORS["TextPrimary"], COLORS["Surface2"], 7.0)
    check(rows, f"{name} accent", COLORS[accent], COLORS["Surface2"], 3.0)

# Surface hierarchy is perceptible without giant luminance jumps. Surface4 is an alias of Surface3,
# so it is not measured as a separate step.
for lower, upper in zip(SURFACES, SURFACES[1:]):
    value = ratio(COLORS[lower], COLORS[upper])
    rows.append((f"surface step {lower}->{upper}", value, 1.04))
    if not 1.04 <= value <= 1.20:
        failures.append(f"surface step {lower}->{upper} is {value:.2f}:1 (expected 1.04-1.20)")

# Border alpha values are checked where Compose paints them; the XML mirror is checked byte-for-byte.
for name, minimum in (("Border", 1.25), ("BorderStrong", 1.25), ("CardBorder", 1.25)):
    match = re.search(rf"val {name} = Color\.White\.copy\(alpha = ([0-9.]+)f\)", THEME)
    if match:
        painted = composite("#FFFFFF", float(match.group(1)), COLORS["Surface1"])
        check(rows, f"{name} on Surface1", painted, COLORS["Surface1"], minimum)

for label, value, minimum in rows:
    if value < minimum:
        failures.append(f"{label}: {value:.2f}:1 < {minimum}:1")

# Basic XML parity for every view-facing token.
mirror = {name: value.upper()[-6:] for name, value in re.findall(r'<color name="(\w+)">#([0-9A-Fa-f]{6,8})</color>', XML)}
mirror_of = {
    "routine_background": "Background", "routine_surface_1": "Surface1", "routine_surface_2": "Surface2",
    "routine_surface_3": "Surface3", "routine_surface_4": "Surface4", "routine_sheet_surface": "SheetSurface",
    "routine_text_primary": "TextPrimary", "routine_text_secondary": "TextSecondary", "routine_text_muted": "TextMuted",
    "routine_spine": "Spine", "routine_timer": "Timer", "routine_primary": "Primary", "routine_success": "Success",
    "routine_error": "Error", "routine_focus": "FocusAccent", "routine_neutral": "Neutral", "routine_warning": "Warning",
}
for xml_name, token in mirror_of.items():
    expected = COLORS[token].lstrip("#").upper()
    if mirror.get(xml_name) != expected:
        failures.append(f"XML {xml_name}: #{mirror.get(xml_name, 'missing')} != #{expected}")

# The window background/widgets must not drift into raw colour literals.
for path in list((ROOT / "app/src/main/res/layout").glob("*.xml")) + list((ROOT / "app/src/main/res/drawable").glob("*.xml")):
    if path.name.startswith("ic_"):
        continue
    if re.search(r'(?:color|Color)="#[0-9A-Fa-f]{6,8}"', path.read_text()):
        failures.append(f"raw XML colour in {path.relative_to(ROOT)}")

if failures:
    print("Contrast gate failed:")
    for failure in failures:
        print(f"  - {failure}")
    sys.exit(1)

print(f"Contrast gate passed: {len(rows)} semantic, glass, hierarchy, and hairline checks; "
      "primary/secondary text and all interactive accents meet their declared role floors.")
