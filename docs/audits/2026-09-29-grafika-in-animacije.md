# Grafika in animacije: kaj stane okvirje in kaj se ni premikalo

29. 9. 2026. Povod: vprašanje, ali je aplikacija grafično in zmogljivostno optimizirana, **ne da bi
se ji spremenil videz**, in ali so pravilno izpeljane *vse* animacije — ne samo prehodi med zasloni,
ampak mikroanimacije, vrstice seznamov in kontrole.

Prejšnji audit (27. 9.) je našel in odpravil vzrok, zaradi katerega se je rekomponiralo celotno drevo:
`LocalPulse` in `LocalGlassTilt` kot vrednosti v statičnem lokalu. To je bil požar. Ta pregled je šel
za njim po istem drevesu z drugim vprašanjem — **kaj še piše ali bere stanje sredi geste** — in našel
sedem mest, ki so med drsenjem, vlečenjem ali pritiskom recomponirala ali alocirala vsak frame, ter
osem animacij, ki niso bile izpeljane do konca.

Metoda je bila statična: v tem okolju ni niti JDK niti Gradle, torej ni bilo mogoče zagnati
prevajalnika, `composeMetrics`, `lintDebug` ali emulatorja. Kar je bilo mogoče, je bilo narejeno:
vseh pet repozitorijskih vrat (`derive_palette --check`, `check_sqlite_integrity`,
`check_translations`, `check_presentation`, `check_contrast`) teče zeleno, vsaka spremenjena datoteka
je preverjena na uravnoteženost blokov in na to, da ima vsak nov simbol svoj uvoz, algoritem za
prekrivanja v Ganttovem diagramu pa je bil pred pisanjem Kotlina preverjen numerično — 60 000 naključnih
primerov proti brute-force hoji po dnevih, brez enega sama odstopanja.

---

## 1. Vrednosti, ki se premikajo, prebrane med kompozicijo

Pravilo je eno in ga je prejšnji audit že zapisal: **animirana številka spada v izrisno fazo**. Kjer
se prebere med kompozicijo, invalidira obseg, ki jo je prebral, vsak frame animacije — in obseg, ki
bere kot puščice, ni puščica, ampak cel razdelek pod njo.

### 1.1 `topInset` — vsak frame zlaganja vrhnje vrstice je premikal ves zaslon

```kotlin
// prej
.onSizeChanged { size -> if (!collapsed) topInset = with(density) { size.height.toDp() } }
```

Vrstica se zloži ob vsaki spremembi smeri drsenja in med tem preide **vsako višino med zloženo in
odprto**. `topInset` pa bere `contentPadding` dnevnega seznama, tedenske mreže, mesečne mreže,
letnega pogleda in ciljev. Vsak frame zlaganja je torej rekomponiral zaslon, znova izmeril seznam in
ga znova razporedil — šestdesetkrat na sekundo, na 120 Hz zaslonu stotdvajsetkrat, ravno takrat, ko
bralec drsi.

**Popravek.** Inset se zapiše samo, kadar je izmerjena višja od že znane:

```kotlin
if (!collapsed) {
    val open = with(density) { size.height.toDp() }
    if (open > topInset) topInset = open
}
```

To ni trik, to je pravilo, ki ga je ta inset že sledil, zdaj pa ga sledi do konca: vsebina se umika
**visoki** vrstici, da ob zlaganju nič ne skoči. Odprta višina se izmeri enkrat; potem animacija ne
zapiše več ničesar. Konfiguracija, ki resnično spremeni višino vrstice — zasuk, velikost prikaza,
merilo pisave — ponovno ustvari aktivnost (manifest nima `android:configChanges`), in merjenje se
z njo začne znova.

### 1.2 Puščice: `Modifier.rotate(kot)` na petih mestih

`rotate(kot)` vzame kot kot **argument**, torej se kot prebere med kompozicijo. Pet razkritij v
aplikaciji je imelo puščico; tri so jo animirala (in s tem vsak frame obračanja rekomponirala svoj
razdelek — v letnem pogledu je to mreža dvanajstih kartic), dve pa sta jo **preskočila** v eni
sliki (glava bloka na časovnici in kartica samodejne rutine), kar je bralcu sporočilo, da se kontrola
ne odziva.

**Popravek.** En par pomožnih funkcij v `RoutineMotion.kt`, ki sta ga prevzela vsa mesta:

```kotlin
@Composable fun rememberChevronTurn(expanded: Boolean): State<Float> =
    animateFloatAsState(if (expanded) 180f else 0f, spatialSpec<Float>(LocalReduceMotion.current), label = "chevron-turn")

fun Modifier.turning(angle: State<Float>): Modifier = graphicsLayer { rotationZ = angle.value }
```

`graphicsLayer` zasuk okoli središča (privzeti `transformOrigin`) — **isti piksli** kot `rotate`,
drugačna je samo faza, v kateri se številka prebere. Puščice, ki so stale, so zdaj enake tistim, ki so
se že obračale; nobena se ne razlikuje po videzu v mirovanju.

### 1.3 `RoutineSwitch` in `LiquidSlider`: vzmet v postavitveni fazi

* **Drsnik** je imel polnilo kot otroka z `fillMaxWidth(shown)`. Delež v postavitvenem modifikatorju
  je postavitev animirane vrednosti: vsak frame vzmeti je znova izmeril stezo, znova izmeril drsnik
  poleg nje in znova posnel sloj, ki ga drsnik lomi. Ker drsenje objavi novo vrednost ob vsakem
  premiku, je bil to vsak frame vsakega vlečenja vsakega drsnika v urejevalniku vnosa. Zdaj se
  polnilo **nariše** (`drawWithContent`) med podlago in lasnico — vrstni red slikanja je enak, kot je
  bil, ko sta bila to dva otroka.
* **Stikalo** je v kompoziciji mešalo barvo podlage in na frame gradilo nov preliv za rob. Oboje je
  zdaj v izrisni fazi enega samega gradnika (`SwitchTrack`), preliv pa je `remember`-an: `Brush` je
  ključ, pod katerim si izrisovalnik zapomni preveden senčilnik, zato sveč primerek na frame pomeni
  svež senčilnik na frame.

Lasnica (`Modifier.border`) je ostala lasnica. Njena barva je edino branje vzmeti, ki je ostalo v
kompoziciji, in ostalo je zavestno: rob v izrisni fazi bi bilo treba risati kot `drawOutline` z
`Stroke`, kar se od `border` razlikuje za pol dp — vidna razlika, ta audit pa videza ne spreminja.

### 1.4 Napis minut med vlečenjem bloka

```kotlin
if (dragging) {
    RoutineLabel(text = stringResource(R.string.drag_minutes, (dragY / dragStepPx).roundToInt() * 15), …)
}
```

`dragY` je bil delegiran `var`, prebran **v telesu kartice**. Vsak frame vlečenja je rekomponiral
kartico z vsem, kar nosi: žetone, akcije, razkriti razdelek — da se je spremenila ena številka.
Odmik je zdaj držan kot objekt stanja (`mutableFloatStateOf` brez delegata), sloj ga bere v izrisni
fazi, napis pa je svoja kompozitna funkcija `DragMinutesLabel(offset, stepPx)`, ki frame zapre vase.

### 1.5 Dvojna animacija velikosti

Kartica bloka in kartica mejnika sta imeli `animateContentSize` **na kartici** in `expandVertically`
**v razkritju** — dve animaciji iste višine, ki se nista ujemali, zato je razkritje ob koncu
zaigralo. `animateContentSize` znova meri celotno poddrevo (in z njim seznam, v katerem kartica
sedi) na vsak frame; `expandVertically` izmeri vsebino enkrat in animira izrez. Ostal je samo
`expandVertically`. Poleg tega sta obe razkritji prej uporabljali gol `tween`, ki nikoli ne vpraša
sistemske nastavitve »odstrani animacije« — zdaj gre vse skozi `revealEnter`/`revealExit`.

---

## 2. Alokacije na frame: čopiči in poti

| Mesto | Prej | Zdaj |
|---|---|---|
| `Modifier.liquidUnderGlow` | radialni preliv na vsak izris — pod plavajočimi karticami, ki se ob drsenju znova posnamejo vsak frame | `drawWithCache`: en preliv na velikost |
| Rob stikala (`RoutineSwitch`) | navpični preliv na frame | `remember`-an preliv, vzmet modulira alfo barve |
| Rob zavihkov kategorij (`CategoryTabs`) | vodoravni preliv na vsak izris, nad vrstico, ki jo prst ravno vleče | `drawWithCache` |
| Kolesce barv (`SubjectColorPicker`) | krožni preliv + radialni na vsak izris, med vsakim vlečenjem po kolescu | preleva v `drawWithCache`, senčilo drsnika v svojem `drawBehind` |
| Ikone koledarja (`MonthMarkIcon`) | `Path()` na vsak izris — nativna alokacija in teselacija; v mesečni mreži jih je do 84 | `drawWithCache`, pot samo za marko, ki se res riše |
| `sheetFlingStabilizer` | nov `NestedScrollConnection` ob vsaki kompoziciji lista | en sam deljen objekt brez stanja |

Kolesce barv je bilo razdeljeno na dva modifikatorja namerno: dva preleva sta odvisna samo od
velikosti, senčilo pa sledi drsniku svetlosti. V enem bloku, ki bere `value`, bi se ob vsakem
premiku drsnika znova prevedla **oba** senčilnika.

---

## 3. Izpeljave, ki so se računale znova, čeprav se nič ni spremenilo

* **`RoutineApp`**: množica ključev opozoril, število zapadlih nalog in seznam nalog dneva so se
  izračunali ob vsaki rekompoziciji — torej ob vsakem utripu ure in ob vsakem stanju shranjevanja.
  Zdaj so v `remember`, ključevani po **majhnih** stvareh: po seznamu opozoril tega dne in po
  `state.planning.tasks`. Nikoli po `data.days` — v letnem pogledu je to 365 vnosov in njihova
  primerjava bi stala več od dela, ki ga shrani.
* **Letni pogled**: vsaka od dvanajstih mesečnih kartic je filtrirala **vseh 365 dni** in na vsakem
  klicala `YearMonth.from` — dvanajst prehodov, štiri tisoč koledarskih izračunov, znova ob vsakem
  razkritju panela. Zdaj en prehod napolni dve mapi (`monthTotalsOf`), kartice pa bereta števili.
* **Ganttov diagram ciljev**: prekrivanja so se iskala s hojo po **vsakem dnevu** razpona (za
  dvoletni EE 730 dni), kjer je vsak dan vprašal vsa okna, koliko jih ga pokriva. Nadomeščeno s
  sweepom po robovih oken, seštetim **na dan**: okno, ki se zapre na dan, ko se drugo odpre, na tem
  dnevu globino spremeni dvakrat, in če se ti dve spremembi štejeta ločeno, se ena sama loma prelomi
  na šivu. Funkcija je `internal` in brez Compose, zato jo test pokriva s sedmimi primeri (šiv,
  gnezdenje, tri okna, dve ločeni sezoni, dotik, prazno).
* **Mesečna mreža**: vseh 42 celic je nosilo `Modifier.alpha(1f)` ali `alpha(0.4f)`. `alpha(1f)` ni
  nič — je sloj. Zdaj ga nosi samo največ 11 celic sosednjega meseca, ki so res zatemnjene.

---

## 4. Animacije: kaj ni bilo izpeljano do konca

| # | Kaj | Prej | Zdaj |
|---|---|---|---|
| 1 | Puščici v glavi bloka in v kartici samodejne rutine | preskočita v enem okvirju | obrneta se na isti vzmeti kot vse ostale |
| 2 | Razkritje akcij v kartici samodejne rutine | gol `if (expanded)` | `AnimatedVisibility` z `revealEnter`/`revealExit` |
| 3 | Razkritja na časovnici in v listu nalog | gol `tween`, ki ne vpraša »odstrani animacije« | deljena specifikacija, ki jo vpraša |
| 4 | Dvojna animacija velikosti (kartica bloka, mejnik) | `animateContentSize` + `expandVertically` | samo `expandVertically` |
| 5 | Onboarding: korak | vsebina se je **zamenjala** v enem okvirju | `AnimatedContent` z drsenjem v smeri potovanja in vzmetjo velikosti — ista os kot menjava merila na časovnici |
| 6 | Onboarding: pike korakov | velikost in barva skočita | rasteta in se obarvata (`animateDpAsState`, `animateColorAsState`) |
| 7 | Morf »Dodaj blok« → list | `tween(420, FastOutSlowInEasing)` zapisan v funkciji | `containerMorphSpec` v besedišču gibanja; številka je poimenovana in dokumentirana |
| 8 | Kolesce za čas | ob **odprtju** je tiknilo, čeprav se ni nič ustavilo | tik odgovarja samo na rob iz `snapshotFlow` (`.distinctUntilChanged().drop(1)`) |

Za 5 in 6 velja isto pravilo kot za vse ostalo: **mirovanje je nespremenjeno**. Druga pika je po
prehodu na korak 2 enako velika in enako obarvana kot prej; spremeni se samo pot do tja. To sta edini
mesti, kjer se je kaj vidnega dodalo, in dodano je bilo zato, ker je bilo vprašanje »ali so vse
animacije pravilno izpeljane« — indikator koraka, ki skoči, in zaslon, ki utripne, nista.

Razen tega je bil preverjen celoten inventar gibanja, ne samo prehodi: `animateItem` na sedmih
seznamih, ki se spreminjajo (dodano v prejšnjem auditu, tokrat preverjeno, da so podane **vse tri**
specifikacije, ker privzeti zbledi nikoli ne vprašajo sistemske nastavitve), neskončni utrip NOW
traku (en sam `rememberInfiniteTransition` za celo aplikacijo, bran v izrisni fazi), vzmet ob
pustitvi vlečenja bloka, `PopSpring` na rdeči piki nalog (edini dovoljeni odboj), stekleni dotik
(`grab`, `glow`, `point` — vsi trije v izrisni fazi) in drsnik drsenja nazaj iz ciljev
(`backProgress` v `graphicsLayer`).

---

## 5. Sistemski klici na glavni niti

* **Haptika je na vsak tik prestopila v drug proces.** `allowed()` je ob vsakem klicu bral
  `Settings.System.getInt(…HAPTIC_FEEDBACK_ENABLED…)` — poizvedba pri ponudniku vsebin, torej binder
  klic — in `vibrate()` je ob vsakem klicu bral `hasVibrator()`, spet binder. Vlečenje bloka tikne na
  vsakih 15 minut koraka, torej je gesta, ki jo bralec **čuti**, vsak detent plačala dva medprocesna
  skoka na glavni niti. Prisotnost motorja se zdaj vpraša enkrat ob konstrukciji; sistemska
  nastavitev se brani največ enkrat na sekundo. Nastavitev se ne more spremeniti hitreje, kot bralec
  zapusti aplikacijo in se vrne, in ob vrnitvu je predpomnilnik potekel, zato je odgovor še vedno
  svež — le da ni več plačan ob vsakem tiku.
* **`RoutineSounds.loaded` je bil navaden `mutableSetOf`.** `setOnLoadCompleteListener` odgovarja na
  SoundPool-ovi niti, `play()` sprašuje na glavni: branje in pisanje navadne množice z dveh niti je
  dirka, v najmilejšem primeru zvok, ki se nikoli ne predvaja, v najslabšem pa resize `HashMap`, ki
  ga hodita dve niti hkrati. Zdaj je `ConcurrentHashMap.newKeySet()`.

---

## 6. Stabilnost: zakaj kompozitne funkcije ni mogoče preskočiti

Compose presodi stabilnost samo tipom, ki jih **sam prevede**. Vse, kar pride iz `:core`, je zato za
`:app` nestabilno, ne glede na to, kako nespremenljivo je — nestabilen parameter pa naredi kompozitno
funkcijo nepreskočljivo, in nepreskočljiva funkcija se rekomponira vedno, ko se njen klicatelj, tudi
če se nič, kar bere, ni spremenilo.

`app/compose-stability.conf` je imel pet domen. Dodanih je bilo 19 tipov, ki jih kompozitne funkcije
res prejemajo (`HealthConfig`, `PlanningConfig`, `ActiveExecution`, `Task`, `Milestone`,
`GoalsProject`, `GoalActivity`, `GoalMilestone`, `GoalProgress`, `BacklogEntry`, `StudyTopic`,
`HistoricalVelocity`, `QuickAddPreset`, `DailyMetrics`, `PositionedBlock`, `PeriodicBreakConfig`,
`SleepSchedule`, `EntryDefaults`, `ResolvedTimelineItem`), vsak prej preverjen, da je `val`-only
`data class` brez spremenljivih zbirk. Vsaka vrstica v tej datoteki je zaupanje, ki ga Compose
izkoristi brez preverjanja, zato v njej ni in ne sme biti repozitorijev, motorjev, planerjev ali
posnetkov baze.

Zraven:

* `DailyTimeline(dueTasks: List<Task>)` → `PersistentList<Task>`. Klicatelj je že predajal
  nespremenljiv seznam; vmesni tip `List` je bil tisti, ki je parametru vzel stabilnost.
* `RoutineLabel` je ob **vsaki** rekompoziciji sestavil nov `TextAutoSize.StepBased`. `RoutineLabel`
  je najbolj rabljena kompozitna funkcija v aplikaciji (vsak časovni žleb, žeton, gumb, zavihek),
  svež primerek pa je svež parameter za besedilo pod njim — torej svež tek iskanja manjše pisave, ki
  se ustavi šele, ko besedilo sede. Zdaj je `remember(autoSize, designed)`.
* `minuteLabel` je uporabljal `String.format`, ki ob vsakem klicu razčleni vzorec in zaboja oba
  argumenta; dan z dvajsetimi bloki ga pokliče štiridesetkrat na rekompozicijo. Obe jezikovni
  različici pišeta ASCII števke, zato je dopolnjevanje do dveh mest aritmetika. Za minuto, ki ni del
  dneva, ostane formatter — nič, kar je prej kaj izpisalo, zdaj ne izpiše nič drugega. Test pokriva
  robove dneva in enomestne ure.

---

## 7. Kaj sem izmeril in pustil pri miru

* **`GoalBar`** animira širino s `fillMaxWidth(width)` — postavitvena faza. Ostane: `RoutineShapes.Chip`
  je `RoundedCornerShape(8.dp)`, in če bi se polnilo risalo namesto merilo, bi bilo zaokrožanje treba
  podvojiti v izrisni fazi. Poleg tega je tu en majhen gradnik, ki se premika kratek čas ob spremembi
  napredka, ne vsak frame gesta.
* **`DailyTimeline`** izpelje `slipped`, `blocks` in `items` ob vsaki rekompoziciji. Ostane: ura tikne
  **enkrat na minuto** (`RoutineApp` se poravna na minuto), torej je to en prehod po 20 vnosih na
  minuto. `remember(day.items)` bi ob vsaki rekompoziciji primerjal 20 blokov s po 15 polji —
  primerjava bi stala več od dela, ki ga shrani. Pravi popravek bi bil izpeljati bloke tam, kjer se
  `DayUi` sestavi (eno samo mesto v `RoutineViewModel`), kar je sprememba oblike stanja in ne
  optimizacija; ostane kot priporočilo.
* **`AppOverlays(state)`** bere celotno stanje. Ostane: je razdelilnik, kjer vsak gostitelj lista
  odneha, preden karkoli naredi, ko je list zaprt. Zožitev parametra bi se dotaknila enajstih klicnih
  mest in ne bi prihranila nobenega okvirja.
* **`routineGlassTouch`** gradi radialni preliv na izris. Ostane: središče je točka pod prstom, ki se
  premika vsak frame, `drawWithCache` pa predpomni po velikosti — predpomnjenje tu ne bi predpomnilo
  ničesar.
* **`AddBlockMorph`** se med morfo rekomponira vsak frame. Ostane (že sklep prejšnjega audita): ena
  majhna komponenta z lastnim obsegom, 420 ms, enkrat na odprtje lista.
* **Osnovni profil** (`baseline-profile`) manjka. Ne more se napisati na roko: potrebuje
  `androidx.benchmark.macro.junit4` na napravi in generiran `baseline-prof.txt`. Priporočeno, ker je
  prvi zagon po hladnem startu natanko tisti, kjer se AOT prevajanje pozna.
* **Notranjosti knjižnice `kyant0/backdrop`** ostajajo nedotaknjene — brez dostopa do izvorne kode
  ni mogoče vedeti, kaj predpomni, in noben popravek tu ne sme sloneti na ugibanju.

---

## 8. Kaj se je spremenilo vidno

Nič v mirovanju. Nobena barva, velikost, razmik, oblika, rob, senca ali besedilo se ni spremenilo;
vsi popravki so ali prestavljeni v drugo fazo (kompozicija → izris), ali zaprti v manjši obseg, ali
izračunani enkrat namesto dvanajstkrat, pri čemer je rezultat isti.

Dve izjemi sta **prehoda**, ne videza, in sta odgovor na drugi del vprašanja: kartica samodejne
rutine zdaj svoje akcije razkrije z zlaganjem namesto z menjavo, onboarding pa korak zdrsne in piki
zrasteta, namesto da bi zaslon utripnil. Oba sta pod sistemsko nastavitvijo »odstrani animacije«
`snap`, tako kot vse ostalo v aplikaciji.

---

## 9. Preverjanje

| Kaj | Kako | Izid |
|---|---|---|
| Besedila, videz, žetoni, steklo, koledar | `python3 tools/check_presentation.py` | zeleno (776 virov, 58 datotek UI) |
| Barvna streha | `python3 tools/check_contrast.py` | zeleno (109 parov) |
| Prevodi | `python3 tools/check_translations.py` | zeleno (783 virov, 1 jezik poleg slovenščine) |
| Paleta | `python3 tools/derive_palette.py --check` | zeleno |
| Celovitost sheme | `python3 tools/check_sqlite_integrity.py` | zeleno (19 testov) |
| Uravnoteženost blokov, vsaka spremenjena datoteka | lasten pregledovalnik | 0 napak |
| Uvozi vsakega novega simbola | lasten pregledovalnik | 0 manjkajočih |
| Algoritem prekrivanj | 60 000 naključnih primerov proti brute-force hoji | 0 odstopanj |
| Nova testa | `GoalGanttOverlapTest` (7 primerov), `MinuteLabelTest` (3) | v `app/src/test`, tečeta v CI |

**Ni bilo mogoče preveriti brez naprave:** `./gradlew :app:assembleDebug`, `:app:lintDebug`,
`:app:composeMetrics` (poročilo o preskočljivih kompozitnih funkcijah — to je poročilo, ki bi
potrdilo 6. razdelek), `composeCompilerReports` za obseg rekompozicij, Perfetto sled enega potega in
`dumpsys gfxinfo … framestats` pred/po na istem scenariju. Ko je gradnja na voljo, je to prvi
naslednji korak, in 6. razdelek je tisti, ki ga številke bodisi potrdijo bodisi ovržejo.
