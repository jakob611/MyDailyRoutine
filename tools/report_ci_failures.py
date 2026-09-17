#!/usr/bin/env python3
"""Publish compiler/test failures as GitHub annotations, usable even when log ZIP downloads fail."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

def escape(value):
    return str(value).replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')

count = 0
# A crashed app produces no TEST-*.xml and no compiler error, so the fatal trace itself has to be
# recognised: without these patterns an instrumentation crash looks like a silent, empty failure.
CRASH = re.compile(r'FATAL EXCEPTION|Fatal signal|Process crashed|SIGSEGV|SIGABRT|Caused by:|'
                   r'There w(?:as|ere) \d+ failure|Test failed|Instrumentation run failed|'
                   r'Unable to find instrumentation|AndroidRuntime:')
context = 0
for log in Path('.').glob('ci-*.log'):
    for line in log.read_text(errors='replace').splitlines():
        plain = re.sub(r'\x1b\[[0-9;]*m', '', line)
        hit = plain.startswith('e: ') or ' error: ' in plain or plain.startswith('ERROR:')
        crash = bool(CRASH.search(plain))
        if crash:
            context = 25  # keep the stack that follows the fatal line
        if hit or crash or context > 0:
            context = max(context - 1, 0)
            print(f'::error title=Android build::{escape(plain[:3000])}')
            count += 1
            if count > 200:
                break
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

# Generated Room schema files are retained by the verification-reports artifact upload.
for schema in Path('app/schemas').rglob('*.json'):
    print(f'Room schema generated: {schema}')

# UI screenshots are pulled from the emulator by tools/ci-device-tests.sh; list them so the
# run log (and the step summary) shows whether the visual audit actually made it into artifacts.
import os

shots = sorted(p for p in Path('app/build/ui-audit').rglob('*') if p.is_file() and p.suffix.lower() in ('.png', '.jpg', '.webp'))
for shot in shots:
    print(f'UI screenshot: {shot} ({shot.stat().st_size} B)')
    print(f'::notice title=UI screenshot::{escape(shot.name)} ({shot.stat().st_size} B)')
if not shots:
    print('::warning title=UI audit::No screenshots were pulled from the emulator (app/build/ui-audit is empty).')
summary_path = os.environ.get('GITHUB_STEP_SUMMARY')
if summary_path and shots:
    lines = ['## UI audit screenshots', '']
    lines += [f'- `{shot.name}` ({shot.stat().st_size // 1024} KB)' for shot in shots]
    lines += ['', 'Download the `device-test-reports` artifact to view them.']
    with open(summary_path, 'a', encoding='utf-8') as summary:
        summary.write('\n'.join(lines) + '\n')
