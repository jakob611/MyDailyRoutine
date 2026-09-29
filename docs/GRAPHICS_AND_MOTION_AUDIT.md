# Pregled izrisa in animacij

Datum: 2026-09-29. Osnova: `7c405ffc`, skupaj s popravki v tej delovni veji.

## Sklep in omejitve

Aplikacija ima dobro osnovo za Compose, vendar iz kode ni mogoče zaključiti, da je že dobro grafično optimizirana ali da so vse animacije pravilno implementirane. Najdene so konkretne nepotrebne recomposition, ponavljajoče se alokacije pri risanju in napake pri prekinitvah interakcij. Del teh je popravljen v tej veji. Dejanskega izboljšanja FPS ali zmanjšanja janka **nisem izmeril**.

Pregled je statičen: struktura modulov, povezava stanja in podatkov z UI, vstopna aktivnost, skupni design system, mesta uporabe animacij/potez ter dnevni, tedenski, mesečni, letni in Goals prikazi. To ni certifikat pravilnosti vsake poslovne funkcije, vsebine arhivov ZIP ali vizualna primerjava na napravi. Ni bilo emulatorja, posnetkov Perfetto ali primerjave pred/po. Lokalni Android build se ustavi že zaradi manjkajoče Jave; tudi dodani Kotlin testi še niso izvedeni.

Običajne barve, oblike, blur/lens parametri, razmiki in animacijske krivulje so ohranjeni. Spremembe vidnega obnašanja so omejene na popravljanje preklicev, hitrih ponovnih interakcij, omejevanje drsnika na dovoljeno območje in upoštevanje zmanjšanega gibanja. Pixel-identičnost ni bila potrjena s screenshot testi.

## Kaj je arhitekturno dobro

- `core` je ločen Kotlin/JVM modul; Android persistence in Compose sta v `app`. `AppGraph` je ekspliciten composition root.
- `RoutineViewModel` uporablja UDF, `StateFlow`, persistent kolekcije in `WhileSubscribed`; `RoutineApp` zbira stanje z `collectAsStateWithLifecycle`.
- Resolving vidnega obdobja in health izračuni v `resolveContent` tečejo na `Dispatchers.Default`. Večina pregledanih repozitorijev IO/transakcije izrecno prestavi z glavne niti.
- Dnevni seznam ima stabilne ključe in ločena `contentType` za bloke/mejnike. Skupni `routineItemAnimation` nastavi vse tri animacije elementa, vključno s fade.
- Skupni pulz je `State<Float>`, ne Float, posredovan po `CompositionLocal`; `pulsing` ga bere znotraj `graphicsLayer`. To ni korenska recomposition ob vsakem frame-u.
- Vsebina in stekleni chrome sta pri glavnem backdropu ločena. Ambientalno ozadje že uporablja `drawWithCache`.
- Release že uporablja R8 in resource shrinking, compiler reports pa se dajo vključiti s `-PcomposeReports=true`.

To so dobre odločitve, ne dokaz tekočnosti na konkretnem GPU. Prav tako komentarji v kodi o hitrosti niso meritve.

## Popravljeno

| Mesto | Ugotovitev | Sprememba |
|---|---|---|
| `RoutineGlass.kt / rememberGlassTouch` | `fraction` in `glow` sta bila pretvorjena v Float že med composition. Vsak frame je zato invalidiral composition lastnika kontrole. | `GlassTouch` hrani State reference in ima snapshot-backed getterje. Ista zapomnjena instanca se bere v layer/draw callbackih. `@Stable` namesto napačne pogodbe o nespremenljivih trenutnih vrednostih. |
| `RoutineGlass.kt / liquidUnderGlow` | Vsak izris je ustvarjal radialni Brush in seznam barv; tudi pri ničelni alfi se je izvajalo risanje. | Brush se pripravi v `drawWithCache`; pri ničelni alfi modifier ne doda risanja. Geometrija in barve ostanejo enake. |
| `RoutineText`, `EntryEditorSheet`, `TasksSheet` | `.rotate(animatedValue)` prebere animacijsko stanje med composition. | `.graphicsLayer { rotationZ = animatedValue }` prestavi branje v layer. |
| `TimelineComponents / TimelineBlockCard` | Vsaka sprostitev je zagnala novo vračanje `dragY`; novo vlečenje starega vračanja ni ustavilo. Dva pisca sta lahko premikala isto kartico. | Shranjen Job se prekliče pred naslednjim vračanjem in ob novem prijemu. Ključi `pointerInput` vključujejo tudi spremembe dovoljenosti vlečenja. |
| Kartice blokov/mejnikov in razširjanje nalog | Nekatere lokalne tween/spring animacije so obšle `LocalReduceMotion`. | V zmanjšanem gibanju snap; sicer prvotne dolžine in vzmeti, vključno s privzeto medium-low vzmetjo velikosti mejnika. |
| `RoutineMotion / rememberReduceMotion` | Sistemske nastavitve so bile prebrane samo enkrat na življenjsko dobo composition. | `ContentObserver` za vse tri skale, osvežitev po registraciji in odjava ob dispose. |
| `RoutineApp` | Onboarding se je vrnil pred ponudnikom reduced motion; predictive-back transform nastavitve ni bral. | Nastavitev je dostopna tudi onboardingu; Goals transform je ob reduced motion nevtralen. |
| `TimeWheelPicker` | Tap na vrstico je vedno poklical animirani scroll. | Ob reduced motion uporabi `scrollToItem`; interaktivnega flinga namenoma ne odstranjujemo. |
| `SwipeToShift` | Rezultat `horizontalDrag` ni bil preverjen: tudi preklic po preseženem pragu je lahko zamenjal obdobje. Ob preklicu pointer coroutine je lahko ostal premik. | Navigacija samo po sprostitvi; reset v `finally`; aktualen callback prek `rememberUpdatedState`; threshold je del remember ključa. |
| `RoutineSheet` | Ponovno odprtje med `hide()` prekliče skrivanje, vendar vsebina ostane mounted in Materialov začetni show effect se ne ponovi. | Pri takem ponovnem odprtju izrecen `show()`. Prvo odprtje ostaja v lasti `ModalBottomSheet`. |
| `LiquidControls` | Dolgoživi pointer handler je lahko uporabljal prvotni `checked` in prvotne callbacke. | `rememberUpdatedState` za trenutno stanje stikala in callbacke stikala/drsnika. |
| Drsnik in `GoalBar` | Animirana vrednost ni bila omejena na veljavni delež po interpolaciji. To je pomembno zlasti pri poddušeni vzmeti drsnika. | Delež pri uporabi za širino je omejen na 0..1; tudi premik drsnika uporablja omejen delež. |

## Pregled vrst animacij in preostala tveganja

### 1. Prehodi strani, glava in fast-add

`RoutineApp` uporablja `AnimatedContent` za Goals/časovno obdobje in naslov, `AnimatedVisibility` za chrome, animirani zamik ter lastno morph plast za dodajanje. Osnovne prehodne specifikacije že upoštevajo reduced motion.

**Za profiliranje:** med prehodom sta lahko aktivni dve drevesi vsebine, poleg njiju pa blur/refraction. `FastAddMorph` bere progress med composition ter vsak frame spreminja velikost in obliko steklene plasti. Hkrati vstopa Materialov bottom sheet. To je konkreten kandidat za dražji frame, ne izmerjen glavni vzrok janka. Ne zamenjati na slepo s scale transformom: to bi spremenilo radije in način vzorčenja stekla.

**Še odprto:** preklic predictive back trenutno v `finally` neposredno nastavi `backProgress = 0f`. Torej je lahko povratek nenaden, ne animiran. Izbrati in testirati povratno vzmet ločeno; to bi spremenilo časovni potek in ni vključeno v ta konservativni popravek.

### 2. Razširjanje kartic, seznamov in sekcij

Skupne chevrone in izpuščene reduced-motion poti smo popravili. Lazy item animacije imajo stabilne ključe. Kartice uporabljajo kombinacijo `animateContentSize` in `AnimatedVisibility`: to ni samo po sebi napačno, vendar size animacija legitimno povzroča meritve/layout skozi celotno trajanje. Ne odstranjevati je samo zato, da bi zmanjšali število layoutov.

**Preveriti na napravi:** hitro odpiranje/zapiranje iste kartice, hkratna sprememba vrstnega reda, dolgi naslovi in 200-% pisava; stabilnost aktivne kartice ob prihodu novega podatkovnega snapshot-a.

### 3. Pritiski, sijaj in pulziranje

Glavna napaka pri branju press animacije med composition je odpravljena. Pulz je že pravilno posredovan kot State.

**Še odprto:** ob `PressInteraction.Release` se `point` takoj izniči. Glow sicer animira proti nič, a risanje zaradi manjkajoče točke takoj preneha. Zato komentar o fade-out ni povsem skladen z izvedbo. Nismo ohranili zadnje točke, ker bi s tem namenoma uvedli trenutno neviden fade-out.

Skupni infinite pulse se ustvari tudi takrat, ko ni vidnega porabnika. Možno nadaljnje izboljšanje je aktivacija samo ob vidnih indikatorjih; to zahteva spremljanje vidnosti in ohranitev faze, ne samo dodatnega `if` pri posamezni kartici.

### 4. Stikalo, drsnik, kolo časa in vlečenje

Popravljeni so zastareli callbacki, preklici in meje deleža. Kolo ima snap fling in stabilne ključe.

**Še odprto:** stikalo v vsakem drag dogodku sproži coroutine za `snapTo`, njegov effect na `checked` pa lahko sočasno retargetira isti Animatable. Drsnik tudi med neposrednim vlečenjem retargetira spring, kar lahko deluje kot zaostajanje za prstom, četudi ni izpuščenih frame-ov. Za spremembo na neposredno sledenje med dragom je potreben ločen interaction test in zavestna odločitev o občutku kontrole.

`RoutineSwitch` še vedno bere položaj za barvo/border med composition, `LiquidSlider` za širino, `GoalBar` pa animira layout širine. To so preostali kandidati za odloženo branje v draw/layout. Risanje ročnega borderja ni avtomatsko pixel-enakovredno sedanjemu Compose borderju, zato ga ta popravek ne zamenja.

### 5. Spodnji paneli in dialogi

`RoutineSheet` zadrži vsebino med hide animacijo in zdaj podpira ponovno odprtje. `sheetFlingStabilizer` namenoma porabi preostalo hitrost flinga, ne ročnega vlečenja.

**Še odprto:** v `AppOverlays` se ob zaprtju urejevalnika `editingBlock` lahko spremeni v null, čeprav je sheet še mounted za izhod. Preveriti, ali se vsebina zato spremeni že med zapiranjem. Za robustno splošno rešitev je smiselno ločiti vidnost panela od zadnjega prikazanega editor modela. Enako preveriti pri editorjih Goals in milestone.

Materialove sheet/dialog animacije upravlja knjižnica in sistemska duration scale; aplikacijski `LocalReduceMotion` sam po sebi ne konfigurira vseh Materialovih notranjih animacij. Preveriti sistemski animator scale 0 ter tudi izolirano window/transition scale 0. Ne trdimo, da je celoten reduced-motion scenarij s tem že certificiran.

### 6. Gostota vsebine in GPU

Tedenski grid sestavi celoten tedenski prikaz znotraj lazy elementa; elementi tedna zato niso individualno lazy. Mesečna/letna mreža je omejena po številu dni/mesecev, zato sama uporaba `forEach` ni problem. Gantt in projektni seznami zahtevajo meritve z večjim številom aktivnosti.

V overview in Goals composable-ih so filtri, grupiranja in sortiranja. Za večje podatke jih je smiselno vezati na vhod s `remember` ali pripraviti v UI modelu. Pred tem preveriti compiler skipping: slepo dodajanje `remember` za vsako majhno zbirko ni profesionalna optimizacija.

Steklo uporablja vibrancy, blur, lens, sence in clipping. Tudi brez recomposition to stane GPU delo. `ContinuousCornerShape` ustvarja generičen Path s trigonometrijo; cena je posebej relevantna pri spreminjanju velikosti. Parametrov stekla ali oblike nisem znižal, saj bi spremenil izgled.

### 7. Arhitektura, povezana z odzivnostjo

`RoutineViewModel` in `TimelineUiState` združujeta veliko funkcionalnosti. Končni combine ponovno filtrira in sortira goal markers tudi pri drugih spremembah stanja, pred `stateIn` pa nima ločenega dispatcherja. To ni dokaz velike blokade, je pa kandidat za ločen izpeljan tok, odvisen samo od goals in časovnega območja.

Dolgoročno priporočilo je ločevanje feature UI state/izračunov, ne velika reorganizacija map v istem performance popravku. Sprememb persistence, sheme ali poslovnih pravil ta patch ne vključuje.

## Izvedena preverjanja

- `python tools/check_presentation.py` — uspešno.
- `python tools/check_translations.py` — uspešno.
- `python tools/check_sqlite_integrity.py` — vseh 19 preverjanj uspešnih.
- `git diff --check` — uspešno.
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` — **ni se izvedlo**: `JAVA_HOME is not set` in `java` ni na PATH.
- Dodan `GlassTouchTest`: ista instanca prebere nove snapshot vrednosti in ohrani overshoot press vzmeti. **Še neizveden.** Ne meri recomposition.
- Dodan `SwipeCancellationTest`: preklican oborožen swipe ne navigira, naslednji sproščen swipe pa navigira. **Še neizveden**, potrebuje Android emulator/napravo.

Obstoječi uspešni CI rezultati v README niso dokaz za ta patch.

## Predlagan protokol v Android Studiu

1. Najprej JDK 17, Android SDK 36 in običajni testi:
   ```sh
   ./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
   ./gradlew :app:connectedDebugAndroidTest
   ./gradlew :app:assembleRelease -PcomposeReports=true
   ```
2. Primerjati osnovni commit in to vejo na istem telefonu, z isto bazo, refresh rate, temperaturo in build varianto. Za hitrost uporabiti optimiziran release; debug uporabiti za Layout Inspector/recomposition diagnostiko. Profiler zahteva ustrezno profileable konfiguracijo ali zajem s sistemskim tracingom.
3. Na 60 Hz je okvir približno 16,7 ms, na 120 Hz 8,3 ms. Zabeležiti frame-duration p50/p90/p95/p99 in delež frame-ov čez dejanski rok. Ločiti UI-thread delo, RenderThread in GPU čakanje v Perfetto; povprečen FPS ni dovolj.
4. Scenariji: 20 ponovitev prehodov dan/teden/mesec/leto/Goals; 30 s hitrega flinga; razširjanje kartic; pritisk steklenih gumbov; vlečenje kartice in takojšnji ponovni prijem; hitri preklopi stikal; drsnik 0→1→0; kolo časa; odprtje/zaprtje/ponovno odprtje vseh panelov; preklic swipe in predictive back.
5. Ponoviti s prazno bazo, demo bazo ter obsežnim urnikom z veliko nalogami/aktivnostmi. Uporabiti vsaj telefon srednjega razreda in 120-Hz telefon; testirati fallback pod API 31 in steklo na novejšem API.
6. Primerjati posnetke mirovanja pred/po, nato vmesne frame-e press/expand/sheet animacij. Preveriti velike pisave, slovenski/angleški jezik ter reduced motion, spremenjen med delovanjem aplikacije.
7. Za dolgoročno regresijsko zaščito dodati Macrobenchmark (`FrameTimingMetric` in startup) ter baseline profile. Trenutni projekt nima namenskega benchmark modula; samo compiler report ni performance benchmark.

**Prioriteta naslednjega koraka:** najprej build in testi tega patcha, nato Perfetto zajem steklenih prehodov in drsnika. Šele na podlagi tega spreminjati dražje GPU poti ali razdeljevati velike UI izračune.
