#!/usr/bin/env python3
"""Measured WCAG ratios, not a claim of whole-app certification.

Small primary/secondary/accent text must pass AA (4.5:1). Category content targets AAA (7:1).
The prescribed tertiary #718096 is decorative only. Runtime subject colours are arbitrary:
only their tinted surfaces carry text; colour stripes are paired with category icons/labels.
Glass tests include a white backdrop (conservative bound after vibrancy/blur/lens), maximum
interactive highlights and opaque pre-Android-12 fallbacks. GPU screenshots/device testing
are still needed; arithmetic does not establish rendering fidelity or frame performance.
"""
import re
import sys
from palette_tools import ROOT, THEME, palette, glass_styles, composite, ratio

colors, categories = palette()
styles, roles, constants = glass_styles()
rows, failures = [], []


def check(label, foreground, background, minimum=4.5):
    value = ratio(foreground, background)
    rows.append((label, value, minimum))
    if value < minimum:
        failures.append(f'{label}: {value:.2f}:1 < {minimum}:1 ({foreground} on {background})')


surfaces = ['SurfaceLowest', 'Background', 'SurfaceLow', 'SurfaceContainer', 'SurfaceHigh',
            'SurfaceHighest', 'SheetSurface', 'PrimaryContainer', 'WarningContainer', 'ErrorContainer']
accents = ['Primary', 'Timer', 'FocusAccent', 'Success', 'Warning', 'Error', 'Cobalt',
           'RecoveryAccent', 'ProjectAccent']
for surface in surfaces:
    for text in ['TextPrimary', 'TextSecondary'] + accents:
        check(f'{text}/{surface}', colors[text], colors[surface])
for name, (accent, container, content) in categories.items():
    check(f'{name} content', content, container, 7.0)
    check(f'{name} accent', accent, container, 3.0)
    for text in ['TextPrimary', 'TextSecondary']:
        check(f'{name} secondary', colors[text], container)

for fill in accents:
    check(f'filled control/{fill}', colors['Background'], colors[fill])

# Cards, weekly blocks, month heat maps: bound even user-picked white subject colours.
for accent in [c[0] for c in categories.values()] + ['#FFFFFF', '#000000']:
    for alpha, base in [(0.06, 'SurfaceContainer'), (0.22, 'Background')]:
        for text in ['TextPrimary', 'TextSecondary']:
            check(f'tinted card {alpha}/{accent}/{text}', colors[text], composite(accent, alpha, colors[base]))
for heat in [0.08, 0.43]:
    check('month heat text', colors['TextPrimary'], composite(colors['FocusAccent'], heat, colors['Background']))

# Ambient washes accumulate at most these opacities (conservative same-position bound).
ambient = colors['Background']
for token in ['AmbientTop', 'AmbientBottom']:
    alpha = float(re.search(rf'val {token}Alpha = ([.\d]+)f', THEME.read_text())[1])
    ambient = composite(colors[token], alpha, ambient)
check('ambient/secondary', colors['TextSecondary'], ambient)

assert set(styles) == {'Card', 'Compact', 'Bar', 'Sheet', 'Control'}, 'Unparsed glass style'
assert {r[0] for r in roles} == {'Bar', 'Sheet', 'Chip', 'Control'}, 'Unparsed glass role'
for role, style_name, surface in roles:
    style = styles[style_name]
    # Chip is an unused, local/static material, not allowed over scrolling white content.
    backdrops = [colors['SurfaceHighest']] if role == 'Chip' else ['#FFFFFF', '#000000']
    for backdrop in backdrops:
        panel = composite(colors[surface], style['alpha'], backdrop)
        panel = composite('#FFFFFF', constants['TiltGlow'] * style['rim'] * 3, panel)
        if role == 'Control':
            panel = composite('#FFFFFF', constants['TouchGlow'], panel)
        for text in ['TextPrimary', 'TextSecondary', 'Primary']:
            check(f'glass {role}/{backdrop}/{text}', colors[text], panel)
    for text in ['TextPrimary', 'TextSecondary', 'Primary']:
        check(f'opaque fallback {role}/{text}', colors[text], colors[surface])
    assert 4 <= style['blur'] <= 10 and style['rim'] <= .16, role

# Current-time pulse alpha must not make small labels unreadable at the trough.
for background in ['Background', 'SurfaceContainer']:
    for accent in ['Timer', 'Warning']:
        check(f'pulsed {accent}/{background}', composite(colors[accent], .78, colors[background]), colors[background])

# The tertiary role is deliberately NOT certified for body text. Prevent accidental adoption.
for path in (ROOT / 'app/src/main/java').rglob('*.kt'):
    if path != THEME and 'RoutineColors.TextTertiary' in path.read_text():
        failures.append(f'Decorative tertiary used outside the palette: {path}')

for label, value, minimum in rows:
    print(f'{"ok" if value >= minimum else "FAIL":4} {label:60} {value:5.2f}:1 (min {minimum})')
if failures:
    print('\nContrast failures:\n' + '\n'.join(failures))
    sys.exit(1)
print(f'\nContrast checks passed: {len(rows)} combinations, {len(categories)} categories, {len(roles)} glass roles.')
