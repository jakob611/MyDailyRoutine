# Sistematični pregled: odrezano besedilo in dolgi drsniki

Datum: 2026-09-16 · Obseg: **celotna aplikacija** (34 UI datotek, 12.062 vrstic, widget, listi)

Pregled je narejen s skripto (`/tmp/audit_text.py`, `/tmp/audit2.py`), ki za vsak klic `RoutineText` /
`RoutineLabel` / `Text` izlušči argumente in označi tveganja. Nič ni pregledano "na oko" — spodaj je
vsako mesto posebej.

> **Stanje: razrešeno.** Vseh 20 mest z `maxLines = 1` je odpravljenih, vseh 23 mest z datumi gre skozi
> `RoutineDate`, dolgi drsniki so razbiti na zavihke in zložljive sekcije, steklo je vpeljano po vsem
> oknu. Pravila, ki to držijo na mestu, so v razdelku 3 in jih `tools/check_presentation.py` preveri
> ob vsakem zagonu CI — nazaj se ne da zdrsniti po nesreči.

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

## 4. Kaj je bilo narejeno

### 4.1 Besedilo (razdelki 1.1 in 1.3)

| Problem | Rešitev |
| --- | --- |
| 20 mest z `maxLines = 1` | Ni jih več. Oznake gumbov, chipov in zavihkov gredo skozi `RoutineLabel`, ki ima `autoSize = TextAutoSize.StepBased(11.sp, načrtovana velikost, 0.5.sp)`: napis se najprej **skrči**, šele ko pade pod 11 sp, ga odreže elipsa. |
| `softWrap = false` na chipih | Odstranjen povsod — brez ovijanja `autoSize` ne deluje. |
| Velike številke (`displaySmall`) z eno vrstico | `execution_elapsed_minutes`, števec dni do konca pouka: zdaj `RoutineLabel`, se skrči namesto da bi se odrezal. |
| Naslov ure v `TimetableImportSheet` | `maxLines = RoutineTextDefaults.Title` (3 vrstice) namesto 1 — dolg nasiv ure se prebere cel. |
| `widthIn(max = 180.dp)` / `ChipMaxWidth = 190.dp` | Ostane kot **zgornja meja**, ne kot rezilo: znotraj nje se napis skrči. |
| `width(82.dp)` v `EntryEditorSheet` | Ni besedilo, ampak polje za minute (2 števki) — geometrija, ostane. |

### 4.2 Datumi (razdelek 1.2)

Vseh 23 mest je zdaj klicev `RoutineDate` (`core/presentation/DisplayFormat.kt`). En sam vir pomeni, da
isti datum povsod v aplikaciji izgleda enako in da je dolžina napisa znana vnaprej:

| Pomožna funkcija | Primer | Kje |
| --- | --- | --- |
| `tight` | `16. 9.` | mreže, žlebi, značke |
| `dayNumber` | `16.` | mesečni koledar |
| `normal` | `16. sep` | vrstice seznamov, mejniki |
| `normalYear` | `16. sep 2026` | vse, kar sega čez šolsko leto |
| `withWeekday` | `sre, 16. sep` | glave zaslonov in listov |
| `withWeekdayYear` | `sre, 16. sep 2026` | načrtovanje |
| `weekdayTight` | `sre 16` | tedenska mreža |
| `weekdayName` / `weekdayFull` | `sreda` / `sreda 16. september` | tedenski opisi |
| `full` / `spoken` | `16. september 2026` / `sreda, 16. september 2026` | urejevalniki |
| `monthAndYear` / `monthTight` / `axisLabel` | `september 2026` / `sep` / `sep 26` | mesec, leto, Gantt |
| `axisDay` | `16/9` | radar mejnikov |
| `timeRange` / `clock` | `07:30–09:00` / `07:30` | bloki, obvestila, widget |
| `range` / `wideRange` | `16. sep – 20. jun` | obdobja |

`DateTimeFormatter` se v `app/src/main` pojavi **samo** v `DisplayFormat.kt` — to preverja CI, zato tudi
widget in obvestila (ki nista Compose) ne moreta uvesti svojega vzorca.

### 4.3 Dolgi drsniki (razdelek 2)

| Zaslon | Prej | Zdaj |
| --- | --- | --- |
| Cilji | en drsnik ~1000 dp: status, Gantt, vse aktivnosti, vsi mejniki | vrstica projektov (drsijo v stran) + štirje zavihki: **Pregled / Aktivnosti / Mejniki / Napredek**; vsak zavihek je kratek |
| Leto | štiri zaporedne sekcije brez konca | odštevalnik ostane viden, **Dvanajst mesecev ravnotežja**, **Radar mejnikov** in **Prostor za oddih** so zložljivi (`CollapsibleSection`) |
| Vsi listi | glava lista je drsila stran | glava in noga vsakega lista sta **lepljivi in stekleni**, telo drsi pod njima |
| Nastavitve | 5 zavihkov ✓ | nespremenjeno — zavihki že razbijejo vsebino, steklena glava zdaj drži naslov |

Nov `CollapsibleSection` (v `RoutineText.kt`) je edini način zlaganja: naslov + ena vrstica konteksta
osta vidna, chevron se zavrti, celotna glava je cilj klika, stanje pa preživi obrambo zaslona
(`rememberSaveable`).

## 5. Steklo in barva: odločitve in razlogi

### 5.1 Tekoče steklo (`io.github.kyant0:backdrop:1.0.0`)

Izbrana različica **1.0.0**: zgrajena s Kotlinom 2.2.21 in Compose 1.9.4, torej pade v obstoječ
toolchain brez nadgradnje česarkoli (1.0.6 bi potegnila Kotlin 2.3.10, 2.x je že KMP in bi zahtevala
`backdrop-android`). API je preverjen neposredno na tagu `1.0.0` v izvorni kodi knjižnice.

Tri pravila, ki jih drži `core/designsystem/glass/RoutineGlass.kt`:

1. **Steklo samo na navigacijski plasti** (Apple HIG): vrhnja vrstica, hitri gumb, glava in noga lista.
   Vsebina — seznami, kartice, mreže — stekla nima; ona je tisto, kar se lomi.
2. **Steklen element je sestra plasti, nikoli njen potomec.** Vozlišče, ki ima hkrati `layerBackdrop(b)`
   in `drawBackdrop(b)`, riše samo sebe vase in podre render thread (SIGSEGV). Zato:
   `routineBackdropLayer` na vsebino, `routineGlass` na plutajočo kromo zunaj nje. Listi živijo v svojem
   oknu in ne morejo vzorčevati okna aplikacije, zato dobijo **svoj** `rememberLayerBackdrop`, ki ga
   napolni barva površine lista.
3. **Berljivost ni nikoli žrtvovana efektu.** Vsak panel čez lomljeno ozadje nariše barvano površino
   (nevtralna kroma 0.58 α, listi 0.72 α), barvni gumb pa najprej `BlendMode.Hue` in šele nato prosojen
   nanos — tako jantar obdrži svojo barvo, vsebina pod njim pa svojo senčenje. Pod Androidom 12
   (`RenderEffect` ne obstoja) isti panel nariše ustrezno polno površino: nič ne počí, nič ne utripa.

Moči so zbrane v enem `GlassRole` (Bar 8 dp blur, Sheet 10 dp + globina, Chip 3 dp, Control 4 dp +
globina), da vsaka vrstica ne izmišljuje svojega polmera. `lens` zahteva `CornerBasedShape` in se pod
API 33 sam izklopi, zato so vse steklene oblike (`GlassTopBar`, `GlassSheetHeader`, `GlassSheetFooter`,
`Pill`) zaokrožene na vseh štirih kotih.

Korenski zaslon zato **ni več `Scaffold`**: telo Scaffolda se začne pod vrhnjo vrstico, tam pa ni ničesar
za lomiti. Zdaj je vsebina polno razprta po oknu, vrstica in gumb pa ležita nanjo — višina vrstice se
izmeri (`onSizeChanged`) in zasloni se ji sami umaknejo (`topInset`), zato se vrstice res drsijo pod
steklo.

### 5.2 Paleta

Globok indigo + jantar na temnem ogljiku, izpeljana po HCT (Material 3) in ne po "na oko" izbranih
hexih:

* **Ogljik, ne črna** — `Background` ni `#000`, ker bi na OLED izgubil vse višinske nivoje; površine
  (`Surface1`, `Surface2`, `SheetSurface`) tvorijo lestvico, ki jo oko bere kot globino.
* **Ambientni nanos** — navpični grafitni prehod + ena indigo in ena jantarja svetloba pod 6 % α. Dve
  nalogi: višinska berljivost na OLED in nekaj vsebine za lomiti tam, kjer vrstica lebdi nad praznino.
* **Poudarki nosijo pomen** — jantar je samo za dejanje (hitri gumb), žajbelj za opravljeno, grimasta za
  zamujeno/rdečo nit, šolska modra za pouk. Noben zaslon ne barva poljubno.
* **Brez barv zunaj palete** — v `app/src/main` ni niti enega `Color(0x…)` ali `Color.Red` zunaj
  `designsystem/theme`; to preverja CI.
* **Kontrast** — besedilo na površinah je nad 4.5:1 (`TextPrimary`/`TextSecondary`), na steklu pa ga
  drži barvani nanos iz pravila 3 zgoraj.

### 5.3 Kaj preverja `tools/check_presentation.py`

1. `DateTimeFormatter` samo v `DisplayFormat.kt`.
2. Noben `maxLines = 1` zunaj `RoutineText.kt` (oznake se krčijo, ne režejo).
3. Vsak `ModalBottomSheet` uporablja `RoutineSheetScaffold`/`RoutineSheetListScaffold` in
   `RoutineColors.SheetSurface`.
4. `RoutineBackdropProvider` in `routineBackdropLayer` natanko enkrat; vsak `.layerBackdrop` ima svoj
   `rememberLayerBackdrop`.
5. Pet zaslonov (`DailyTimeline`, `Weekly/Monthly/Yearly`, `GoalsScreen`) sprejme `topInset: Dp`.
6. Nobena barva zunaj `designsystem/theme`.
