#!/usr/bin/env python3
"""Validate the shipped Deep Oceanic Slate palette.

The Compose palette is the source of truth. This gate intentionally checks both the semantic tokens
and the XML mirror used by the widget/window, so a screen cannot silently fall back to the previous
carbon/turquoise palette or introduce a second set of literals.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
THEME = ROOT / "app/src/main/java/com/example/mydailyroutine/core/designsystem/theme/DesignSystem.kt"
XML = ROOT / "app/src/main/res/values/colors.xml"

EXPECTED = {
    "Background": "090D16",
    "SurfaceLowest": "05070B",
    "SurfaceLow": "0F1422",
    "Surface1": "151C2E",
    "Surface2": "1D263D",
    "Surface3": "26324F",
    "TextPrimary": "F1F5F9",
    "TextSecondary": "A8B3C2",
    "TextMuted": "718096",
    "Spine": "52627C",
    "Primary": "2DD4BF",
    "Timer": "67E8F9",
    "FocusAccent": "A78BFA",
    "Success": "34D399",
    "Warning": "FBBF24",
    "Error": "FB7185",
    "AmbientTop": "0EA5E9",
    "AmbientBottom": "8B5CF6",
    "WarningContainer": "4B3B1B",
}
EXPECTED_ALPHA = {
    "AmbientTopAlpha": 0.07,
    "AmbientBottomAlpha": 0.045,
    "GlassTintAlpha": 0.52,
    "GlassTintCompactAlpha": 0.58,
    "GlassTintStrongAlpha": 0.68,
}
LEGACY = {
    "131515", "2B2C28", "2F302C", "333430", "383835", "1B1D1C", "FFFAFB",
    "ADD8D1", "98BEB8", "54575F", "787880", "9CC5FF", "7DE2D1", "A4D98C",
    "FF9B8A", "D3B0FA", "5EC4E8", "A8D8FF", "F5C542", "BCC3BF", "0B0E13",
}


def parse_colors(text: str) -> dict[str, str]:
    return {name: value.upper() for name, value in re.findall(r"val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", text)}


def parse_alphas(text: str) -> dict[str, float]:
    return {name: float(value) for name, value in re.findall(r"val (\w+Alpha) = ([0-9.]+)f", text)}


def parse_xml(text: str) -> dict[str, str]:
    return {name: value.upper() for name, value in re.findall(r'<color name="(\w+)">#(?:FF)?([0-9A-Fa-f]{6,8})</color>', text)}


def main() -> int:
    theme = THEME.read_text()
    xml = XML.read_text()
    failures: list[str] = []
    colors = parse_colors(theme)
    alphas = parse_alphas(theme)
    for name, expected in EXPECTED.items():
        if colors.get(name) != expected:
            failures.append(f"{name}: expected #{expected}, found #{colors.get(name, 'missing')}")

    aliases = {"Surface4": "Surface3", "SheetSurface": "SurfaceLow", "GlassTint": "Surface1",
               "GlassRim": "TextPrimary", "InkOnPrimary": "Background", "InkOnSecondary": "Background",
               "InkOnTertiary": "Background", "InkOnError": "Background"}
    for alias, source in aliases.items():
        if not re.search(rf"val {alias} = {source}\b", theme):
            failures.append(f"{alias} must remain a semantic alias of {source}")

    for name, expected in EXPECTED_ALPHA.items():
        actual = alphas.get(name)
        if actual is None or abs(actual - expected) > 0.0001:
            failures.append(f"{name}: expected {expected}, found {actual}")

    # The checked app must not contain the superseded Direction-B literals. Historical audit files
    # are intentionally left as history; production source and resource mirrors are not.
    production = "\n".join(p.read_text(errors="ignore") for p in
                           list((ROOT / "app/src/main").rglob("*.kt")) +
                           list((ROOT / "app/src/main/res").rglob("*.xml")))
    for legacy in sorted(LEGACY):
        if re.search(rf"(?i)(?:#|0x(?:FF)?){legacy}\b", production):
            failures.append(f"legacy colour #{legacy} still appears in app/src/main")

    # Palette literals belong in DesignSystem.kt (the persistence-only SubjectPalette is a separate
    # ARGB-long contract). UI feature files may only reference RoutineColors.
    for path in (ROOT / "app/src/main/java").rglob("*.kt"):
        if path == THEME or path.name == "SubjectPalette.kt":
            continue
        if re.search(r"Color\(0x[0-9A-Fa-f]{8}\)", path.read_text()):
            failures.append(f"hardcoded Compose colour outside DesignSystem.kt: {path.relative_to(ROOT)}")

    mirror = parse_xml(xml)
    mirror_of = {
        "routine_background": "Background", "routine_surface_lowest": "SurfaceLowest",
        "routine_surface_low": "SurfaceLow", "routine_surface_1": "Surface1",
        "routine_surface_2": "Surface2", "routine_surface_3": "Surface3", "routine_surface_4": "Surface4",
        "routine_sheet_surface": "SheetSurface", "routine_text_primary": "TextPrimary",
        "routine_text_secondary": "TextSecondary", "routine_text_muted": "TextMuted", "routine_spine": "Spine",
        "routine_timer": "Timer", "routine_primary": "Primary", "routine_success": "Success",
        "routine_error": "Error", "routine_focus": "FocusAccent", "routine_neutral": "TextSecondary",
        "routine_warning": "Warning",
    }
    resolved = {**colors, "Surface4": colors.get("Surface3"), "SheetSurface": colors.get("SurfaceLow"),
                "GlassTint": colors.get("Surface1"), "GlassRim": colors.get("TextPrimary"),
                "InkOnPrimary": colors.get("Background"), "InkOnSecondary": colors.get("Background"),
                "InkOnTertiary": colors.get("Background"), "InkOnError": colors.get("Background")}
    for xml_name, token in mirror_of.items():
        expected = resolved.get(token)
        if mirror.get(xml_name) != expected:
            failures.append(f"XML mirror {xml_name}: expected #{expected}, found #{mirror.get(xml_name, 'missing')}")

    stray_xml = []
    for path in list((ROOT / "app/src/main/res/layout").glob("*.xml")) + list((ROOT / "app/src/main/res/drawable").glob("*.xml")):
        if path.name.startswith("ic_"):
            continue
        for value in re.findall(r'(?:color|Color)="(#[0-9A-Fa-f]{6,8})"', path.read_text()):
            stray_xml.append(f"{path.relative_to(ROOT)}: {value}")
    if stray_xml:
        failures.append("raw view colour literals: " + "; ".join(stray_xml))

    if failures:
        print("Palette gate failed:")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    print("Palette gate passed: 6 surface roles, semantic accents, glass alphas, XML mirror, and hardcoded-colour scan are in sync.")
    if "--check" not in sys.argv:
        print("Primary surface: #090D16  |  cards: #151C2E  |  highest container: #26324F")
        print("Primary: #2DD4BF  |  timer: #67E8F9  |  focus: #A78BFA")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
