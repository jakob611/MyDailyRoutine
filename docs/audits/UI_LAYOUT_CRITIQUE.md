# Kritična analiza vizualne plasti (prekrivanje, prelom, elipsa, velikost gumbov)

**Status:** samo analiza, brez sprememb kode.
**Datum:** 2026-09-14 · **Veja:** `arena/01a0a0f9-mydailyroutine` · **Osnova:** `8e47bf8`
**Obseg:** `app/src/main/java/**/presentation/**`, `features/timeline/components`, `widget`, `core/designsystem`, `res/values/strings.xml`, `res/layout/widget_*.xml`

---

## 0. Odgovor na vprašanje v eni vrstici

Da — vse tri zahteve so v Composeju dosegljive in so *pravilo*, ne izjema:

| Zahteva | Mehanizem | Stanje v projektu |
|---|---|---|
| Besedila se ne smejo prekrivati | nič `Modifier.offset` za besedilo; absolutno pozicioniranje samo prek `Layout`/`SubcomposeLayout` z merjenjem; `zIndex` + neprosojno ozadje samo za namensko plast (NOW) | ❌ kršeno na 4 mestih (NOW-marker, tedenski grid, Gantt, drag-preview) |
| Besedilo ne sme teči navpično (1 črka na vrstico) | `maxLines` + `TextOverflow.Ellipsis` na vsakem `Text` v omejeni širini; skupine gumbov v `FlowRow` namesto v `Row`; `widthIn(min=…)` | ❌ sistemsko: od **495** `Text(...)` jih ima **36** (7 %) `maxLines`/`TextOverflow` |
| Gumb se mora razširiti ali pa povedati, kaj je | `FlowRow` ( prelom v novo vrsto) → `weight(1f)` + elipsa → `autoSize` (Material3 1.4) → `TooltipBox`/razširljiv naslov | ❌ elipsa obstaja, a brez širitve gumba, brez `tooltip`, brez razkritja celotnega naslova |

Material3 **1.4.0** (priložen BOM `2025.09.01`, `gradle/libs.versions.toml:11`) že vsebuje `Text` z `autoSize = TextAutoSize.StepBased(...)`, Compose 1.8+ pa `TextOverflow.MiddleEllipsis` in **stabilen** `FlowRow`/`FlowColumn` — torej orodja so že v odvisnostih, samo uporabljena niso (aplikacija še vedno nosi `@OptIn(ExperimentalLayoutApi::class)` za `FlowRow`, ki od 1.8 ni več eksperimentalen).

---

## 1. Zakaj je trenutni videz "obupen" — 5 sistemskih vzrokov

1. **Ni besedilne pogodbe (text contract).** `Text` se kliče neposredno povsod (495×), brez skupnega ovoja, brez privzetega `maxLines`/`overflow`. Posledica: vsak zaslon se obnaša drugače, napake pa so odvisne od dolžine slovenskih nizov (ki so v povprečju daljši od angleških: *Shrani in dodaj naslednjo uro*, *Zaključi in shrani*, *Sredina dneva: prostor za lažji ritem*).
2. **Ni prilagajanja na širino in velikost pisave.** Projekt ne uporablja `WindowSizeClass`, `BoxWithConstraints` je na **dveh** mestih, `fontScale` ni nikjer obravnavan, `LocalDensity` se bere samo za pretvorbo px↔dp pri vlečenju (`TimelineComponents.kt:70`). Vse ostalo je trdno v `dp` → ob `fontScale ≥ 1,3` ali na 320‑dp zaslonu se vse, kar je trdno, zlomi.
3. **`Row` brez uteži z več besedilnimi otroki.** 93 `Row(...)`; na ~30 mestih je v vrsti več `Text`/`TextButton` brez `weight(1f)` in brez `maxLines` → Compose zadnjemu otroku dodeli ostanek širine, besedilo pa se prelomi **na vsaki črki** (navpično). To je točno pojav, ki ga opisuješ.
4. **Absolutno pozicioniranje z `Modifier.offset` namesto pravega layouta.** `offset` ne sodeluje pri merjenju: sosedje se ne umaknejo, višina se ne poveča → besedilo gre čez sosednje besedilo (tedenski grid, Gantt, NOW-marker).
5. **Ni vizualne hierarhije in ni merjenja.** Glava zaslona zavzame ~170–190 dp (trivrstični `topBar`), "povzetek dneva" doda še 8 elementov, preden pride prvi blok; razmiki so 4/5/6/8/10/12/14/16/18/20/22/24 dp brez sistema; `TimelineUiTest` zajema posnetke le pri privzeti velikosti pisave in ne preverja preloma/elipse.

---

## 2. Konkretne napake po sklopih (z vrsticami in izračuni)

### 2.1 Prekrivanje besedila — resnične kršitve

| # | Mesto | Kaj se zgodi |
|---|---|---|
| A1 | `TimelineComponents.kt:198` + `NowMarker` (`:232`) | NOW-marker je **brat** vrstice z ure in kartico, postavljen z `offset(y=…)`, `fillMaxWidth()`. Rdeča črta prečka časovni stolpec (`TimeGutter`) in besedilo kartice; podlaga je samo za uro (`Modifier.background(RoutineColors.Background)`), ne za črto. Ker `heightPx` meri celoten `Box` (vključno z razširjeno kartico), marker ob razširitvi skoči na napačno višino. |
| A2 | `ManagedRoutineCard.kt:70` | Enak vzorec znotraj kartice: `NowMarker(...).offset(y=…)` teče **prek** naslova in opisa spanca/odmora. |
| A3 | `DailyTimeline.kt:166` (`TimelineGap`) | `offset(y = (height - 12.dp) * nowFraction)` — pri `minutes < 15` je `height` 12 dp, offset pa je lahko negativen/nič, marker pa leže na besedilo "… prostega časa". |
| A4 | `OverviewScreens.kt:103–113` (tedenski grid) | Bloki so `offset(x,y)` + `width(laneWidth-4)` + `height(...)`. Pri dveh vzporednih blokih je `laneWidth = dayWidth/2 ≈ 47 dp` → **39 dp** za besedilo. `Column` nima omejitve višine, zato besedilo 15‑minutnega bloka (18 dp) **štrli iz barve čez sosednji blok**. To je dobesedno "en čez drugega". |
| A5 | `GoalsScreen.kt:267–318` (Gantt) | Meseci so `Box(offset(x = laneLabel + xOf(markerDate)))` brez `widthIn` → pri `cellWidth < 40` se oznake prekrivajo (preskok `index % 2` pomaga le delno: `"sep 26"` je ~38 dp, razmik pa 40 dp). Vrstice aktivnosti: `width = (xOf(end)-left).coerceAtLeast(18.dp) - 4.dp` = **14 dp**, naslov pa `labelSmall maxLines=1 ellipsis` → vidiš samo `…`. |
| A6 | `TimelineComponents.kt:97` (drag) | `graphicsLayer { translationY = dragY; scaleX/Y = 1.02 }` + `zIndex(1f)`: med vlečenjem kartica prekriva sosednji kartici, NOW-marker pa zaradi `zIndex` utripa nad/pod njo. |
| A7 | `TimelineComponents.kt:118` | `heightIn(min = maxOf(112.dp, 2.dp * block.durationMinutes))` → 4‑urna priprava dobi 480 dp prazne kartice (besedilo zgoraj, nič spodaj), 15‑minutni blok pa 112 dp. Višina ni časovno skladna, prazen prostor pa deluje kot napaka. |

### 2.2 Navpično lomljenje besedila (1–2 črki na vrstico)

| # | Mesto | Zakaj |
|---|---|---|
| B1 | **`GoalsScreen.kt:180–187`** — vrstica "Besede: x / y" + `+100 besed`, `+250 besed`, `+500 besed` | Štirje otroci z `weight(1f)`, `contentPadding = PaddingValues(horizontal = 2.dp)`, `spacedBy(2.dp)`. Na 360 dp: 328 − 28 (padding kartice) − 6 (razmiki) = 294 → **~73 dp na gumb**; M3 `TextButton` ima 24 dp notranjega robu + `minimumInteractiveComponentSize` 48 dp → za napis ostane **~45–49 dp**, `"+500 besed"` pa potrebuje ~78 dp → prelomi se v 2–3 vrstice, pri `fontScale 1,3` v stolpec črk. **To je najverjetneje tvoj primer.** |
| B2 | `GoalsScreen.kt:441–444` — "Ure" + `+30 min`, `+1 h`, `+2 h` | `weight(1f)` na napisu, trije gumbi brez uteži → napis "Ure" dobi 328 − 3×~72 = **~86 dp**, pri večji pisavi 0 in se zlomi navpično. |
| B3 | `GoalsScreen.kt:336–338`, `:365–368` — naslov seznama + `Dodaj aktivnost` / `Dodaj mejnik` | `Text(weight(1f))` + `FilledTonalButton` brez omejitve → naslov "Aktivnosti" ostane brez širine. |
| B4 | `GoalsScreen.kt:373–379` — vrstica mejnika: `Checkbox` + naslov (2 vrsti) + `Uredi` | Gumb `Uredi` nima `weight`; pri daljšem naslovu mejnika in večji pisavi se naslov stisne na ~10 znakov/vrstico. |
| B5 | `GoalsScreen.kt:192–197` — "Naslednji mejnik" + naslov (`weight(1f)`, elipsa) + relativni datum | Dva napisa brez uteži skupaj ~210 dp → naslov mejnika dobi ~70 dp in je takoj `…`; relativni datum ("čez 34 dni") se prelomi v 2 vrstici. |
| B6 | `DailyTimeline.kt:76–82` — `Uskladi zamudo` (FilledTonal) + `Čakalna vrsta: N` (TextButton) | `Row` brez `FlowRow` in brez uteži: skupaj ~250 dp pri 1,0×, ~325 dp pri 1,3× → na 320‑dp zaslonu se drugi napis zlomi navpično, pri 1,3× pa na vseh. |
| B7 | `DailyTimeline.kt:46–50` — trije `MetricTile(weight(1f))` | `titleLarge` (22 sp) v ~96 dp celici: `"3 h 15 min"` → 2 vrstici že pri privzeti pisavi; `"Opravljeno"` → 2 vrstici; pri 1,3× → 3 vrstice in neenake višine ploščic. Manjka `maxLines`/`autoSize`. |
| B8 | `RoutineApp.kt:133–138` — `SingleChoiceSegmentedButtonRow` (Dan/Teden/Mesec/Leto) | 4 enaki segmenti na ~328 dp = 82 dp na segment; M3 segment zahteva 24 dp robov → 58 dp za napis. `"Mesec"` pri 1,3× ne gre v eno vrstico → **navpično lomljenje v navigaciji**, kar je najbolj opazno mesto v aplikaciji. |
| B9 | `EntryEditorSheet.kt:157–163` — segmenti Blok/Rok/Test | Isti mehanizem; pri `fontScale 1,45` prelom. |
| B10 | `SummaryComponents.kt:31–38` — `DateNavigator` | Naslov ima `maxLines=2`, a `TextButton("Danes")` + 3× `IconButton` brez uteži: pri daljšem naslovu ("šol. leto 2026/2027") gumb `Danes` dobi ostanek → prelom. |
| B11 | `TasksSheet.kt:133–137` — naslov razdelka + `Počisti` | `Text(weight(1f))` + `TextButton` brez `maxLines`; pri `"Opravljeno (12)"` + `Počisti` na 320 dp → prelom. |
| B12 | `SubjectEditorDialog.kt:57–60` — `dismissButton = Row { "Izbriši predmet", "Prekliči" }` | Vrstica gumbov v `AlertDialog` je omejena (~240–260 dp), oba napisa skupaj ~230 dp → pri večji pisavi se `Prekliči` stisne ali odreže (M3 `AlertDialog` gumbov ne lomi v novo vrstico). |
| B13 | `ActualCompletionDialog.kt:39–43` — `Zaključi in shrani` + `Prekliči` | Enako: ~225 dp proti ~240 dp na voljo — na robu preloma. |
| B14 | `WeekdayPicker.kt:31–34` — `Delovniki` + `Vsak dan` | `Row` brez uteži; ob `FlowRow` zgoraj deluje neusklajeno, pri 1,3× se prelomi. |
| B15 | `EntryEditorSheet.kt:234–239` — `Checkbox` + `"Po pouku dodaj odmor"` (`weight(1f)`) + `OutlinedTextField(width = 82.dp)` | Polje je **trdnih 82 dp** ne glede na `fontScale` → oznaka "Min" in besedilo se stisneta, sosednji napis pa zlomi. |
| B16 | `OverviewScreens.kt:299–306` — `MilestoneRadar` | `Row(...).height(130.dp)` z vsebino 14 + 6 + do 75 + 6 + 14 = **115–131 dp** → pri `count == largest` spodnji datum (`"14/9"`) odreže, ker `Column` nima `heightIn` in vrstica je trdno 130 dp. |
| B17 | `OverviewScreens.kt:239–248` — letne kartice mesecev | Tri kartice z `weight(1f)` v `Row`: `"Prosti dnevi: 12"` (`labelSmall`) v ~54 dp → 2 vrstici; ker `Row` nima `height(IntrinsicSize.Min)`, so kartice **različnih višin**. |
| B18 | `OverviewScreens.kt:250–258` — `ListItem` za počitnice | `headlineContent = Text(title)` brez `maxLines`; dolg naslov ("Novoletne počitnice …") potisne `trailingContent` (število dni) v prelom. |

### 2.3 Elipsa, ki nič ne pove (informacijska izguba)

| # | Mesto | Problem |
|---|---|---|
| C1 | `GoalsScreen.kt:305` | Naslov aktivnosti v Ganttu: `maxLines=1` + elipsa v ~14–80 dp → `Raz…`, `Pri…`. Uporabnik ne more ugotoviti, kaj je aktivnost; tap odpre urejevalnik, a ni **nobene** vizualne affordance (ni `TooltipBox`, ni `contentDescription` z naslovom na samem besedilu — `semantics` je le na tedenskih blokih). |
| C2 | `OverviewScreens.kt:111–112` | Naslov bloka v tedenskem gridu pri `< 40 min` = 1 vrstica v 39–90 dp → `Mat…`; trajanje se izpiše šele pri `≥ 60 min`, zato je 45‑minutni blok brez obeh podatkov. |
| C3 | `TimelineComponents.kt:124` | Naslov bloka: `maxLines = if (expanded) 6 else 2` + elipsa — dobro, a razširitev (`expanded`) je edina pot do celotnega naslova in ni nakazana (ni "več"/puščice); `contentDescription` kartice je `"Za premik pridržite in povlecite…"` (`:99`) — **ne vsebuje naslova bloka**, zato TalkBack prebere navodilo namesto vsebine. |
| C4 | `TimelineComponents.kt:212–217` (`MetaChip`) | `MetaChip` nima `maxLines` niti `widthIn`; niz `circadian_hint` = *"Sredina dneva: prostor za lažji ritem"* (36 znakov, `labelSmall` + 0,5 sp razmik, krepko) → v `FlowRow` zasede celo vrstico in potisne kategorijo ter `ZDAJ` v drugo vrsto; vizualno je to "čip, ki je daljši od naslova". |
| C5 | `OverviewScreens.kt:302` | `Text(marker.title, fontWeight = FontWeight.Medium)` brez `maxLines` — dolgi naslovi mejnikov (`"Oddaja EE — osnutek 4000 besed pri mentorju"`) se lomijo v 3–4 vrstice in raztegnejo kartico. |
| C6 | `TimelineComponents.kt:158–170` | Štirje pogojni napisi (`drag_minutes`, `carry_in`, `school_inactive`, `overlap_notice`, `clock_change`) so izpisani **eden za drugim brez `maxLines`**; `clock_change` je 55 znakov → 2–3 vrstice v že tako natrpani kartici. |
| C7 | `DailyTimeline.kt:59–61` | `dueTasks.take(2).joinToString(" · ")` + `maxLines=2` → pri dveh dolgih naslovih vidiš `Matematika · Angleščina — esej o…` in ne veš, ali sta 2 ali 12 nalog. Manjka `"… +N"`. |
| C8 | `AgendaWidget.kt:139–158` + `res/layout/widget_text.xml` | `TextView` ima `maxLines=3 ellipsize=end`, a `GlanceModifier.defaultWeight()` na `RemoteViews` z `layout_width="match_parent"` ni zanesljiv → naslov vrstice in status ("NASLEDNJE") si delita vrstico brez elipse; `"Osveženo ob 07:12"` + `"＋ Dodaj"` v nogi sta brez `maxLines` na Glance strani. |

### 2.4 Geometrija, zamiki in "neenakomernost"

- **Trije različni časovni stolpci:** `TimeGutter` = 60 dp z `end=8` → besedilo na 0–52 dp (`TimelineComponents.kt:205`), `MilestoneCard` = 60 dp **brez** odmika (`:253`), `ManagedRoutineCard` = 60 dp brez odmika (`:38`), hrbtenica pa risana pri **53 dp** (`:84`) in `Spacer(53.dp)` v `TimelineGap` (`DailyTimeline.kt:162`) ter **48 dp** v tedenskem gridu (`OverviewScreens.kt:72,82`). Rezultat: navpična črta teče 1–7 dp levo od besedil ur, ure mejnikov in rutin pa niso poravnane z urami blokov. To je glavni razlog za "nepospravljen" občutek dnevnega pogleda.
- **Glava zaslona** (`RoutineApp.kt:107–140`): `TopAppBar` z dvovrstičnim naslovom (`app_name` + `app_tagline` v `labelSmall`) + 4 ikone, pod njo `DateNavigator` **brez vodoravnega odmika** (ikone so na x=0, vsebina pa na 16 dp → vidna neosna), pod tem še segmentirana vrstica. Skupaj ~170–190 dp od ~640 dp → 27 % zaslona je krmilnikov, preden se začne vsebina.
- **Povzetek dneva** (`DailyTimeline.kt:44–84`): naslov, 3 ploščice, trak obremenitve, kartica nalog, kartica izvajanja, 2 gumba, namig, rezerva, predlogi, opozorilo koledarja = do **11 skladanih elementov** z različnimi stili (`titleLarge`, `labelMedium`, `bodySmall` × 3 barve). Brez grupiranja v kartico in brez `Divider` deluje kot debug izpis.
- **Razmiki brez sistema:** `spacedBy` vrednosti 2, 3, 4, 5, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24 in odmiki 4/6/8/10/12/14/16/24 — ni lestvice (4/8/12/16/24), zato se kartice med zasloni ne ujemajo.
- **Tipografija** (`Type.kt`): `bodySmall` 12 sp in `labelSmall` **11 sp krepko + 0,5 sp razmik** (v tedenskem gridu celo **10 sp**, `OverviewScreens.kt:112`) — pod M3 minimumom 12 sp; `tnum` (`fontFeatureSettings`) je vsiljen **vsem** slogom, tudi naslovom in opisom, kjer tabularne številke niso potrebne; `displaySmall` = 36 sp `Black` za odštevanje letnika (ob velikem `fontScale` zasede dve vrstici).
- **Dotiki:** `Checkbox(...).size(40.dp)` (`TimelineComponents.kt:135`), `IconButton.size(32.dp)` (`TasksSheet.kt:103,214`), celice Gantta 14–20 dp, pasovi prekrivanja, `Dot(6.dp)` — pod 48 dp; `Modifier.minimumInteractiveComponentEnforcement` ni nikjer uporabljen.
- **Barve:** `TextMuted #64748B` na `#000000` = **4,77:1** (za 11–12 sp je to pod priporočilom 4,5:1 za AA le "komaj"), in ta barva se uporablja za *pomembna* stanja (pretekli bloki, čas konca, opisi). Na Ganttovih pasovih je črno besedilo na `accent.copy(alpha = 0.85f)` — pri vijolični (`#8B5CF6`) je kontrast ~3,9:1.
- **Dvojni sistem za widget:** `res/layout/widget_text*.xml` (TextView) + Glance → dva vira resnice za velikosti/elipso; `widget_loading.xml` podvaja vizualno identiteto.

### 2.5 Kaj manjka na ravni infrastrukture

- Ni `WindowSizeClass`/`BoxWithConstraints` prilagajanja (2 uporabi `BoxWithConstraints` v celotni aplikaciji).
- Ni obravnave `fontScale` (niti `nonScaledSp`, niti "kompaktne" različice postavitev).
- Ni `TooltipBox` / `Tooltip` nikjer → elipsa je brez razkritja.
- Ni `@Preview` komponent (0 pojavitev), zato se prelomi odkrijejo šele na napravi.
- `TimelineUiTest` (androidTest) zajame posnetke `capture("01-day")` … pri privzeti pisavi in ne trdi ničesar o `maxLines`, prelomu ali prekrivanju; ni Paparazzi/Roborazzi/`showkase`.
- `lint { abortOnError = true }` je vklopljen, a manjkajo ustrezna pravila (npr. `ContentDescription`, `UnusedResources`) in — kar je ključno — **ni statične kontrole, ki bi prepovedala goli `Text` v `Row` brez uteži**. To je mogoče doseči z detekt/Compose-lint pravilom ali z lastno "design-system only" mejo (glej §3.1).

---

## 3. Predlogi

### 3.1 Pravila, ki jih zapišemo enkrat (design-system pogodba)

1. **Vsak `Text` dobi `maxLines` in `overflow`** — prek ovoja, ne ročno:
   ```kotlin
   // core/designsystem/components/RoutineText.kt (predlog, še ne obstaja)
   @Composable fun RoutineText(text: String, style: TextStyle, modifier: Modifier = Modifier,
       maxLines: Int = 2, overflow: TextOverflow = TextOverflow.Ellipsis, ...)
   ```
   Pravilo: `maxLines = 1` za čase/oznake/števce, `2` za naslove v karticah, `3–4` za opise; `Int.MAX_VALUE` samo za odstavke v listih (opozorila, namigi).
2. **`Row` sme imeti največ enega otroka brez uteži.** Vsi ostali dobijo `Modifier.weight(1f)` + elipso. Kjer sta dva napisa enakovredna → `FlowRow`.
3. **Skupine gumbov = `ActionButtonRow` (FlowRow)**: `FlowRow(horizontalArrangement = spacedBy(8.dp))` + `Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)` na vsakem gumbu. Gumb se **ne sme** stisniti; če ni prostora, gre v novo vrstico. Za primere, ko mora ostati v eni vrstici: `weight(1f)` + `maxLines=1` + `autoSize` + `TooltipBox`.
4. **Besedilo se nikoli ne postavlja z `Modifier.offset`.** Za časovne mreže (tedenski grid, Gantt) uporabimo `Layout { measurables, constraints -> ... }`: izmerimo otroke, nato jih postavimo; če izmerjena višina besedila presega višino bloka, **izpustimo besedilo** (ne odrežemo ga) in prikažemo samo barvo + `semantics`/tooltip.
5. **Ena konstanta za časovni stolpec**: `val GutterWidth = 64.dp` v `DesignSystem.kt`, uporabljena v `TimeGutter`, `MilestoneCard`, `ManagedRoutineCard`, `TimelineGap`, hrbtenici (`drawBehind`) in tedenskem gridu.
6. **Lestvica razmikov**: `RoutineSpacing { xs=4, sm=8, md=12, lg=16, xl=24 }` in uporaba samo teh vrednosti.
7. **Najmanjša velikost pisave 12 sp**; `labelSmall` → 12 sp brez 0,5 sp razmika pri ALL-CAPS (ali pa ohrani razmik, a 12 sp). 10–11 sp se umakne.
8. **Dotik ≥ 48 dp** (`Modifier.minimumInteractiveComponentEnforcement(true)` ali `defaultMinSize`), razen pri gostih mrežah, kjer je cilj tap celotne celice.

### 3.2 Kako preprečiti prekrivanje (A1–A7)

- **NOW-marker** naj ne bo brat z `offset`, ampak **vrstica v `LazyColumn`/`Column`** (kot je že delno v `DailyTimeline.kt:120–124` za "now-before"), ali pa `Box` z `Alignment.TopStart` + `padding(top = progress * height)` **in** `zIndex(2f)` + neprosojno podlago čez celotno širino (`Surface(color = Background)`), da rdeča črta ne teče čez besedilo. Če ostane `offset`, potem obvezno: `clipToBounds()` na starša, podlaga za cel marker in `progress` izračunan iz višine **kartice**, ne `Box`a.
- **Drag-preview**: namesto `translationY` na pravi kartici uporabi `Modifier.graphicsLayer` na **kopiji** (overlay) ali `androidx.compose.foundation.draganddrop`-sloj; prava kartica naj ohrani svojo geometrijo, sosedje pa naj se animirajo z `animateItem()` (že obstaja). Trenutno `zIndex` + `scale` povzročita prekrivanje sosednjih kartic.
- **Tedenski grid**: `Layout` z izmerjenimi bloki; pravilo "če `blockHeight < textHeight + 8.dp` → brez naslova, samo barva + `contentDescription`". Lane naj ima `widthIn(min = 56.dp)` in grid naj se takrat **vodoravno pomika** (že se, `horizontalScroll`), namesto da se pasovi stisnejo na 39 dp.
- **Gantt**: enako; dodatno `monthCount` naj se izračuna iz dejanske širine (`BoxWithConstraints` že obstaja) — `cellWidth` naj bo `(maxWidth - laneLabel) / monthCount` **brez** `coerceIn(20f, 64f)`, ki danes povzroči, da je grid širši od vsebnika, a se ne more pomikati (širina je odvisna od `monthCount*cellWidth`, ne od `maxWidth`).
- **`heightIn(min = 2.dp * minutes)`** → zamenjaj z `minHeight = 88.dp` in *notranjo* časovno lestvico samo v tedenskem pogledu; dnevni pogled je seznam, ne časovnica, in naj bo enakomeren.

### 3.3 Kako preprečiti navpično lomljenje (B1–B18) — vzorci

```kotlin
// 1) Skupina dejanj: prelom namesto stiskanja
@Composable fun ActionRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    FlowRow(modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) { /* otroci z defaultMinSize(48.dp) */ }
}

// 2) Gumb, ki se mora prilagoditi: najprej širina, potem manjša pisava, potem elipsa + tooltip
@Composable fun AdaptiveTextButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    TooltipBox(positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        TextButton(onClick, modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp, stepSize = 1.sp))
        }
    }
}

// 3) Naslov + dejanje: naslov vedno dobi ostanek
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    RoutineText(title, titleLarge, Modifier.weight(1f), maxLines = 2)
    ActionChip(...) // ali ikona + contentDescription, če < 360 dp
}
```

Za `SegmentedButtonRow` (B8/B9): M3 ne dovoli preloma, zato (a) skrajšaj nize (`Dan/Ted/Mes/Let` ni sprejemljivo) → bolje **(b) zamenjaj s `ScrollableTabRow` ali `SingleChoiceFilterChipRow` (`FlowRow`)**, ali **(c) ohrani segmente in dodaj `autoSize` + `maxLines=1`** ter preveri pri `fontScale = 1,45` in 320 dp.

Za `MetricTile` (B7): `Column(Modifier.weight(1f))` + `RoutineText(value, titleLarge, maxLines = 1, autoSize = StepBased(14.sp, 22.sp))` + `RoutineText(label, labelMedium, maxLines = 1)`; dodaj `Row(Modifier.height(IntrinsicSize.Min))`, da so ploščice enake višine.

Za `+100/250/500 besed` (B1): `FlowRow` in ** brez `weight(1f)`** na gumbih; napis "Besede: 1 240 / 4 000" v svojo vrstico (`fillMaxWidth`, `maxLines=1`). Če mora ostati v eni vrstici: `OutlinedButton` z `contentPadding = PaddingValues(horizontal = 10.dp)` + `autoSize` + napis `+100` (enota "besed" gre v `contentDescription`).

### 3.4 Kako narediti elipso uporabno (C1–C8)

1. **Razkritje celotnega besedila** — trije nivoji:
   - `TooltipBox` na vseh elipsiranih napisih (Gantt, tedenski grid, čipi).
   - Tap na kartico bloka že odpre razširitev (`expanded`) → dodaj vidno puščico/`"Več"` in v razširjenem stanju `maxLines = Int.MAX_VALUE`.
   - Za dolge naslove mejnikov/nalog: podnaslov `"… +N"` namesto tihega `take(2)`.
2. **Nikoli ne odreži podatka, ki je edini identifikator.** V tedenskem gridu: če ni prostora za naslov, prikaži **kratek** identifikator (prve 3 črke + barva predmeta) ali samo barvni pas z `contentDescription`; Gantt: naslov **nad** pasom, če je pas ožji od ~60 dp (pas ostane, besedilo se preseli v vrstico lane).
3. **`semantics`/`contentDescription` naj vsebuje naslov**, ne navodila (`TimelineComponents.kt:99` danes prebere "Za premik pridržite in povlecite" namesto naslova bloka).
4. **`MiddleEllipsis`** za datume in "Predpisan test · Matematika" — ohrani pomemben desni del (`Compose 1.8+`).

### 3.5 Prilagoditev na velikost pisave in zaslon

- Dodaj `androidx.compose.material3.adaptive` (`WindowSizeClass`) in tri razrede: `< 360 dp` (kompaktno: ikone namesto napisov na sekundarnih dejanjih, 2 ploščici namesto 3), `360–600` (zdajšnje), `> 600` (dvostolpčni `ListDetail` za dan/urnik in cilje).
- `fontScale`: za meritve uporabi `BoxWithConstraints` + `LocalDensity.current.fontScale`; kjer je prostor trdno določen (ure v stolpcu, Gantt), uporabi `nonScaledSp`-podoben pristop (`(12 / fontScale).sp`, omejeno na `≥ 10 sp`) — a **samo** za številčne oznake, ne za naslove.
- `TimeGutter` naj bo `width(IntrinsicSize.Min)` z `widthIn(min = 56.dp, max = 72.dp)` namesto trdnih 60 dp, da `fontScale` ne razbije poravnave.

### 3.6 Proces in preverjanje (da se ne vrne)

1. **Compose Multiplatform screenshot testi (Paparazzi/Roborazzi)** za 8 ključnih komponent × 3 konfiguracije: `360×640 @1,0`, `320×568 @1,0`, `360×640 @1,45`. V CI (`.github/workflows/android.yml`) kot ločen korak.
2. **Test preloma**: v `TimelineUiTest` dodaj `compose.onNode(...).fetchSemanticsNode().config[TextStyle]` + `onNodeWithText(...).getUnclippedBoundsInWindow()` in trdi, da višina napisa ne presega pričakovane (npr. `assert(height <= 2 * lineHeight)`), ter da nobena dva vozlišča nimata preseka mej (`assertIsDisplayed` + bounds check).
3. **Detekt/Compose-lint pravilo**: prepovej `androidx.compose.material3.Text(` zunaj `core/designsystem/components` — tako vsi napisi tečejo čez `RoutineText`, kontrola pa je statična in ne more nazadovati.
4. **`@Preview` za vsako komponento** z dolgimi nizi: dodaj `values-sl/strings_preview.xml`-podoben nabor "najdaljših" nizov (npr. `"Predpisan test · Matematika in računalništvo"`) in ga uporabi v preview/testih.

### 3.7 Prednostna lestvica (učinek / trud)

| Prioriteta | Ukrep | Učinek | Trud |
|---|---|---|---|
| **P0** | `RoutineText` + `ActionButtonRow`/`AdaptiveTextButton` v `core/designsystem`, zamenjava v `GoalsScreen`, `DailyTimeline`, `TasksSheet`, `RoutineApp` | odpravi B1–B11, B14 (navpično lomljenje) in C1 delno | 1–2 dni |
| **P0** | Enoten `GutterWidth` + poravnava hrbtenice (53/60/48 → ena vrednost) | takojšen "pospravljen" občutek dnevnega pogleda | 2 h |
| **P0** | NOW-marker: neprosojna podlaga + `zIndex` + izračun iz višine kartice; `clipToBounds` | odpravi A1–A3 (prekrivanje besedila) | 3 h |
| **P1** | Tedenski grid in Gantt na `Layout` z merjenjem + pravilo "brez besedila, če ni prostora" | odpravi A4, A5, C1, C2 | 2–3 dni |
| **P1** | Glava zaslona: `DateNavigator` z odmikom 16 dp, `app_tagline` ven iz `TopAppBar` (ali v `headline` praznega dneva), segmenti → `FlowRow`/`ScrollableTabRow` | −40 dp višine glave, odpravi B8/B10 | 0,5 dni |
| **P1** | Povzetek dneva v eno kartico z mrežo 2×2 in manj napisi (`reserve`, `health`, `auto_heal_hint` → razširljivo) | bistveno manj hrupa | 0,5 dni |
| **P2** | `WindowSizeClass` + `fontScale` presejanje + `autoSize` za `MetricTile`/segmente | odpornost na 320 dp in 1,45× | 1–2 dni |
| **P2** | Tipografija: `labelSmall` ≥ 12 sp, `tnum` samo za številke, `displaySmall` brez `Black` | berljivost, manj prelomov | 1 h |
| **P2** | Screenshot testi + detekt pravilo + preview z dolgimi nizi | prepreči nazadovanje | 1 dan |
| **P3** | Dotiki ≥ 48 dp, `TooltipBox` povsod, kontrast `TextMuted` → `#7C8AA0`, widget na en sam vir (Glance brez `RemoteViews`) | dostopnost | 1–2 dni |

---

## 4. Kaj je že dobro (naj se ne pokvari)

- `WeeklyLayout.position` (greedy interval coloring, `core/.../ScheduleMetrics.kt:43`) je pravilno zasnovan — problem ni v logiki prekrivanja, temveč v tem, da **izhod logike (lane) ni vezan na merjenje besedila**.
- `categoryAllocation` (disjoint slices) → `DayLoadBar` je korekten in ne more prekriti.
- `FlowRow` se že uporablja za akcije v razširjeni kartici (`TimelineComponents.kt:184`) in za čipe predmetov/kategorij — vzorec obstaja, samo ni dosledno uporabljen.
- `RoutineSheet` (hoisted `SheetState` z animiranim izhodom), `WeekdayPicker` z `testTag`, `Modifier.testTag("fast-add")` → dobra izhodišča za UI teste.
- `timetable import` in `EntryEditorSheet` uporabljata `weight(1f)` za časovna polja in `widthIn(max = 180.dp)` za čipe predmetov — to je pravilen vzorec, ki ga je treba razširiti na ostalo.
- OLED paleta je konsistentna in uporabljena tudi v Glance/notifikacijah (en vir resnice za barve).

---

## 5. Predlagan vrstni red izvedbe (če se odločiš za popravke)

1. `core/designsystem/components/RoutineText.kt`, `ActionButtonRow.kt`, `AdaptiveTextButton.kt` + konstante `GutterWidth`, `RoutineSpacing`.
2. Migracija `DailyTimeline` + `TimelineComponents` + `ManagedRoutineCard` (P0 zgoraj) → to je zaslon, ki ga uporabnik vidi 90 % časa.
3. Migracija `GoalsScreen` (najhujše lomljenje) → nato `TasksSheet`, `EntryEditorSheet`, `SettingsSheet`.
4. `OverviewScreens` (tedenski grid, mesečne celice, radar, letne kartice) na `Layout` z merjenjem.
5. Testi: Paparazzi/Roborazzi 3 konfiguracije + detekt pravilo + razširjen `TimelineUiTest`.

> Vsak korak je neodvisen in ga je mogoče pregledati posebej; nič v tem dokumentu ni bilo uveljavljeno v kodi.

---

## 6. Izvedeno (2026-09-15)

Analiza je bila uveljavljena v kodi. Povzetek po sklopih:

### 6.1 Design system (novi datoteki)

- `core/designsystem/components/RoutineText.kt`
  - `RoutineText` — ovojnica okoli Material `Text` z **privzetima `maxLines` in `TextOverflow.Ellipsis`**; privzeti slog je `LocalTextStyle.current`, da ne pregasi sloga gumba/čipa.
  - `RoutineLabel` — enovrstične oznake (`softWrap = false`), za gumbe, čipe in stolpce z znano širino; nikoli se ne lomi navpično.
  - `ActionRow` — `FlowRow` za skupine gumbov: ko zmanjka širine, se gumb prestavi v novo vrstico namesto da bi se napis zlomil ali izginil.
  - `RoutineSheetScaffold` / `RoutineSheetListScaffold` — **enoten skelet lista** (glava z naslovom in zapiranjem, drsno telo ali `LazyColumn`, lepljivo dno z akcijami). Vsi listi v aplikaciji gredo skozi njega.
  - `CategoryTabs` — zavihki iz `FilterChip` s `testTag` za vsak zavihek.
  - `SettingRow` / `SettingSwitch`, `SheetPrimaryButton` / `SheetSecondaryButton`, `SectionHeader`.
- `core/designsystem/components/TimelineChrome.kt`
  - `TimeGutter` (enotna širina časovnega stolpca), `NowBand` (merjen `Layout`, neprosojna plast — **brez `Modifier.offset`**), `MetaChip`, `Modifier.timelineRail`, `Modifier.categoryBar`.
- `core/designsystem/theme/DesignSystem.kt` — `RoutineSpacing` (xs–xl) in `RoutineMetrics` (širina žleba, `MinLabelWidth = 56.dp`, `MinLabelHeight`, `ActionMinWidth = 48.dp`, `ChipMaxWidth`, geometrija NOW traku, `WeekMinuteHeight`, `MonthCellRatio`).
- `core/designsystem/theme/Type.kt` — pregledana tipografija (tnum za čase, brez 10sp).

### 6.2 P0: prekrivanje in lomljenje

- **Dan**: `DailyTimeline`, `TimelineComponents`, `ManagedRoutineCard`, `SummaryComponents` — NOW-marker je neprosojna plast merjenega `Layout`a, časovni stolpec je en sam (`RoutineMetrics.GutterWidth`), vse akcije so v `ActionRow`, vsa besedila imajo `maxLines` + elipso.
- **Teden/mesec/leto**: `OverviewScreens` — tedenski stolpci so merjen `Layout` (besedilo bloka se izriše samo, če celica doseže `MinLabelWidth`/`MinLabelHeight`), mesečne celice imajo fiksen razmerje (`MonthCellRatio`), radar mejnikov utežene stolpce v `Column` z znano višino.
- **Cilji (CAS/EE)**: `GoalsScreen` — Gantt je merjen `Layout` (pasovi in mejniki se ne prekrivajo več; oznaka v pasu samo, če je pas dovolj širok), `+100/250/500 besed` so v `ActionRow` (ne več `weight(1f)` z navpičnim lomom), editorji so `RoutineSheetScaffold`, brisanje uporablja skupni `DeleteConfirmation`.

### 6.3 Dolgi meniji in podvojene funkcije

- **Nastavitve**: en 40-postavkarski drsni seznam → **5 zavihkov** (`settings-tab-rhythm|reminders|plan|rules|data`). Pragovi zdravja niso več skriti za »Napredne nastavitve«, temveč so neposredno v zavihku Pravila. Vsak sklop ima natanko en gumb za shranjevanje.
- **Načrtovalnik**: en dolg seznam → **3 zavihki** (`planning-tab-backlog|topics|markers`).
- **Naloge**: izbirnik roka in predmeta je zdaj **ena sama komponenta** (`TaskAttributePickers`), uporabljena tako v hitrem vnosu kot v urejevalniku naloge — prej sta bili dve skoraj enaki kopiji.
- **Urejevalnik bloka**: `AlertDialog` s petimi polji → list (`BlockEditorSheet`) z lepljivim dnom; stikala so `SettingRow`.
- **Uvoz urnika**: list z zapiranjem; štetje blokov je samo na gumbu (ne dvakrat); dnevi so v slovenščini (ne več `Locale.getDefault()`).
- **Predmeti**: brisanje je v telesu dialoga (celotna širina), gumba dialoga ostaneta »Shrani«/»Prekliči« — trije napisi se ne tepejo več za en ozek prostor.
- **Widget**: odštevanje dni je prikazano **enkrat** (velika številka + »do konca pouka« ali povedna oblika), kratke oznake so enovrstične (`widget_text_single.xml`), najmanjša velikost je 11sp.

### 6.4 Testi in varovalke

- `TimelineUiTest`: posodobljen za zavihke (`settingsTabsExposeRulesAndOptInDemo`), nova testa `menusAreSplitIntoTabsInsteadOfOneLongScroll` in `longSlovenianButtonLabelsStayOnOneLine` (meri višino gumba in napisa — prelom v drugo vrsto pade na testu).
- `tools/check_presentation.py` — nova pravila: v UI kodi ni `Modifier.offset`, vsak `ModalBottomSheet` uporablja skupni skelet, noben `Text()` ne obide ovojnic `RoutineText`/`RoutineLabel`, besedilo widgeta ni pod 11sp in ima omejitev vrstic.
