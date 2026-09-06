#!/usr/bin/env python3
"""Publish compiler/test failures as GitHub annotations, usable even when log ZIP downloads fail."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

def escape(value):
    return str(value).replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')

count = 0
for log in Path('.').glob('ci-*.log'):
    for line in log.read_text(errors='replace').splitlines():
        plain = re.sub(r'\x1b\[[0-9;]*m', '', line)
        if plain.startswith('e: ') or ' error: ' in plain or plain.startswith('ERROR:'):
            print(f'::error title=Android build::{escape(plain[:3000])}')
            count += 1
for folder in ('app/build', 'core/build'):
    for report in Path(folder).rglob('TEST-*.xml'):
        try: tree = ET.parse(report)
        except ET.ParseError: continue
        for case in tree.iter('testcase'):
            for error in list(case.findall('failure')) + list(case.findall('error')):
                detail = (error.attrib.get('message', '') + '\n' + (error.text or ''))[:3500]
                title = case.attrib.get('classname', '') + '.' + case.attrib.get('name', '')
                print(f'::error title={escape(title)}::{escape(detail)}')
                count += 1
print(f'Published {count} compiler/test failure annotations.')

for report in Path('app/build/reports').glob('lint-results*.xml'):
    try: tree = ET.parse(report)
    except ET.ParseError: continue
    for issue in tree.iter('issue'):
        if issue.attrib.get('severity') not in ('Error', 'Fatal'): continue
        location = issue.find('location')
        filename = location.attrib.get('file', '') if location is not None else ''
        try: filename = str(Path(filename).relative_to(Path.cwd()))
        except ValueError: pass
        line = location.attrib.get('line', '1') if location is not None else '1'
        print(f'::error file={escape(filename)},line={line},title={escape(issue.attrib.get("id", "Lint"))}::{escape(issue.attrib.get("message", ""))}')
