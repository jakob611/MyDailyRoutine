#!/usr/bin/env python3
"""WCAG 2.1 contrast verification for the whole palette, including the liquid-glass panels.

Every value is parsed straight out of `DesignSystem.kt`, so what is checked here is what ships: the
palette cannot drift away from the numbers in this file without CI noticing.

Thresholds. WCAG 2.1 AA (4.5:1 text, 3.0:1 non-text) is the *floor*, not the target: the dark-theme
literature is unanimous that grey-on-grey is how dark UIs become unreadable, so this gate holds the
palette to the levels it actually ships at, per role:

  * 11.0:1 primary text, 7.5:1 secondary, 5.5:1 muted (worst surface in the ramp),
  * 5.0:1 for accents, because they are used as text (metrics, chips, countdowns) and not only as fill,
  * 7.0:1 for category content on its container,
  * 7.0:1 / 4.5:1 / 4.5:1 for primary / secondary / muted text on glass, worst case,
  * surface ramp steps inside 1.05-1.20 (Material 3's dark ramp steps 1.05-1.17),
  * hairlines at >= 1.25:1, since thin borders need more brightness to survive on dark,
  * and a halation guard: no pure black background, no pure white text, and the strongest pair in
    the app stays under 19:1. White on black is 21:1 and reads as vibration for readers with
    astigmatism or low contrast sensitivity.

Glass is verified as a *worst case*, not as a screenshot. A translucent panel only has to fail once —
when the brightest thing in the palette scrolls underneath it — for the text on it to become
unreadable, so the tint is composited over every saturated accent and the brightest surface and the
result has to pass for all of them. Dark glass needs more opacity than light glass; that is why the
alpha lives in the palette and is asserted here instead of being tuned by eye.
"""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
theme = (root / 'app/src/main/java/com/example/mydailyroutine/core/designsystem/theme/DesignSystem.kt').read_text()

COLOR = re.compile(r'val (\w+) = Color\(0x([0-9A-Fa-f]{8})\)')
ALPHA = re.compile(r'val (\w+Alpha) = ([0-9.]+)f')
CATEGORY = re.compile(r'val (\w+) = CategoryStyle\((\w+), Color\(0x([0-9A-Fa-f]{8})\), Color\(0x([0-9A-Fa-f]{8})\)\)')

colors = {name: '#' + argb[2:] for name, argb in COLOR.findall(theme)}
alphas = {name: float(value) for name, value in ALPHA.findall(theme)}
categories = {name: (accent, '#' + container[2:], '#' + content[2:]) for name, accent, container, content in CATEGORY.findall(theme)}

failures = []
rows = []


def channel(value: float) -> float:
    return value / 12.92 if value <= 0.03928 else ((value + 0.055) / 1.055) ** 2.4


def luminance(hex_color: str) -> float:
    body = hex_color.lstrip('#')[-6:]
    r, g, b = (int(body[i:i + 2], 16) / 255 for i in (0, 2, 4))
    return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)


def ratio(one: str, other: str) -> float:
    a, b = luminance(one), luminance(other)
    high, low = max(a, b), min(a, b)
    return (high + 0.05) / (low + 0.05)


def composite(foreground: str, alpha: float, background: str) -> str:
    """Paints `foreground` at `alpha` over `background` — what a glass tint actually does."""
    top, bottom = foreground.lstrip('#')[-6:], background.lstrip('#')[-6:]
    mixed = (round(int(top[i:i + 2], 16) * alpha + int(bottom[i:i + 2], 16) * (1 - alpha)) for i in (0, 2, 4))
    return '#' + ''.join(f'{value:02X}' for value in mixed)


def check(label: str, foreground: str, background: str, minimum: float) -> None:
    value = ratio(foreground, background)
    rows.append((label, foreground, background, value, minimum))
    if value < minimum:
        failures.append(f'{label}: {value:.2f}:1 < {minimum}:1 ({foreground} on {background})')


def color(name: str) -> str:
    if name not in colors:
        failures.append(f'Palette is missing {name}; check_contrast.py and DesignSystem.kt disagree')
        return '#FF00FF'
    return colors[name]


SURFACES = ['Background', 'Surface1', 'Surface2', 'Surface3', 'Surface4', 'SheetSurface']
TEXTS = ['TextPrimary', 'TextSecondary', 'TextMuted']
ACCENTS = ['Cobalt', 'Amber', 'Sage', 'Crimson', 'Violet', 'Teal', 'Indigo', 'Warning']

# 1. Every text token on every surface it can appear on. The minimums are per role: the tokens that
#    carry the reading load (times, subjects, hints) are the ones a dark theme usually starves.
TEXT_MINIMUM = {'TextPrimary': 11.0, 'TextSecondary': 7.5, 'TextMuted': 5.5}
for text in TEXTS:
    for surface in SURFACES:
        check(f'text {text}/{surface}', color(text), color(surface), TEXT_MINIMUM[text])

# 2. Accents are used as text (metrics, chips, countdowns), so they carry the text threshold too.
for accent in ACCENTS:
    for surface in SURFACES:
        check(f'accent {accent}/{surface}', color(accent), color(surface), 5.0)

# 3. Category wells: content on container, and the accent stripe on its container.
for name, (accent, container, content) in sorted(categories.items()):
    check(f'category {name} content', content, container, 7.0)
    check(f'category {name} accent', color(accent), container, 3.0)

# 4. Fixed pairs the app relies on.
check('fast-add label', color('Background'), color('Amber'), 4.5)
check('warning banner', color('Warning'), color('WarningContainer'), 4.5)

# 5. Liquid glass, worst case: the tint over the brightest thing that can scroll under it.
bright = [color(name) for name in ACCENTS] + [color('Surface4'), color('SheetSurface'), color('Background'),
                                              color('TextPrimary')]
for role, alpha_name in (('bar/chip', 'GlassTintAlpha'), ('sheet', 'GlassTintStrongAlpha')):
    if alpha_name not in alphas:
        failures.append(f'Palette is missing {alpha_name}')
        continue
    alpha = alphas[alpha_name]
    for behind in bright:
        panel = composite(color('GlassTint'), alpha, behind)
        # Muted text is deliberately excluded, and that exclusion is what buys the transparency: the
        # worst case here is bright text scrolling under the bar, where TextMuted would land at 4.15:1.
        # Glass chrome therefore uses TextPrimary and TextSecondary only, and the tint can sit at 0.74
        # instead of 0.82 - which is the difference between a panel that reads as glass and one that
        # reads as a grey rectangle. `tools/check_presentation.py` keeps the call sites honest.
        check(f'glass {role} over {behind}', color('TextPrimary'), panel, 7.0)
        check(f'glass {role} over {behind}', color('TextSecondary'), panel, 4.5)

# 6. The solid fallback (below Android 12) has to be as readable as the real thing.
FALLBACK_MINIMUM = {'TextPrimary': 7.0, 'TextSecondary': 4.5}
for fallback in ('GlassFallback', 'GlassFallbackStrong'):
    for text, minimum in FALLBACK_MINIMUM.items():
        check(f'fallback {text} on {fallback}', color(text), color(fallback), minimum)

# 7. Elevation: the surface ramp has to be perceptible but must not eat text contrast.
#    Material 3's own dark ramp steps between 1.05 and 1.17; outside 1.05-1.20 something is wrong.
ramp = [color(name) for name in ('Background', 'Surface1', 'Surface2', 'Surface3', 'Surface4')]
for lower, upper in zip(ramp, ramp[1:]):
    step = ratio(lower, upper)
    rows.append(('surface step', lower, upper, step, 1.05))
    if not 1.05 <= step <= 1.20:
        failures.append(f'Surface step {lower}->{upper} is {step:.3f}: outside 1.05-1.20, elevation will '
                        f'either vanish or band')

# 8. Hairlines are decorative (spacing and the surface step do the grouping), but they must be visible.
for border in ('Border', 'BorderStrong', 'CardBorder'):
    if border not in theme:
        continue
    alpha = re.search(rf'val {border} = Color\.White\.copy\(alpha = ([0-9.]+)f\)', theme)
    if not alpha:
        continue
    painted = composite('#FFFFFF', float(alpha.group(1)), color('Surface1'))
    step = ratio(painted, color('Surface1'))
    rows.append((f'border {border}', '#FFFFFF@' + alpha.group(1), color('Surface1'), step, 1.25))
    if step < 1.25:
        failures.append(f'{border} is invisible: {step:.2f}:1 against Surface1')

# 9. The XML mirror has to be the same palette, not a second one.
#    Only RemoteViews (the home-screen widget) and the pre-first-frame window background can read
#    XML, so `res/values/colors.xml` mirrors a handful of RoutineColors values. A mirror that is not
#    checked is a palette that drifts the first time someone retunes the Compose side, and the widget
#    is exactly the surface a reader sees without opening the app.
res = root / 'app/src/main/res'
mirror_xml = (res / 'values/colors.xml').read_text()
mirror = {name: value.upper() for name, value in re.findall(r'<color name="(\w+)">#([0-9A-Fa-f]{8})</color>', mirror_xml)}

MIRROR_OF = {
    'routine_background': 'Background', 'routine_surface_1': 'Surface1', 'routine_surface_2': 'Surface2',
    'routine_surface_3': 'Surface3', 'routine_surface_4': 'Surface4', 'routine_sheet_surface': 'SheetSurface',
    'routine_text_primary': 'TextPrimary', 'routine_text_secondary': 'TextSecondary', 'routine_text_muted': 'TextMuted',
    'routine_spine': 'Spine', 'routine_cobalt': 'Cobalt', 'routine_amber': 'Amber', 'routine_sage': 'Sage',
    'routine_crimson': 'Crimson', 'routine_violet': 'Violet', 'routine_indigo': 'Indigo', 'routine_warning': 'Warning',
}
WHITE_AT = {'routine_border': 'Border', 'routine_border_strong': 'BorderStrong', 'routine_card_border': 'CardBorder'}

parity = []
for xml_name, kt_name in sorted(MIRROR_OF.items()):
    expected = 'FF' + color(kt_name).lstrip('#').upper()
    actual = mirror.get(xml_name)
    parity.append((xml_name, actual, expected))
    if actual != expected:
        failures.append(f'res/values/colors.xml {xml_name} is #{actual}, DesignSystem.kt {kt_name} is #{expected}')
for xml_name, kt_name in sorted(WHITE_AT.items()):
    alpha = re.search(rf'val {kt_name} = Color\.White\.copy\(alpha = ([0-9.]+)f\)', theme)
    if not alpha:
        continue
    expected = f'{round(float(alpha.group(1)) * 255):02X}FFFFFF'
    actual = mirror.get(xml_name)
    parity.append((xml_name, actual, expected))
    if actual != expected:
        failures.append(f'res/values/colors.xml {xml_name} is #{actual}, DesignSystem.kt {kt_name} '
                        f'(white @ {alpha.group(1)}) is #{expected}')

# 10. No stray hex colours in view XML: every colour a reader sees comes from the one palette.
stray = []
for path in sorted(list((res / 'layout').glob('*.xml')) + list((res / 'drawable').glob('*.xml'))):
    # Icons are monochrome masks the system tints (launcher artwork, the notification glyph), so
    # their fill is not a palette decision.
    if path.name.startswith('ic_'):
        continue
    body = path.read_text()
    for found in re.findall(r'(?:color|Color)="(#[0-9A-Fa-f]{6,8})"', body):
        stray.append(f'{path.relative_to(root)}: {found}')
if stray:
    failures.append('stray hex colours in res/ (use @color/routine_*): ' + '; '.join(stray))

# 11. Halation guard. Pure white on pure black is 21:1 and reads as vibration, not as contrast, which
#     is the most common complaint about dark UI (astigmatism, low contrast sensitivity). The base may
#     stay near-black for OLED, but the surface text actually sits on has to be a dark grey — Material
#     3's own dark surface is 1.13x black — and the brightest text has to be a cool off-white.
base = ratio(color('Background'), '#000000')
card = ratio(color('Surface1'), '#000000')
white = ratio(color('TextPrimary'), '#FFFFFF')
strongest = ratio(color('TextPrimary'), color('Background'))
rows.append(('halation: base off black', color('Background'), '#000000', base, 1.05))
rows.append(('halation: card surface grey', color('Surface1'), '#000000', card, 1.15))
rows.append(('halation: text off white', color('TextPrimary'), '#FFFFFF', white, 1.01))
rows.append(('halation: headroom under 19:1', color('TextPrimary'), color('Background'), 19.0 - strongest, 0.0))
if base < 1.05:
    failures.append(f'Background {color("Background")} is pure black')
if card < 1.15:
    failures.append(f'Surface1 {color("Surface1")} is too close to black: text needs a dark grey card, '
                    f'not a black one (Material 3 dark surface is 1.13x black)')
if white < 1.01:
    failures.append(f'TextPrimary {color("TextPrimary")} is pure white; use a cool off-white')
if strongest > 19.0:
    failures.append(f'Strongest text/background pair is {strongest:.2f}:1 (limit 19:1): halation risk')

width = max(len(label) for label, *_ in rows)
for label, foreground, background, value, minimum in rows:
    mark = 'ok  ' if value >= minimum else 'FAIL'
    print(f'  {mark} {label:<{width}}  {value:6.2f}:1  (min {minimum}:1)  {foreground} on {background}')

if failures:
    print('\nContrast failures:')
    for failure in failures:
        print('  - ' + failure)
    sys.exit(1)

for xml_name, actual, expected in parity:
    mark = 'ok  ' if actual == expected else 'FAIL'
    print(f'  {mark} mirror {xml_name:<28} #{actual} == #{expected}')
print(f'  ok   res/ hex colours            {len(stray)} stray (icon masks exempt)')

print(f'\nContrast checks passed: {len(rows)} pairs, {len(colors)} palette colours, '
      f'{len(categories)} category wells, glass verified at '
      f'{alphas.get("GlassTintAlpha", 0):.2f}/{alphas.get("GlassTintStrongAlpha", 0):.2f} tint alpha '
      f'over the brightest content in the palette. XML mirror: {len(parity)} colours in sync with '
      f'DesignSystem.kt.')
