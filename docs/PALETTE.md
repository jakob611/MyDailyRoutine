# Barvni sistem in Liquid Glass

Aplikacija uporablja eno globoko modro-skrilasto paleto. Compose je vir resnice; `res/values/colors.xml` je samo preverjen zrcalni sloj za widget, obvestila in prvi frame okna.

## Površine

| Token | HEX | Vloga |
| --- | --- | --- |
| `Background` | `#090D16` | glavno ozadje |
| `SurfaceLowest` | `#05070B` | vdolbine in inputi |
| `SurfaceLow` / `SheetSurface` | `#0F1422` | sekundarne površine, bottom sheet |
| `Surface1` | `#151C2E` | kartice in koledarski bloki |
| `Surface2` | `#1D263D` | izbrane kartice in floating elementi |
| `Surface3` | `#26324F` | dialogi, meniji in najvišji nivo |

## Besedilo in stanje

- Primarno: `#F1F5F9`
- Sekundarno: `#A8B3C2`
- Tretji nivo/metapodatki: `#718096`
- Primarni interaktivni accent: `#2DD4BF`
- Aktivni timer: `#67E8F9`
- Fokus/Pomodoro: `#A78BFA`
- Uspeh: `#34D399`
- Opozorilo: `#FBBF24`
- Napaka/kritični rok: `#FB7185`

`TextMuted` je namenjen samo sekundarnim metapodatkom, ne glavnemu besedilu. Kontrastni gate ga zato preverja z ločenim pragom za metadata vlogo; primarno in sekundarno besedilo imata strožji prag.

## Liquid Glass

V `RoutineGlass.kt` so učinki ločeni od barv v `GlassRole`:

- standardni chrome: `blur = 6.dp`, lens `18.dp / 32.dp`, surface alpha `0.52`;
- kompaktni gumbi in kapsule: `blur = 4.dp`, lens `14.dp / 24.dp`, surface alpha `0.58`;
- sheet: `blur = 6.dp`, lens `24.dp / 44.dp`, surface alpha `0.68`;
- učinki: `vibrancy()`, nato zmeren `blur()`, nato `lens()`;
- brez agresivnega saturacijskega, kontrastnega ali exposure filtra;
- rob uporablja belo alfo približno `0.10–0.18`, z močnejšim zgornjim specular highlightom;
- ambientna svetloba je lokalna in šibka: modra `#0EA5E9 @ 0.07` ter vijolična `#8B5CF6 @ 0.045` za backdrop, ne za polnilo kartice.

`SubjectPalette` v modulu `core` vsebuje samo ARGB-long pogodbo za persistence/backup; Compose jo zrcali v `RoutineColors.subjectSwatches`, zato backup ne uvaža presentation sloja.

## Preverjanje

CI pred Gradlom zažene:

```bash
python3 tools/derive_palette.py --check
python3 tools/check_contrast.py
python3 tools/check_presentation.py
```

Ti testi preverijo vse UI module, XML mirror, stare Direction-B literale, raw `Color(0x...)` zunaj design systema, role-specific kontrast stekla in vse barvne reference obvestil/widgeta.
