# Neodvisna recenzija: videz, kakovost kode in smer produkta

**Datum:** 2026-09-21 · **Veja:** `arena/01a0c42e-mydailyroutine` (commit `f28ffb4`, popravek prevajalnika `e7f4201`) · **PR:** #11
**Naročnik:** Jakob (jakob611/MyDailyRoutine) · **Naloga:** natančen pregled **brez popravljanja kode**, sodba o smeri glede na trg, načrt nadaljnjih korakov in prompti za druge AI.
**Vsebina tega dokumenta ne spreminja nobene vrstice aplikacijske kode.**

Legenda trditev:
`✅` preverjeno neposredno v izvorni kodi repozitorija (z vrstico/citiranim izsekom) ·
`⚠️` sklep iz kode, ki ga je treba potrditi na napravi (nisem mogel pognati Android gradnje ne videti posnetkov z naprave) ·
`📊` tržni/znanstveni vir (povezava v razdelku 10).

---

## 0. Verdikt v sedmih vrsticah

| Vprašanje | Odgovor |
|---|---|
| Ali je **videz načeloma v redu**? | Da — to ni amaterski UI. Videti je sistem: ena paleta z računskim gate-om, ena besedilna pogodba, merjeni layouti, steklo po pravilih, gibanje po Appleovem besednjaku. Detajli, ki običajno izdajo hobij projekt (literali besedila, `Text(` povsod, `.offset(`, ročne barve), so tu **odstranjeni**. |
| Kaj je **vizualno najbolj problematično**? | Velikosti tap-targetov pri **lastnih** (nesteklenih) kontrolah: stekleni gumbi v vrhnji vrstici **40 × 40 dp**, zavihki **≈86 × 40 dp**, stikalo v nastavitvah **52 × 32 dp** (15 mest). Spodnja meja Materiala/WCAG je 48 dp — Materialove komponente jo same uveljavijo, lastne pa ne, ker v celem projektu ni niti enega klica `minimumInteractiveComponentSize`. `✅` Najhujše je stikalo: napačen tap **preklopi napačno nastavitev**. |
| Ali je kaj **neumno narejeno**? | Pet stvari, ki bi jih v resnem pregledu označil: (1) tap-targeti, (2) brez Undo (11 potrditvenih dialogov namesto razveljavitve), (3) tiho preoblikovanje neveljavnega dneva v **ponedeljek** pri uvozu varnostne kopije, (4) 5-sekundna anketna zanka (`20 × delay(250)`) namesto toka, (5) malo dokazov o delovanju pri povečani pisavi (fontScale) in brez posnetkov zaslona za Goals/sheetе. |
| Ali si šel v **pravo smer**? | **Da, s tremi opozorili.** Časovni bloki + ponavljanje + zajem sta uveljavljen vzorec (Structured je dokazal trg), IB + slovenski šolski koledar + zaščita počitka pa sta **resnična diferenciacija**, ki je noben tuji app nima. Opozorila: (a) Android-only pokriva **62,6 %** slovenskih mobilnih naprav, iOS **37,4 %** — med dijaki je delež iPhonov verjetno še višji; (b) trg je v 2026 šel v AI-avtoschedule in integracije, ti si namerno brez obojega — to je pozicija, ne napaka, a mora biti **zavestna in komunicirana**; (c) retencija je pravi sovražnik (mediana D30 za vse app-e ~4 %, za dobre produktivnostne 8–18 %), zato je prvi zagon in »prvi uspeh v petih minutah« pomembnejši od vsake nove funkcije. |
| Kaj naj naredim **naslednje**? | Razdelek 7: tri P0 stvari pred širjenjem (tap-targeti, onboarding/prvi uspeh, dokazi o povečani pisavi), nato P1 (Undo, opisne napake, merjenje retencije), higiena šele potem. |
| Kaj naj narediš **ti**? | Razdelek 7.2: 12-stopenjski test na telefonu, 5 posnetkov zaslona, ena odločitev o ciljni publiki (osebno orodje / slovenski dijaki / širše). |
| **Najboljša poteza naslednjih 14 dni** | Ne dodajati funkcij. Testirati APK z **resničnim tednom svojega urnika**, popraviti tap-targete in a11y, in napisati *en* dokument `docs/STATUS.md`, ki pove, kaj je trenutno res. |

---

## 1. Kaj sem pregledal in kaj nisem mogel

### 1.1 Obseg

| Predmet | Obseg |
|---|---|
| Izvorna koda | 147 datotek `.kt`, 19.844 vrstic (`app` + `core`), 135 `@Composable` funkcij |
| Besedilni viri | 712 nizov (`values/strings.xml`), 5 množinskih oblik (slovenska dvojina pravilno) |
| Dokumentacija | 21 datotek, 2.780 vrstic (`docs/`) |
| CI | zadnji run `35608535970` (build ✅ 7 m 23 s, device-tests ✅ 5 m 36 s, 54 inštrumentacijskih testov) |
| Testi | 159 `@Test` v `core/src/test`, 31 v `app/src/test`, 54 v `app/src/androidTest` |
| Gate-i | `derive_palette --check` ✅, `check_sqlite_integrity` ✅ (19 preverjanj), `check_presentation` ✅, `check_contrast` ✅ (99 preverjanj) |
| Tržna raziskava | 6 iskanj, 2026-viri (Structured, Motion, Reclaim, Sunsama, Akiflow, MyStudyLife, Anki in drugo) + znanstvene meta-analize |

### 1.2 Česa **nisem** mogel preveriti (pošteno, da veš, koliko velja katera trditev)

- **Nisem prevajal in nisem pognal aplikacije.** Sandbox nima JDK, Android SDK, emulatorja; tuja mreža (dl.google.com, repo1.maven.org, Azure blob) je blokirana, zato tudi **posnetkov zaslona z emulatorja ne morem prenesti**.
- **Vizualne sodbe so zato izpeljane iz kode** (geometrija, mere, barve, tipografija, gibanje) in ne iz slike. Kar se tiče »ali je lepo« — barvni občutek, kakovost blura, obnašanje stekla pod prstom — tega **ne morem** oceniti in tega ne bom pretvarjal v trditev.
- **Kako do posnetkov, ki jih CI že dela:** GitHub → *Actions* → run `35608535970` → artifact **`device-test-reports`**; v njem je `app/build/ui-audit/` z osmimi posnetki: `01-day`, `02-week`, `03-month`, `04-year`, `05-quick-add`, `05-glass-day`, `10-year-folds`, `11-back-stack`. `✅` (imena so v `TimelineUiTest.capture(...)`). Priporočam, da jih pogledaš — to je edini poceni način, da vidiš, kaj CI v resnici ujame.

---

## 2. Videz in grafika: kaj je **dobro narejeno** (z dokazi)

| Področje | Kaj je narejeno | Dokaz |
|---|---|---|
| Barvni sistem | Paleta **ni ročno izbrana**: 6 površinskih stopenj + semantični poudarki, izpeljani iz objavljenih konstant; `check_contrast.py` preverja 99 parov (primarno 11:1, sekundarno 7:1 AAA, muted 5,5:1) in prepoveduje čisto črno in belo (halation) | `✅` gate izpis + `RoutineColors` v `DesignSystem.kt:20–90` |
| Besedilna pogodba | 397 klicev skozi `RoutineText`/`RoutineLabel`, **samo 1 surov `Text(`** v celem UI-ju; 174 mest ima `maxLines`; privzeto »skrči se preden odreže« (`autoSize` do 11 sp) | `✅` meritev + `RoutineText.kt:96–127` |
| Postavitev | **Nič `.offset(`** v UI-ju (0 zadetkov) — vse absolutno pozicioniranje gre skozi merjene `Layout`/`SubcomposeLayout`; 9 × `BoxWithConstraints`; gate to tudi prepoveduje | `✅` `check_presentation.py` + meritev |
| Steklo | Tri pravila so zapisana in upoštevana: steklo samo za navigacijsko plast; node s `layerBackdrop` **nikoli** tudi `drawBackdrop` (SIGSEGV); pod API 31 (brez `RenderEffect`) enaka ploskev kot polna površina | `✅` `RoutineGlass.kt:11–70`, `glassSupported` |
| Gibanje | Appleov besednjak vzmeti (`k`, `ζ`) ločen od Materialovih; `LocalReduceMotion` upoštevan povsod; prehod sledi smeri potovanja | `✅` `RoutineMotion.kt`, uporaba v `RoutineApp.kt:428–440` |
| Tipografija | Appleova Dynamic Type lestvica (Large Title 34/41 … Caption 12/16), Roboto Flex z `opsz`, tabularne številke v widgetu | `✅` `Type.kt`, `README.md:61` |
| Tema | Dark-only (namerno), transparentna statusna vrstica, `dialogTheme` in `alertDialogTheme` preusmerjena na temno (beli sistemski barvi pod sheetom sta najglasnejše možno nasprotje OLED-dark app-a) | `✅` `values/themes.xml` |
| Widget | `updatePeriodMillis="0"` — osvežuje se ob alarmih/dogodkih, ne v prazno po uri; besedilo ≥ 11 sp, `maxLines` obvezno | `✅` `agenda_widget_info.xml`, gate v `check_presentation.py` |
| Zasebnost | `INTERNET` in `ACCESS_NETWORK_STATE` sta **izrecno odstranjena** (`tools:node="remove"`), `allowBackup="false"`, brez računa, brez telemetrije | `✅` `AndroidManifest.xml:1–16` |
| Kultura kode | Preklic korutine je v resnih poteh pravilno speljan (`catch (error: CancellationException) { throw error }` pred `catch (_: Exception)`) — to dela manj kot pol projekov, ki jih vidim | `✅` `RoutineViewModel.kt:197,568`, `AppGraph.kt:47`, `ScheduleCoordinator.kt:85` |
| Slovenska dvojina | `plurals` z `one/two/few/other` (`%1$d bloka danes`, `pred %1$d dnevoma`) — pravilno, ne angleški `one/other` | `✅` `strings.xml:295,400,611,617` |

**Sklep razdelka:** vizualna in inženirska osnova je nad povprečjem ljubiteljskih Android projektov. Kar sledi, so luknje, ki jih take osnove ne pokrijejo same.

---

## 3. Videz: konkretna tveganja in slabosti

### 3.1 P0 — dotik in dostopnost (uporablja vsak, vpliva takoj)

**Ključna ugotovitev:** v aplikaciji obstajata dve vrsti klikljivih elementov in razlika med njima je natanko tisto, kar je tu narobe.

- **Materialove komponente** (npr. `Surface(onClick = …)`, `Checkbox`) **same** razširijo tarčo na 48 dp prek `minimumInteractiveComponentSize()`. `✅` Preverjeno v izvorni kodi Material 3 (`Surface.kt`, v. s `onClick`, vrstica 494): značka `WarningBadge` ima zato vizualno 36 dp, **tarčo pa 48 dp** — in to je v redu. `✅`
- **Lastne steklene kontrole** (vse v `LiquidControls.kt` in `RoutineApp.kt`) uporabljajo `Modifier.clickable` / `toggleable` **neposredno na `Box`** — tam razširitve ni, ker `Box` ni Materialova komponenta. V celem projektu ni enega samega klica `minimumInteractiveComponentSize` (0 zadetkov). `✅`

| # | Kaj | Dokaz | Izmerjena tarča | Zakaj je problem |
|---|---|---|---|---|
| V1 | **Stekleni ikonski gumbi** (`GlassIconButton`), 4 × v vrhnji vrstici (načrtovanje, naloge, cilji, nastavitve) | `✅` `LiquidControls.kt:306` + `:310`, `DesignSystem.kt:164` (`GlassControlSize = 40.dp`), `RoutineApp.kt:403,405,422,423` | **40 × 40 dp** | Priporočilo Materiala in WCAG je **48 dp**. To so najpogosteje tapnjeni gumbi v app-u; 20 % premajhna tarča pomeni opazno število zgrešitev v gibanju. |
| V2 | **Zavihki Dan/Teden/Mesec/Leto** v traku | `✅` `RoutineApp.kt:449` (`height(40.dp)`), `:471` (`Box(...).clickable(role = Role.Tab)`) | **≈ 86 × 40 dp** | Glavna navigacija. Višina 40 dp, pri `fontScale ≥ 1,3` pa se štiri celice še stisnejo po širini (napisi berejo `labelLarge` brez `weight` omejitve). |
| V3 | **`RoutineSwitch`** (stikalo v nastavitvah) — **uporabljeno na 15 mestih** | `✅` `LiquidControls.kt:112` (`.size(SwitchWidth, SwitchHeight)` = `52 × 32 dp`) + `:114` (`.toggleable(…)`) | **52 × 32 dp** | Najresnejša postavka v tem razdelku: v seznamu nastavitev je višina tarče 32 dp, in napačen tap **preklopi drugo nastavitev**. Tu se napaka ne vidi — povzroči spremembo vedenja app-a. |
| V4 | **`CollapsibleSection`** (zložljive sekcije na letnem pregledu) | `✅` `RoutineText.kt:289–293` (`.clickable` + `padding(vertical = RoutineSpacing.sm)` = 8+8 dp okrog besedila `labelMedium`) | **≈ 34 dp višine** | Odpiranje/zapiranje treh dolgih sekcij; pod 48 dp. |
| V5 | **TalkBack:** 0 × `heading()`, 0 × `liveRegion`, 0 × `stateDescription`, 0 × `customActions`; **14 ikon** z `contentDescription = null` (nekatere dekorativne, nekatere ne) | `✅` meritev po `app/src/main` | – | Bralnik zaslona ne ve, da je »Naslov dneva« naslov, ne pove, da se je **NOW premaknil**, in ne ponudi »označi kot opravljeno« kot akcijo na kartici. |

> **Zakaj je to P0 in ne kozmetika:** štiri meritve dotika (V1–V4) zadevajo **pogostost** in **zanesljivost**, peta (V5) pa dostopnost. Uporabnik, ki dvakrat zgreši gumb »Dodaj blok«, si ustvari mnenje o app-u prej kot ob katerikoli novi funkciji. **Popravek je tudi eden najcenejših v celem seznamu:** dodaj `.minimumInteractiveComponentSize()` v `GlassIconButton`, v celice zavihkov, v `RoutineSwitch` in v `CollapsibleSection` — to je napaka, ki jo Material reši v eni vrstici, ker smo šli mimo njegovih komponent.

### 3.2 P1 — kar bi videl, če bi gledal dalj časa

| # | Kaj | Dokaz | Pojasnilo |
|---|---|---|---|
| V6 | **Ni dokazov za veliko pisavo ali ozek zaslon.** 0 × `fontScale`, 0 × `WindowSizeClass`, `BoxWithConstraints` le 9 ×; CI posname posnetke samo pri privzeti pisavi in brez `08-goals` / `09-tasks` / sheetov razen `05-quick-add` | `✅` meritev + `TimelineUiTest.kt:37–45,153,194,205` | Prejšnja revizija (`UI_LAYOUT_CRITIQUE.md`) je prav tu našla najhujše napake (navpično lomljenje besedila). Popravki so vidni (18 × `FlowRow`, autoSize), a **regresija ni zavarovana** — ni testa, ki bi tekel pri `fontScale = 1,5`. |
| V7 | **Barva nosi pomen** v tedenskem gridu, mesecu in Ganttu (kategorija = barva; `MinLabelWidth = 56.dp` pomeni, da ozek blok pokaže **samo barvo**) | `✅` `DesignSystem.kt:159` | Za ~8 % moških (barvna slepota) je tedenski grid takrat serija sivih lis. Kartice v dnevnem pogledu imajo besedilo in so v redu. |
| V8 | **Gostota dneva:** naslov + povzetek (3 ploščice + pas + opozorila) pred prvim blokom | `✅` `DailyTimeline.kt:33–120` | Na 6,1" telefonu je prvi blok lahko pod pregibom. Verjetno namerno; vredno izmeriti na napravi. |
| V9 | **Najdaljši nizi so 229 znakov** (`settings_health_body`), `timetable_import_hint` 210, `battery_note` 184 | `✅` meritev `strings.xml` | So razlagalna besedila v nastavitvah — praviloma v redu, a slovensko besedilo je ~15 % daljše od angleškega; na 320-dp zaslonu to pomeni dolge odstavke v drobni pisavi. |
| V10 | **Temna tema brez alternative.** Dark-only je zavestna odločitev (OLED), a na soncu na hodniku šole je to najslabša možna izbira berljivosti | `✅` `themes.xml` | Ne predlagam light tema; predlagam **preverjanje na soncu** — to je test, ki ga CI nikoli ne bo naredil. |

### 3.3 P2 — brus

- 252 literalov `dp`/`sp` v UI kodi (sistem `RoutineSpacing`/`RoutineMetrics` obstaja, a ni popoln). `✅`
- `RoutineApp.kt` ima **podvojene uvoze** (`graphicsLayer`, `scaleIn`). `✅` (higiena, ne napaka)
- 33 `testTag` v produkcijski kodi (`app/src/main`) — sprejemljivo, a to je testna infrastruktura v izdelku. `✅`

---

## 4. »Neumno narejene stvari« — po resnosti, z dokazi

> Sem štejem tisto, kar ima **jasno boljšo rešitev**, ne okus.

### 4.1 P1 (resne)

| # | Mesto | Kaj je narobe | Kako bi bilo prav |
|---|---|---|---|
| K1 | `RoomBackupRepository.kt:280–284` — `mapDay()` | Neveljavno ime dneva v JSON varnostni kopiji se **tiho** spremeni v **`MONDAY`**. Enako `mapCategory()` → `SCHOOL`, `parseTime()` → 8:00. | Uvoz naj take vrstice **prijavi** (»3 vrstice imajo neveljaven dan, uvožene kot ponedeljek? [Prekliči] [Uvozi]«) ali jih preskoči z opozorilom. Tihi popravek podatkov je najslabša vrsta napake: uporabnik izgubi podatke, ne da bi izvedel. |
| K2 | `RoutineViewModel.kt:174–186` — `RequestActualById` | Zanka `repeat(20) { … delay(250) }` — do 5 sekund **anketiranja** stanja, da najde blok iz obvestila. | Repository naj ponudi `suspend fun awaitBlock(id): Block?` prek `StateFlow`/`first { }` z isto logiko, ali naj obvestilo nosi ključ bloka. Anketiranje z `delay` v ViewModelu je indikator, da manjka pot iz podatkovnega sloja. |
| K3 | `RoutineViewModel.kt:74–100` + `RoutineApp.kt` | **En ogromen `TimelineUiState`** (6+ tokov z `combine`) + 508-vrstični `@Composable RoutineApp`, ki bere `state` neposredno; **0 × `derivedStateOf`** | Vsaka sprememba (npr. `isSaving`) invalidira celotno telo `RoutineApp`. Compose skipping delno pomaga (pod-composables imajo ozke parametre, npr. `DailyTimeline(day, now, …)`), a brez meritev ne veš, koliko. **Konkretno:** vklopi `-PcomposeReports=true` (že podprto v `app/build.gradle.kts`) in poglej *composables that are not skippable*; nato razbiti `RoutineApp` na 4–5 composables z ozkimi parametri. |
| K4 | `RoutineViewModel.kt:104–147` — `resolveContent` | Ob spremembi **zdravstvene** preference se ponovno rezolvira **celotno obdobje**; v letnem načinu je to 365 dni × `forDate()` na `Dispatchers.Default`. `distinctUntilChanged()` na `(health, periodic)` ublaži, a ob spremembi gre vse znova. | Rezolviraj **vidni dnevi najprej** (dnevni način: 1 dan), ostalo pa leno/ob `LocalDate` ključu; ali predpomni `prepared.forDate(date)` v LRU. Merljivo: čas do prvega izrisa letnega pogleda na srednjem telefonu. |
| K5 | `RoutineViewModel.kt:577–578`, `RoomBackupRepository.kt:270,282`, `ShareTextParser.kt:94` | `catch (_: Exception)` brez loga. V korutinskih poteh je preklic pravilno speljan (`✅`), a tam, kjer ostane, uporabnik vidi »Napaka pri shranjevanju« **brez** vzroka, ti pa na napravi ne moreš diagnosticirati (v `logcat` ni ničesar). | `Log.w` z izjemo + `TimelineEffect.Message(res, detail)` v debug varianti; ali `RunCatching` z zapisom v zadnjih N napak v DataStore (brez mreže). |
| K6 | Odločitev v `RoutineApp.kt` + `RoutineState.kt` | **Ni Undo.** Destruktivne in pol-destruktivne akcije (izbris, premik, zapiranje seje) so zaščitene z **11 `AlertDialog`** potrditvami. | Za premik bloka in brisanje je sodobna praksa **razveljavitev**: izvedi takoj, pokaži `Snackbar` z »Razveljavi« 5 s. Potrditveni dialog naj ostane samo za nepovratno (izbris predmeta, uvoz/primer podatkov). Manj klikov, manj napak. |

### 4.2 P2 (higiena, vzdrževanje)

| # | Mesto | Kaj |
|---|---|---|
| K7 | `GoalsScreen.kt` 1.416 vrstic; znotraj `ActivityEditorSheet` 223, `StatusCard` 137, `GoalsScreen` 131 | Datoteka je že večja od vsakega smiselnega zaslona; iskanje in pregled sta draga. Razbiti po funkcijah v `goals/presentation/components/`. |
| K8 | `app/src/main/java/**` — 27 × `Modifier.size(< 48dp)`, od tega **štirje dejansko premajhni klikljivi elementi** (glej V1–V4) | Večina je namerna (ikone znotraj večjih tarč), a brez avtomatske kontrole ne veš, katera je klikljiva. Compose ima za to že vgrajena orodja: `SemanticsNode.touchBoundsInRoot` (prava tarča, ne vidna škatla) in assertions `assertTouchWidthIsEqualTo` / `assertTouchHeightIsEqualTo` — poceni in trajno. |
| K9 | Repozitorij: `workspace-01a0c42a-…zip` **15,78 MB**, `all.zip` 0,49 MB, `clicks.zip`, `1789728586476.jpg`, mapa `.idea/` (5 datotek) | Vse je v gitu. Repo naj bo za kodo; snapshote in zvočne zbirke daj v izdaje (*releases*) ali v `.gitignore`. |
| K10 | `README.md:16–17` | Številke so zastarele: **135** core testov (zdaj 159), **533** nizov (zdaj 712), **117** datotek (zdaj 147), **17** SQLite preverjanj (zdaj 19), **157** kontrastnih parov (gate zdaj poroča 99 preverjanj). Zastarele številke v README so majhna laž, ki se s časom ne popravi sama. |
| K11 | `docs/` — 21 datotek, 2.780 vrstic, brez enega indeksa stanja | Dokumenti imajo v glavi »Stanje: implementirano« / »samo analiza«. Manjka **`docs/STATUS.md`**: ena tabela »kaj je res zdaj, kaj je predlog, kaj je zavrnjeno«. Brez tega vsak nov agent (in ti) bere 2.780 vrstic, da ugotovi, kaj velja. |
| K12 | `RoutineApp.kt:29,94` | Podvojeni uvozi (`scaleIn`, `graphicsLayer`) — ne škodi, a kaže na urejanje brez `Optimize imports`. |

### 4.3 Kar **je videti kot vonj, pa ni**

- `catch (_: Exception)` v `Receivers.kt:50` — namerno: 8-sekundni `withTimeout` + `armRetry()`; če poči, se alarm ponovi. `✅`
- `boundsInRoot()` namesto `boundsInWindow()` (moj popravek iz PR #11) — ni bila bližnjica: celotni model morpha meri in postavlja **v rootu**. `✅`
- `updatePeriodMillis="0"` v widgetu — ni lenoba; osveževanje vodijo alarmi mejnikov. `✅`
- `Surface(onClick = …)` v Material 3 **sam** doda `minimumInteractiveComponentSize()`, zato je npr. `WarningBadge` (videti 36 dp) v resnici 48 dp tarča. To ni pomota, to je pravilno — in hkrati dokaz, da te razširitve **ni** pri lastnih steklenih kontrolah (V1–V4). `✅`
- Senzor vrtenja za nagib stekla (`SENSOR_DELAY_UI`, registriran/prekinjen v `DisposableEffect`) — poraba je majhna in se ustavi ob izhodu. `✅` Vprašanje je le, ali je nagib vreden senzorja; zate je to vprašanje okusa, ne napake.

---

## 5. Surove meritve (da jih lahko primerjaš čez pol leta)

| Mera | Vrednost danes |
|---|---|
| Vrstic kode (`app` + `core`, `.kt`) | 19.844 (147 datotek) |
| `@Composable` funkcij | 135 |
| Besedilnih virov (SL) | 712 (19 podvojenih vrednosti) |
| Klicev skozi `RoutineText`/`RoutineLabel` | 397 (surovih `Text(`: **1**) |
| `maxLines` / `autoSize` / `FlowRow` | 174 / 2 / 18 |
| `.offset(` v UI-ju | **0** |
| `rememberSaveable` / `derivedStateOf` | 146 / **0** |
| `withTransaction` / `@Query` | 21 / 71 |
| `AlertDialog` (potrditve) | 11 |
| `testTag` v produkciji | 33 |
| `minimumInteractiveComponentSize()` | **0** (Materialove komponente razširijo same, lastne steklene ne) |
| Izmerjene tarče pod 48 dp | stekleni gumb **40 × 40**, zavihki **≈86 × 40**, `RoutineSwitch` **52 × 32** (15 mest), `CollapsibleSection` **≈34 visok** |
| Testi (`@Test`) | 159 (core) + 31 (app unit) + 54 (naprava) |
| Najdaljši besedilni vir | 229 znakov |
| Prenos repozitorija (veliki dodatki) | ~17 MB (zipi, jpg) |

---

## 6. Tržna in znanstvena raziskava: je smer prava?

### 6.1 Konkurenca (2026), z cenami in pozicioniranjem

| Aplikacija | Model | Cena (2026) | Kaj ti pove | Vir |
|---|---|---|---|---|
| **Structured** | Ročno planiranje na vizualni dnevni časovnici; ponavljanje + inbox za zajem | viri 2026 se ne strinjajo: ~2,99–6,99 $/mes., 19,99–29,99 $/l., ponekod tudi enkratni nakup 25–64,99 $ | Najbližji tvojemu jedru. Dokazuje: **ena časovnica je dovolj**, če je hitra. Njihova šibkost: ni AI, ni globine za šolo (IB/CAS/EE ne obstaja). | 📊 [1][2][4] |
| **Motion** | AI sam sestavi in prestavi cel dan | ~19–49 $/mes. | Trg je v 2026 v veliki meri AI-avtoschedule. Ti tega ne delaš — to je tvoja pozicija, mora biti komunicirana, ne pa »ker se ni dalo«. | 📊 [1][2] |
| **Reclaim.ai** | AI brani fokus in navade okrog sestankov | brezplačno do ~10–18 $/mes. | Ideja »obrani fokus« je **tvoja** ideja (zaščiten počitek, samodejna poravnava). Dobro. | 📊 [1][3] |
| **Sunsama** | Ritual dnevnega planiranja, brez avtomatike po izbiri | ~16–22 $/mes. | Dokazuje, da se da prodajati **obred/mir**, ne avtomatika. Tvoj dnevni povzetek je zametek tega, a brez »rituala« (glej 6.6). | 📊 [1][2] |
| **TickTick / Todoist** | Naloge + koledar, pomodoro | 0–5 $/mes. | Konkurent za *naloge*, ne za urnik. Tvoj »Čakalnik« (backlog) je pravilna meja. | 📊 [1][4] |
| **MyStudyLife** | Šolski urnik + naloge; 2025/26: prevzem, redesign, **izguba podatkov po posodobitvah**, paywall čez prej brezplačne funkcije | 0–29,99 $/l. | **Najpomembnejši vir za tvojo smer.** Njihove kritike (2026) so natančno tisto, kar ti delaš prav: *podatki ostanejo na napravi*, *brez računa*, *brez prisilnega synca*. Njihova napaka: **rotacijski urniki (A/B tedni, 1./2. teden)** so bili njihova najboljša funkcija — in to imaš ti (tedensko ponavljanje + šolski koledar). | 📊 [5][6] |
| **Anki** | Razmik ponovitev | brezplačno (iOS 25 $) | Uporabniki ne menjajo Ankija; tvoj »Razpored ponovitev IB« je *planiranje* ponovitev, ne kartice. Komplementaren, ne konkurenčen. | 📊 [7] |

### 6.2 Kaj trg v 2026 zahteva (in kar imaš / česar nimaš)

| Tržno pričakovanje | Pri tebi |
|---|---|
| Takojšen zajem (mobilno, manj kot 3 tap-i) | ✅ hitri vnosi, pilula »Dodaj blok«, uvoz urnika iz besedila, `SEND` iz drugih app-ov |
| Časovnica kot glavni zaslon | ✅ dnevna/ledenska/mesečna/letna |
| Ponavljanje kot vzorec, ne instance | ✅ `repeatDaysMask`, serije, »ta pojavitev ali serija« |
| AI (prioritizacija, generiranje) | ❌ **ni** (zavestno) — pripravljen argument, glej 6.5 |
| Sin­hronizacija med napravami | ❌ **ni** (zavestno; telefonska varnostna kopija je edina pot) |
| Onboarding v prvem zagonu | ❌ ni (README: noben primer urnika se ne vstavi samodejno; `Naloži primer podatkov` je skrita v nastavitvah) |
| Več platform | ❌ Android-only |
| Zasebnost brez računa | ✅✅ **izrazita prednost** — `INTERNET` odstranjen na manifestu |

### 6.3 Kje si **resnično drugačen** (in to je tvoja cena)

1. **IB-specifičen model** (CAS/EE/TOK, roki, 8-tedenska gostota mejnikov, `docs/plans/IB_TASKS_AND_CAS_EE_PLANNER.md`). Noben tuji app tega ne pozna; IB dijaki so globalno zvesta niša. 📊
2. **Preverjen slovenski šolski koledar 2026/27** (praz­niki, počitnice, matura) + `ManageBac` uvoz ukraditelja besedila. To je lokalna prednost, ki je tuji app nikoli ne bo imel. 📊 (analogno: MyStudyLife se je zataknil prav pri lokalnih urnikih)
3. **»Zaščiten počitek« in samodejna poravnava** (RSEM rezerve, petfazno celjenje zamude). Reclaim to dela za odrasle s sestanki; ti to delaš za dijaka, ki je zamudil učenje do testa. **To je prava ideja produkta.**
4. **Zasebnost kot arhitektura**, ne politika: brez mrežnega dovoljenja (ne »brez telemetrije«, ampak *tehnično onemogočeno*). Trg to ceni posebej pri mladoletnih uporabnikih (GDPR/šolske politike). 📊 [8]
5. **Merjenje dejanskega časa** in kalibracija ocen (`VelocityCalibrator`, `ActualCompletionDialog`) — to je tisto, kar razlikuje *načrt* od *samozavedanja*. Znanstveno podprto, glej 6.6.

### 6.4 Kje si v nasprotju s trgom (tveganja)

| Tveganje | Podatek | Kaj s tem |
|---|---|---|
| **Android-only** | V Sloveniji (julij 2026): Android **62,6 %**, iOS **37,4 %**; pri tablicah 68/31. 📊 [9] | Zavestna odločitev. Če ciljaš samo nase in prijatelje, ni problema. Če ciljaš trg: iOS je med dijaki verjetno **nad** 37 %. Edina realna pot je Compose Multiplatform — in zanimivo: knjižnica za steklo, ki jo uporabljaš, ima CMP izdajo 2.x (`gradle/libs.versions.toml` to izrecno omenja). To ni prepis, je pa velik projekt (~30–40 % kode je čista domena, ki bi prenesla brez sprememb). |
| **Brez synca** | Telefon se izgubi/pokvari → podatki izginejo (razen ročnega JSON izvoza) | Varnostna kopija obstaja, a je skrita. **Predlagam:** vklop `android:allowBackup="true"` ni v redu (zasebnost), a redna opomniška varnostna kopija (npr. »Teden dni od zadnje kopije — izvozi?«) je poceni in reši katastrofo. |
| **Obseg** | 7 zaslonov + CAS/EE planer + zdravstveni mehanizmi; 19.844 vrstic kode za en uporabnik | Vsaka funkcija ima vzdrževalno ceno. Za dijaka je meja, kjer se app začne **zapirati počasneje, kot se ga odpre**, realna. Meri **čas do prvega izrisa** (`04-year` in `09-goals`) — to je najbolj oprijemljiva mera »je preveč«. |
| **Brez AI** | 2026 trg: AI prioritizacija je privzeta v Motion/Reclaim/TickTick | Ne kupuj AI-ja. Pripravi **pošteno pozicijo** (razdelek 8.7) — v Evropi je »brez oblaka, brez modela, ki bi bral tvoje ocene« **argument**, ne zaostalost. |
| **Retencija** | Mediana D30 vseh app-ov ~4 %; produktivnostni: mediana 8 %, dobri 12–18 %; povprečno 77 % uporabnikov odpade v 3 dneh. 📊 [10][11] | Tvoja prava metrika ni število funkcij, ampak »ali je app odprt 4. dan zapored«. To vodi v P0 #2 v razdelku 7.1 (prvi uspeh v 5 minutah). |

### 6.5 Kaj pravijo dokazi (in kaj to pomeni za tvoj produkt)

| Dokaz | Številka | Kaj iz tega sledi za app |
|---|---|---|
| **Implementacijske namere** (»če X, potem Y«) | d = 0,65 (94 študij, 8.000+ udeležencev, 2006); posodobitev 2024 čez 642 testov: d = 0,27–0,66 📊 [12][13] | Tvoje planiranje je v bistvu »if-then« pogodba: *ko pride 16:00 v sredo, delam nalogo iz matematike*. To je najmočnejši mehanizem v celem izdelku. Zato naj **vsak blok nosi opomnik s konkretnim dejanjem** (obstaja: »Začni« / »Zabeleži dejansko«), in **napoved vnaprej** (»jutri ob 16:00: matematika, 45 min«). Cue je treba *videti*, ne le hraniti. |
| **Stopnja izvedbe namere** | ljudje z močno namero uspejo le ~53 % časa | Tvoja največja vrednost ni *načrt*, ampak **poravnava, ko načrt pade** (tvoje celjenje zamude). To je prava marketinška zgodba: »načrt ne dela — LockIn ga popravi«. |
| **Samoregulirano učenje (SRL)** | g = 0,36 (182 velikosti, trening); »planning and goal setting« = .55, največji učinek 📊 [14] | Planiranje + zastavljanje ciljev je **najbolj donosna** komponenta. Tvoje ciljne plošče (Goals, mejniki, Gantt) so torej prav tam, kjer je učinek; a morajo biti **povezane s časovnico** (so: `goalMarkers`). |
| **Razmik ponovitev** | RCT 2025: 11,42 → 16,24 / 20 proti 11,58 → 11,89 v kontrolni skupini (p < 0,0001) 📊 [15] | Tvoj »Razpored ponovitev IB« je redek primer *planerja*, ki zna razmik. Ne poskušaj postati Anki; bodi app, ki **ve, kdaj** ponoviti. |
| **Upravljanje časa** | korelacije z uspehom so majhne (r ≈ 0,14–0,22) 📊 [16] | Pouk: app sam po sebi ne dvigne ocen. Dvigne jih, ko zmanjša **trenje ob nameri**. Zato so hitrost zajema, opomniki in poravnava pomembnejši od lepote (in lepota je pomembna predvsem zato, ker te uporabnik odpre). |

### 6.6 Sodba o smeri

**Smer je prava, s tremi popravki fokusa.**

1. **Jedro imaš prav:** ena časovnica, vzorec ponavljanja kot vir resnice, ločitev blok / naloga / čakalnik (to je natanko delitev *event / task / reminder*, ki jo Google Calendar in Structured uporabljata), hitri zajem, merjenje dejanskega časa. 📊 [1][3]
2. **Diferenciacija je lokalna in osebna, ne generična.** »Še en time-blocking app« je ob 20 konkurentih po 10–30 $/mes. slabo. »App, ki pozna slovenski šolski koledar, IB roke in CAS/EE, in pri katerem tvoji podatki nikoli ne zapustijo telefona« je obrambna pozicija, ki si jo tujci težko privoščijo.
3. **Manjka tisto, kar odloča o preživetju: prvi uspeh in navada.** Onboardinga ni, prazna stran je, primer podatkov je skrit v nastavitvah, opomnik za varnostno kopijo ne obstaja, in nič ne meri, ali se uporabnik vrne. Retencija je tu edina prava konkurenca — ne Motion. 📊 [10][11]

**Če bi izbiral med scenariji:**
- **A — osebno orodje (najverjetneje tvoje stanje):** ne investiraj v iOS/AI/oblak; investiraj v **hitrost in zanesljivost** in v to, da ti bo app služil štiri leta brez izgube podatkov.
- **B — produkt za slovenske dijake:** P0 so onboarding, deljenje urnika (koda/VPN-less), varnostna kopija in — najverjetneje — iOS prek CMP.
- **C — širši trg:** tukaj bi šele potreboval AI in sync, in to je povsem drug projekt.

---

## 7. Načrt naprej (orchestrator)

### 7.1 Moje nadaljnje naloge, po prioriteti in oceni

| # | Naloga | Zakaj zdaj | Obseg | Kriterij sprejema |
|---|---|---|---|---|
| **M1 (P0)** | Tap-targeti: `.minimumInteractiveComponentSize()` v `GlassIconButton`, v celice zavihkov, v `RoutineSwitch` (15 mest) in v `CollapsibleSection`; vidna višina traku zavihkov 40 → 44–48 dp | V1–V4 — najbolj uporabljen del zaslona; napačen tap na stikalo **spremeni nastavitev** | 1–2 h | Gate v `check_presentation.py`: vsak klikljiv node ≥ 48 dp (z izjemami označenimi v kodi) + inštrumentacijski test prek `touchBoundsInRoot` ≥ 48 dp na treh komponentah |
| **M2 (P0)** | A11y: `semantics { heading() }` na naslove, `liveRegion` na NOW pas, `customActions` (»Opravljeno«, »Premakni +15 min«) na kartice, `stateDescription` na spremenljivke | TalkBack je edina pot do app-a za del uporabnikov; poceni, trajno | 3–4 h | Nov inštrumentacijski test: obstoj heading vozlov in en `customAction` na kartici |
| **M3 (P0)** | Dokazi o povečani pisavi: posnetki + test pri `fontScale = 1,3` in `1,5` na 320-dp širini; dodaj `08-goals` in `09-settings` v `capture(...)` | Prejšnje najhujše napake so bile prav tu; regresija zdaj ni zavarovana | 3 h | CI posnetki pri obeh merilih; test preveri, da noben gumb ne naraste preko meje |
| **M4 (P1)** | Onboarding + prvi uspeh: ob prvem zagonu 3 koraki (urnik iz besedila / hitri vnosi / »Naloži primer«), cilj: **dan z blokom v < 5 min**. Premakni `Naloži primer podatkov` iz nastavitev na prazno stran | Retencija; 📊 [10][11] | 1 dan | Meritev v `logcat`/DataStore (brez mreže): čas od prvega zagona do prvega shranjenega bloka |
| **M5 (P1)** | Undo namesto potrditvenih dialogov za premik/izbris (+ `Snackbar` z »Razveljavi«) | K6 | 1 dan | 11 dialogov → ≤ 4 (samo nepovratno) |
| **M6 (P1)** | Uvoz varnostne kopije: **javi** neveljavne vrstice, ne popravljaj tiho; shema + verzija v datoteki | K1 — tiha sprememba podatkov | 4 h | Test: pokvarjen JSON → prijavljeno število napak, nič tihega popravljanja |
| **M7 (P1)** | Diagnostika: `Log.w` + »zadnjih 20 napak« (DataStore) + gumb »Kopiraj diagnostiko« v Naprednih nastavitvah | K5 — brez tega ne boš mogel ugotoviti, kaj je šlo narobe na napravi | 4 h | Napaka pri shranjevanju pusti sled |
| **M8 (P1)** | Merjenje retencije (lokalno, brez mreže): število odprtij/dan, dnevi zaporedoma, »zadnji zagon«; prikaz v nastavitvah kot »Tvoja navada« | Edini podatek, ki ti pove, ali izdelek dela | 4 h | Številke vidne tebi, nikoli poslane nikamor |
| **M9 (P2)** | Compose metrike (`-PcomposeReports=true`) + razbiti `RoutineApp` (508 vrstic) na 4–5 composables; obravnavaj »not skippable« seznam | K3 | 1–2 dni | Poročilo: koliko ne-skippable composables ostane |
| **M10 (P2)** | Lenivo rezolviranje letnega pogleda (K4) + meritev časa do prvega izrisa | Letni pogled je najdražji ekran | 1 dan | Izmerjen *time to first frame* pred/po |
| **M11 (P2)** | Higiena: odstrani 17 MB dodatkov iz repa (izdaje/`.gitignore`), `docs/STATUS.md`, posodobi številke v README, odstrani podvojene uvoze | K7–K12 | 2 h | Repo < 5 MB brez zgodovine; README številke ustrezajo meri |

**Vrstni red:** M1 → M3 → M2 → M6 → M7 → M4 → M5 → M8 → M9 → M10 → M11.
**Pravilo:** nič novega, dokler M1–M3 niso v masterju. To so tri stvari, ki jih uporabnik **občuti** vsak dan, in nobena ni »funkcija«.

### 7.2 Kaj naj narediš ti (človek) — 12-stopenjski test na telefonu

Namesti APK iz [izdaje `debug-latest`](https://github.com/jakob611/MyDailyRoutine/releases/tag/debug-latest) in pojdi skozi to po vrsti. Ob vsaki postavki zapiši **eno** vrstico (kaj je šlo narobe, s katerim prstom, kolikokrat).

1. **Prvi zagon** (sveža namestitev): koliko sekund do prvega koristnega dejanja? Je prazna stran vabilo ali zid?
2. **Hitri vnos**: od zaprtega app-a do shranjenega bloka — štej tape. Cilj < 5.
3. **Dotik**: 10 × tapni ikono »Naloge« v vrhnji vrstici. Koliko zgrešitev?
4. **Steklo**: ali se vsebina pod vrstico vidi dovolj ostro, da ostane berljiva? Vrzi app v ozadje/iz ozadja 3 ×.
5. **Morph** »Dodaj blok«: zraste iz pilule v list brez preskoka? (To je bil PR #11.)
6. **Trzanje**: hitro drsi znotraj lista in ob robu; list naj se ne pogrezne.
7. **Premik bloka**: pridrži + povleci 15 min; haptika na vsak korak? Snackbar? Ali lahko razveljaviš? (M5)
8. **Prekoračitev**: začni blok, pusti 10 min čez konec; kaj naredi »poravnava«? Je razumljivo **kaj** je premaknil? (Tukaj je tvoja najboljša ideja — ali je tudi razložena?)
9. **Opomnik iz žepa**: zakleni telefon, počakaj na opomnik; delujejo akcije »Začni« / »Zabeleži dejansko«? Kaj naredi, če je app zaprt?
10. **Velika pisava**: Nastavitve → Zaslon → Pisava **največja**; ponovi korake 2, 3, 8. Kje se zalomi?
11. **Sončna svetloba**: poglej dan in mesec na prostem. Berljivo?
12. **Varnostna kopija**: izvozi JSON, spremeni en dan v datoteki na neveljaven (npr. `"day": "Sreda"`), uvozi. **Kaj se zgodi?** (To je K1 — pričakuj tiho spremembo v ponedeljek.)

**Plus 5 posnetkov zaslona**, ki jih prosim pošlji (to je vse, kar potrebujem, da vizualno oceno dvignem iz `⚠️` v `✅`): (1) dan z bloki, (2) teden, (3) mesec, (4) Goals z Ganttom, (5) odprt sheet »Dodaj blok« + (6) dan pri največji pisavi.

### 7.3 Odločitve, ki jih moram dobiti od tebe (blokirajo večji del načrta)

1. **Kdo je uporabnik?** samo ti / ti + sošolci / slovenski dijaki / širše. (Določa M4–M8 in vprašanje iOS.)
2. **iOS kdaj?** nikoli / v enem letu / zdaj. (Če »v enem letu«, naj Compose Multiplatform izvedljivostna študija steče vzporedno **zdaj**, ne takrat.)
3. **AI**: nikoli / lokalno (brez mreže) / v oblaku. (Če »nikoli« — rabimo pozicionirno besedilo, glej prompt 8.7.)
4. **Ali app ostane zaseben projekt ali gre na Google Play?** (Play pomeni: politika zasebnosti, starostna oznaka, `targetSdk` vzdrževanje, testiranje na več napravah.)

---

## 8. Prompti za druge AI (pripravljeni za kopiranje)

> Vsak prompt je samostojen. Kjer piše **【PRILOGA】**, priloži navedeno. Vsi so napisani tako, da odgovor zahtevajo v strukturi, s katero lahko delaš naprej.

### 8.1 Deep research: trg in pozicioniranje (za model z dostopom do spleta)

```
Ti si produktni raziskovalec specializiran za izobraževalne in produktivnostne mobilne aplikacije.
Datum: september 2026. Jezik odgovora: slovenščina (izrazi v angleščini v oklepaju, kjer je izraz strokoven).

KONTEKST PRODUKTA (dejstva, ne spreminjaj jih):
- Android aplikacija, Slovenija, slovenski jezik, v celoti na napravi: brez računa, brez oblaka,
  brez mrežnega dovoljenja (INTERNET je na manifestu odstranjen), podatki v lokalni bazi Room.
- Ciljni uporabnik: dijak mednarodne mature (IB) v Sloveniji, 16–19 let.
- Jedro: dnevna časovnica s časovnimi bloki, ponavljanje po dnevih v tednu, šolski koledar 2026/27
  (počitnice, prazniki, matura), karierne/IB roke (CAS, EE, TOK), čakalnik (backlog), merjenje
  dejanskega časa izvedbe, samodejna poravnava ob zamudi, razpored ponovitev (spaced repetition
  planiranje, ne kartice), widget za domači zaslon, opomniki (exact alarm) z akcijama
  "Začni" in "Zabeleži dejansko".
- Brez AI. Brez sin­hronizacije med napravami. Trenutno samo Android.

NALOGA (v tem vrstnem redu):
1. Preglej 2026 stanje na trgu aplikacij za šolske urnike in time-blocking za dijake (naknadno
   omeni tudi: Structured, Tiimo, Power Planner, MyStudyLife, Shovel, Notion Calendar, TickTick,
   Reclaim, Motion, Sunsama). Za vsako: model, cena, platforme, kaj uporabniki hvalijo/grajajo
   v zadnjih 12 mesecih (recenzije!), in ali pozna lokalne šolske sisteme.
2. Poišči specifično: obstaja kdo, ki pokriva IB (CAS/EE/TOK, roke, 8 tednov pred rokom)?
   Če obstaja, kdo, kako, koliko. Če ne, to izrecno zapiši.
3. Poišči podatke o tem, KAJ dijaki v resnici uporabljajo (ne, kar trg ponuja): ankete, Reddit/forum
   niti (r/IBO, r/Slovenia, r/ApplyingToCollege), šolske raziskave. Navedi vir in leto.
4. Izpelji 3 pozicioniranja za ta produkt, vsako v 1 stavku, z navedbo, proti komu je obrambno
   in kaj bi moral produkt NAREDITI, da bi pozicioniranje držalo (max 3 stvari).
5. Ocena trga: velikost IB populacije v Sloveniji in v EU (z viri, jasno označi negotovost).

ZAHTEVE:
- Vsaka netrivialna trditev ima vir (URL + datum). Brez vira = označi "brez vira, sklep".
- Loči: DEJSTVO / SKLEP / PRIPOROČILO.
- Ne ponavljaj mojega konteksta nazaj; začni z ugotovitvami.
- Na koncu: 5 vprašanj, na katera bi moral odgovoriti lastnik produkta, preden doda funkcijo.
```

### 8.2 Deep research: znanstvena podlaga in merilni načrt učinka

```
Ti si raziskovalec kognitivne psihologije učenja in vedenjske ekonomije.
Jezik: slovenščina. Namen: preveriti, ali ima produkt "LockIn" (planer časovnih blokov za dijake)
znanstveno podlago, in kako izmeriti njegov učinek pri enem uporabniku brez spleta.

TRDITVE, KI JIH MORAŠ PREVERITI (in popraviti, če so napačne):
- Implementacijske namere: d = 0,65 (Gollwitzer & Sheeran, 2006; 94 študij); novejša meta-analiza
  (Sheeran, Listrom & Gollwitzer, 2024) čez 642 testov navaja razpon d = 0,27–0,66.
- Samoregulirano učenje: treningi g = 0,36; največji učinek pri "planning and goal setting" (~0,55).
- Razmik ponovitev: RCT 2025 porast rezultata 11,42 → 16,24 od 20 proti kontrolni skupini.
- Upravljanje časa: korelacija z uspehom le r ≈ 0,14–0,22.

NALOGE:
1. Za vsako trditev: vir, velikost učinka, meje veljavnosti, kje popularni viri pretiravajo.
2. Kaj od tega lahko APP naredi ZA uporabnika (ne, kar mora uporabnik naučiti sam)? Razvrsti po
   razmerju učinek/trud: (a) prikaz if-then namigov, (b) opomniki s konkretnim dejanjem,
   (c) merjenje dejanskih časov in kalibracija ocen, (d) dnevni povzetek, (e) zaščita počitka,
   (f) razporejanje ponovitev. Kaj od tega sploh NE pomaga (dokaži ali zavrzi).
3. Oblikuj MERILNI NAČRT (n=1, brez mreže, vse lokalno v aplikaciji): kaj meriti, kako izračunati,
   koliko tednov za signal, in kako ločiti učinek aplikacije od učinka novega šolskega leta.
   Vključi kontrolni teden.
4. Kje tvega produkt škodo: pretirano načrtovanje, občutek krivde ob neizvedenih blokih,
   "planning fallacy" v lastnih ocenah. Predlagaj 3 zasnove v UI, ki to ublažijo.

ZAHTEVE: vsaka trditev z virom in letom; jasno označi, kjer so dokazi šibki; brez morale.
```

### 8.3 Vizualna kritika posnetkov (za model z vidom — GPT/Gemini/Claude s slikami)

```
Si art director za mobilne aplikacije, specializiran za Android/Material 3 in temne OLED teme.
Dobil boš 6 posnetkov zaslona aplikacije (dnevna časovnica, tedenski grid, mesečni pregled,
zaslon ciljev z Ganttovim diagramom, odprt spodnji list "Dodaj blok", in dnevni pogled pri
največji sistemski pisavi).

OKVIR ZNAMKE (ne predlagaj spremembe identitete, razen če je nekaj objektivno napačno):
- Temna tema ("Deep Oceanic Slate"), površine #090D16 / #151C2E / #26324F, poudarki #2DD4BF (teal),
  #67E8F9, #A78BFA, #34D399, #FBBF24, #FB7185; besedilo #F1F5F9 / #A8B3C2 / #718096.
- Vrhnja vrstica je lebdeča plošča iz "liquid glass" (zameglitev 4–6 dp, 22 dp zaobljeni vogali),
  pod njo teče vsebina.
- Tipografija: Appleova Dynamic Type lestvica, Roboto Flex.

NALOGA (za vsak posnetek posebej, nato skupno):
1. Hierarhija: kaj oko vidi prvo? Je to pravo? Kaj je preglasno / preslabotno?
2. Gostota: kje je vrstica s 3+ elementi, kjer bi morala biti dva? Kje je praznega prostora preveč
   (kartica z veliko praznine na dnu)?
3. Poravnave: poišči vse, kar ni poravnano na skupno mrežo (levi robovi, bazne linije, razmiki).
4. Barve: kje barva nosi pomen, ki ga besedilo ne pove (barvna slepota)? Kje se dva odtenka
   zlijeta?
5. Kontrast v resničnih razmerah: preberi najmanjše besedilo, povej, kje bi ob soncu odpovedalo.
6. Zadnji krog: 10 najcenejših popravkov (vsak v eni vrstici: kaj, kje, zakaj, ocena 15 min–2 h),
   razvrščenih po razmerju učinek/trud. Brez estetskih preferenc, ki niso utemeljene.

IZHOD: tabela Popravek | Posnetek | Zakaj | Ocena | Tveganje. Ni uvodnih vljudnosti.
```

### 8.4 Pregled zmogljivosti (Compose) — za model, ki bere kodo

```
Si inženir za Jetpack Compose, specializiran za zmogljivost in recomposition.
Repozitorij: android app "MyDailyRoutine" (Kotlin 2.2.10, Compose BOM 2025.09.01, Room, Glance).

ZNANO STANJE (preverjeno):
- 147 .kt datotek, ~19.800 vrstic; datoteka app/presentation/RoutineApp.kt ima 508-vrstični
  @Composable RoutineApp; GoalsScreen.kt ima 1.416 vrstic.
- RoutineViewModel drži en velik TimelineUiState, sestavljen iz combine() nad 6+ tokovi.
- V celem projektu 0 x derivedStateOf, 16 x graphicsLayer, Lazy list 10 x s ključi.
- app/build.gradle.kts že podpira -PcomposeReports=true (compose compiler reports + metrics).
- Ni poenotenega Stable/Immutable označevanja razen app/compose-stability.conf za domenske tipe.

NALOGE:
1. Opiši natančen mehanizem, kako sprememba PanelsState.isSaving (npr. med shranjevanjem bloka)
   vpliva na recomposition drevesa v tem arhitekturnem vzorcu (kateri scope se invalidira, kaj
   Compose preskoči zaradi stabilnih parametrov, kje skippability odpove).
2. Poišči 5 najverjetnejših mest za "ne-skippable" composable in 5 za nepotrebno recomposition
   v tem repozitoriju (utemelji z vzorci: lambda parametri brez remember, List/Map namesto
   ImmutableList, brain-dead state reads v velikih composable-ih).
3. Napiši konkretne ukrepe (ne splošne "uporabite remember"): kaj spremeniti v kateri datoteki,
   v kakšnem vrstnem redu, in s kakšnim merljivim kriterijem uspeha.
4. Pripravi načrt merjenja: kako uporabiti Compose compiler metrics (composables that are not
   skippable, unstable classes) + Macrobenchmark (frame timing) v CI, in katere pragove postaviti.
5. Kaj od tega se NE izplača optimizirati (z argumentom).

ZAHTEVE: vsak ukrep v obliki "datoteka:linija → sprememba → pričakovan učinek → tveganje".
Brez predpostavk o kodi, ki je ne vidiš; kjer potrebuješ vpogled, napiši, kaj bi prebral.
```

### 8.5 Pisanje testov (naprava, dostopnost, velika pisava) — za agenta v repozitoriju

```
Napiši inštrumentacijske teste (androidTest, Compose UI test) za app MyDailyRoutine.
Obstoječe: app/src/androidTest/.../TimelineUiTest.kt (275 vrstic, vzorec capture(name), awaitText(...)).
Pravila repozitorija: besedila samo prek R.string (brez literalov), testTag za vozle, "measured layouts"
(brez .offset), vsak klikljiv node mora imeti contentDescription ali besedilo.

Naloge:
1. Test pri povečani pisavi: uporabi `DeviceConfigurationOverride`/`LocalDensity` z fontScale 1,5 in
   preveri: (a) tarča vsakega gumba v vrhnji vrstici (`touchBoundsInRoot`) je vsaj 48 × 48 dp; (b) napisi zavihkov Dan/Teden/Mesec/Leto
   ostanejo v eni vrstici; (c) kartica bloka ne preseže 1,6 x višine, izmerjene pri fontScale 1,0.
2. Test dostopnosti: vsi klikljivi vozli imajo razrešen opis (semantics contentDescription ali besedilo),
   naslovi imajo Heading semantiko, kartica bloka ponuja vsaj eno custom akcijo.
3. Test velikosti tarč: vsak node z Role.Button ima širino in višino >= 48 dp (z izjemami, ki jih
   v kodi označiš).
4. Test varnostne kopije: izvozi JSON, pokvari en dan ("day": "Sreda"), uvozi, in preveri, da app
   POROČA napako (ne tihega popravka). (Ta test bo najprej rdeč — to je namen.)
5. Test praznega stanja: sveža baza → prazna stran ponuja pot do prvega bloka v <= 3 tapih.

Za vsak test napiši: kaj preverja, zakaj je to pomembno, in kaj bi pomenilo, če je rdeč.
Ne spreminjaj produkcijske kode razen testTag/dostopnostnih oznak, kjer je to nujno za test.
```

### 8.6 Študija izvedljivosti: iOS prek Compose Multiplatform

```
Si mobilni arhitekt, ki pozna Kotlin Multiplatform / Compose Multiplatform (CMP) v 2026.
Vprašanje: koliko dela je prenesti obstoječo Android aplikacijo "MyDailyRoutine" na iOS?

DEJANSKO STANJE (preverjeno v repozitoriju):
- Android-only: Jetpack Compose, Room (z 8 migracijami in schema JSON), DataStore Preferences,
  Glance widget, AlarmManager + BroadcastReceiver, foreground-aware samodejna poravnava.
- Knjižnica za "liquid glass" je github.com/Kyant0/Backdrop; projekt uporablja 1.0.0 (Android),
  v gradle/libs.versions.toml je zapisano, da je 2.x že Compose Multiplatform na Kotlin 2.4.
- 147 datotek, ~19.800 vrstic; domena (domain/*) in večina logike je čista Kotlin/JVM (brez Android API).
- Uporabniška baza: dijaki v Sloveniji; iOS delež med mobilnimi napravami v SI je ~37 %.

NALOGE:
1. Razčleni prenos po plasteh: (a) domena čista, (b) podatki (Room → SQLDelight? Room KMP?),
   (c) UI (Compose → CMP, kaj odpove: steklo/blur, haptika, senzorji), (d) platforma (alarmi,
   obvestila z akcijami, widget, sistemske nastavitve). Za vsako plast: &lt; 1 teden / 1–4 tedne /
   &gt; 1 mesec, s seznamom konkretnih tveganj.
2. Kaj v arhitekturi tega app-a boš moral PONOVNO NAPISATI, tudi če je domena čista?
3. Kaj so realne alternative CMP-ju in kdaj bi bile boljše (npr. obdržati Android + spletna
   aplikacija z lokalno bazo? native iOS z SwiftUI?).
4. Priporočilo: naprej ali ne, in če naprej — kaj narediti v prvih dveh tednih (seznam).
5. Kateri deli kode so "prenosljivi" (meri v vrsticah po mapah) — daj oceno v odstotkih.

ZAHTEVE: nobenih marketinških fraz; če je odgovor "ne", povej in utemelji; vsak teden ocene
označi kot grobo (Fermi) in povej, kaj bi jo spremenilo.
```

### 8.7 Pozicioniranje in besedilo za trgovino (Play Store + SL copy)

```
Si pisec besedil za izdelke (product copy), specializiran za slovenski trg, ki zna spoštovati
zasebnost uporabnika in ne obljublja čudežev.

PRODUKT: "LockIn" (v repozitoriju MyDailyRoutine) — planer za dijake IB v Sloveniji.
Dejstva: vse na napravi; brez računa; brez dovoljenja za internet (tehnično); slovenski šolski
koledar; IB roki (CAS/EE/TOK); časovni bloki; ponavljanje; merjenje dejanskega časa; zaščita
počitka in samodejna poravnava ob zamudi; widget; temna tema; brez AI; brez sin­hronizacije.

NALOGE:
1. Naslov (max 30 znakov) in podnaslov (max 80) — 3 različice, vsaka za drugo pozicioniranje:
   (a) "urnik, ki se popravi sam", (b) "za IB", (c) "nič ne zapusti telefona".
2. Kratek opis (max 80 besed) — 1 različica, ki ne obljubi več, kot je res.
3. 5 "feature bullets", vsak z mejno vrednostjo (npr. "Planiraj blok v 3 dotikih").
4. Kaj NIKOLI ne smemo napisati (seznam fraz, ki jih uporabljajo konkurenti in so prazne).
5. Odgovori na 5 tipičnih vprašanj/prepovedi uporabnikov: "Kaj bo, če izgubim telefon?",
   "Zakaj ni za iPhone?", "Ali ima AI?", "Se sinhronizira z Google Calendar?", "Je zastonj?"
6. Pravila glasu: kaj je prepovedano (vzkliki, "revolucionarno", "AI"), in ton (miren, točen,
   kot dober sošolec).
IZHOD: tabela za vsako točko; brez uvoda.
```

### 8.8 Robustnost: uvoz urnika in varnostne kopije (za agenta v repozitoriju)

```
Si inženir za kakovost podatkov. V repozitoriju MyDailyRoutine obstajata dva parserja:
1. Uvoz urnika iz besedila (features/entry/presentation/TimetableImportSheet.kt + core/platform/ShareTextParser.kt)
   — uporabnik prilepi kopijo iz ManageBac ali šolskega sistema.
2. Varnostna kopija (features/backup/data/RoomBackupRepository.kt) — izvoz/uvoz JSON.

NALOGE (samo analiza + predlogi, brez popravkov, razen če to izrecno zahtevam):
1. Zberi 15 realnih oblik vhodnega besedila (urnik: z vezaji, brez, z "ura", z "1.-2.", z dvema
   terminoma v vrstici, s predmetom v oklepaju, z malimi tiskanimi, s tabs, z nerazdeljnimi presledki,
   z zapisi v angleščini, s ponedeljkom in "pon.", s časom 8.00 in 8:00, s "od 8 do 9.30",
   z majhnimi tipkarskimi napakami) in za vsak zapis oceni, kaj bi tvoj parser naredil.
2. Za vsak primer: pravilno / napačno / tiho napačno (najhujše). Za "tiho napačno" predlagaj,
   kako ga spremeniti v "glasno napačno" (opozorilo v UI, ne v konzolo).
3. Varnostna kopija: definiraj shemo datoteke (verzija, čas, kanonična imena), pravila za naprej/
   nazaj združljivost, in politiko za neveljavne vrstice (nikoli tihega popravka). Napiši test-matrico
   (10 primerov datotek, od popolnoma veljavne do namerno pokvarjene).
4. Varnost: kje ta koda lahko poškoduje uporabnikove podatke (delni uvoz, ki ob polovici poči, in
   pusti bazo v vmesnem stanju)? Predlagaj transakcijsko mejo in preverjanje pred potrditvijo.
IZHOD: tabela primerov + matrika testov + seznam tveganj.
```

---

## 9. Viri

**Trg (2026)**
1. Toolfinder — *Best Time Blocking Software in 2026*: https://toolfinder.com/best/time-blocking-software
2. Ariah — *Best Time Blocking Apps 2026 (11 planners ranked)*: https://arahi.ai/blog/best-time-blocking-apps-and-planners-2026
3. Temporal — *Best time blocking apps 2026*: https://temporal.day/blog/best-time-blocking-apps-2026
4. FlowSavvy — *9 Best Time Blocking Apps in 2026*: https://flowsavvy.app/top-time-blocking-apps
5. StudyToolGuide — *Is MyStudyLife still worth using in 2026?*: https://studytoolguide.com/comparisons/is-mystudylife-still-worth-using-2026-red-flags-alternatives
6. MWM — *My Study Life – School Planner* (recenzije 2026): https://mwm.ai/apps/my-study-life-school-planner/910639339
7. Laxu — *Best Study Apps for Students in 2026* (Anki, Forest, Notion): https://laxuai.com/blog/best-study-apps-students
8. Super Productivity / iFeeltech — lokalno-prve produktivnostne aplikacije: https://super-productivity.com/use-cases/privacy-productivity/ · https://ifeeltech.com/blog/privacy-first-productivity-apps
9. StatCounter — *Mobile Operating System Market Share Slovenia* (julij 2026): https://gs.statcounter.com/os-market-share/mobile/slovenia

**Retencija**
10. UXCam / Userpilot — *Mobile App Retention Benchmarks 2026*: https://uxcam.com/blog/mobile-app-retention-benchmarks/ · https://userpilot.com/blog/mobile-app-retention/
11. EngageLab — *App retention benchmarks by milestone*: https://www.engagelab.com/blog/increase-app-retention

**Znanstvena podlaga**
12. Gollwitzer & Sheeran (2006), meta-analiza 94 študij, d = 0,65 — pregled: https://www.thebehavioralscientist.com/glossary/implementation-intentions
13. Sheeran, Listrom & Gollwitzer (2024), posodobitev čez 642 testov (d = 0,27–0,66): https://goalsandprogress.com/implementation-intentions-research/
14. Theobald (2021), meta-analiza SRL treningov (g = 0,36; planning & goal setting ≈ ,55): https://www.researchgate.net/publication/351610766
15. RCT razmik ponovitev (2025), 11,42 → 16,24 / 20: https://www.morso.app/blog/best-spaced-repetition-apps-2026
16. Aeon et al. (2021) *Does time management work? A meta-analysis* + korelacije SRL (r ≈ 0,14): https://www.sciencedirect.com/science/article/abs/pii/S0360131525000478

**Lastni dokumenti repozitorija, ki jih je vredno brati skupaj s tem**
- `docs/audits/UI_DEEP_REVIEW.md` (drugi krog, 2026-09-17), `docs/audits/UI_LAYOUT_CRITIQUE.md` (2026-09-14)
- `docs/audits/2026-09-18-deep-research-sinteza.md` (prednostni načrt P0/P1/P2 — del je že izveden)
- `docs/CHRONOBIOLOGY_ENGINE.md` (matematični model), `docs/HEALTH_RULES.md`

---

## 10. Kaj bi moral ta dokument spremeniti pri tvojih odločitvah (3 stavki)

1. **Nič novega, dokler niso popravljeni tap-targeti, dostopnost in dokaz pri veliki pisavi** — to so tri stvari, ki jih uporabnik občuti vsak dan, in nobena ni funkcija.
2. **Tvoja najboljša ideja ni časovnica, ampak poravnava ob zamudi in zaščita počitka** — trg to dela za odrasle s sestanki (Reclaim), za dijaka pa ne. To postavi v sredino zaslona in v besedilo ob izdaji.
3. **Odloči se, kdo je uporabnik (ti / slovenski dijaki / širše), preden dodaš še eno funkcijo** — od tega visi vse ostalo, vključno z vprašanjem iPhona in AI-ja.
