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
# The subject picker pairs its labels with the swatch shelf one for one: a name that drifts a place
# out of step with its colour is a picker that lies about what the reader just chose.
palette = root / 'core/src/main/kotlin/com/example/mydailyroutine/domain/model/SubjectPalette.kt'
dialog = root / 'app/src/main/java/com/example/mydailyroutine/features/subjects/presentation/SubjectEditorDialog.kt'
palette_text = palette.read_text()
shelf = palette_text[palette_text.index('val swatches'):]
swatches = len(re.findall(r'0x[0-9A-Fa-f]{8}L', shelf))
labels = len(re.findall(r'R\.string\.color_\w+', dialog.read_text()))
assert swatches == labels, f'Subject swatch/label mismatch: {swatches} colours, {labels} labels'
assert swatches >= 12, f'Too few subject colours to tell a school year apart: {swatches}'

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
# Screens' rules for one shared geometry (the 2026-09-23 pass): a screen and a sheet start at the
# same inset, a list ends above the floating control, every card uses the decorative hairline, and no
# feature file rounds its own corners or picks its own icon size. Drift here is what makes two screens
# look like two apps, and it is invisible in a diff.
for file in ui_files:
    code = file.read_text()
    if '/designsystem/' in str(file):
        continue
    assert not re.search(r'RoundedCornerShape\(\s*\d', code), f'Corner radius invented outside the design system: {file}'
    assert 'BorderStroke(1.dp, RoutineColors.Border)' not in code, f'Card outline is not the shared CardBorder: {file}'
    assert not re.search(r'\.size\(\s*\d+(?:\.\d+)?\.dp\)', code), \
        f'Size invented per screen (use a RoutineMetrics token): {file}'
# A semantics block is a plain lambda, not a composable scope: reading a string there compiles only
# until the compiler is run, and this repository has no local compiler.
for file in ui_files:
    code = file.read_text()
    for match in re.finditer(r'\.semantics\s*[({]', code):
        depth, i = 0, match.end() - 1
        while i < len(code):
            if code[i] in '({': depth += 1
            elif code[i] in ')}':
                depth -= 1
                if depth == 0: break
            i += 1
        block = code[match.end():i]
        assert 'stringResource(' not in block and 'pluralStringResource(' not in block, \
            f'String read inside a semantics block (not a composable scope): {file}'

for name, rel in (('DailyTimeline', 'features/timeline/presentation/DailyTimeline.kt'),
                  ('WeeklyOverview', 'features/timeline/presentation/overview/OverviewScreens.kt'),
                  ('GoalsScreen', 'features/goals/presentation/GoalsScreen.kt')):
    code = (ui_root / rel).read_text()
    if 'contentPadding = PaddingValues' in code:
        assert 'RoutineMetrics.ScreenPadding' in code, f'{name} does not use the shared screen inset: {rel}'
        assert 'RoutineMetrics.ListBottomInset' in code, f'{name} does not clear the floating control: {rel}'
# CAS and EE run in parallel for two years: the goals screen has to be able to show both plans at
# once, with every row naming the plan it came from. It could not, and the reader could not use the
# two plans together.
goals = (ui_root / 'features/goals/presentation/GoalsScreen.kt').read_text()
assert 'goals_all_projects' in goals, 'the goals screen cannot show every plan at once'
assert 'projectNameOf' in goals, 'rows in the goals lists do not say which plan they belong to'
assert 'goals_project_label' in goals, 'a new row never says which plan it will be saved into'

for rel in ('features/timeline/presentation/overview/OverviewScreens.kt',):
    code = (ui_root / rel).read_text()
    assert 'padding(RoutineSpacing.lg)' not in code, f'Card body uses the screen inset instead of CardPadding: {rel}'
sheet_skeleton = (ui_root / 'core/designsystem/components/RoutineText.kt').read_text()
assert sheet_skeleton.count('RoutineMetrics.ScreenPadding') >= 3, 'sheet scaffold no longer starts at the screen inset'

screens = {'DailyTimeline': 'features/timeline/presentation/DailyTimeline.kt',
           'WeeklyOverview': 'features/timeline/presentation/overview/OverviewScreens.kt',
           'MonthlyOverview': 'features/timeline/presentation/overview/OverviewScreens.kt',
           'YearlyOverview': 'features/timeline/presentation/overview/OverviewScreens.kt',
           'GoalsScreen': 'features/goals/presentation/GoalsScreen.kt'}
for name, rel in screens.items():
    code = (ui_root / rel).read_text()
    assert re.search(rf'fun {name}\((?:[^()]|\([^()]*\))*topInset: Dp', code, re.S), f'{name} cannot clear the glass top bar: {rel}'

# A helper that reads a string must be @Composable, and a semantics block is not a composable
# scope at all. Both mistakes compile as far as this repository can tell — there is no local
# compiler — and both cost a full CI cycle (runs 35854122996 and 35905305407), so they are
# checked here instead.
def body_of(code, open_index):
    depth, i = 0, open_index
    while i < len(code):
        if code[i] == '{':
            depth += 1
        elif code[i] == '}':
            depth -= 1
            if depth == 0:
                return code[open_index:i]
        i += 1
    return code[open_index:]

declaration = re.compile('^((?:private |internal |public |protected )?fun [A-Za-z0-9_.]+[(])', re.M)
for file in ui_files:
    code = file.read_text()
    for match in declaration.finditer(code):
        line_end = code.find(chr(10), match.end())
        signature_tail = code[match.end():line_end if line_end != -1 else len(code)]
        if '=' in signature_tail and '{' not in signature_tail:
            # An expression body usually continues on the following lines, indented:
            # take them up to where the next member starts at column zero.
            rest = code[match.end():match.end() + 800]
            cut = re.search(chr(10) + '[^ ]', rest)
            body = rest[:cut.start()] if cut else rest
        else:
            close_paren = code.find(')', match.end() - 1)
            brace = code.find('{', match.end() - 1)
            if brace == -1 or close_paren > brace:
                continue  # an interface or an expected declaration, with no body to read
            body = body_of(code, brace)
        # A LazyListScope builder is not composable itself: its item{} lambdas are, and the
        # strings inside them are read in a composable scope. Those builders are skipped.
        builder = 'LazyListScope' in match.group(1) or 'LazyGridScope' in match.group(1)
        if builder or not re.search('stringResource[(]|pluralStringResource[(]', body):
            continue
        # The annotations of this declaration: everything back to the previous blank line or to
        # where the previous member ended with a closing brace.
        head = code[max(0, match.start() - 400):match.start()]
        # rfind, not a regex: the segment is everything after the previous blank line
        # or the closing brace of the previous member, whichever came last.
        boundary = max(head.rfind('}' + chr(10)), head.rfind(chr(10) + chr(10)))
        segment = head[boundary + 1:]
        if '@Composable' not in segment:
            name = match.group(1).split('fun ')[1].rstrip('(')
            assert False, 'Helper ' + name + '() in ' + str(file) + ' reads a string but is not @Composable'

# A format string and the value handed to it are two halves of one promise, and neither the
# compiler nor the resource merger checks them: `%1$d` fed a String throws at runtime, inside a
# Compose composition, which is a crash on a screen the reader is looking at (run 35905963160).
# Values are resolved one assignment deep — enough for the shape this keeps taking: a helper
# builds the text, the call site passes it to a %d.
specifiers = {}
for resource in (root / 'app/src/main/res/values/strings.xml',).__iter__():
    for name, value in re.findall(r'<string name="([^"]+)"[^>]*>(.*?)</string>', resource.read_text(), re.S):
        found = re.findall(r'%([0-9]+)[$][a-zA-Z.]*([a-zA-Z])', value)
        if found:
            slots = {}
            for index, kind in found:
                slots[int(index)] = kind
            specifiers[name] = slots
numeric_tail = re.compile(r'(toInt|roundToInt|toLong|toFloat|count|size|length|ordinal|days|hour|minute)[(]?$')
number_literal = re.compile(r'^-?[0-9]+([.][0-9]+)?$')
text_helper = re.compile(r'(Label|Text|Title|Name|Hint|Description|Body|Message)[(]')
string_call = re.compile(r'stringResource[(]|pluralStringResource[(]|toString[(]')
quoted = re.compile('"[^"]*"')

def is_string_value(expr):
    expr = expr.strip()
    if '{' in expr:
        return False  # a lambda in the expression is not the value being formatted
    if numeric_tail.search(expr):
        return False
    return bool(string_call.search(expr) or text_helper.search(expr) or quoted.search(expr))

def is_number_value(expr):
    expr = expr.strip()
    return bool(number_literal.match(expr) or numeric_tail.search(expr))

for file in ui_files:
    code = file.read_text()
    bindings = dict(re.findall(r'(?:val|var) ([A-Za-z_][A-Za-z0-9_]*) = ([^\n]+)', code))
    for match in re.finditer('stringResource[(]R[.]string[.]([A-Za-z0-9_]+)', code):
        name = match.group(1)
        if name not in specifiers:
            continue
        depth, i = 0, code.index('(', match.start())
        while i < len(code):
            if code[i] == '(':
                depth += 1
            elif code[i] == ')':
                depth -= 1
                if depth == 0:
                    break
            i += 1
        args = [part for part in code[match.end():i].split(',')]  # the name itself was consumed
        args = [a for a in args[1:] if a.strip()]
        for position, kind in specifiers[name].items():
            if position - 1 >= len(args):
                continue
            raw = args[position - 1].strip()
            value = bindings.get(raw, raw)
            if kind in 'dxXofe' and is_string_value(value):
                assert False, ('String into %' + str(position) + '$' + kind + ' of ' + name + ' in ' + str(file))
            if kind == 's' and is_number_value(value) and not is_string_value(value):
                assert False, ('Number into %' + str(position) + '$s of ' + name + ' in ' + str(file))

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

# The calendar is stored in dataset keys and translated at the display, so every key the bundled
# dataset can seed must have a mapping. A key with no mapping falls through to its Slovenian self
# and shows up untranslated in the English interface — silently, and only for that one row.
calendar_dataset = (root / 'core/src/main/kotlin/com/example/mydailyroutine/domain/calendar/SlovenianAcademicCalendar.kt').read_text()
calendar_text = (root / 'app/src/main/java/com/example/mydailyroutine/core/platform/CalendarText.kt').read_text()
mapped = set(re.findall(r'^\s*"([^"]+)" -> getString\(', calendar_text, re.M))
seeded = set(re.findall(r'(?:day|Triple)\("[\d-]+",\s*"[\d-]*"?,?\s*"([^"]+)"', calendar_dataset))
seeded |= set(re.findall(r'day\("[\d-]+",\s*"([^"]+)"', calendar_dataset))
seeded |= set(re.findall(r'title = "([^"]+)"', calendar_dataset))
missing = sorted(seeded - mapped)
assert not missing, 'Calendar keys with no translation mapping: ' + ', '.join(missing)
print(f'Calendar gate passed: {len(seeded)} bundled calendar keys all map to a translated title.')
