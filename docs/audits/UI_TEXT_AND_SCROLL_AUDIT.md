# Sistematični pregled: odrezano besedilo in dolgi drsniki

Datum: 2026-09-16 · Obseg: **celotna aplikacija** (34 UI datotek, 12.062 vrstic, widget, listi)

Pregled je narejen s skripto (`/tmp/audit_text.py`, `/tmp/audit2.py`), ki za vsak klic `RoutineText` /
`RoutineLabel` / `Text` izlušči argumente in označi tveganja. Nič ni pregledano "na oko" — spodaj je
vsako mesto posebej.

## 1. Kako se besedilo trenutno reže

| Mehanizem | Kje | Posledica |
| --- | --- | --- |
| `maxLines = 1` + `overflow = Ellipsis` (privzeto v `RoutineText`) | 20 mest | napis se konča z `…` |
| `softWrap = false` (privzeto v `RoutineLabel`) | vsi chipi, gumbi, časovne značke | beseda se ne more prelomiti → takoj `…` |
| `widthIn(max = 180.dp)` / `width(82.dp)` | `EntryEditorSheet` (predmet, gumb) | fiksen okvir reže tudi kratek napis |
| `TextOverflow` eksplicitno | 3 mesta | samo v ovojnici |
| Widget: `android:maxLines="3" android:ellipsize="end"` | `widget_text*.xml` | naslov bloka se lahko konča z `…` |
| Widget: `maxLines="1" singleLine="true"` | `widget_text_single.xml` | `…` pri dolgem napisu |

**Datumi in časi** so najbolj izpostavljeni, ker so sestavljeni iz dveh delov (dan + mesec + leto) in
pogosto sedijo v ozkih vrsticah: 23 mest oblikuje datum neposredno z
`DateTimeFormatter.ofPattern(...)` v UI kodi, vsak s svojim vzorcem — zato ni pravila, kdaj je napis
kratek in kdaj dolg.

### 1.1 Vseh 20 mest z `maxLines = 1`

| # | Datoteka:vrstica | Kaj se reže | Ocena |
| --- | --- | --- | --- |
| 1 | `app/presentation/RoutineApp.kt:116` | naslov »Cilji« v vrhnji vrstici | srednje |
| 2 | `app/presentation/RoutineApp.kt:121` | ime aplikacije | nizko |
| 3 | `app/presentation/RoutineApp.kt:122` | podnaslov aplikacije | **visoko** (dolg slovenski stavek) |
| 4 | `core/designsystem/components/RoutineText.kt:228` | `CategoryTabs` → vsi zavihki v vseh listih | **visoko** |
| 5 | `features/entry/presentation/EntryEditorSheet.kt:188` | predlogi za hitro dodajanje | srednje |
| 6 | `features/entry/presentation/EntryEditorSheet.kt:206` | »Vsi predmeti« | nizko |
| 7 | `features/entry/presentation/EntryEditorSheet.kt:221` | **ime predmeta** + `widthIn(max = 180.dp)` | **visoko** |
| 8 | `features/entry/presentation/EntryEditorSheet.kt:224` | »Nov predmet« | nizko |
| 9 | `features/entry/presentation/EntryEditorSheet.kt:238` | oznaka predloga v izbirnem meniju | srednje |
| 10 | `features/entry/presentation/EntryEditorSheet.kt:272` | `duration_minutes` (»45 minut«) | srednje |
| 11 | `features/entry/presentation/EntryEditorSheet.kt:282` | možnosti trajanja | srednje |
| 12 | `features/entry/presentation/TimetableImportSheet.kt:202` | **naslov ure** (`weight(1f)`) | **visoko** |
| 13 | `features/planning/presentation/ActualCompletionDialog.kt:79` | `duration_minutes` | srednje |
| 14 | `features/planning/presentation/TopicEditorSheet.kt:155` | ime teme | **visoko** |
| 15 | `features/planning/presentation/TopicEditorSheet.kt:178` | naslov mejnika | **visoko** |
| 16 | `features/routines/presentation/WeekdayPicker.kt:48` | kratka imena dni (pon, tor …) | nizko |
| 17 | `features/settings/presentation/SettingsSheet.kt:371` | predlog »navadno leto« | srednje |
| 18 | `features/settings/presentation/SettingsSheet.kt:378` | predlog »zaključni letnik« | srednje |
| 19 | `features/timeline/presentation/DailyTimeline.kt:197` | `execution_elapsed_minutes` | srednje |
| 20 | `features/timeline/presentation/overview/OverviewScreens.kt:417` | »opravljeno« / številka | nizko |

### 1.2 Vseh 23 mest z datumi/časi

| # | Datoteka:vrstica | Vzorec | Tveganje |
| --- | --- | --- | --- |
| 1 | `app/presentation/RoutineApp.kt:258` | `EEE, d. MMM` (naslov dneva) | srednje |
| 2 | `app/presentation/RoutineApp.kt:260` | `date_range` z dvema `d. MMM` | **visoko** (dva datuma v eni vrstici) |
| 3 | `app/presentation/RoutineApp.kt:261` | `LLLL yyyy` (mesec) | srednje |
| 4 | `core/presentation/DisplayFormat.kt:19` | `HH:mm` (ura) | nizko |
| 5 | `core/presentation/DisplayFormat.kt:21` | `%02d:%02d` | nizko |
| 6 | `features/entry/presentation/BlockEditorSheet.kt:40,81` | `EEEE, d. MMMM yyyy` (podnaslov lista) | **visoko** (najdaljši možni napis) |
| 7 | `features/entry/presentation/TimetableImportSheet.kt:194` | `%02d:%02d – %02d:%02d` | srednje |
| 8 | `features/goals/presentation/GoalsScreen.kt:348` | `date_range` z dvema `d. MMM yyyy` | **visoko** |
| 9 | `features/goals/presentation/GoalsScreen.kt:520` | `LLL yy` (Gantt os) | srednje (ozke celice) |
| 10 | `features/goals/presentation/GoalsScreen.kt:597` | `d. MMM` (semantika mejnika) | nizko |
| 11 | `features/goals/presentation/GoalsScreen.kt:660-661` | `d. MMM` × 2 | srednje |
| 12 | `features/goals/presentation/GoalsScreen.kt:721` | `date_range` | **visoko** |
| 13 | `features/goals/presentation/GoalsScreen.kt:797` | `d. MMM · relativno` | **visoko** (dva dela) |
| 14 | `features/goals/presentation/GoalsScreen.kt:917,925` | `Začetek · d. MMM` | srednje |
| 15 | `features/goals/presentation/GoalsScreen.kt:976` | `d. MMM` (vnos napredka) | srednje |
| 16 | `features/goals/presentation/GoalsScreen.kt:1096` | `d. MMM yyyy` | srednje |
| 17 | `features/goals/presentation/GoalsScreen.kt:1184,1192` | `Konec · d. MMM` | srednje |
| 18 | `features/planning/presentation/PlanningSheet.kt:244` | `planning_topic_due` | srednje |
| 19 | `features/planning/presentation/PlanningSheet.kt:276` | datum mejnika | srednje |
| 20 | `features/tasks/presentation/TasksSheet.kt:247` | datum opravila | srednje |
| 21 | `features/timeline/components/TimelineComponents.kt:296,356` | `d. M.` / `carry_in` | srednje |
| 22 | `features/timeline/presentation/DailyTimeline.kt:345` | `begins_on` z `d. M.` | srednje |
| 23 | `features/timeline/presentation/overview/OverviewScreens.kt:137,161,426,472,508,565,626` | `EEE d`, `%02d:%02d`, `d MMMM yyyy`, `MMM`, `date_range`, `d/M`, `d MMM` | **visoko** (mreže z ozkimi celicami + najdaljši vzorec `d MMMM yyyy`) |

### 1.3 Fiksne širine, ki lahko stisnejo besedilo

| Datoteka:vrstica | Vrednost | Zakaj je tvegano |
| --- | --- | --- |
| `core/designsystem/components/TimelineChrome.kt:105` | `widthIn(max = ChipMaxWidth = 190.dp)` | dolg naziv predmeta/kategorije |
| `features/entry/presentation/EntryEditorSheet.kt:221` | `widthIn(max = 180.dp)` | ime predmeta v chipu |
| `features/entry/presentation/EntryEditorSheet.kt:297` | `width(82.dp)` | gumb z dolgim slovenskim napisom |
| `features/timeline/presentation/overview/OverviewScreens.kt:130-210` | `GutterTextWidth`, `dayWidth` | urni žleb in dnevne celice (že merjeno) |
| `features/goals/presentation/GoalsScreen.kt:514-549` | `GanttLaneLabel`, `cellWidth` | Gantt (že merjeno) |
| `features/timeline/presentation/DailyTimeline.kt:393-395` | `RailX`, `SpineWidth` | geometrijsko, ne besedilo |

## 2. Dolgi vertikalni drsniki

| Datoteka | Vrsta | Stanje | Ukrep |
| --- | --- | --- | --- |
| `features/goals/presentation/GoalsScreen.kt:33` | `Column(verticalScroll)` | **en sam neskončen drsnik**: status, stolpci, Gantt, projekti, aktivnosti, mejniki, napredek | zavihki (Projekti / Aktivnosti / Mejniki / Napredek) |
| `features/timeline/presentation/overview/OverviewScreens.kt:399` | `LazyColumn` (leto) | dolg seznam mesecev + radar mejnikov | zložljive sekcije po četrtletjih |
| `features/settings/presentation/SettingsSheet.kt:19,185` | `verticalScroll` × 2 | že razbito na 5 zavihkov ✓ | podsekcije z zlaganjem, kjer je > 6 vrstic |
| `features/planning/presentation/PlanningSheet.kt` | zavihki ✓ | 3 zavihki | — |
| `features/tasks/presentation/TasksSheet.kt:25` | `LazyColumn` ✓ | sekcije z glavami | lepljiva glava |
| `features/entry/presentation/EntryEditorSheet.kt:10` | `verticalScroll` | kratek obrazec | — |
| `features/entry/presentation/TimetableImportSheet.kt` | `LazyColumn` ✓ | seznam ur | — |
| `features/subjects/presentation/SubjectEditorDialog.kt:18,80` | `verticalScroll` × 2 | kratek obrazec | — |
| `features/timeline/presentation/DailyTimeline.kt:20,98` | `LazyColumn` ✓ | dnevni pogled | — |
| `features/timeline/presentation/overview/OverviewScreens.kt:23,102,287` | `LazyColumn` ✓ | teden/mesec/leto | — |
| `core/designsystem/components/RoutineText.kt:285,309` | skelet lista ✓ | vsi listi gredo skozenj | — |

## 3. Pravila, ki jih uvaja ta prenova

1. **Noben pomemben podatek se ne sme končati z `…`.** Datumi, trajanja, imena predmetov in naslovi
   imajo `autoSize` (pisava se pomanjša) ali dve vrstici — nikoli elipse.
2. **En sam vir za oblikovanje datumov** (`core/presentation/DisplayFormat.kt` → `RoutineDate`).
   V UI kodi `DateTimeFormatter.ofPattern` ni več dovoljen; `tools/check_presentation.py` to preveri.
3. **Fiksna širina samo za geometrijo** (žleb, mreža, Gantt), nikoli za besedilo.
4. **En drsnik na zaslon**, razdeljen na zavihke/zložljive sekcije; v listih je glava lepljiva.
5. **Najmanjša berljiva pisava 11 sp** (widget) oziroma 12 sp (aplikacija); `autoSize` se ustavi pri
   tej meji in šele takrat dovoli elipso.
