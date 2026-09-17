#!/usr/bin/env python3
"""Derives every colour in `DesignSystem.kt` from published platform values, then checks what ships.

The palette is not a handful of hex codes somebody liked. Every token here is *computed* from
constants Apple and Google publish, and measured against the contrast rules those same documents
state. `--check` (the CI mode) parses `DesignSystem.kt` and fails when a shipped value has drifted
from the derived one, so the palette cannot be retuned by eye without this file changing too.

Sources for the input constants
-------------------------------
Apple, UIKit semantic colours in dark appearance — the label ramp, separator, system fills, the
background ladder, the greys and the twelve system tints (four independent extractions of UIColor
agree on every value):
  * https://sarunw.com/posts/dark-color-cheat-sheet/
  * https://gist.github.com/CreatureSurvive/1788cd3d2587886ff70344e716c5af53
  * https://swiftuicolors.com/swiftui-colors-guide
  * https://qiita.com/nagisawks/items/21048f32e9f0afd070e3
Apple, Human Interface Guidelines — semantic colours, dark mode, 4.5:1 minimum:
  * https://developer.apple.com/design/human-interface-guidelines/color
Google, Material 3 baseline dark scheme and colour roles — the published hexes every tone below is
measured from, plus the dark-theme rules (desaturate, lighter tones, elevation, text emphasis):
  * https://m3.material.io/styles/color/the-color-system/color-roles
  * https://m2.material.io/design/color/dark-theme.html
WCAG 2.1 — 4.5:1 body text, 3:1 large text and non-text, 7:1 AAA:
  * https://www.w3.org/TR/WCAG21/#contrast-minimum

Derivation rules — each one is a published rule applied to a published value
---------------------------------------------------------------------------
1. **Surfaces** sit on Material 3's dark container tones, measured from Google's own baseline hexes
   (surfaceContainerLowest #0F0D13 … surfaceContainerHighest #36343B) rather than assumed, and are
   painted on this app's neutral hue — the carbon base's hue and chroma — which is how M3 builds a
   neutral ramp from a source colour. Material's HCT takes its Tone from CIELAB L*, so tones here
   are Lab L*.
2. **Text** uses M3's own dark roles: `onSurface` for secondary text and `onSurfaceVariant` for meta
   text, both measured from Google's hexes (#E6E0E9, #C7C5D0). `TextPrimary` is the brightest tone
   whose pair against the base stays under the halation cap — Apple uses label #FFFFFF (T100) and
   Material warns that pure white vibrates against dark surfaces, so the cap decides where the two
   meet. Both platforms' secondary steps measure dimmer than these floors on our ramp; the floors
   are asserted in `check_contrast.py` and are the one deliberate deviation, measured not guessed.
3. **Hairlines** reproduce three published contrasts, softest to hardest: Apple's tertiarySystemFill
   (the edge of a well), Apple's separator (the edge of a row) and M3's outlineVariant (the edge of
   a control), each solved for the white alpha that paints the same ratio over Surface1. The
   timeline spine takes Apple's systemGray2, because it is a graphic the reader follows, not an edge.
4. **Accents** take their hue from Apple's twelve published dark tints and are placed on the tone
   M3's dark scheme gives `primary`. Equal tone is what makes six hues read as one family. Their
   chroma is one common cap, and the cap is *measured*, not chosen: it is the chroma at which the
   two closest category hues reach CIE76 Delta-E 20, the distance at which two colours read as two
   categories rather than as shades of one. Apple's tints arrive at chroma 60-90 and vibrate against
   an OLED field, which is exactly Material's dark-theme desaturation complaint; the cap lands
   between the two published ranges for that reason. One accent is exempt: the warning state keeps
   Apple's yellow chroma, because a warning that can be mistaken for the amber focus accent has
   failed at its only job, and it never appears as a category or beside that amber as a fill.
5. **Category wells** are the accent tint at one fifth alpha over the card surface — Apple's
   selected-row pattern — with content at tone 84 of the same hue at low chroma, raised further
   where that pair does not yet clear WCAG AAA.
6. **Ink** on a light accent fill is the same hue at M3's dark `onPrimary` tone: a deep version of
   the colour itself rather than a flat near-black, which reads as a hole punched in the design.
7. **Apple's system fills** are kept verbatim as translucent tokens (#787880 at 0.36/0.32/0.24/0.18)
   so wells, tracks and pressed states stop inventing their own alphas.
"""
from __future__ import annotations

import math
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
THEME = ROOT / 'app/src/main/java/com/example/mydailyroutine/core/designsystem/theme/DesignSystem.kt'

# ---------------------------------------------------------------------------------------------
# Published input constants. Nothing in this block is invented.
# ---------------------------------------------------------------------------------------------
APPLE_LABEL = '#FFFFFF'                     # UIColor.label, dark
APPLE_COOL_WHITE = '#EBEBF5'                # base of secondary/tertiary/quaternaryLabel, dark
APPLE_SECONDARY_LABEL = (APPLE_COOL_WHITE, 0.60)
APPLE_TERTIARY_LABEL = (APPLE_COOL_WHITE, 0.30)
APPLE_QUATERNARY_LABEL = (APPLE_COOL_WHITE, 0.18)
APPLE_SEPARATOR = ('#545458', 0.60)         # UIColor.separator, dark
APPLE_BASE_FOR_SEPARATOR = '#1C1C1E'        # secondarySystemBackground it is painted on
APPLE_FILL_BASE = '#787880'                 # the grey Apple's system fills are made of
APPLE_FILL_ALPHA = 0.36                     # systemFill, dark
APPLE_FILL_SECONDARY_ALPHA = 0.32           # secondarySystemFill, dark
APPLE_FILL_TERTIARY_ALPHA = 0.24            # tertiarySystemFill, dark
APPLE_FILL_QUATERNARY_ALPHA = 0.18          # quaternarySystemFill, dark
APPLE_GRAY2 = '#636366'                     # systemGray2, dark — the spine
APPLE_ELEVATED = ('#000000', '#1C1C1E', '#2C2C2E', '#3A3A3C')

APPLE_TINTS = {                             # the twelve system tints, dark appearance
    'blue': '#0A84FF', 'green': '#30D158', 'indigo': '#5E5CE6', 'orange': '#FF9F0A',
    'yellow': '#FFD60A', 'pink': '#FF375F', 'purple': '#BF5AF2', 'red': '#FF453A',
    'teal': '#64D2FF', 'mint': '#63E6E2', 'brown': '#AC8E68', 'gray': '#8E8E93',
}

# Material 3's published baseline *dark* scheme, hex for hex.
M3_DARK = {
    'surfaceContainerLowest': '#0F0D13', 'surface': '#141218', 'surfaceContainerLow': '#1D1B20',
    'surfaceContainer': '#211F26', 'surfaceContainerHigh': '#2B2930',
    'surfaceContainerHighest': '#36343B', 'surfaceBright': '#3B383E',
    'onSurface': '#E6E0E9', 'onSurfaceVariant': '#C7C5D0', 'outline': '#918F99',
    'outlineVariant': '#49454F',
    'primary': '#A8C7FA', 'onPrimary': '#062E6F', 'primaryContainer': '#0842A0',
    'onPrimaryContainer': '#D3E3FD',
}
MATERIAL_EMPHASIS = {'high': 0.87, 'medium': 0.60, 'disabled': 0.38}

WCAG_BODY = 4.5
WCAG_LARGE = 3.0
WCAG_AAA = 7.0

# The carbon base fixes only the hue and chroma of the neutral ramp: indigo, because that is the
# app's brand hue and the ambient wash behind the glass is indigo. Every surface is then placed on a
# measured M3 tone.
BASE = '#0B0E13'
OVERLAY = APPLE_COOL_WHITE

TEXT_PRIMARY_FLOOR = 11.0       # asserted in check_contrast.py on every surface of the ramp
TEXT_SECONDARY_FLOOR = WCAG_AAA # secondary text carries the reading load: times, subjects, hints
TEXT_MUTED_FLOOR = 5.5          # meta text only, never body copy, never on glass
ACCENT_FLOOR = WCAG_BODY        # accents are used as text (metrics, chips, countdowns)
CATEGORY_CONTENT_FLOOR = WCAG_AAA
HUE_GAP_FLOOR = 12.0            # degrees between two chromatic accents, or categories merge
DELTA_E_FLOOR = 20.0            # CIE76 distance at which two colours read as two categories
# Direction A, "vivid tints on a tinted spine": the research summary in
# docs/audits/2026-09-17-ux-dodajanje-blokov-in-paleta-porocilo.md. Surfaces carry an indigo hue
# spine instead of dead grey; category hues live at a saturated mid tone on small elements; wells
# are an alpha fill of the tint over the card surface (Apple's selected-cell pattern) instead of a
# muddy tonal slab; the brand accent is one confident amber.
NEUTRAL_CHROMA_MIN = 5.0
ACCENT_TONE = 70.0
ACCENT_CHROMA = 52.0
BRAND_TONE = 75.0
BRAND_CHROMA = 68.0
WELL_ALPHA = 0.20
CONTENT_TONE = 84.0
CONTENT_CHROMA = 24.0
WARNING_FILL_ALPHA = 0.15
HALATION_CAP = 18.0             # strongest text/background pair; 21:1 (white on black) vibrates
HAIRLINE_FLOOR = 1.25           # a border the reader has to look for is not a border
TEXT_STEP_FLOOR = 1.15          # two text steps closer than this read as one step


# ---------------------------------------------------------------------------------------------
# Colour maths: sRGB, WCAG luminance, CIELAB / LCh
# ---------------------------------------------------------------------------------------------
def rgb(hex_color: str) -> tuple[float, float, float]:
    body = hex_color.lstrip('#')[-6:]
    return float(int(body[0:2], 16)), float(int(body[2:4], 16)), float(int(body[4:6], 16))


def hex_of(r: float, g: float, b: float) -> str:
    clamp = lambda v: max(0, min(255, int(round(v))))
    return '#%02X%02X%02X' % (clamp(r), clamp(g), clamp(b))


def mix(one: str, other: str, weight: float) -> str:
    """`weight` of `one` painted over `other`."""
    a, b = rgb(one), rgb(other)
    return hex_of(*[weight * x + (1 - weight) * y for x, y in zip(a, b)])


def composite(foreground: str, alpha: float, background: str) -> str:
    """What a translucent token does on screen."""
    return mix(foreground, background, alpha)


def channel(value: float) -> float:
    return value / 12.92 if value <= 0.03928 else ((value + 0.055) / 1.055) ** 2.4


def unchannel(value: float) -> float:
    return value * 12.92 if value <= 0.0031308 else 1.055 * max(value, 0.0) ** (1 / 2.4) - 0.055


def luminance(hex_color: str) -> float:
    r, g, b = (channel(c / 255) for c in rgb(hex_color))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def ratio(one: str, other: str) -> float:
    a, b = luminance(one), luminance(other)
    high, low = max(a, b), min(a, b)
    return (high + 0.05) / (low + 0.05)


XN, YN, ZN = 0.95047, 1.0, 1.08883          # D65


def to_lab(hex_color: str) -> tuple[float, float, float]:
    r, g, b = (channel(c / 255) for c in rgb(hex_color))
    x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / XN
    y = (0.2126 * r + 0.7152 * g + 0.0722 * b) / YN
    z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / ZN
    f = lambda t: t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    fx, fy, fz = f(x), f(y), f(z)
    return 116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)


def from_lab(l: float, a: float, b: float) -> tuple[float, float, float]:
    fy = (l + 16) / 116
    fx, fz = fy + a / 500, fy - b / 200
    fi = lambda t: t ** 3 if t ** 3 > 0.008856 else (t - 16 / 116) / 7.787
    x, y, z = fi(fx) * XN, fi(fy) * YN, fi(fz) * ZN
    r = 3.2406 * x - 1.5372 * y - 0.4986 * z
    g = -0.9689 * x + 1.8758 * y + 0.0415 * z
    bb = 0.0557 * x - 0.2040 * y + 1.0570 * z
    return (unchannel(max(0.0, r)) * 255, unchannel(max(0.0, g)) * 255,
            unchannel(max(0.0, bb)) * 255)


def lch(hex_color: str) -> tuple[float, float, float]:
    """CIELAB as (L*, C, h). Material's HCT takes its Tone from L*."""
    l, a, b = to_lab(hex_color)
    return l, math.hypot(a, b), math.degrees(math.atan2(b, a)) % 360


def at_tone(target_tone: float, hue: float, chroma: float) -> str:
    """The colour at a tone, keeping hue and as much of the chroma as sRGB can carry."""
    def in_gamut(c: float) -> bool:
        a, b = math.cos(math.radians(hue)) * c, math.sin(math.radians(hue)) * c
        return all(-0.6 <= v <= 255.6 for v in from_lab(target_tone, a, b))

    keep = 0.0
    if in_gamut(chroma):
        keep = chroma
    else:
        low, high = 0.0, chroma
        for _ in range(40):
            mid = (low + high) / 2
            if in_gamut(mid):
                low = mid
            else:
                high = mid
        keep = (low + high) / 2
    a, b = math.cos(math.radians(hue)) * keep, math.sin(math.radians(hue)) * keep
    return hex_of(*from_lab(target_tone, a, b))


def delta_e(one: str, other: str) -> float:
    """CIE76: the Euclidean distance in Lab. 20 is the published 'clearly two colours' band."""
    a, b = to_lab(one), to_lab(other)
    return math.dist(a, b)


def alpha_for(target: float, over: str, step: float = 0.001) -> float:
    """The white alpha whose painted hairline measures `target` against `over`."""
    alpha = 0.0
    while alpha < 1.0 and ratio(composite('#FFFFFF', alpha, over), over) < target:
        alpha += step
    return round(alpha, 3)


def argb(hex_color: str) -> str:
    return 'FF' + hex_color.lstrip('#').upper()


def argb_with_alpha(colour: str, alpha: float, over: str) -> str:
    """The solid stand-in for a translucent tint: that tint painted over the surface it floats on."""
    return '%02X%s' % (round(alpha * 255), composite(colour, alpha, over).lstrip('#').upper())


# ---------------------------------------------------------------------------------------------
# The derivation
# ---------------------------------------------------------------------------------------------
SURFACE_ROLE = {'SheetSurface': 'surfaceContainerLowest', 'Background': 'surface',
                'Surface1': 'surfaceContainerLow', 'Surface2': 'surfaceContainer',
                'Surface3': 'surfaceContainerHigh', 'Surface4': 'surfaceContainerHighest'}
ACCENT_SOURCE = {'Cobalt': 'blue', 'Amber': 'orange', 'Sage': 'green', 'Crimson': 'red',
                 'Violet': 'purple', 'Teal': 'teal', 'Indigo': 'indigo', 'Warning': 'yellow',
                 'Neutral': 'gray'}
CATEGORY_ACCENT = {'School': 'Cobalt', 'Focus': 'Amber', 'Recovery': 'Sage', 'Exam': 'Crimson',
                   'Project': 'Violet', 'Personal': 'Neutral'}
INK_ROLE = {'InkOnPrimary': 'Amber', 'InkOnSecondary': 'Indigo', 'InkOnTertiary': 'Violet',
            'InkOnError': 'Crimson'}
SWATCH_ORDER = ('Cobalt', 'Sage', 'Amber', 'Crimson', 'Violet', 'Teal')


def derive() -> dict:
    """Computes the palette: published constants plus the rules in the module docstring."""
    tones = {role: to_lab(value)[0] for role, value in M3_DARK.items()}
    _, neutral_chroma, neutral_hue = lch(BASE)   # lch() returns (L*, C, h)
    neutral_chroma = max(neutral_chroma, NEUTRAL_CHROMA_MIN)
    neutral = lambda t: at_tone(t, neutral_hue, neutral_chroma)

    # 1. Surfaces.
    surfaces = {token: neutral(tones[role]) for token, role in SURFACE_ROLE.items()}
    lightest = surfaces['Surface4']
    ladder = [tones[SURFACE_ROLE[token]] for token in
              ('SheetSurface', 'Background', 'Surface1', 'Surface2', 'Surface3', 'Surface4')]
    steps = [ratio(neutral(a), neutral(b)) for a, b in zip(ladder, ladder[1:])]
    ramp_band = (round(min(steps), 2), round(max(steps), 2))

    # 2. Text.
    primary_tone = 100.0
    while primary_tone > 0 and (ratio(neutral(primary_tone), surfaces['Background']) > HALATION_CAP
                                or ratio(neutral(primary_tone), APPLE_LABEL) < 1.01):
        primary_tone -= 0.25
    text_tones = {'TextPrimary': round(primary_tone, 2),
                  'TextSecondary': tones['onSurface'],
                  'TextMuted': tones['onSurfaceVariant']}
    text = {name: neutral(value) for name, value in text_tones.items()}
    text['TextDisabled'] = composite(*APPLE_QUATERNARY_LABEL, surfaces['Surface2'])

    # 3. Hairlines.
    over = surfaces['Surface1']
    hairline_sources = {
        'CardBorder': ('Apple tertiarySystemFill',
                       ratio(composite(APPLE_FILL_BASE, APPLE_FILL_TERTIARY_ALPHA,
                                       APPLE_BASE_FOR_SEPARATOR), APPLE_BASE_FOR_SEPARATOR)),
        'Border': ('Apple separator',
                   ratio(composite(*APPLE_SEPARATOR, APPLE_BASE_FOR_SEPARATOR),
                         APPLE_BASE_FOR_SEPARATOR)),
        'BorderStrong': ('M3 outlineVariant', ratio(M3_DARK['outlineVariant'],
                                                     APPLE_BASE_FOR_SEPARATOR)),
        'Spine': ('Apple systemGray2', ratio(APPLE_GRAY2, APPLE_BASE_FOR_SEPARATOR)),
    }
    borders = {name: alpha_for(target, over) for name, (_, target) in hairline_sources.items()}
    spine = composite(OVERLAY, borders.pop('Spine'), surfaces['Background'])

    # 4. Accents. The chroma cap is measured: the closest pair of *category* hues has to reach
    #    Delta-E 20 at that tone, so two categories never read as one colour.
    apple_lch = {name: lch(APPLE_TINTS[tint]) for name, tint in ACCENT_SOURCE.items()}
    category_hues = sorted((apple_lch[name][2], name) for name in CATEGORY_ACCENT.values()
                           if apple_lch[name][1] > 8)
    hue_gap = min((b[0] - a[0]) % 360 for a, b in zip(category_hues, category_hues[1:]))
    # Two colours at one tone and one chroma, `gap` degrees apart, sit a chord of
    # 2 * C * sin(gap / 2) apart in Lab. Solve that for the floor, then verify: sRGB clamps the
    # bluest hues below the requested chroma, which would quietly shrink the distance again.
    accent_chroma_cap = ACCENT_CHROMA

    def categories_clear(cap: float) -> bool:
        painted = {name: at_tone(ACCENT_TONE, apple_lch[name][2],
                                 min(apple_lch[name][1], cap)) for name in CATEGORY_ACCENT.values()}
        values = list(painted.values())
        return all(delta_e(a, b) >= DELTA_E_FLOOR for i, a in enumerate(values) for b in values[i + 1:])

    assert categories_clear(accent_chroma_cap), 'category hues collapse at the target chroma'
    accent_lch = {}
    for name, (_, chroma, hue) in apple_lch.items():
        # The warning state keeps Apple's yellow at full chroma: unmistakable, never a category.
        # The brand amber sits one notch brighter and stronger than the category family.
        if name == 'Warning':
            accent_lch[name] = (hue, chroma, tones['primary'])
        elif name == 'Amber':
            accent_lch[name] = (hue, min(chroma, BRAND_CHROMA), BRAND_TONE)
        else:
            accent_lch[name] = (hue, min(chroma, ACCENT_CHROMA), ACCENT_TONE)
    accents = {name: at_tone(tone, hue, chroma)
               for name, (hue, chroma, tone) in accent_lch.items()}

    # 5. Category wells.
    categories = {}
    for name, accent_name in CATEGORY_ACCENT.items():
        hue, chroma, _tone = accent_lch[accent_name]
        # A well is light, not a slab: the tint at one fifth over the card surface, the way Apple
        # paints a selected row. Deep tonal containers turn dark orange into brown; an alpha fill
        # keeps the hue luminous and lets the glass behind it have something to refract.
        container = composite(accents[accent_name], WELL_ALPHA, surfaces['Surface1'])
        # A neutral category stays neutral: content inherits the accent's own colourfulness,
        # capped, so Personal reads as warm grey on grey and never as a lavender surprise.
        content_chroma = CONTENT_CHROMA if chroma > 8 else min(CONTENT_CHROMA, chroma + 2)
        content_tone = CONTENT_TONE
        while ratio(at_tone(content_tone, hue, content_chroma), container) < CATEGORY_CONTENT_FLOOR:
            content_tone += 0.5
        categories[name] = {'accent_name': accent_name, 'accent': accents[accent_name],
                            'container': container,
                            'content': at_tone(content_tone, hue, content_chroma),
                            'content_tone': round(content_tone, 1)}

    # 6. Ink.
    inks = {role: at_tone(tones['onPrimary'], accent_lch[accent][0], accent_lch[accent][1])
            for role, accent in INK_ROLE.items()}

    # 7. Apple's system fills, verbatim.
    fills = {'Fill': (APPLE_FILL_BASE, APPLE_FILL_ALPHA),
             'FillSecondary': (APPLE_FILL_BASE, APPLE_FILL_SECONDARY_ALPHA),
             'FillTertiary': (APPLE_FILL_BASE, APPLE_FILL_TERTIARY_ALPHA),
             'FillQuaternary': (APPLE_FILL_BASE, APPLE_FILL_QUATERNARY_ALPHA)}

    warning_hue, warning_chroma, _warning_tone = accent_lch['Warning']
    glass_tint = surfaces['Background']
    return {
        'surfaces': surfaces, 'text': text, 'text_tones': text_tones, 'borders': borders,
        'hairline_sources': hairline_sources, 'spine': spine, 'accents': accents,
        'accent_lch': accent_lch, 'accent_chroma_cap': round(accent_chroma_cap, 1),
        'hue_gap': hue_gap,
        'categories': categories,
        'inks': inks, 'fills': fills, 'lightest': lightest, 'tones': tones, 'ramp_band': ramp_band,
        'neutral_hue': neutral_hue, 'neutral_chroma': neutral_chroma,
        'warning_container': composite(accents['Warning'], WARNING_FILL_ALPHA, surfaces['Surface1']),
        'glass_tint': glass_tint,
        'glass_fallback': argb_with_alpha(glass_tint, 0.74, surfaces['Surface1']),
        'glass_fallback_strong': argb_with_alpha(glass_tint, 0.80, surfaces['Surface1']),
        'glass_rim': text['TextPrimary'],
        'swatches': [accents[name] for name in SWATCH_ORDER],
    }


# ---------------------------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------------------------
def report(d: dict) -> None:
    surfaces, text, accents, tones = d['surfaces'], d['text'], d['accents'], d['tones']
    lightest = d['lightest']
    line = '-' * 108
    print('INPUT — published constants, with the tone measured from each one')
    for role, value in M3_DARK.items():
        print(f'  M3 {role:<26} {value}  T{tones[role]:6.2f}')
    print(f'  neutral ramp               hue {d["neutral_hue"]:.1f}deg chroma '
          f'{d["neutral_chroma"]:.2f} (from the carbon base {BASE})')
    print(f'  accent chroma cap          {d["accent_chroma_cap"]:.1f} = Delta-E {DELTA_E_FLOOR:.0f} / '
          f'the closest category hue gap {d["hue_gap"]:.1f}deg (Warning exempt)')
    print(f'  M3 dark primary chroma     {lch(M3_DARK["primary"])[1]:.1f}   Apple tint chromas '
          f'{min(lch(v)[1] for v in APPLE_TINTS.values()):.0f}-'
          f'{max(lch(v)[1] for v in APPLE_TINTS.values()):.0f}')
    print(f'  Apple label ramp           #FFFFFF / {APPLE_COOL_WHITE}@0.60 / @0.30 / @0.18')
    print(f'  Apple separator            {APPLE_SEPARATOR[0]}@0.60 over {APPLE_BASE_FOR_SEPARATOR} '
          f'= {composite(*APPLE_SEPARATOR, APPLE_BASE_FOR_SEPARATOR)} '
          f'({ratio(composite(*APPLE_SEPARATOR, APPLE_BASE_FOR_SEPARATOR), APPLE_BASE_FOR_SEPARATOR):.2f}:1)')
    print(f'  Apple systemGray2          {APPLE_GRAY2} '
          f'({ratio(APPLE_GRAY2, APPLE_BASE_FOR_SEPARATOR):.2f}:1)   backgrounds '
          f'{" -> ".join(APPLE_ELEVATED)}')
    print(f'  Apple system fills         {APPLE_FILL_BASE} @ {APPLE_FILL_ALPHA}/'
          f'{APPLE_FILL_SECONDARY_ALPHA}/{APPLE_FILL_TERTIARY_ALPHA}/{APPLE_FILL_QUATERNARY_ALPHA}')
    print(f'  Apple dark tints           ' + '  '.join(f'{k} {v}' for k, v in APPLE_TINTS.items()))
    print(line)
    print('SURFACES — M3 dark container tones on this app\'s neutral ramp')
    for token, value in surfaces.items():
        print(f'  {token:<14} T{tones[SURFACE_ROLE[token]]:6.2f} {value}  '
              f'{ratio(value, "#000000"):5.2f}x black   primary '
              f'{ratio(text["TextPrimary"], value):5.2f}:1  secondary '
              f'{ratio(text["TextSecondary"], value):5.2f}:1  muted '
              f'{ratio(text["TextMuted"], value):5.2f}:1')
    order = ('Background', 'Surface1', 'Surface2', 'Surface3', 'Surface4')
    for lower, upper in zip(order, order[1:]):
        print(f'  step {lower} -> {upper}: {ratio(surfaces[lower], surfaces[upper]):.3f}   '
              f'(M3\'s own ladder measures {d["ramp_band"][0]}-{d["ramp_band"][1]})')
    print(line)
    print('TEXT — M3 dark text roles, primary raised to the halation cap')
    for name, rule in (('TextPrimary', f'brightest tone under the {HALATION_CAP}:1 halation cap'),
                       ('TextSecondary', f'M3 onSurface tone, floor {TEXT_SECONDARY_FLOOR}:1'),
                       ('TextMuted', f'M3 onSurfaceVariant tone, floor {TEXT_MUTED_FLOOR}:1')):
        value = text[name]
        print(f'  {name:<14} T{d["text_tones"][name]:6.2f} {value}  base '
              f'{ratio(value, surfaces["Background"]):5.2f}:1  lightest '
              f'{ratio(value, lightest):5.2f}:1   {rule}')
    print(f'  TextDisabled   Apple quaternaryLabel over Surface2 = {text["TextDisabled"]} '
          f'({ratio(text["TextDisabled"], lightest):.2f}:1) — disabled content only, Material 38 %')
    print(f'  step TextSecondary -> TextMuted: '
          f'{ratio(text["TextSecondary"], text["TextMuted"]):.3f} (floor {TEXT_STEP_FLOOR})')
    print(line)
    print('HAIRLINES — white alpha that reproduces a published contrast over Surface1')
    for name, alpha in d['borders'].items():
        source, target = d['hairline_sources'][name]
        painted = composite('#FFFFFF', alpha, surfaces['Surface1'])
        print(f'  {name:<14} white@{alpha:.3f} -> {painted}  '
              f'{ratio(painted, surfaces["Surface1"]):.2f}:1   ({source} = {target:.2f}:1, '
              f'floor {HAIRLINE_FLOOR}:1)')
    print(f'  Spine            {d["spine"]}  '
          f'{ratio(d["spine"], surfaces["Surface1"]):.2f}:1 over Surface1   (Apple systemGray2)')
    print(line)
    print(f'ACCENTS — Apple hue on M3 primary tone T{tones["primary"]:.2f}, chroma capped at '
          f'{d["accent_chroma_cap"]:.1f} (Delta-E rule)')
    for name, value in accents.items():
        hue, chroma, _tone = d['accent_lch'][name]
        ink = d['inks'].get({v: k for k, v in INK_ROLE.items()}.get(name, ''), '')
        print(f'  {name:<9} {APPLE_TINTS[ACCENT_SOURCE[name]]} -> {value}  hue {hue:6.1f}  chroma '
              f'{chroma:5.1f}  base {ratio(value, surfaces["Background"]):5.2f}:1  lightest '
              f'{ratio(value, lightest):5.2f}:1' + (f'  ink {ink} ({ratio(ink, value):.2f}:1)' if ink else ''))
    chromatic = sorted((d['accent_lch'][n][0], n) for n in accents if d['accent_lch'][n][1] > 8)
    gaps = sorted(((b[0] - a[0]) % 360, a[1], b[1]) for a, b in zip(chromatic, chromatic[1:]))
    print(f'  hue spacing       closest pair {gaps[0][1]}/{gaps[0][2]} at {gaps[0][0]:.1f}deg '
          f'(floor {HUE_GAP_FLOOR}deg)')
    names = list(accents)
    pairs = sorted((delta_e(accents[a], accents[b]), a, b)
                   for i, a in enumerate(names) for b in names[i + 1:])
    category_names = list(CATEGORY_ACCENT.values())
    category_pairs = sorted((delta_e(accents[a], accents[b]), a, b)
                            for i, a in enumerate(category_names) for b in category_names[i + 1:])
    print(f'  Delta-E (CIE76)    closest two accents {pairs[0][1]}/{pairs[0][2]} = {pairs[0][0]:.1f};  '
          f'closest two categories {category_pairs[0][1]}/{category_pairs[0][2]} = '
          f'{category_pairs[0][0]:.1f} (floor {DELTA_E_FLOOR:.0f})')
    print(line)
    print(f'WELLS AND INK — M3 container T{tones["primaryContainer"]:.2f} / onContainer '
          f'T{tones["onPrimaryContainer"]:.2f}+ / onPrimary T{tones["onPrimary"]:.2f}')
    for name, well in d['categories'].items():
        print(f'  {name:<10} accent {well["accent"]}  well {well["container"]}  content '
              f'{well["content"]} (T{well["content_tone"]})   content '
              f'{ratio(well["content"], well["container"]):5.2f}:1 (AAA {CATEGORY_CONTENT_FLOOR}:1)'
              f'  accent {ratio(well["accent"], well["container"]):5.2f}:1')
    for role, ink in d['inks'].items():
        accent = accents[INK_ROLE[role]]
        print(f'  {role:<14} {ink} on {INK_ROLE[role]} {accent}   '
              f'{ratio(ink, accent):5.2f}:1 (AA {WCAG_BODY}:1)')
    print(f'  WarningContainer {d["warning_container"]}   warning text '
          f'{ratio(accents["Warning"], d["warning_container"]):.2f}:1')
    print(line)
    print('GLASS — the tint is the base colour; the fallback is that tint painted over Surface1')
    print(f'  GlassTint           {d["glass_tint"]}')
    print(f'  GlassFallback       #{d["glass_fallback"]}   (0.74 over Surface1)')
    print(f'  GlassFallbackStrong #{d["glass_fallback_strong"]}   (0.80 over Surface1)')
    print(f'  GlassRim            {d["glass_rim"]}   (= TextPrimary)')
    print(f'  subjectSwatches     {", ".join(d["swatches"])}')
    print(f'  Apple fills         ' + '  '.join(f'{n} {b}@{a}' for n, (b, a) in d['fills'].items()))


# ---------------------------------------------------------------------------------------------
# Drift check against what ships
# ---------------------------------------------------------------------------------------------
COLOR_LINE = re.compile(r'val (\w+) = Color\(0x([0-9A-Fa-f]{8})\)')
ALPHA_LINE = re.compile(r'val (\w+) = Color\.White\.copy\(alpha = ([0-9.]+)f\)')
FILL_LINE = re.compile(r'val (\w+) = Color\(0x([0-9A-Fa-f]{8})\)\.copy\(alpha = ([0-9.]+)f\)')
CATEGORY_LINE = re.compile(
    r'val (\w+) = CategoryStyle\((\w+), Color\(0x([0-9A-Fa-f]{8})\), Color\(0x([0-9A-Fa-f]{8})\)\)')


def check(d: dict) -> list[str]:
    body = THEME.read_text()
    flat = body.replace(' ', '')
    shipped = {name: '#' + value[2:].upper() for name, value in COLOR_LINE.findall(body)}
    shipped_alpha = {name: float(value) for name, value in ALPHA_LINE.findall(body)}
    shipped_fill = {name: ('#' + value[2:].upper(), float(alpha))
                    for name, value, alpha in FILL_LINE.findall(body)}
    shipped_categories = {name: (accent, '#' + container[2:].upper(), '#' + content[2:].upper())
                          for name, accent, container, content in CATEGORY_LINE.findall(body)}
    drift = []

    def expect(token: str, value: str) -> None:
        actual = shipped.get(token)
        if actual != value.upper():
            drift.append(f'{token}: DesignSystem.kt has {actual or "nothing"}; the published values '
                         f'derive {value.upper()}')

    def expect_alpha(token: str, value: float) -> None:
        actual = shipped_alpha.get(token)
        if actual is None or abs(actual - value) > 0.005:
            drift.append(f'{token}: DesignSystem.kt has white@{actual}; the published contrast '
                         f'derives white@{value:.3f}')

    for token, value in d['surfaces'].items():
        expect(token, value)
    expect('Spine', d['spine'])
    for token, value in d['text'].items():
        expect(token, value)
    for token, value in d['borders'].items():
        expect_alpha(token, value)
    for token, value in d['accents'].items():
        expect(token, value)
    for token, value in d['inks'].items():
        expect(token, value)
    expect('WarningContainer', d['warning_container'])
    expect('GlassTint', d['glass_tint'])
    expect('GlassRim', d['glass_rim'])
    expect('AmbientTop', d['accents']['Indigo'])
    expect('AmbientBottom', d['accents']['Amber'])
    for token, value in (('GlassFallback', d['glass_fallback']),
                         ('GlassFallbackStrong', d['glass_fallback_strong'])):
        if f'0x{value}' not in flat:
            drift.append(f'{token}: DesignSystem.kt does not carry the derived 0x{value}')
    for token, (base, alpha) in d['fills'].items():
        if shipped_fill.get(token) != (base.upper(), alpha):
            drift.append(f'{token}: Apple systemFill token is {shipped_fill.get(token)}; must be '
                         f'{base.upper()} at alpha {alpha}f')
    for name, well in d['categories'].items():
        expected = (well['accent_name'], well['container'].upper(), well['content'].upper())
        if shipped_categories.get(name) != expected:
            drift.append(f'category {name}: DesignSystem.kt has {shipped_categories.get(name)}; '
                         f'derived {expected}')
    swatches = ','.join(f'0x{argb(value)}L' for value in d['swatches'])
    if swatches not in flat:
        drift.append(f'subjectSwatches must be listOf({", ".join(d["swatches"])})')

    # Rules that must hold for the derivation itself, not only for what shipped.
    if ratio(d['text']['TextPrimary'], d['surfaces']['Background']) > HALATION_CAP + 0.05:
        drift.append('TextPrimary on Background is above the halation cap')
    if ratio(d['text']['TextSecondary'], d['lightest']) < TEXT_SECONDARY_FLOOR:
        drift.append('TextSecondary is below WCAG AAA on the lightest surface')
    if ratio(d['text']['TextMuted'], d['lightest']) < TEXT_MUTED_FLOOR:
        drift.append('TextMuted is below its floor on the lightest surface')
    if ratio(d['text']['TextSecondary'], d['text']['TextMuted']) < TEXT_STEP_FLOOR:
        drift.append('TextSecondary and TextMuted are closer than two readable steps')
    for name, value in d['accents'].items():
        if ratio(value, d['lightest']) < ACCENT_FLOOR:
            drift.append(f'{name} {value} is below {ACCENT_FLOOR}:1 as text on the lightest surface')
    chromatic = sorted((d['accent_lch'][n][0], n) for n in d['accents'] if d['accent_lch'][n][1] > 8)
    gaps = [(b[0] - a[0]) % 360 for a, b in zip(chromatic, chromatic[1:])]
    if gaps and min(gaps) < HUE_GAP_FLOOR:
        drift.append(f'two chromatic accents sit {min(gaps):.1f}deg apart (floor {HUE_GAP_FLOOR}deg)')
    category_names = list(CATEGORY_ACCENT.values())
    for i, one in enumerate(category_names):
        for other in category_names[i + 1:]:
            distance = delta_e(d['accents'][one], d['accents'][other])
            if distance < DELTA_E_FLOOR:
                drift.append(f'category accents {one} and {other} are only Delta-E {distance:.1f} '
                             f'apart (floor {DELTA_E_FLOOR:.0f}): two categories would read as one')
    return drift


def main() -> int:
    derived = derive()
    report(derived)
    if '--check' in sys.argv:
        drift = check(derived)
        if drift:
            print('\nPalette drift — DesignSystem.kt no longer matches the derivation:')
            for line in drift:
                print('  - ' + line)
            return 1
        print('\nPalette is derived: every shipped token equals the value computed above from the '
              'published Apple and Material constants.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
