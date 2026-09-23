#!/usr/bin/env python3
"""Compares the screenshots of this run with a seeded baseline (N16).

The device tests already photograph every screen (see `UiAudit.kt`). What was missing is the second
half of a regression test: a number that says whether the picture changed. This script reads two
directories of PNG files, pairs them by name, and reports the share of pixels that differ per screen.
A screen above the threshold is reported as a warning with the percentage, never as a silent pass.

Design notes, because a picture comparison is easy to get wrong:

* No image library. The PNG is decoded here (bit depth 8, colour type 2 or 6, non-interlaced), which is
  what `Bitmap.compress(PNG, 100)` produces. Anything else is reported and skipped instead of guessed.
* A pixel counts as different only when a channel differs by more than `TOLERANCE` levels. Anti-aliased
  text is rendered identically by the same emulator image, so the tolerance is there for the encoder,
  not to hide a layout change.
* Missing files are the loudest signal of all: a screenshot that was captured before and is not
  captured now means a screen stopped being visited, which no pixel comparison would notice.

Exit code is 0 even when the comparison finds changes — the plan asked for a note, not a blocked build.
`--strict` turns warnings into a failure for anyone who wants that in a local run.
"""
from __future__ import annotations

import argparse
import pathlib
import struct
import sys
import zlib

TOLERANCE = 8
CHANGE_THRESHOLD = 0.02


def read_png(path: pathlib.Path) -> tuple[int, int, list[tuple[int, int, int]]] | None:
    """Returns (width, height, pixels) or None when the file is not a format this reads."""
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    position = 8
    header: tuple[int, int, int, int, int] | None = None
    raw = bytearray()
    while position < len(data):
        length = struct.unpack(">I", data[position:position + 4])[0]
        kind = data[position + 4:position + 8]
        body = data[position + 8:position + 8 + length]
        position += 12 + length
        if kind == b"IHDR":
            width, height, depth, colour, _, _, interlace = struct.unpack(">IIBBBBB", body)
            if depth != 8 or colour not in (2, 6) or interlace != 0:
                return None
            header = (width, height, depth, colour, interlace)
        elif kind == b"IDAT":
            raw += body
        elif kind == b"IEND":
            break
    if header is None:
        return None
    width, height, _, colour, _ = header
    channels = 3 if colour == 2 else 4
    stride = width * channels
    inflated = zlib.decompress(bytes(raw))
    previous = bytearray(stride)
    pixels: list[tuple[int, int, int]] = []
    offset = 0
    for _ in range(height):
        filter_type = inflated[offset]
        line = bytearray(inflated[offset + 1:offset + 1 + stride])
        offset += 1 + stride
        for index in range(stride):
            left = line[index - channels] if index >= channels else 0
            up = previous[index]
            up_left = previous[index - channels] if index >= channels else 0
            value = line[index]
            if filter_type == 1:
                value += left
            elif filter_type == 2:
                value += up
            elif filter_type == 3:
                value += (left + up) // 2
            elif filter_type == 4:
                estimate = left + up - up_left
                distances = (abs(estimate - left), abs(estimate - up), abs(estimate - up_left))
                value += (left, up, up_left)[distances.index(min(distances))]
            line[index] = value & 0xFF
        for x in range(width):
            base = x * channels
            pixels.append((line[base], line[base + 1], line[base + 2]))
        previous = line
    return width, height, pixels


def compare(baseline: pathlib.Path, current: pathlib.Path) -> tuple[list[str], bool]:
    """Returns the report lines and whether anything needs attention."""
    notes: list[str] = []
    attention = False
    names = sorted({path.name for path in baseline.glob("*.png")} | {path.name for path in current.glob("*.png")})
    if not names:
        notes.append("no screenshots in either directory: nothing to compare")
        return notes, False
    for name in names:
        before = baseline / name
        after = current / name
        if not before.exists():
            notes.append(f"new screen {name}: no baseline to compare with")
            continue
        if not after.exists():
            notes.append(f"MISSING {name}: the baseline has it and this run does not — a screen stopped being captured")
            attention = True
            continue
        first = read_png(before)
        second = read_png(after)
        if first is None or second is None:
            notes.append(f"skipped {name}: not an 8-bit non-interlaced PNG this script can read")
            continue
        if (first[0], first[1]) != (second[0], second[1]):
            notes.append(f"RESIZED {name}: {first[0]}x{first[1]} became {second[0]}x{second[1]}")
            attention = True
            continue
        total = len(first[2])
        different = 0
        for left, right in zip(first[2], second[2]):
            if abs(left[0] - right[0]) > TOLERANCE or abs(left[1] - right[1]) > TOLERANCE or abs(left[2] - right[2]) > TOLERANCE:
                different += 1
        share = different / total if total else 0.0
        if share > CHANGE_THRESHOLD:
            notes.append(f"CHANGED {name}: {share * 100:.1f}% of pixels differ (threshold {CHANGE_THRESHOLD * 100:.0f}%)")
            attention = True
        else:
            notes.append(f"ok {name}: {share * 100:.2f}% of pixels differ")
    return notes, attention


def annotations(lines: list[str]) -> None:
    """GitHub reads these lines out of the log and shows them as notes and warnings on the step."""
    for line in lines:
        if line.startswith(("MISSING", "CHANGED", "RESIZED")):
            print(f"::warning title=Screenshot regression::{line}")
        else:
            print(f"::notice title=Screenshot comparison::{line}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare two directories of UI screenshots.")
    parser.add_argument("--baseline", required=True, type=pathlib.Path)
    parser.add_argument("--current", required=True, type=pathlib.Path)
    parser.add_argument("--strict", action="store_true", help="fail when a screen changed")
    arguments = parser.parse_args()
    if not arguments.current.is_dir():
        print(f"::notice::no screenshots to compare in {arguments.current}")
        return 0
    if not arguments.baseline.is_dir():
        print(f"::notice::no baseline in {arguments.baseline}: capture one with the 'Seed UI baseline' run and the comparison starts by itself")
        return 0
    lines, attention = compare(arguments.baseline, arguments.current)
    for line in lines:
        print(line)
    annotations(lines)
    return 1 if (attention and arguments.strict) else 0


if __name__ == "__main__":
    sys.exit(main())
