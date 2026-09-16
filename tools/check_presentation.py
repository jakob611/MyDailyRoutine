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
models = list((root / 'core/src/main').rglob('*.kt')) + list((root / 'app/src/main').rglob('*.kt'))
assert sum('sealed interface ResolvedTimelineItem' in p.read_text() for p in models) == 1
for file in (root / 'core/src/main').rglob('*.kt'):
    assert not re.search(r'^import (android\.|androidx\.)', file.read_text(), re.M), file
for file in models:
    assert not re.search(r'TODO|NotImplementedError|TODO\(', file.read_text()), file
font = res / 'font/roboto_flex.ttf'
assert font.read_bytes()[:4] in (b'\x00\x01\x00\x00', b'OTTO'), 'Bundled font is not a font'
print(f'Presentation checks passed: {len(known)} Slovenian string resources, bundled font, tabular widget text, '
      f'one domain model, {len(ui_files)} UI files with measured layouts and wrapped text.')

# Data adapters must not import feature presentation or the app coordinator.
for file in (root / 'app/src/main/java/com/example/mydailyroutine/features').rglob('*.kt'):
    if '/data/' in str(file):
        code=file.read_text()
        assert not re.search(r'^import com\.example\.mydailyroutine\.(?:features\.[^.]+\.presentation|app\.presentation)',code,re.M), file
