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
    if '/main/' in str(file) and '/ui/' in str(file):
        assert not re.search(r'\bText\(\s*"[A-Za-zŠČŽšćčž]', code), f'Literal UI text: {file}'
        assert not re.search(r'contentDescription\s*=\s*"[A-Za-zŠČŽšćčž]', code), f'Literal accessibility text: {file}'
for file in res.rglob('*.xml'): ET.parse(file)
for name in ('widget_text.xml', 'widget_text_bold.xml', 'widget_loading.xml'):
    assert 'fontFeatureSettings="tnum"' in (res / 'layout' / name).read_text(), name
models = list((root / 'core/src/main').rglob('*.kt')) + list((root / 'app/src/main').rglob('*.kt'))
assert sum('sealed interface ResolvedTimelineItem' in p.read_text() for p in models) == 1
for file in (root / 'core/src/main').rglob('*.kt'):
    assert not re.search(r'^import (android\.|androidx\.)', file.read_text(), re.M), file
for file in models:
    assert not re.search(r'TODO|NotImplementedError|TODO\(', file.read_text()), file
font = res / 'font/roboto_flex.ttf'
assert font.read_bytes()[:4] in (b'\x00\x01\x00\x00', b'OTTO'), 'Bundled font is not a font'
print(f'Presentation checks passed: {len(known)} Slovenian string resources, bundled font, tabular widget text, one domain model.')
