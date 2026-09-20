#!/usr/bin/env python3
"""Verify the 2026-09-20 brand brief and generate/check the Android XML mirror.

The old Apple/Lab derivation has been retired: these are product tokens, not published
platform colours. Compose is the source of truth; --write updates RemoteViews resources.
CI uses --check and never silently rewrites source files. Contrast is independently checked
by check_contrast.py, including every shipped glass role.
"""
import sys
import re
from palette_tools import palette, XML

# A regression contract for the approved brief, not a second production palette.
BRIEF = {
    'Background': '090D16', 'SurfaceLowest': '05070B', 'SurfaceLow': '0F1422',
    'SurfaceContainer': '151C2E', 'SurfaceHigh': '1D263D', 'SurfaceHighest': '26324F',
    'TextPrimary': 'F1F5F9', 'TextSecondary': 'A8B3C2', 'TextTertiary': '718096',
    'Border': '263247', 'Primary': '2DD4BF', 'Timer': '67E8F9', 'FocusAccent': 'A78BFA',
    'Success': '34D399', 'Warning': 'FBBF24', 'Error': 'FB7185',
}
# Only native XML consumers need this subset. Other components use RoutineColors directly.
MIRROR = (
    'Background', 'SurfaceLowest', 'SurfaceLow', 'SurfaceContainer', 'SurfaceHigh', 'SurfaceHighest',
    'SheetSurface', 'TextPrimary', 'TextSecondary', 'Border', 'CardBorder', 'Spine',
    'Primary', 'Timer', 'FocusAccent', 'Success', 'Warning', 'Error', 'Cobalt', 'RecoveryAccent',
    'ProjectAccent', 'Neutral',
)


def xml_name(name):
    return 'routine_' + re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()


def expected_xml(colors):
    entries = [(xml_name(name), colors[name]) for name in MIRROR]
    entries.append(('routine_launcher_shadow', '#44000000'))  # artwork shadow, not a UI text role
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Generated from RoutineColors by tools/derive_palette.py (write mode). -->\n<resources>\n'
            + ''.join(f'    <color name="{name}">{value}</color>\n' for name, value in entries)
            + '</resources>\n')


def main():
    colors, _ = palette()
    failures = [f'{name}: {colors.get(name)} != #FF{hex_value}' for name, hex_value in BRIEF.items()
                if colors.get(name) != '#FF' + hex_value]
    xml = expected_xml(colors)
    if '--write' in sys.argv:
        XML.write_text(xml)
    if XML.read_text() != xml:
        failures.append('XML mirror drift: run python3 tools/derive_palette.py --write')
    if failures:
        print('\n'.join(failures))
        return 1
    print(f'Palette brief: {len(BRIEF)} exact tokens; XML mirror: {len(MIRROR)} synchronized roles.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
