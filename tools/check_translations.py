#!/usr/bin/env python3
"""Guards the two translations against drifting apart.

A translation breaks silently: a new string lands in `values/` and nobody notices that English is
missing it until an English phone shows a Slovenian sentence in the middle of an English screen, or a
format placeholder gets dropped and the app crashes with `MissingFormatArgumentException` on one
language only. Both are mechanical, so both are checked here:

* the same names, in both `<string>` and `<plurals>`, with no duplicates;
* the same format specifiers per name (positional arguments must not be reordered or lost);
* the plural quantities a language actually needs: Slovenian has four, English two;
* the locales declared in `res/xml/locales_config.xml` match the translated resource folders;
* no empty translation;
* no user-visible sentence typed straight into a composable call instead of a string resource.
"""
from __future__ import annotations

import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
DEFAULT_LOCALE = "values"
TRANSLATIONS = {"values-en": "en"}
SL_PLURAL_QUANTITIES = {"one", "two", "few", "other"}
EN_PLURAL_QUANTITIES = {"one", "other"}

# A real Android format specifier: no space between `%` and the flags, so the ordinary prose
# "20 % dnevne zmogljivosti" (a literal percent sign) is not mistaken for one.
SPECIFIER = re.compile(r"%(?:(\d+)\$)?[-#+0,(]*\d*(?:\.\d+)?([a-zA-Z])")


def specifiers(text: str) -> list[tuple[str, str]]:
    """Format specifiers, ignoring `%%` and normalising positional indexes."""
    found = []
    for index, conversion in SPECIFIER.findall(text or ""):
        if conversion == "%":
            continue
        found.append((index or "", conversion))
    return sorted(found)


def read(path: pathlib.Path) -> dict[str, dict]:
    root = ET.fromstring(path.read_text(encoding="utf-8"))
    entries: dict[str, dict] = {}
    for element in root:
        if element.tag not in ("string", "plurals"):
            continue
        name = element.get("name")
        if name in entries:
            raise SystemExit(f"{path.name}: duplicate resource name {name}")
        if element.tag == "string":
            entries[name] = {"kind": "string", "text": element.text or "", "quantities": set()}
        else:
            items = {item.get("quantity"): (item.text or "") for item in element}
            entries[name] = {"kind": "plurals", "text": "", "quantities": set(items)}
            entries[name]["items"] = items
    return entries


WORD = re.compile(r"[A-Za-z\u00c0-\u017f]{3,}")
INTERPOLATION = re.compile(r"\$\{[^}]*\}|\$\w+")
CALL_WITH_LITERAL = re.compile(r"\b(?:RoutineText|RoutineLabel|RoutineTextHeading)\(\s*(?:text\s*=\s*)?\"([^\"]*)\"")


def hardcoded_text(folder: pathlib.Path) -> list[str]:
    """Text handed to a text composable as a literal, ignoring `"$value · $other"` building."""
    findings = []
    for file in folder.rglob("*.kt"):
        code = file.read_text(encoding="utf-8")
        for match in CALL_WITH_LITERAL.finditer(code):
            literal = INTERPOLATION.sub("", match.group(1))
            if WORD.search(literal):
                findings.append(f"{file.relative_to(folder.parent.parent.parent)}: {literal.strip()}")
    return findings


def main() -> int:
    problems: list[str] = []
    base = read(ROOT / "app/src/main/res" / DEFAULT_LOCALE / "strings.xml")

    for folder, language in TRANSLATIONS.items():
        path = ROOT / "app/src/main/res" / folder / "strings.xml"
        if not path.exists():
            problems.append(f"{folder}: no strings.xml for the declared language {language}")
            continue
        translated = read(path)

        missing = sorted(set(base) - set(translated))
        extra = sorted(set(translated) - set(base))
        for name in missing:
            problems.append(f"{folder}: missing translation for {name}")
        for name in extra:
            problems.append(f"{folder}: {name} exists only in the translation")
        if missing or extra:
            continue

        for name, source in base.items():
            target = translated[name]
            if source["kind"] != target["kind"]:
                problems.append(f"{folder}: {name} is a {target['kind']}, expected {source['kind']}")
                continue
            if source["kind"] == "string":
                if not target["text"].strip():
                    problems.append(f"{folder}: {name} is empty")
                if specifiers(source["text"]) != specifiers(target["text"]):
                    problems.append(
                        f"{folder}: {name} format specifiers differ "
                        f"({specifiers(source['text'])} vs {specifiers(target['text'])})"
                    )
            else:
                if not (source["quantities"] >= SL_PLURAL_QUANTITIES):
                    problems.append(f"{DEFAULT_LOCALE}: {name} is missing a Slovenian plural form")
                if target["quantities"] != EN_PLURAL_QUANTITIES:
                    problems.append(
                        f"{folder}: {name} plural quantities are {sorted(target['quantities'])}, "
                        f"English needs {sorted(EN_PLURAL_QUANTITIES)}"
                    )
                # English has two forms against Slovenian's four: compare like with like, and fall
                # back to the Slovenian `other` for a quantity English does not distinguish.
                for quantity, text in target["items"].items():
                    reference = source["items"].get(quantity) or source["items"].get("other", "")
                    if specifiers(reference) != specifiers(text):
                        problems.append(f"{folder}: {name} ({quantity}) format specifiers differ")

    # The locale list the system shows must be exactly the set of translations that exists.
    declared = set(re.findall(r'android:name="([a-zA-Z-]+)"',
                              (ROOT / "app/src/main/res/xml/locales_config.xml").read_text(encoding="utf-8")))
    expected = {"sl"} | set(TRANSLATIONS.values())
    if declared != expected:
        problems.append(f"locales_config.xml declares {sorted(declared)}, expected {sorted(expected)}")
    for folder in (ROOT / "app/src/main/res").glob("values-*"):
        language = folder.name.split("-", 1)[1]
        if language not in TRANSLATIONS and language != "en":
            problems.append(f"{folder.name}: a translated folder nobody declared in tools/check_translations.py")

    # A sentence that never reached a string resource cannot be translated at all.
    for literal in hardcoded_text(ROOT / "app/src/main/java"):
        problems.append(f"user-visible literal in a composable call: {literal}")

    if problems:
        print("Translation checks failed:")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    print(
        f"Translation checks passed: {len(base)} resources in {DEFAULT_LOCALE} and "
        f"{len(TRANSLATIONS)} further locale(s), identical names, specifiers and plural rules."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
