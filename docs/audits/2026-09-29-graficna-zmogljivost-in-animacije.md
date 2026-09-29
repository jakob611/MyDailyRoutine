# Grafična zmogljivost in animacije — pregled aktualne kode

**Datum:** 29. 9. 2026  
**Osnova:** `7c405ffc6cb8e9b66dbd3e9c1a5e690ac8279534`, veja `arena/01a0edd0-mydailyroutine`  
**Vrsta pregleda:** statična analiza, omejeni popravki implementacije, dodani regresijski testi. **Ni meritev FPS/janka na napravi.**

## 1. Sklep

**Arhitekturna osnova je dobra, grafične optimiziranosti pa še ni mogoče potrditi. Vse animacije niso bile pravilno oziroma dosledno implementirane.** Obstajajo konkretne napake v življenjskem ciklu gest, nepotrebna branja animacijskega stanja med kompozicijo in nepotrebno ustvarjanje grafičnih objektov. Del teh težav je popravljen v priloženih spremembah. Nekaj pomembnih poti ostaja odprtih in je opisanih spodaj.

Ne priporočam odstranjevanja stekla, spremembe pisave, manjše zameglitve ali poenostavitve vizualnega jezika na slepo. Najprej je treba odstraniti nepotrebno delo, nato izmeriti CPU in GPU ločeno. `Dispatchers.Default` ne rešuje stroškov kompozicije, merjenja ali GPU-učinkov.

### Obseg

- Inventar izhodiščne aktivne kode: **132 datotek Kotlin, 21.463 vrstic** v `app/src/main/java` in `core/src/main`.
- Strukturni pregled obeh modulov, gradnje, odvisnosti, toka stanja, podatkovnih adapterjev, zagona, alarma/widget poti, virov, testov in CI.
- Podroben pregled grafično pomembnih poti: glavni zaslon in prekrivni zasloni, dnevni/tedenski/mesečni/letni pogled, cilji/Gantt, urejevalniki, nastavitve, naloge, uvodni tok, skupne komponente, geste, gibanje, steklo in oblike. Popisane so tudi mikroanimacije, ne le navigacija.
- ZIP-i in oblikovalski dokumenti v korenu niso aktivna izvorna koda Gradle gradnje. Pretekli auditi so kontekst, ne dokaz trenutnega časa okvirjev.
- To **ni funkcionalna verifikacija vseh poslovnih algoritmov**, meritev na telefonu ali slikovna primerjava pred/po. Statična analiza ne more potrditi, da aplikacija zdaj dosega 60/120 FPS.

## 2. Kaj je v projektu dobro

| Področje | Ugotovitev |
|---|---|
| Ločitev odgovornosti | `core` je JVM domena brez Compose/Android odvisnosti; `app` vsebuje adapterje in UI. Dva modula sta smiselna, novih modulov ne bi uvajal samo zaradi videza »profesionalnosti«. |
| Sestavljanje odvisnosti | `AppGraph` je jasen composition root. Poslovni algoritmi niso raztreseni po composable funkcijah. |
| Tok podatkov | `TimelineAction → RoutineViewModel → repository → StateFlow`; UI uporablja `collectAsStateWithLifecycle`. |
| Težji izračuni | `resolveContent()` uporablja `Dispatchers.Default`; snapshoti in transakcije podatkovnih adapterjev uporabljajo IO/Room. |
| UI modeli | Persistent kolekcije in razdelitev na `content`, `panels`, `planning`, `goals` omogočajo omejevanje sprememb. Stabilnost je pogodba: navaden `List` v `@Immutable` modelu se kljub oznaki ne sme mutirati. |
| Seznami | Dnevna časovnica, naloge, predmeti in načrtovanje uporabljajo lazy sezname ter stabilne ključe. Dinamične vrstice večinoma že imajo `routineItemAnimation()`. |
| Steklo | Glavni backdrop in plavajoči chrome sta ločena; `SheetShell` ima svoj backdrop. To je prava smer, ne razlog za odstranjevanje efekta. |
| Utripanje | `LocalPulse` že nosi `State<Float>`, `pulsing` bere vrednost v `graphicsLayer`. Stara napaka z animacijskim `Float` v korenskem statičnem localu v tej osnovi ni več prisotna. |
| Merjenje listov | Glava in vsebina `SheetShell` sta urejeni s podkompozicijo; začetni odmik telesa ni več odvisen od naslednjega okvirja `onSizeChanged`. |
| Gradnja | Release že uporablja R8 in resource shrinking. Odvisnosti so pripete; nadgradnja Compose/Backdrop med tem pregledom bi dodala nepotrebno spremenljivko. |
| Oblikovanje | Centralizirani žetoni, prevodi, geometrija in kontrastna preverjanja. Tega za optimizacijo ni treba podreti. |

## 3. Konkretne težave, popravljene v tem pregledu

Vse poti v tej sekciji so relativne glede na `app/src/main/java/com/example/mydailyroutine/`.

### A. Stekleni pritisk je animacijske vrednosti bral v kompoziciji

**Datoteka:** `core/designsystem/glass/RoutineGlass.kt`, `rememberGlassTouch()` / `routineGlassTouch()`.

Prej je funkcija sestavila `GlassTouch(source, izračunIzFraction, glow, point)`. S tem je animacijski `State` dereferencirala med kompozicijo. Čeprav je porabnik nato uporabil `graphicsLayer`, je bilo prepozno: branje v kompoziciji se je že zgodilo. Klic za spodnji gumb je v `RoutineApp`, zato lahko ta vzorec invalidira precej širši obseg kot majhen gumb.

**Popravek:** stabilen, zapomnjen `GlassTouch` hrani reference na `State`; vrednosti se preberejo šele v layer/draw povratnih klicih. Geometrija, vzmet in barve ostanejo iste. To odstranjuje znan vir invalidacij; ne dokazuje, da se je prej ob vsakem pritisku nujno ponovno izrisalo prav vse poddrevo — Compose lahko otroke preskoči.

**Dodatna napaka:** ob `Release/Cancel` se je `point` takoj nastavil na `null`, izris pa je zato preskočil še vedno tekoči fade-out. Zdaj točka ostane na voljo do izteka sija. Obris se predpomni po velikosti/obliki.

### B. Drsnik je med vlečenjem lovil prst z dodatno vzmetjo

**Datoteka:** `core/designsystem/components/LiquidControls.kt`, `LiquidSlider()`.

- Vsaka nova vrednost med vlečenjem je postala nova tarča `glassMorphSpec`: nastal je nameren animacijski zaostanek, ki ga uporabnik občuti kot lag tudi brez izgubljenih okvirjev.
- `fillMaxWidth(shown)` je animacijski položaj bral med kompozicijo in spreminjal mero polnila.
- Detektorja sta lahko zadržala star `onValueChange`.

**Popravek:** neposreden prikaz trenutne vrednosti med aktivnim vlečenjem; obstoječa vzmet ostane pri tapu in zunanjih spremembah. Polnilo se riše z enako zaoblitvijo in zaokroževanjem širine na piksle, animacijske vrednosti pa se berejo v draw/layer fazi. Povratni klic je posodobljen z `rememberUpdatedState`; preklic detektorja počisti `dragging`. Dodani sta tudi semantika napredka in dostopna akcija za spremembo vrednosti.

**Omejitev:** obstoječa geometrija položaja gumba ni preoblikovana. Njegov `translationX` še vsebuje dodatek polovice širine gumba; to zahteva ločen pregled končnih položajev, če se bo popravljala poravnava. V tem koraku nisem tiho spreminjal njegove postavitve.

### C. Optični učinki so neposredno uporabljali vrednost odskočne vzmeti

**Datoteka:** `LiquidControls.kt`, `liquidDisc()`.

Vzmet sme preseči interval 0..1, polmer zameglitve in alpha pa nista prostorski animaciji. Neomejen `grab` je pomenil možnost negativnega polmera zameglitve ali intenzitete zunaj pričakovanega intervala.

**Popravek:** ločitev prostorske rasti od optične intenzitete. Rast ohrani vzmet; učinki uporabljajo omejeno intenziteto 0..1, prebrano prek lambde v efektni/draw poti. Notranja senca pri ničelni intenziteti ni ustvarjena. S tem niso odstranjene vidne sence ali steklo v normalnem stanju.

### D. Staro stanje v gesti stikala

**Datoteka:** `LiquidControls.kt`, `RoutineSwitch()`.

`pointerInput(enabled, reduceMotion, travelPx)` ne zažene detektorja znova samo zato, ker se spremeni `checked` ali `onCheckedChange`. Lambda je zato lahko ob koncu/preklicu draga odločala na podlagi starega stanja.

**Popravek:** `rememberUpdatedState` za trenutno potrjeno stanje in callback, brez ponovnega zagona geste ob vsaki spremembi.

### E. Preklic vodoravne geste je lahko sprožil navigacijo

**Datoteka:** `core/designsystem/components/SwipeToShift.kt`.

Rezultat `horizontalDrag()` se je ignoriral. `false` pomeni preklic, ne potrditve. Poleg tega stanje ni bilo nujno ponastavljeno, če se je coroutine detektorja preklicala ob odstranitvi/onemogočenju modifierja.

**Popravek:** navigacija le po uspešno zaključeni gesti, čiščenje v `finally`, aktualen callback/haptika in pravilno upoštevanje `threshold` pri `remember`. Upoštevan je tudi premik prek začetnega touch slopa. Vizualna upornost in omejitev odmika ostaneta isti.

### F. Podvojene animacije vračanja dnevne kartice

**Datoteka:** `features/timeline/components/TimelineComponents.kt`.

Vsak spust/preklic je ustvaril nov `Animatable` v ločeni coroutine, vse pa so pisale isti `dragY`. Hiter nov drag je zato lahko tekmoval s prejšnjim vračanjem.

**Popravek:** hranjenje in preklic prejšnjega `Job` ob novi gesti/vračanju; zmanjšano gibanje zaključi vračanje brez vzmeti. Ključi detektorja zajamejo tudi spremembe samega bloka in merila draga.

### G. Politika zmanjšanega gibanja ni bila dosledna in se ni osveževala

**Datoteke:** `RoutineMotion.kt`, `TimelineComponents.kt`, `TasksSheet.kt`, `RoutineApp.kt`.

- `rememberReduceMotion()` je sistemske nastavitve prebral samo enkrat. Povratek iz nastavitev ne pomeni nujno nove Activity/kompozicije.
- Razširitve kartic, nekatere vidnosti v nalogah in vračanje kartice niso uporabljali lokalne politike.
- `AddBlockMorph` ni imel spremembe `reduceMotion` med ključi učinka.

**Popravek:** `ContentObserver` za tri nastavitve, z odjavo ob odstranitvi; dosledne `snap` veje na navedenih mestih; osvežitev morfa; local je na voljo tudi uvodnemu toku. Normalne uporabljene specifikacije gibanja so ohranjene.

**Pomembna razlika:** Compose sam že upošteva `ANIMATOR_DURATION_SCALE`. Trdo zapisana vzmet zato še ne dokazuje kršitve Androidovega stikala »Odstrani animacije«. Napaka je bila v dodatni pogodbi aplikacije, ki upošteva tudi window/transition scale, in v enkratnem branju. To ni trditev, da so bili vsi Material učinki napačni.

### H. Nepotrebne alokacije pri risanju gradientov

**Datoteki:** `RoutineGlass.kt` in `features/subjects/presentation/SubjectColorPicker.kt`.

`liquidUnderGlow` je ob vsakem risanju ustvarjal gradient, tudi pri nevidni intenziteti. Barvno kolo je ob risanju ustvarjalo sweep/radial gradienta.

**Popravek:** `drawWithCache` za geometrijo in čopiče, neviden podsvet izpuščen, svetlost barvnega kolesa prebrana v draw fazi. Paleta, polmeri in položaji niso spremenjeni.

To zmanjšuje alokacije. Brez sledi GPU ne trdim, da vsak nov `Brush` nujno pomeni novo prevajanje GPU-programa.

### I. Napredek ciljev se je sestavljal znova po okvirjih

**Datoteka:** `features/goals/presentation/GoalsScreen.kt`, `GoalBar()`.

**Popravek:** širina animacije se prebere med merjenjem ozkega polnila, ne med kompozicijo. Oblika polnila ostane odvisna od dejanske širine, prostorska vzmet ostane ista. Širina je omejena na 0..1, da odskok ne prekorači razpoložljive mere. Pri zmanjšanem gibanju začetni prikaz ne začne umetno pri nič.

### J. Časovno kolesce: napačna predpostavka o mirovanju in podvojena haptika

**Datoteka:** `core/designsystem/components/TimeWheelPicker.kt`.

Potrditev je uporabljala `firstVisibleItemIndex` s predpostavko, da uporabnik ne more pritisniti potrditve med flingom. To lahko stori. Kolesce je tudi oddalo haptiko ob prvem mirovanju in lahko dvakrat ob tapu (takoj + ob koncu pomika).

**Popravek:** izbira najbližje dejansko sredinske vrstice iz `layoutInfo`, vključno z odmiki content paddinga; en odziv ob novi umirjeni izbiri, brez začetnega odziva. Programski pomik pri zmanjšanem gibanju uporabi `scrollToItem`.

## 4. Odprte ugotovitve — niso prikazane kot rešene

### P1 — Prehod lahko animira nalaganje namesto dejanske strani

`RoutineViewModel.content` ob spremembi datuma/merila najprej odda `TimelineContent(..., isLoading = true)`. `RoutineApp` uporablja `AnimatedContent` s ključem `(date, mode)`.

To pomeni: prehod se začne proti novemu ključu s spinnerjem; ko prispe vsebina istega ključa, se spinner zamenja z vsebino brez novega prostorskega prehoda. Pri dražjem letu ali hladni bazi lahko uporabnik upravičeno vidi »prehodov skoraj ni«, četudi so specifikacije pravilne.

**Naslednji korak:** ločiti želeni navigacijski cilj od zadnje pripravljene vsebine; prehod z dejansko vsebino in jasnim stanjem nalaganja. Ne samo odstraniti `onStart`, ker bi s tem tvegali prikaz starih podatkov pod novim datumom.

### P1 — Zgornji odmik se med odpiranjem glave ponovno meri prek stanja

`RoutineApp`: `topInset` začne pri nič, `onSizeChanged` ga spreminja, kadar `!collapsed`. Ob ponovnem odpiranju glave je `collapsed` že `false`, `AnimatedVisibility` pa še povečuje višino. Zato lahko merilni povratni klic posodablja padding seznamov med animacijo. Ob prvem prikazu obstaja tudi pot od ničelnega do izmerjenega odmika.

**Naslednji korak:** ločiti polno rezervirano višino glave od njene trenutne animirane višine, upoštevati font scale/insete/orientacijo in prehode v Goals. To je sorodna, vendar ne ista težava kot že popravljeni `SheetShell`. Potrebna je slikovna in merilna regresija.

### P1 — Izhod spodnjega lista ne ohranja vedno podatkov urejevalnika

`RoutineSheet` pravilno obdrži vsebino do konca `hide()`, vendar klicatelj medtem lahko že odstrani podatke. Primer: `AppOverlays` ob `editingBlock = null` še vedno kliče vsebino lista, `EntryEditorSheet` pa iz veje `OccurrenceEditor` preide v navadni urejevalnik. Podobno je treba pregledati reset začetnih vrednosti pri urejevalnikih ciljev in mejnikov.

**Naslednji korak:** seja lista mora ohraniti payload/osnutek do konca izhoda. Preveriti tudi `visible: true → false → true` med `hide()`: trenutni host ob ponovnem odprtju že montiranega lista ne zahteva izrecno `show()`.

### P1/P2 — Predictive back in običajni izhod nista usklajena

`backProgress` je pravilno prebran v `graphicsLayer`. Toda `finally` ga takoj nastavi na nič, tudi ob preklicu. Preklic zato nima animiranega povratka; ob potrditvi sledi še običajen `AnimatedContent` izhod. To je konkretna pot do vizualnega preskoka, ne dokaz iz meritve.

**Naslednji korak:** uskladiti stanja gesture/commit/cancel in prehod; izhod naj se nadaljuje iz dejansko doseženega položaja. Potreben test z Android predictive-back dogodki, ne samo `onBackPressed()`.

### P2 — Še vedno se opravlja ponovljivo delo v kompoziciji

- `GoalGantt`: filtriranje dejavnosti po projektu, sprehod čez dneve za prekrivanja in pakiranje vrstic znotraj kompozicije.
- `YearlyOverview`: filtriranje dnevov za posamezne mesece; večkratni agregati ob ponovni kompoziciji.
- `DailyTimeline`: ponovljene projekcije/filterji, ter filtriranje opozoril za posamezno vrstico.
- `RoutineSwitch`: barva/obroba proge še bereta `position.value` v kompoziciji. To je manjši lokalni obseg, ne enaka težava kot koren aplikacije.
- Nekateri vsebinsko dolgi odseki ciljev so en sam `LazyColumn.item` z notranjimi stolpci. Zunanji lazy seznam ne virtualizira vseh notranjih dejavnosti.

**Naslednji korak:** profiliranje pri reprezentativnem številu vnosov; nespremenljive projekcije/cache po podatkovnih ključih, po potrebi priprava v predstavitveni plasti. `remember` ne sme imeti samo »praznih ključev«, ker bi podatki zastarali. Majhne fiksne mreže (npr. 42 dni) niso avtomatično napaka in jih ni treba vseh pretvarjati v lazy mreže.

### P2 — Leto računa polne dnevne podatke za 365/366 dni

`PeriodRanges.YEAR` in `resolveContent()` izvajata razreševanje ter zdravstvena pravila za celotno šolsko leto. Delo je pravilno zunaj glavne niti, vendar lahko pomeni zakasnitev do pripravljene strani in dodatne alokacije. To ni dokaz, da GPU zamuja.

**Naslednji korak:** meriti latenco priprave in preklic hitrih navigacij, nato preučiti predpomnjenje/projekcije. Ne odstraniti opozoril ali pravil samo zaradi hitrosti.

### P2 — GPU strošek stekla je še neizmerjen

Vibrancy, blur, lens, sence, podsvet kartic, clipping in istočasna izhodna/vhodna stran so potencialno dragi. Manj rekompozicij še ne pomeni manj GPU časa. `LocalPulse` je en skupen vir, a animirani elementi znotraj posnetega backdropa lahko še vedno zahtevajo ponovno risanje njegove vsebine. Pulz se tudi ustvari na korenu, ko na trenutni strani ni njegovega vidnega porabnika.

**Naslednji korak:** FrameTimeline/RenderThread/GPU sled med scrollom in odpiranjem lista. Diagnostični A/B izklop efektov v lokalni meritvi je uporaben, trajna vizualna sprememba brez dogovora pa ne.

### P2/P3 — Dodatna doslednost in dostopnost

- `rememberReduceTransparency()` ostaja enkratno branje, brez poslušalca za spremembo kontrasta. V tem patchu ni prenovljen.
- `ManagedRoutineCard` razširi akcije z golim `if`; chevroni dnevnih/mejnih/upravljanih kartic se obračajo s takojšnjo zamenjavo 0/180, ne interpolacijo. To je manjkajoča mikroanimacija, ne nujno problem zmogljivosti.
- Zavihki nastavitev/načrtovanja in koraki onboardinga nimajo vsi posebnega vsebinskega prehoda. Dodajanje bi pomenilo novo vedenje; ni bilo narejeno na slepo.
- `SwipeToShift` ob nedokončanem potegu še vedno takoj ponastavi odmik. Popravljena sta preklic in napačna navigacija, ni dodana nova vzmet vračanja.
- `RoutineLabel` privzeto uporablja auto-size: to je veljavna vizualna zahteva, a pomeni dodatno merjenje besedila. Ne izklapljati globalno samo zaradi hitrosti.

## 5. Inventar animacij

| Pot / komponenta | Ocena po tem patchu |
|---|---|
| Dan/teden/mesec/leto, premik po času | Specifikacije in ključ imajo smisel; ostaja težava prehoda na loading vsebino. |
| Goals: običajni vhod/izhod | Fade + prostorski prehod; meriti dvojno drevo in integracijo predictive back. |
| Predictive back | Branje v layer fazi dobro; preklic/zaključek imata zgoraj opisano odprto pot. |
| Zložljiva glava | Specifikacije obstajajo; težava je posodabljanje odmika vsebine med merjenjem. |
| Indikator Dan/Teden/Mesec/Leto | Animacija pomika se bere v layer callbacku; ni potrebe po prepisu v ročne offsete. |
| Značka nalog | Vhod/izhod obstajata in upoštevata lokalno politiko. |
| `AddBlockMorph` | 420-ms geometrijski morf hkrati z Material listom; zdaj se odzove na reduce-motion. Med gibanjem še vedno meri/komponira majhen pane in poganja steklo; strošek ni izmerjen. |
| Material spodnji listi/dialogi | Vhod/izhod knjižnice nista sama po sebi napaka. Problem ohranitve payload-a in hitrega ponovnega odprtja ostaja. |
| CollapsibleSection, napredne možnosti vnosa | Uporabljajo skupne specifikacije; animacija velikosti upravičeno zahteva layout. |
| Naloge, dnevne kartice, mejniki | Usklajena lokalna motion politika; običajno trajanje/oblike ohranjeni. |
| Lazy vrstice | Stabilni ključi in skupna animacija večinoma že pravilni; to ne pomeni animiranega vsakega notranjega stolpca. |
| Napredek ciljev | Vrednost preložena iz kompozicije v merjenje polnila. |
| NOW/health pulz | Že uporablja `State` in layer alpha; globalno porabo v mirovanju je še treba izmeriti. |
| Stekleni press/glow | Popravljeni faza branja, stabilnost objekta, obris in fade-out. |
| Switch / slider | Popravljeni aktualni callbacki, drag zaostanek drsnika in omejitev optičnih intenzitet. |
| Dolgi pritisk in prestavitev bloka | Prejšnje vračanje se prekliče pred novo gesto; ostaja testiranje hitrih kombinacij z dejanskim shranjevanjem. |
| Swipe datumov | Preklic ne potrjuje več navigacije; varno čiščenje stanja. |
| Časovno kolesce | Pravilna geometrijska potrditev med flingom; haptika ne podvaja začetka/konca. |
| Barvno kolo | Neposredna gesta, ne umetna animacija; gradienta predpomnjena. |
| Material ripple, checkbox, progress indikatorji | Obstoječe knjižnične animacije; ne prepisovati brez dokaza napake. |

## 6. Kaj je in kaj ni bilo preverjeno

### Izvedeno v tem okolju — uspešno

- `python3 tools/derive_palette.py --check`
- `python3 tools/check_sqlite_integrity.py` — **19 testov**.
- `python3 tools/check_translations.py` — **783 virov** v obeh prevodih, enaki placeholderji/plurali.
- `python3 tools/check_presentation.py` — **776 slovenskih nizov**, obstoječe predstavitvene/arhitekturne invariance in koledarski ključi.
- `python3 tools/check_contrast.py` — **109 preverjanj**.
- `git diff --check`.

Ti testi **ne dokazujejo**, da je Kotlin preveden, da je izris slikovno enak ali da ni janka.

### Dodano, vendar tu NEIZVEDENO

- `MotionRegressionTest`: **6 instrumentalnih testov** — brez frame-rekompozicij klicatelja steklenega pritiska; fade-out; preklican swipe; aktualen swipe callback; slider callback/semantika; aktualen switch state; zmanjšano gibanje (nekateri testi preverjajo več lastnosti).
- `MotionSpecTest`: **2 JVM testa** za snap in ohranitev normalnih specifikacij.
- `WheelMathTest`: **2 dodatna JVM primera** za padded viewport in potrditev med flingom.

Poskus `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` se je ustavil **pred gradnjo**, ker ni Jave/JDK. Tudi Android SDK ni na voljo. Poskusa dostopa do distribucij Gradle in Android command-line tools sta se končala z napako TLS povezave. Ni bilo lokalnega emulatorja ali telefona. Zato **ni potrjene gradnje, linta, Android testov, screenshot regresije ali meritev pred/po** za ta patch.

Obstoječi CI za splošne device teste uporablja `disable-animations: true`. To je uporabno za funkcionalne teste, ni pa dokaz pravilnosti vmesnih okvirjev. Novi izolirani testi zato eksplicitno podajo `MotionDurationScale = 1f` in kjer je potrebno ročno vodijo Compose testno uro. Tudi njihov uspeh ne bi nadomestil GPU-meritve.

## 7. Merilni načrt brez spremembe izgleda

### Najprej potrdi prevajanje in vedenje

```bash
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.mydailyroutine.MotionRegressionTest
# Nato celotna instrumentalna zbirka:
./gradlew :app:connectedDebugAndroidTest
```

Za analizo compiler stabilnosti:

```bash
./gradlew :app:assembleRelease -PcomposeReports=true
```

Poročila so v `app/build/compose-reports` in `app/build/compose-metrics`. Preveriti predvsem restartability/skippability glavnega zaslona in komponent z navadnimi kolekcijami. Ne širiti `compose-stability.conf` na mutable tipe samo zaradi lepšega poročila.

### Nato meritve na istem telefonu, isti vsebini in isti gradnji

1. Primerjaj osnovni commit in patch v **release** načinu. Debug uporabljaj za recomposition counts in diagnostiko, ne kot končno oceno FPS.
2. Isti nabor podatkov, jezik, font scale, osveževalna frekvenca, napajanje in stanje baterijskega varčevanja. Naprava ne sme biti termično omejena. Hladni in ogreti scenarij meriti ločeno.
3. Vsak scenarij vsaj petkrat, z animacijami vključenimi:
   - 15–20 s hiter scroll polnega dneva;
   - odpiranje/zapiranje ter scroll nastavitev;
   - ponavljajoči preklopi dan/teden/mesec/leto in hitro spreminjanje datumov;
   - razširitev/zaprtje kartice, naloge, naprednih možnosti;
   - dolg pritisk → drag → preklic → nov drag;
   - oba roba in hitra sprememba smeri sliderja/switcha;
   - časovno kolesce in potrditev med flingom;
   - Goals napredek/Gantt/predictive back s potrditvijo in preklicem;
   - 30 s mirovanje s pulzom, brez pulza in z odprtim listom.
4. Ponovi funkcionalni del z zmanjšanim gibanjem, visokim kontrastom, povečano pisavo in najmanj API 30/31/33+ za različne glass fallbacke.

Osnovni zajem, med reset in zajemom ročno izvedi en kratek scenarij:

```bash
mkdir -p app/build/performance
adb shell dumpsys gfxinfo com.example.mydailyroutine reset
# Izvedi scenarij na telefonu.
adb shell dumpsys gfxinfo com.example.mydailyroutine framestats \
  > app/build/performance/day-scroll.txt
```

`gfxinfo` je dopolnilo, ne popolna GPU diagnoza; buffer je omejen in ponovitve naj bodo kratke. V Android Studio System Trace/Perfetto zajemi FrameTimeline, glavno nit, RenderThread, razporejanje niti in razpoložljive GPU podatke. Za podrobno profiliranje minificirane gradnje po potrebi dodaj namenski **profileable benchmark build**, ne spreminjaj produkcijskega izgleda ali izklapljaj R8.

Pri 60 Hz je okvirni proračun 16,67 ms, pri 120 Hz 8,33 ms; dejanski deadline določa FrameTimeline. Poročaj delež zamujenih okvirjev, p50/p95/p99 trajanj/prekoračitev in ločeno odzivni čas priprave strani. Ne poročaj samo povprečnega FPS.

Za ohranitev videza primerjaj iste končne zaslone in izbrane vmesne okvirje na isti napravi/API. Barve, tipografija, glass parametri in razporeditev niso namenoma preoblikovani; popravljeno vedenje pritiska, preklica in zmanjšanega gibanja je pričakovana razlika. **Pixel-identičnost še ni preverjena.**

## 8. Priporočen vrstni red nadaljevanja

1. **Gradnja + novi regresijski testi + release preizkus na telefonu.**
2. **Payload in življenjski cikel listov, header inset ter loading/prehod.** To so jasno sledljive poti do preskokov, ne estetske spremembe.
3. **Predictive back**, vključno s preklicem in hitrim ponovnim poskusom.
4. **Perfetto meritev** in šele na njeni podlagi optimizacija agregatov, virtualizacije, backdropa in pulza.
5. Po potrebi benchmark modul/Baseline Profile za trajno varovalo zmogljivosti. Baseline Profile izboljšuje zagonsko/ogrevalno pot; ne odpravi napačnega branja stanja ali predragega GPU okvirja.

**Zaključek:** ni potrebe po prepisu aplikacije ali spremembi njenega oblikovanja. Potrebna je doslednejša implementacija interakcij in merilno vodeno odpravljanje preostalega dela. Ta patch popravi več dokazljivih problemov v kodi, ne predstavlja pa zagotovila, da je vse zatikanje že odpravljeno.
