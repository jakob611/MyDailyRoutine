"""Shared parsing/math for the palette gates. No third-party dependencies or Android SDK."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
THEME = ROOT / 'app/src/main/java/com/example/mydailyroutine/core/designsystem/theme/DesignSystem.kt'
GLASS = ROOT / 'app/src/main/java/com/example/mydailyroutine/core/designsystem/glass/GlassStyles.kt'
XML = ROOT / 'app/src/main/res/values/colors.xml'


def palette():
    body = THEME.read_text().split('\ndata class CategoryStyle')[0]
    colors = {'Color.White': '#FFFFFFFF', 'Color.Black': '#FF000000'}
    for name, value in re.findall(r'val (\w+) = ([^\n]+)', body):
        literal = re.fullmatch(r'Color\(0x([0-9A-Fa-f]{8})\)', value)
        if literal:
            colors[name] = '#' + literal[1].upper()
        elif value in colors:
            colors[name] = colors[value]
    categories = {}
    for name, arguments in re.findall(r'val (\w+) = CategoryStyle\((.*)\)', body):
        values = []
        for token in arguments.split(', '):
            literal = re.fullmatch(r'Color\(0x([0-9A-Fa-f]{8})\)', token)
            values.append('#' + literal[1] if literal else colors[token])
        categories[name] = tuple(values)
    return {k: v for k, v in colors.items() if not k.startswith('Color.')}, categories


def rgb(value):
    return tuple(int(value[-6:][i:i + 2], 16) for i in (0, 2, 4))


def composite(foreground, alpha, background):
    return '#' + ''.join(f'{round(a * alpha + b * (1 - alpha)):02X}'
                         for a, b in zip(rgb(foreground), rgb(background)))


def luminance(value):
    def linear(channel):
        c = channel / 255
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    return sum(linear(c) * w for c, w in zip(rgb(value), (0.2126, 0.7152, 0.0722)))


def ratio(one, other):
    light, dark = sorted((luminance(one), luminance(other)), reverse=True)
    return (light + 0.05) / (dark + 0.05)


def glass_styles():
    body = GLASS.read_text()
    styles = {name: dict(alpha=float(alpha), blur=float(blur), height=float(height),
                        amount=float(amount), rim=float(rim))
              for name, alpha, blur, height, amount, rim in re.findall(
                  r'val (\w+) = GlassStyle\(([.\d]+)f, ([.\d]+).dp, ([.\d]+).dp, ([.\d]+).dp, ([.\d]+)f', body)}
    roles = re.findall(r'(\w+)\(GlassStyles\.(\w+), RoutineColors\.(\w+)\)', body)
    constants = {name: float(value) for name, value in re.findall(r'const val (\w+) = ([.\d]+)f', body)}
    return styles, roles, constants
