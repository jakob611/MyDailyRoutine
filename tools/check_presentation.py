#!/usr/bin/env python3
"""Fast resource, localization and architecture checks; Android compilation still runs in CI."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
res = root / 'app/src/main/res'
strings = ET.parse(res / 'values/strings.xml').getroot()
names = [e.attrib['name'] for e in strings if e.tag == 'string']
assert len(names) == len(set(names)), 'Duplicate resource names'
known = set(names)
for file in (root / 'app/src').rglob('*.kt'):
    code = file.read_text()
    missing = set(re.findall(r'R\.string\.(\w+)', code)) - known
    assert not missing, (file, missing)
    if '/main/' in str(file) and ('/features/' in str(file) or '/core/designsystem/' in str(file) or '/app/presentation/' in str(file)):
        assert not re.search(r'\bText\(\s*"[A-Za-zŠČŽšćčž]', code), f'Literal UI text: {file}'
        assert not re.search(r'contentDescription\s*=\s*"[A-Za-zŠČŽšćčž]', code), f'Literal accessibility text: {file}'
for file in res.rglob('*.xml'): ET.parse(file)
for name in ('widget_text.xml', 'widget_text_bold.xml', 'widget_text_single.xml', 'widget_loading.xml'):
    assert 'fontFeatureSettings="tnum"' in (res / 'layout' / name).read_text(), name
# Widget labels are read at arm's length: tabular digits, a line cap and never below 11sp.
for name in ('widget_text.xml', 'widget_text_bold.xml', 'widget_text_single.xml'):
    assert 'android:maxLines=' in (res / 'layout' / name).read_text(), name
widget = root / 'app/src/main/java/com/example/mydailyroutine/widget/AgendaWidget.kt'
widget_sizes = [float(size) for size in re.findall(r'\b(\d+(?:\.\d+)?)f\b', widget.read_text())]
assert widget_sizes and min(widget_sizes) >= 11.0, f'Widget text below 11sp: {min(widget_sizes)}sp'

# Layout invariants from the 2026 UI audit: nothing is nudged into place with offsets, every bottom
# sheet uses the shared skeleton, and no text bypasses the maxLines/ellipsis defaults.
ui_root = root / 'app/src/main/java/com/example/mydailyroutine'
ui_files = [f for f in ui_root.rglob('*.kt')
            if {'features', 'presentation', 'designsystem'} & set(f.parts)]
for file in ui_files:
    code = file.read_text()
    assert '.offset(' not in code, f'Absolutely positioned UI (use a measured Layout): {file}'
    if 'ModalBottomSheet(' in code:
        assert 'RoutineSheetScaffold' in code or 'RoutineSheetListScaffold' in code, f'Sheet without the shared skeleton: {file}'
    if file.name != 'RoutineText.kt':
        assert not re.search(r'(?<![A-Za-z])Text\(', code), f'Raw Text() bypasses RoutineText defaults: {file}'
# Invariants from the 2026 liquid-glass / typography pass: one date source, no silent single-line
# truncation, one shared sheet surface, and glass that is layered once and never folded into itself.
main_files = list((root / 'app/src/main/java').rglob('*.kt'))
dates = [f for f in main_files if 'DateTimeFormatter' in f.read_text()]
assert [f.name for f in dates] == ['DisplayFormat.kt'], f'Date formatting outside RoutineDate: {[str(f) for f in dates]}'
for file in ui_files:
    code = file.read_text()
    if file.name != 'RoutineText.kt':
        assert not re.search(r'maxLines\s*=\s*1\b', code), f'Single-line text truncates instead of shrinking: {file}'
    if 'ModalBottomSheet(' in code:
        assert 'containerColor = RoutineColors.SheetSurface' in code, f'Sheet not on the shared surface: {file}'
    if '.layerBackdrop(' in code:
        assert 'rememberLayerBackdrop' in code, f'Glass layer without its own backdrop: {file}'
callers = [f for f in main_files if 'designsystem/glass' not in str(f)]
# One palette: no screen invents its own colour, everything resolves through RoutineColors.
raw = [f for f in main_files if 'designsystem/theme' not in str(f)
       and re.search(r'Color\(0x|Color\.(?:White|Black|Red|Green|Blue|Yellow|Gray|Cyan|Magenta)\b', f.read_text())]
assert not raw, f'Colour outside the palette: {[str(f) for f in raw]}'
glass = [f for f in callers if 'RoutineBackdropProvider' in f.read_text()]
assert len(glass) == 1, f'The window backdrop must be provided exactly once: {[str(f) for f in glass]}'
layers = [f for f in callers if '.routineBackdropLayer(' in f.read_text()]
assert len(layers) == 1, f'The window content layer must be marked exactly once: {[str(f) for f in layers]}'
screens = {'DailyTimeline': 'features/timeline/presentation/DailyTimeline.kt',
           'WeeklyOverview': 'features/timeline/presentation/overview/OverviewScreens.kt',
           'MonthlyOverview': 'features/timeline/presentation/overview/OverviewScreens.kt',
           'YearlyOverview': 'features/timeline/presentation/overview/OverviewScreens.kt',
           'GoalsScreen': 'features/goals/presentation/GoalsScreen.kt'}
for name, rel in screens.items():
    code = (ui_root / rel).read_text()
    assert re.search(rf'fun {name}\((?:[^()]|\([^()]*\))*topInset: Dp', code, re.S), f'{name} cannot clear the glass top bar: {rel}'

models = list((root / 'core/src/main').rglob('*.kt')) + list((root / 'app/src/main').rglob('*.kt'))
assert sum('sealed interface ResolvedTimelineItem' in p.read_text() for p in models) == 1
for file in (root / 'core/src/main').rglob('*.kt'):
    assert not re.search(r'^import (android\.|androidx\.)', file.read_text(), re.M), file
for file in models:
    assert not re.search(r'TODO|NotImplementedError|TODO\(', file.read_text()), file
font = res / 'font/roboto_flex.ttf'
assert font.read_bytes()[:4] in (b'\x00\x01\x00\x00', b'OTTO'), 'Bundled font is not a font'
print(f'Presentation checks passed: {len(known)} Slovenian string resources, bundled font, tabular widget text, '
      f'one domain model, {len(ui_files)} UI files with measured layouts, wrapped text, one date source, '
      f'one window backdrop and {len(screens)} screens clearing the glass top bar.')

# Data adapters must not import feature presentation or the app coordinator.
for file in (root / 'app/src/main/java/com/example/mydailyroutine/features').rglob('*.kt'):
    if '/data/' in str(file):
        code=file.read_text()
        assert not re.search(r'^import com\.example\.mydailyroutine\.(?:features\.[^.]+\.presentation|app\.presentation)',code,re.M), file

# Palette audit includes launcher artwork, notification resources, all XML qualifiers and JVM code.
# User-supplied Color(subject.colorHex) is data, not a hardcoded theme; numeric constructors aren't.
for file in main_files:
    if '/designsystem/theme/' not in str(file):
        code = file.read_text()
        assert not re.search(r'\bColor\s*\(\s*(?:0[xX]|\d)', code), f'Numeric colour outside palette: {file}'
        assert not re.search(r'["\']#[0-9a-fA-F]{3,8}["\']', code), f'Hex string outside palette: {file}'
for file in res.rglob('*.xml'):
    if file == res / 'values/colors.xml':
        continue
    literals = re.findall(r'(?:color|Color)="(#[0-9a-fA-F]{3,8})"', file.read_text())
    # Android recolours this one monochrome notification alpha mask; it is not visible white ink.
    if file.name == 'ic_notification.xml':
        assert set(literals) <= {'#FFFFFFFF'}, file
    else:
        assert not literals, f'XML colour outside generated palette: {file}: {literals}'

# Regression: text must not fade just because an item is completed or outside the selected month.
overview = (ui_root / 'features/timeline/presentation/overview/OverviewScreens.kt').read_text()
goals = (ui_root / 'features/goals/presentation/GoalsScreen.kt').read_text()
assert '.alpha(if (inMonth)' not in overview
assert 'Modifier.alpha(0.55f)' not in goals
print('Palette source checks passed: no literal UI colours outside the palette/XML mirror.')

# All input wells share the same opaque recessed container, including native error states.
for file in main_files:
    code = file.read_text()
    fields = len(re.findall(r'\bOutlinedTextField\(', code))
    if fields:
        assert code.count('colors = RoutineTextFieldColors()') == fields, f'Input bypasses shared surface: {file}'
