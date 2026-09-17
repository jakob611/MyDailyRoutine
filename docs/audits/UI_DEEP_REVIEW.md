# Globok pregled: videz, gibanje in dostopnost cele aplikacije

Datum: 2026-09-16 · Veja: `arena/01a0a0f9-mydailyroutine` · Obseg: **celotna aplikacija** —
29 presentation/component datotek (8.920 vrstic), `core/designsystem`, `res/` (widget, tema,
barve), `AndroidManifest.xml`, CI gate-i.

Pregled ni narejen »na oko«. Vsaka trditev spodaj je bodisi izmerjena (skripte v `tools/`),
bodisi prebrana v dokumentaciji/virih (razdelka 6 in 7), bodisi preverjena v CI. Kjer je bila stvar
zavrnjena kot nepotrebna, je to zapisano v razdelku 5.

> **Stanje: implementirano.** Popravki so v kodi, ne samo v tem dokumentu. Vsak popravek ima
> v razdelku 3 svojo vrstico: težava → praksa → kje je popravljeno → kako je zavarovano v CI.
>
> **Dva kroga.** Razdelki 0–5 so prvi krog (2026-09-16). Po testu APK-ja na napravi so prišle tri
> pripombe — gumb nazaj ne dela nikjer, besedilo je pogosto slabo berljivo, steklo še ni pravo —
> zato je **razdelek 6 drugi krog (2026-09-17)** in njegove številke so trenutne; kjer si
> nasprotujejo s tabelami zgoraj, velja razdelek 6.

---

## 0. Ocena v petih vrsticah

| Področje | Pred | Zdaj | Zakaj je to profesionalno |
|---|---|---|---|
| Barvna paleta | 6 površin z koraki 1,03–1,09 (plosko), `TextMuted` 3,6:1 (**WCAG padec**) | koraki 1,07–1,12, `TextMuted` 4,70:1, 137 preverjenih parov | M3 temna rampa stopnjuje med 1,05 in 1,17; 4,5:1 je prag za besedilo (WCAG 1.4.3) |
| Steklo | tint 0,78 → besedilo čez najbolj svetel accent 4,2:1 | tint 0,82/0,86 → najslabši primer 6,09:1 | steklo se preverja proti *najslabšemu* ozadju, ne proti posnetku zaslona |
| Vrhnja vrstica | 3 vrstice + marketinški podnaslov na **vsakem** zaslonu, vedno enake | zložljiva: ob scrollu ostane naslov + zavihki, datumska vrstica se pospravi | koledarske aplikacije (Notion Calendar, Structured) datumsko vrstico zložijo v naslov |
| Gibanje | vse `tween(180)`; premik naprej/nazaj enak; brez upoštevanja sistemske nastavitve | prostorske vzmeti + efektni tween, smer prehoda sledi smeri potovanja, `snap()` ob »Odstrani animacije« | M3 loči prostorski in efektni gibalni sistem; WCAG 2.3.3 in sistemska nastavitev zahtevata izklop |
| Paleta v XML | widget `#F8FAFC`/`#94A3B8`/`#121316` (svoje barve), `colors.xml` = template (purple/teal) | `res/values/colors.xml` je ogledalo palete, widget in ozadje okna bereta iz njega, CI preverja enakost | en vir resnice; ogledalo, ki ga nihče ne preverja, je paleta, ki razide |

---

## 1. Kaj je bilo pregledano in kako

| Korak | Metoda | Rezultat |
|---|---|---|
| Struktura | štetje vrstic, klicev `Text`/`RoutineText`/`RoutineLabel`, stekla, `testTag`, drsnikov po datoteki | največji zaslon `GoalsScreen.kt` 1.411 vrstic; **1** surov klic `Text(` v vseh presentation datotekah (ostalo gre skozi besedilno pogodbo) |
| Kontrast | `tools/check_contrast.py`: parsira `DesignSystem.kt`, izračuna luminanco po WCAG 2.1 | 137 parov, 24 barv, 5 kategorijskih skled, vsi OK |
| Steklo | worst-case kompozicija: tint nad vsakim accentom, najsvetlejšo površino in `TextPrimary` | najslabši primer 6,09:1 (prej 4,2:1) |
| Besedilo | `tools/check_presentation.py` (že obstoječ gate) | 701 slovenskih nizov, tabularne številke v widgetu, 43 UI datotek z merjenimi postavitvami, 5 zaslonov pod stekleno vrstico |
| XML | parse vseh `res/**.xml`, iskanje prostih hex barv | 0 prostih hex (ikone so izjema — to so enobarvne maske, ki jih sistem obarva sam) |
| Gibanje | pregled vsakega `tween`/`spring`/`animate*AsState`/`AnimatedVisibility`/`rememberInfiniteTransition` | 8 mest, vsa povezana na dva žetona (razdelek 3.4) |
| Raziskava | splet: Material 3, WCAG, dokumentacija `backdrop` (kyant0), koledarske aplikacije | razdelek 2 in 6 |

---

## 2. Kaj počnejo najboljše aplikacije (in kaj smo vzeli iz tega)

### 2.1 Urniki in načrtovalniki
Notion Calendar, Sunsama, Structured, Things 3 in Morgen se vsi držijo istih treh pravil:

1. **Datum je naslov, ne orodna vrstica.** Ko uporabnik drsi navzdol po dnevu, se datumsko
   krmiljenje pospravi, naslov pa prevzame datum. Nasprotno (ves čas vidna orodna vrstica s
   štirimi vrsticami) pomeni, da 25 % zaslona na telefonu nikoli ne pripada vsebini.
2. **Prehod med obdobji ima smer.** Teden → naslednji teden drsi levo; dan → mesec se *ne* drsi,
   ker si pogleda ne delita geometrije (fade-through). Enak drsni prehod za oboje je tisto, kar
   uporabniki opisujejo kot »čuden skok«.
3. **En pogled = ena naloga.** Structured (dan), Sunsama (načrt dneva), Things 3 (seznam) ne
   mešajo štirih časovnih meril na enem zaslonu; mi jih ločimo z zavihki (dan/teden/mesec/leto),
   kar je že bilo narejeno — pregled je potrdil, da je to prava izbira in ne kompromis.

Za dijaka z IB CAS/EE cilji je pomembno še to, da cilji **niso** v istem drsniku kot dan:
dolgoročni cilji (EE, CAS ure) živijo v svojem zaslonu z zavihki (pregled/aktivnosti/kilometrine/
napredek), dnevni urnik pa ostane dnevni. To je že tako; pregled ni našel razloga za spremembo.

### 2.2 Material 3 in gibanje
M3 deli gibanje na **prostorsko** (položaj, velikost, oblika — lahko je vzmet, lahko tudi rahlo
prestreli) in **efektno** (barva, prosojnost — nikoli ne sme prestreliti, ker prosojnost nad 1 ni
učinek, ampak utrip). Priporočena trajanja za na dotik vezane prehode so 200–300 ms, mikro
interakcije 40–150 ms. M3 Expressive dodaja `MotionScheme.expressive()` (nižje dušenje, več
odboja) za aplikacije, kjer je gibanje del užitka; za utilitarno orodje (načrtovalnik) je prava
`standard` shema z višjim dušenjem. Zato:

* prostorsko = `spring(DampingRatioNoBouncy, StiffnessMedium)` — vzmet, ker jo je mogoče
  sredi geste retargetirati (prekinitev prehoda ne skoči),
* efektno = `tween(180)` — znotraj M3 okna in brez preseganja,
* edini odboj v aplikaciji ostaja značka za zamujene naloge (`PopSpring`), kar *je* herojski trenutek.

### 2.3 Tekoče steklo (kyant0 `backdrop`)
`io.github.kyant0:backdrop` je uporabljen po njegovi dokumentaciji: `rememberLayerBackdrop` za
plast vsebine, `Modifier.drawBackdrop` za krom, efekti `blur` + `lens` + `vibrancy`. Dve pravili,
ki ju dokumentacija poudarja in ki sta bili upoštevani:

* steklo na steklu uporabi `exportedBackdrop` (uporaba `layerBackdrop` za `drawBackdrop` na istem
  vozlišču pomeni rekurzijo → SIGSEGV);
* plast in krom morata biti **sorojenca**, nikoli gnezdena — zato je `RoutineApp` poln sklad
  (`Box`) in ne `Scaffold`, katerega telo se začne pod orodno vrstico in tako nima česa lomiti.

Dodatno: temno steklo potrebuje **višjo** neprosojnost kot svetlo (20–30 % je za svetlo, za temno
je tint praktično neprosojen), ker kontrast na temnem pade hitreje. Naš tint je 0,82 (vrstica,
chipi) in 0,86 (listi) — to ni okus, temveč izmerjen minimum, pri katerem `TextSecondary` ostane
nad 4,5:1 čez *vsak* accent v paleti. Pravilo, ki iz tega sledi in je zapisano v kodi: na steklu
samo `TextPrimary` in `TextSecondary`, nikoli `TextMuted` (najslabši primer 3,91:1).

### 2.4 Navigacija
Praksa: spodnja vrstica 3–5 primarnih razdelkov, segmentni stik 2–4 izključujoče možnosti, FAB
**eno** dejanje, in ne kombinirati spodnje navigacije z zavihki. Aplikacija nima spodnje vrstice
(vsebina je en dan + štirje pogledi + trije listi), ima segmentni stik (4 pogledi) in en FAB
(»Dodaj blok«) — skladno s prakso. Zavihki znotraj listov (nastavitve/načrt/cilji) so pod-nivo,
ne konkurirajo navigaciji.

### 2.5 Dostopnost
* **WCAG 1.4.3** (4,5:1 za besedilo) in **1.4.11** (3:1 za ikone in pomembne meje) — preverjeno
  za vseh 137 parov, vključno s kompozicijo stekla.
* **Odstrani animacije** (Android: Nastavitve → Dostopnost → Odstrani animacije) nastavi
  `ANIMATOR_DURATION_SCALE`/`TRANSITION_ANIMATION_SCALE`/`WINDOW_ANIMATION_SCALE` na 0. Aplikacija
  to bere in vse prehode preklopi na `snap()`; neskončni utrip (NOW-indikator) se ustavi na polni
  vrednosti. To ni okus, ampak vestibularna motnja (WCAG 2.3.3).
* **Predvidljiv nazaj** (predictive back): `android:enableOnBackInvokedCallback="true"` — brez tega
  sistem na Androidu 14+ ne more pokazati predogleda prejšnjega zaslona med gesto.

---

## 3. Ugotovitve in popravki

### 3.1 Paleta in kontrast (`core/designsystem/theme/DesignSystem.kt`)

**Težava.** Rampa površin je bila ploska (koraki 1,03–1,09), zato se kartica na listu ni ločila;
`TextMuted #6C7789` je na `Surface4` dosegel 3,6:1 — pod WCAG pragom; bordi 0,06/0,12/0,05 so bili
pod 1,15:1, torej nevidni; tint stekla 0,78 je pri najbolj svetlem accentu pod sabo spustil
besedilo na 4,2:1.

**Popravek.**

| Žeton | Prej | Zdaj | Izmerjeno |
|---|---|---|---|
| `Surface1..4` | `#0D1015`, `#12161C`, `#171C24`, `#1E242D` | `#0E1218`, `#151A22`, `#1D222B`, `#252B36` | koraki 1,07 / 1,08 / 1,09 / 1,12 |
| `SheetSurface` | `#0D1015` | `#0B0E13` | list je temnejši od vsebine, ne svetlejši |
| `TextMuted` | `#6C7789` | `#8A95A8` | min 4,70:1 (prej 3,6:1 — padec) |
| `GlassTintAlpha` | 0,78 | 0,82 | najslabši primer 6,09:1 |
| `GlassTintStrongAlpha` | 0,82 | 0,86 | najslabši primer 6,51:1 |
| `Border`/`BorderStrong`/`CardBorder` | 0,06/0,12/0,05 | 0,10/0,18/0,08 | 1,30 / 1,72 / 1,22:1 |
| `GlassFallback`/`Strong` | — | `#F20D1015` / `#F70B0E13` | enaka barva kot tint, samo neprosojna (za naprave brez `blur`) |

Reference: M3 temna rampa stopnjuje 1,05–1,17 (izračunano iz `#141218 → #1D1B20 → #211F26 →
#2B2930 → #36343B`); naša je znotraj tega obsega.

**Zavarovanje.** `tools/check_contrast.py` (nov) je del CI gate-a (`Static gates` v
`.github/workflows/android.yml`): parsira paleto neposredno iz `DesignSystem.kt`, preveri 137 parov
(besedilo × površine, accenti × površine, kategorijske sklede, FAB, opozorila, worst-case stekla,
fallback trdne barve), obseg korakov rampe (1,03–1,25) in vidnost bordov (≥1,15:1). Ob vsaki
spremembi katerekoli barve se to izračuna znova; nazaj se ne da zdrsniti po nesreči.

### 3.2 Steklo po celotni aplikaciji

Steklo je na **kromu**, ne na vsebini: vrhnja vrstica, FAB, chipi (`GlassRole.Bar/Control/Chip`) in
listi (`GlassRole.Sheet`). Vsebina (kartice, vrstice urnika) je neprosojna — ker bi drugače
bralec izgubil kontrast tam, kjer je besedila največ, in ker praksa svetuje največ dve plasti
stekla hkrati. Listi (bottom sheet) so neprosojni `SheetSurface` prav zato, ker ležijo *nad*
stekleno vrstico: steklo na steklu je tisto, kar dokumentacija `backdrop` odsvetuje.

### 3.3 Vrhnja vrstica: zložljiva, brez marketinškega podnaslova (`app/presentation/RoutineApp.kt`)

**Težava.** Vrhnja vrstica je imela tri vrstice (naslov + podnaslov, datum, zavihki) in podnaslov
»TVOJ ČAS. TVOJ RITEM.« na **vsakem** zaslonu za vedno. To je stalni strošek višine za stavek, ki
ga uporabnik prebere enkrat; poleg tega je bil podnaslov ravno tisti niz, ki se je pri večji pisavi
najprej rezal na `…` (zabeleženo v `UI_TEXT_AND_SCROLL_AUDIT.md`, točka 3).

**Popravek.**

* Podnaslov je izginil iz vrstice in je zdaj **uvodna vrstica praznega dne** (`DailyTimeline`):
  tam je prostor in tam deluje kot povabilo, ne kot reklama.
* Vrsta z datumom se zloži, ko bralec drsi navzdol (`NestedScrollConnection.onPreScroll`:
  y < −1 → zloži, y > 1 → razkrij), in se razkrije ob prvem drsenju navzgor. Zavihki ostanejo vedno.
* Ko je vrstica zložena, naslov prevzame datum (`AnimatedContent` z efektним prehom), ko je
  razprta, piše ime aplikacije. Naslov je vedno berljiv, krmiljenje pa zavzame prostor samo takrat,
  ko se ga uporablja.
* Vsebina se umika **razprti** višini (`onSizeChanged` zapiše inset samo, ko vrstica ni zložena),
  zato ob zložitvi sredi drsenja nič ne skoči: elementi samo potujejo navzgor skozi prostor, ki ga
  zložena vrstica ne pokriva več — pod steklom, kar je videti namerno.
* Ob vrnitvi iz ciljev ali ob spremembi merila (dan → teden) se vrstica vedno razkrije: uporabnik
  je ravnokar nekaj izbral in krmiljenje, s katerim je izbral, mora ostati na zaslonu.

**Preverjeno z obstoječim testom.** `glassChromeOverlaysTheContentInsteadOfPushingItDown` še vedno
drži (vrstica ostaja sorojenec vsebine in se z njo prekriva); `allFourSlovenianViewsAndFastAddAre
Reachable` najde ime aplikacije ob zagonu, ker je vrstica na začetku razprta.

### 3.4 Gibanje: dva žetona, smer potovanja, izklop (`core/designsystem/motion/RoutineMotion.kt`)

Nov modul z dvema žetonoma in eno dostopnostno stikalo:

* `spatialSpec(reduceMotion)` → `spring(DampingRatioNoBouncy, StiffnessMedium)` ali `snap()`
* `effectSpec(reduceMotion, millis = 180)` → `tween(millis)` ali `snap()`
* `rememberReduceMotion()` bere tri sistemske lestvice; če je katerakoli 0, je gibanje izklopljeno
* `LocalReduceMotion` ga razdeli po drevesu (en branje na okno)

Preklopljenih je vseh 8 mest, ki so imela svoje trajanje:

| Mesto | Prej | Zdaj |
|---|---|---|
| `RoutineApp` prehod dan/teden/mesec/leto | `slideInHorizontally` vedno z leve | **smer sledi potovanju**: isto merilo → drsenje v smeri datuma; sprememba merila → fade-through (pogleda si ne delita geometrije) |
| `RoutineApp` prehod na cilje | `tween(180)` | prostorska vzmet + efektni fade |
| `RoutineApp` značka za zamujene naloge | `scaleIn(PopSpring)` + `fadeIn(tween(120))` | enako, a pod »Odstrani animacije« brez skaliranja in brez fade |
| Naslov vrhnje vrstice | — | `AnimatedContent` z efektnim fade (samo barva/prosojnost → brez vzmeti) |
| `CollapsibleSection` (puščica + razkritje) | `tween(180)` | puščica = prostorska vzmet, razkritje = vzmet + fade |
| `GoalsScreen` zamenjava prazno/vsebina + vrstica napredka | `tween`/`SnappySpring` | prostorska vzmet / `snap()` ob izklopu |
| `TasksSheet` preurejanje seznama + dve puščici | `TaskListSpring` (konstanta) | `taskListSpec()` — vzmet, ki ob izklopu izgine (vrstica samo pade na novo mesto) |
| `TimelineComponents` aktivni okvir + `pulseAlpha()` | `SnappySpring`, neskončni utrip 200 ms | vzmet; **utrip se ob izklopu ustavi** na polni vrednosti in se zanka sploh ne zažene |
| `EntryEditorSheet` razkritje ure in »več možnosti« | privzeti `AnimatedVisibility`, puščica brez animacije | prostorska vzmet + fade, puščica se zavrti z vzmetjo |

Zakaj vzmet in ne trajanje: vzmet se da retargetirati sredi geste. Če uporabnik prekine prehod z
novim dotikom, `tween` skoči, vzmet nadaljuje od trenutne hitrosti.

### 3.5 Dolgi drsniki

Že razbiti na zavihke in zložljive sekcije (nastavitve 3 zavihki, načrt 3, cilji 4, leto 3
zložljive sekcije) — pregled je potrdil, da so to pravi rezi, in jih zavaroval s testi
(`menusAreSplitIntoTabsInsteadOfOneLongScroll`, `goalsAreSplitIntoTabsInsteadOfOneLongScroll`,
`yearOverviewFoldsItsLongSections`). Ostalo je zapisano v razdelku 5.

### 3.6 Widget in XML ogledalo palete

**Težava.** `res/values/colors.xml` je bil še vedno Android Studio template (`purple_200`,
`teal_700`, …) — 7 mrtvih virov. Widget je imel svoje barve: besedilo `#F8FAFC`/`#94A3B8`,
ozadje `#121316`, bord `#14FFFFFF`. Nobena od teh ni bila v paleti, zato je bil widget edini del
izkušnje, ki je bil videti kot druga aplikacija (in `#94A3B8` na `#121316` je 6,6:1 — sicer OK,
a naključno).

**Popravek.** `colors.xml` je zdaj **ogledalo** 20 vrednosti iz `DesignSystem.kt` (površine,
besedilo, bordi, accenti), widget in `android:windowBackground` bereta iz njega, prostih hex barv v
`res/layout` in `res/drawable` ni več (ikone so izjema — enobarvne maske, ki jih sistem obarva sam).
Dinamični del widgeta (Glance) je že prej uporabljal `RoutineColors`, zato se sprememba pozna samo
na statičnih postavkah.

**Zavarovanje.** `check_contrast.py` primerja vsako vrednost v `colors.xml` z vrednostjo v
`DesignSystem.kt` (vključno z izračunom bajta za prosojnost: `round(alpha × 255)`) in pade, če se
razhajata ali če se v `res/` pojavi prosta hex barva. Preverjeno v obe smeri: namerno pokvarjena
vrednost vrne exit 1 z imenom barve in obema številkama.

### 3.7 Predvidljiv nazaj

`android:enableOnBackInvokedCallback="true"` v `AndroidManifest.xml`. Brez tega sistem na Androidu
13+/14+ ne more narisati predogleda prejšnjega zaslona med gesto nazaj; z njim gesta nazaj v listih
(cilji, nastavitve, načrt, urejevalnik) pokaže, kam gre, preden jo uporabnik dokonča.

Zastavica sama ne naredi sklada: test na napravi je pokazal, da nazaj zapre aplikacijo, ker je v
kodi takrat bilo **0** `BackHandler` klicev. Pravi sklad (merilo → cilji → listi) je implementiran v
drugem krogu, §6.3.

---

## 4. Kaj zdaj preverja CI

| Gate | Kaj | Stanje |
|---|---|---|
| `tools/check_sqlite_integrity.py` | shema, FK, migracije, indeksi | 19 testov OK |
| `tools/check_presentation.py` | slovenski nizi, pisava, tabularne številke, en domen model, merjene postavitve, en vir datuma, en backdrop okna, zasloni pod stekleno vrstico | 701 nizov, 43 datotek, 5 zaslonov |
| `tools/check_contrast.py` (**nov**) | 141 parov WCAG z nivojskimi minimumi, worst-case stekla čez `TextPrimary`, halacijski varoval, koraki rampe, bordi, ogledalo palete v XML, proste hex barve (2. krog, §6.4) | exit 0 |
| `:core:test`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug` | prevajanje, enote, lint (`abortOnError = true`) | uspešno (run 35136305318) |
| `:app:connectedDebugAndroidTest` | 9 UI testov na napravi + 10 posnetkov | uspešno (run 35136305318) |

### 4.1 Preverjeno v CI

**Commit `eb141b4`, [run 35136305318](https://github.com/jakob611/MyDailyRoutine/actions/runs/35136305318) — oba joba uspešna.**

| Job | Koraki | Rezultat |
|---|---|---|
| `build` | static gates (shema, predstavitev, kontrast), `:core:test`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`, objava debug APK | success (18:44:48 → 18:49:20) |
| `device-tests` | emulator API 35, `:app:connectedDebugAndroidTest` | success (18:44:49 → 18:52:57), **10 posnetkov** zbranih v `device-test-reports`: `01-day`, `02-week`, `03-month`, `04-year`, `05-glass-day`, `05-quick-add`, `06-settings-data`, `07-planning-tabs`, `09-goals-tabs`, `10-year-folds` |

Posnetki so dokaz, da zložljiva vrhnja vrstica ni podrla obstoječih UI testov:
`glassChromeOverlaysTheContentInsteadOfPushingItDown` (steklo se še vedno prekriva z vsebino) in
`allFourSlovenianViewsAndFastAddAreReachable` (ime aplikacije je ob zagonu vidno, ker je vrstica
razprta) tečeta na vsakem zagonu.

Med implementacijo so se ujemale tri napake, ki jih je CI našel in so popravljene v
`985638a → 69420f9 → e5bc5c1 → eb141b4`: `staticCompositionLocalOf` potrebuje lambda; `onSizeChanged`
poda `IntSize` (ne `Int`); klic `AnimatedVisibility` znotraj `Box{}` v `actions` mora biti s polnim
imenom, ker je `RowScope` označen z `@LayoutScopeMarker` (DslMarker) in zato njegov razširitveni
prek implicitnega sprejemnika ni kandidat. Vse tri so zdaj zapisane tudi v komentarjih v kodi.

---

## 5. Kar je ostalo (iskreno)

1. **`GoalsScreen.kt` je 1.411 vrstic.** Deluje in je prekrit s testi, a je največja datoteka v
   predstavitveni plasti; razdelitev na `GoalOverview`/`GoalActivities`/`GoalMilestones`/
   `GoalProgress` bi olajšala vsako naslednjo spremembo. Ni narejeno, ker je to refaktor brez
   vidne spremembe za uporabnika in bi podaljšal to vejo.
2. **Prehod med zavihki ciljev ni prostorski.** Vsebina zavihkov je v istem `LazyColumn` (predmeti
   z različnimi ključi), zato bi `AnimatedContent` okoli `when (tab)` pomenil gnezden drsnik v
   drsniku. Trenutno se zavihek preklopi s presnovkom ključev (elementi vstopijo/izstopijo z
   `animateItem`), kar je berljivo, ni pa tako mehko kot drsenje v smeri zavihka.
3. **Shared element transitions niso uporabljene.** Prehod blok → urejevalnik bi lahko nosil naslov
   bloka čez (`SharedTransitionLayout`), a to zahteva stabilne ključe na obeh straneh in en
   `AnimatedVisibilityScope` — smiselno šele, ko je 1. in 2. točka razčiščeni. Pravilo iz
   dokumentacije: ne kombinirati drsenja in shared-elementa hkrati (postane nepredvidljivo).
4. **Samo temna tema.** To je bila zavestna odločitev (OLED, dolge seje zvečer), ne pomanjkljivost;
   `OledColorScheme` je podan tudi Glance widgetu, zato svetlega načina ni niti na domačem zaslonu.
   Če bi ga kdaj dodali, `check_contrast.py` potrebuje drugo tabelo parov, ne drugega mehanizma.
5. **Dynamic Color (Material You) ni vklopljen.** Namerno: paleta je del identitete in je
   preverjena za kontrast; barve iz ozadja uporabnika tega zagotovila nimajo.
6. **`TextMuted` na steklu je prepovedan** in to je zapisano samo v komentarju ter izmerjeno v
   gate-u (2. krog: worst-case je izračunan čez `TextPrimary`, zato je pravilo zdaj iz pravega
   najslabšega primera) — strojna prepoved (npr. lint pravilo ali parameter tipa) bi bila močnejša,
   a bi zahtevala spremembo podpisov `RoutineText`/`RoutineLabel`.
7. **Haptika je odvisna od motorja.** `RoutineHaptics` je štiri nivoje globok in najnižji nivo ni
   naš: na napravah, ki ne znajo risati primitivov (`arePrimitivesSupported()` vrne false) ali so pod
   API 29, odločitev pade na `View.performHapticFeedback`, kjer jakost uglašuje OEM. *Vrsta*
   povratne informacije (izbira / pritisk / uspeh / napaka) ostane prava povsod, natančna jakost ne.
8. **Interaktivno steklo je na enem kontrolniku** (plavajoči »Dodaj«). Apple interaktivnost uporablja
   tam, kjer je pritisk res dogodek; deset hkrati animiranih steklenih plasti bi bilo nasprotno od
   tistega, kar steklo počne — miruje v ustaljenih stanjih.
9. **Koti niso »continuous« (superelipsa).** Appleovi zaobljeni koti niso krožni loki, ampak
   zvezna superelipsa (`RoundedRectangle(style: .continuous)`), in to je del tistega, kar iOS 26 dela
   mehkejšega na pogled. Compose ima za to `GenericShape`, a kyant0-jev `lens()` zahteva
   `CornerBasedShape`, torej krožne loke — superelipsa bi izklopila lom in s tem steklo. Koncentričnost
   je delno pokrita: lestvica Card 16 → Chip 8 ob 8 dp odmika **je** koncentrična (16 − 8 = 8), kar je
   Appleovo pravilo (notranji polmer = zunanji − odmik); listi v pladnju z 16 dp odmika pa ostanejo pri
   16 dp, ker bi 8 dp na Androidu delovalo zastarelo.
10. **Optične velikosti pisave v Composeu ni mogoče nastaviti.** Roboto Flex ima os `opsz`, SF Pro ima
    enako zamisel, javni `TextStyle`/`SpanStyle` pa parametra za različice pisave nima — preverjeno v
    izvozu javnega API `ui-text` (`api/current.txt`), v izvorni kodi obeh razredov in z gradnjo, ki je
    javila `No parameter with name 'fontVariationSettings' found`. Widget jo dobi prek `TextView`
    (razdelek 6.6.2), Compose del ostane na privzeti optiki 14. Obhod bi bil lasten `FontFamily` z več
    statičnimi rezinami (ena na optično velikost): nekaj sto KB v APK in ročna izbira rezine po slogu,
    kar za zdaj presega korist.
11. **Adaptivne sence in »Reduce Transparency«/»Increase Contrast« ostanejo nedosegljivi.** Appleove
   sene se prilagajajo vsebini pod steklom, kar zahteva vzorčenje svetlosti ozadja — kyant0-jev backdrop
   tega ne izpostavlja. Sistemskih nastavitev za prosojnost in kontrast na Androidu ni; naš odgovor je,
   da je tint že na Appleovi ravni »zmanjšane prosojnosti« (0,74/0,80) in da kontrastni gate meri
   najslabši primer čez steklo.
12. **Sklad nazaj ima dve plasti, ne poljubno globok seznam.** Merilo in cilji sta edini plasti, ki
   živita znotraj activity-ja; če bi kdaj pribil tretji celozaslonski pogled, bi ga bilo treba
   dodati v vrstni red sestavljanja v `RoutineApp.kt` (globina = vrstni red). Splošnejša rešitev bi
   bil Navigation 3 z `NavDisplay`, kar je prevelik poseg za to vejo.

---

## 6. Drugi krog (2026-09-17): berljivost, steklo, sklad nazaj

Tri pripombe po testu na napravi, vsaka obravnavana z raziskavo in meritvijo, ne z ugibanjem:

| Pripomba | Vzrok, ki ga je raziskava pokazala | Popravek |
|---|---|---|
| »Back gumb ne funkcionira nikjer« | v repozitoriju je bilo **0** klicev `BackHandler`; vsi zasloni so stanje znotraj enega activity-ja, zato je sistemski nazaj vedno padel do zaganjalnika | izpeljan sklad nazaj (§6.3) |
| »Tekst pogosto težko berljiv« | prag 4,5:1 je bil obravnavan kot cilj; telesni nivoji (`TextSecondary` 6,3:1, `TextMuted` 4,7:1) so nosili večino branja, accenti pa so imeli več rezerve | paleta v2 z nivojskimi minimumi + halacijski varoval (§6.1, §6.4) |
| »Liquid glass in izgled še ni najboljši« | blur 3–10 dp (to je motno steklo, ne tekoče), brez dviga nasičenosti/svetlosti, **brez roba** (rim), lom brez disperzije | steklo v2 po parametrih materialov (§6.2) |

### 6.1 Paleta v2: zakaj je bilo besedilo težko berljivo

Literatura o temnih temah se strinja v treh točkah, ki jih prvi krog ni upošteval dovolj:

* **Niti čisto črno niti čisto belo.** Belo na črnem je 21:1 in pri astigmatizmu ali nižji
  občutljivosti na kontrast bere kot vibriranje in halo. Material 3 dark zato ne uporablja `#000000`
  in ne `#FFFFFF`: osnova je `#141218`, `onSurface` `#E6E1E5`, `onSurfaceVariant` `#CAC4D0`.
* **Barve se v temnem načinu desaturira** (okoli 10–20 točk nasičenosti): popolnoma nasičena barva
  na temnem polju »krvavi« preko robov in tekmuje z besedilom.
* **4,5:1 je tla, ne cilj.** Za besedilo, ki nosi bralno obremenitev (ure, predmeti, namigi),
  ciljne vrednosti praktičnih sistemov segajo k 7:1 in več.

Prvi krog je zgrešil ravno tretjo točko: gate je zahteval 4,5:1 za vse, zato so telesni nivoji
pristali tik nad pragom, medtem ko so accenti imeli rezervo. Berljivost pa določajo telesni nivoji.

| Žeton | 1. krog | 2. krog | Najslabši primer po celem sistemu |
|---|---|---|---|
| `Background` | `#07080B` | `#0B0E13` | 1,086× luminanca čiste črne (halacijski prag 1,05) |
| `Surface1..4` | `#0E1218` … `#252B36` | `#141922` … `#2B3342` | koraki 1,09 / 1,09 / 1,12 / 1,14 (kartica 1,192× črne) |
| `SheetSurface` | `#0B0E13` | `#101419` | list ostane temnejši od vsebine |
| `TextPrimary` | `#F2F5FA` (13,0:1) | `#F4F7FC` | **11,82:1** — namenoma pod 13:1, da ni hala; 2,6× praga |
| `TextSecondary` | `#A3ADBE` (6,3:1) | `#C6CEDC` | **8,01:1** |
| `TextMuted` | `#8A95A8` (4,7:1) | `#AAB3C3` | **6,01:1** |
| Accenti (8) | nasičenost 100 % | nasičenost −9 do −11 točk, enak odtenek in svetlost | najšibkejši (`Indigo`) 5,2:1 |
| `Border`/`Strong`/`Card` | 0,10/0,18/0,08 | 0,12/0,20/0,10 | ≥1,25:1 (gate) |
| Kategorijske sklede | `#131C31` … | `#16203A` … | vsebina na skledi ≥12,1:1 |
| `GlassTintAlpha`/`Strong` | 0,82/0,86 | **0,74/0,80** | glej §6.2: najslabši primer je zdaj bel tekst, ne accent |

**Ključna sprememba pri steklu.** Prvi krog je worst-case stekla računal proti najbolj svetlemu
*accentu* (amber). To ni najslabši primer: pod vrhnjo vrstico se pomika **belo besedilo** `#F4F7FC`.
Preračunano čez njega tint 0,74 pusti 8,16:1 za primarni in 5,53:1 za sekundarni tekst; `TextMuted`
pade na 4,15:1, zato ostaja pravilo prvega kroga — muted nikoli na steklu — zdaj pa je izračunano
iz pravega worst case-a in vpisano v gate.

**Halacijski varoval (nov, §11 v `check_contrast.py`):** osnova ≥1,05× črne, kartica ≥1,15× črne,
najsvetlejši tekst ≤1,10× bele, najmočnejši par v aplikaciji ≤19:1 (izmerjeno 18,00). To je strojna
oblika pravila »niti črno niti belo«: paleta ne more zdrsniti niti v eno skrajnost.

### 6.2 Steklo v2: parametri materialov

Raziskava (Apple Material tiers + implementacije tekočega stekla) da štiri številke, ki jih prvi krog
ni imel: debelina blurja po vlogi, dvig nasičenosti in svetlosti, **specularni rob** in disperzija.

| Vloga | blur | lom (višina/količina) | globina | disperzija | rob | tint |
|---|---|---|---|---|---|---|
| `Bar` (vrhnja vrstica, segmented) | **24 dp** | 14 / 22 dp | ne | da | 0,30 | 0,74 |
| `Sheet` (glava/noga lista) | **28 dp** | 14 / 26 dp | da | da | 0,26 | 0,80 |
| `Chip` (filtri, zavihki, pilule) | **14 dp** | 8 / 14 dp | ne | **ne** | 0,22 | 0,74 |
| `Control` (FAB, plavajoči gumbi) | **18 dp** | 16 / 26 dp | da | da | 0,38 | 0 (hue-blend) |

Reference za debelino: Apple thin 16–20, regular 28–34, navigacijska vrstica 40–48 px. Prejšnjih
3–10 dp je bilo motno steklo — efekt, ki ga je bilo komaj videti, a je kljub temu jemal tint.

* **Dvig barv:** `colorControls(brightness = 0.04, contrast = 1.02, saturation = 1.6)` namesto
  `vibrancy()` (ki je natanko saturacija 1,5 brez svetlosti). To je tisto, zaradi česar steklo bere
  kot osvetljeno od zadaj in ne kot siv pravokotnik.
* **Specularni rob:** `RoutineColors.GlassRim` `#F4F8FF` (hladno bela, ne čista bela — rob je nad
  vsem in bi čista bela naredila krom najsvetlejši element na zaslonu), linearni prehod
  0,30 → 0,28× → 0,04× od zgoraj levo navzdol desno, poteza 1,6 dp centrirana na obris (obrez
  pusti ~0,8 dp svetlobe). Rob je narisan **tudi** v rezervni poti (pod Androidom 12), ker je prav
  rob tisti, ki pove, da je panel površina in ne madež.
* **Disperzija (kromatska aberacija)** je vklopljena za vrstico, list in FAB, izklopljena za chip:
  na 32 dp visokem chipu barvni robovi berejo kot tiskarska napaka, ne kot optika.

**Preverjeno proti izvorni kodi verzije, ki jo dejansko uporabljamo** (`backdrop:1.0.0`,
`gradle/libs.versions.toml`), ne proti dokumentaciji za 2.x: `lens(refractionHeight, refractionAmount,
depthEffect, chromaticAberration)` in `colorControls(brightness, contrast, saturation)` obstajata v
1.0.0; vrstni red effectov je colorFilter ⇒ blur ⇒ lens; `blur` nastavi `padding = radius` (ker je
colorFilter že nastavil `renderEffect`), `lens` pa ga zmanjša za `refractionHeight` — ostane
10 / 14 / 6 / 2 dp, vse pozitivno, torej brez artefaktov na robovih. Edina trda omejitev je
`CornerBasedShape` (vse naše oblike so `RoundedCornerShape` ali `CircleShape`).

### 6.3 Sklad nazaj

Model: **sklad nazaj je stanje**, ne pomnjena struktura. Zasloni v tej aplikaciji so že stanje
(`state.panels.showGoals`, `data.mode`), zato se sklad izpelje iz tega, kar je trenutno na zaslonu —
vsaka plast prispeva en callback, Compose pa odgovori z **najglobljo vklopljeno** (callbacks tečejo
v obratnem vrstnem redu dodajanja). Vrstni red sestavljanja v `RoutineApp.kt` je torej globina sklada:

1. **Merilo (drill-down).** Tedenski/mesečni/letni pogled → dotik dneva preklopi v `DAY`
   (`SelectDate(date, openDay = true)` v `RoutineViewModel`). Prejšnje merilo se shrani
   (`drilledFrom`) in nazaj vrne nanj — ena globina, ker dan ne drill-a naprej. To je vedenje, ki ga
   bralec koledarja pričakuje.
2. **Cilji.** `PredictiveBackHandler(enabled = state.panels.showGoals)`: gesta sama animira izhod
   (`graphicsLayer` prebere napredek v risalni fazi → `scaleX/Y = 1 − 0,08·p`, `alpha = 1 − 0,30·p`),
   preklic vrne `CancellationException` naprej (požiranje bi podrlo strukturno sočasnost),
   `finally` ponastavi napredek.
3. **Listi in dialogi.** `ModalBottomSheet` in `AlertDialog` živita v svojem oknu in namestita svoj
   callback, zato odgovorita **pred** obema plastema zgoraj; `onDismissRequest` je bil že povsod
   pravilno pripet (preverjeno v prvem krogu).
4. **Izhod.** Ko ni vklopljene nobene plasti, nazaj zapre aplikacijo — in sistem na Androidu 13+
   pokaže predogled, ker je `android:enableOnBackInvokedCallback="true"` že v manifestu.

Podpis API-ja preverjen za `activity-compose:1.11.0`:
`PredictiveBackHandler(enabled: Boolean = true, onBack: suspend (Flow<BackEventCompat>) -> Unit)`,
stabilen (brez opt-in na klicni strani).

### 6.4 Gate v2 (`tools/check_contrast.py`)

| Preverjanje | 1. krog | 2. krog |
|---|---|---|
| Minimumi za besedilo | 4,5:1 povsod | **po nivojih**: primarni ≥11,0, sekundarni ≥7,5, muted ≥5,5 |
| Accenti | 4,5:1 | ≥5,0 |
| Vsebina na kategorijski skledi | 4,5:1 | ≥7,0 (izmerjeno ≥12,1) |
| Worst-case stekla | proti najbolj svetlemu accentu | **proti `TextPrimary`**: TP ≥7,0, TS ≥4,5; muted izključen in dokumentiran |
| Koraki rampe | 1,03–1,25 | 1,05–1,20 |
| Bordi | ≥1,15 | ≥1,25 |
| Halacija | — | **nov §11**: osnova ≥1,05× črne, kartica ≥1,15× črne, tekst ≤1,10× bele, najmočnejši par ≤19:1 |
| Obseg | 137 parov, 20 barv | **141 parov, 25 barv**, ogledalo XML 20 barv sinhrono |

Nivojski minimumi so bistvo: gate zdaj ne preverja, ali je paleta *legalna*, ampak ali je
*berljiva* — in ali je ostala takšna na vsakem nivoju posebej.

### 6.5 Tretji krog (2026-09-17): otip in gibanje po iOS

Poročilo uporabnika je bilo, da je steklo »še ne najboljše«, naročilo pa natančno: preuči iOS
liquid glass — **točne vibracije in animacije** — in jih naredi na Androidu. Rezultat so trije
sklopi: semantična haptika, Appleove vzmeti pretvorjene po formuli (ne približane) in interaktivno
steklo, ki se odziva na prst.

#### 6.5.1 Haptika: iOS ne pozna »vibracije«, pozna tri družine
(`core/designsystem/haptics/RoutineHaptics.kt`)

| iOS | metoda | API 31+ (`Composition`) | API 29+ | API 24+ (OEM) |
|---|---|---|---|---|
| impact `.light` | `tap()` | `PRIMITIVE_TICK` 0,55 | `EFFECT_TICK` | `VIRTUAL_KEY` |
| impact `.medium` | `press()` | `PRIMITIVE_CLICK` 0,80 | `EFFECT_CLICK` | `CONTEXT_CLICK` |
| impact `.heavy`/`.rigid` | `impact()` | `PRIMITIVE_THUD` 1,00 | `EFFECT_HEAVY_CLICK` | `LONG_PRESS` |
| `selectionChanged()` | `selection()` | `PRIMITIVE_LOW_TICK` 0,50 | `EFFECT_TICK` | `CLOCK_TICK` |
| detent | `dragThreshold()` | `PRIMITIVE_TICK` 0,70 | `SEGMENT_TICK` (30+) | `CLOCK_TICK` |
| notification `.success` | `confirm()`, `complete()` | `TICK` 0,50 → `CLICK` 0,85 | `EFFECT_DOUBLE_CLICK` | val 25/30 ms, 70/110 |
| notification `.warning` | `warning()` | `PRIMITIVE_CLICK` 1,00 | `EFFECT_CLICK` | val 20/20 ms, 50/40 |
| notification `.error` | `reject()` | `TICK` 0,40 → `CLICK` 0,70 → `THUD` 1,00 | `EFFECT_HEAVY_CLICK` | val naraščajoče |
| gesture start/end | `dragStart()`, `dragEnd()` | — | `GESTURE_START`/`_END` (30+) | `LONG_PRESS`/`VIRTUAL_KEY` |
| stikalo | `toggle(on)` | — | `TOGGLE_ON`/`TOGGLE_OFF` (34+) | `confirm()` / `tap()` |

Dve pošteni razliki proti Appleu, zapisani tudi v kodi:

* **Sharpnessa ni.** Core Haptics (AHAP) ima dva zvezna parametra — intenziteto in ostrino; javni
  Android API izpostavlja samo intenziteto. Ostrina je zato v *izbiri primitiva*: `TICK` je kratek in
  oster (soft 0,4/0,4), `THUD` nizek in mehak (strong 1,0/0,8). Izbira primitiva **je** izbira ostrine.
* **Štirje nivoji nazaj, ne eden.** `PRIMITIVE_THUD`, `PRIMITIVE_LOW_TICK` in `PRIMITIVE_SPIN` so
  API 31, `addPrimitive` API 30, in primitivi so *izbirna strojna oprema* — motor, ki jih ne zna
  risati, to pove prek `arePrimitivesSupported()`. Zato je vsaka metoda: sestavljen primitiv z
  eksplicitno intenziteto → preddefiniran efekt → platformina konstanta → valovna oblika.

**Popravljen `warning()`:** bil je `DOUBLE_CLICK`, kar je na iOS *success* vzorec — opozorilo in
uspeh sta se torej občutila enako. Zdaj je sredinski enojni tap, kot v `UINotificationFeedbackGenerator`.

**En vir resnice za dejanja.** Prej je 42 mest klicalo `tap()`: vsak gumb je sam odločal, vse pa se
je občutilo enako. Zdaj haptiko dejanj določa ovojnica v `RoutineApp.onAction`, razvrščena po *pomenu*
dejanja — izbira (način, datum, premik, danes) = `selection()`; odpiranje površine (Dodaj, Načrt,
Nastavitve, Naloge, Cilji, urejanje, zagon izvedbe) = `press()`; shranjevanje/uvoz/obnovitev/
samopopravek = `confirm()`; brisanje/preklic/preskok = `reject()`; nastavitvena stikala =
`toggle(vklopljeno)`; zapiranje in vse ostalo = `tap()`. `ToggleComplete`, `SyncExecution` in
preklopi ciljev ostanejo tihi, ker odgovorijo prek svojega efekta (`TimelineEffect.Completed`).
Lokalni haptiki, ki bi dejanje **podvojili**, so odstranjeni: uvoz urnika, potrditev brisanja v
ciljih in dolgi pritisk na predmet (vsak od njih pošlje dejanje, ovojnica pa ga že sliši).

#### 6.5.2 Gibanje: Appleove vzmeti po formuli, ne po občutku
(`core/designsystem/motion/RoutineMotion.kt`)

WWDC23 »Animate with springs« pove pretvorbo točno: masa 1, `k = (2π ÷ trajanje)²`,
`ζ = 1 − bounce`. `appleSpring(trajanje, bounce)` je ta formula; `AppleMotion` hrani številke z
izvorom, da nihče ne ugiba, ali je vrednost izmišljena ali prebrana.

| SwiftUI | trajanje | bounce | `appleSpring` → stiffness |
|---|---|---|---|
| `.smooth` / `.snappy` / `.bouncy` | 0,5 s | 0 / 0,15 / 0,30 | 158 |
| `.bouncy(duration: 0.4)` | 0,4 s | 0,30 | 247 |
| drag release v Appleovih vzorcih za liquid glass | 0,3 s | 0,40 | 439 |

Compose-ov `StiffnessMedium` (1500) je vzmet 0,16 s — približno trikrat hitrejša. Obe platformi imata
prav; napaka je mešanje znotraj ene interakcije. Zato velja pravilo, zapisano v obeh datotekah:
**`spatialSpec` (Material) premika vsebino, `glassTouchSpec`/`glassMorphSpec` (Apple) premikata
stekleni krom.** Vse tri funkcije ob izklopu animacij vrnejo `snap()`.

#### 6.5.3 Interaktivno steklo in 15-minutni detenti
(`core/designsystem/glass/RoutineGlass.kt`, `features/timeline/components/TimelineComponents.kt`)

`rememberGlassTouch()` + `Modifier.routineGlassTouch()` sta prevod `.glassEffect(.regular.interactive())`:

* **Skrčenje na 0,96** ob pritisku in izpust na Appleovi vzmeti 0,3 s / ζ 0,6 (rahel prekorač, kot
  pri spuščanju povlečenega stekla);
* **osvetlitev v točki dotika** — radialni sij prek `drawOutline` (ne krog: modifier sedi *zunaj*
  panelovega clip-a, krog bi se razlil čez zaobljene robove), jakost do `GlassTouchGlow` 0,22,
  animirana z efektnim tweenom 120 ms. Premik sme prekoračiti cilj, svetlost ne — zato dva spec-a;
* **brez Material valovčka**: `clickable(interactionSource = touch.source, indication = null)`,
  ker je skrčenje *že* stanjska plast; valovček čez lomni panel je drug, protisloven odgovor;
* **vrstni red**: `routineGlassTouch` je prvi v verigi, pred `routineGlass`, da scale ovije cel panel
  (steklo + rob + vsebina) namesto da se bori s panelovim clip-om.

Namerno **izpuščeno**, z razlogom v kodi: *shimmer* (na OLED deluje kot šum; Apple ga uporabi le ob
prvem pojavu materiala) in *morphing med dvema stekloma* (zahteva vzorčenje druge plasti, kar je
glass-on-glass — v dokumentaciji kyant0 in v Appleovih vzorcih izrecno odsvetovano; naš sklad že
uporablja dve plasti).

Drag bloka na časovnici zdaj odda `dragThreshold()` **na vsak prestopljeni 15-minutni korak** — prst
sliši detente, ki jih na gostem dnevu ne vidi (iOS picker rail). Preklic drag-a vrne `dragEnd()`;
uspešen spust ne vrne ničesar lokalno, ker `SaveBlockEdit` v ovojnici že dobi `confirm()`.

#### 6.5.4 Test in CI

`systemBackClosesGoalsAndWalksOutOfTheDay` je padel dvakrat, vsakič kasneje. Prvič z
`IndexOutOfBoundsException`: naslov tedna pride pred stolpci (stolpci so leno sestavljen element),
test pa je segel po `[0]` — popravljeno z `waitUntil`. Drugič s `ComposeTimeoutException` pri
`goals_empty_body`, in tu je bil kriv test, ne aplikacija: `goalsAreSplitIntoTabsInsteadOfOneLongScroll`
teče v razredu četrti (JUnit razvrsti metode po hashu imena, ne po vrstnem redu v datoteki), poseje
projekt CAS, Roomova baza pa preživi activity, ki ga pravilo za vsak test znova ustvari. Test zato
zdaj čaka **plast** (naslov vrhnje vrstice »CAS in EE«, ki je enak v obeh stanjih ciljev) in ne njene
vsebine. `awaitText` poleg tega ne čaka več samo na obstoj besedila, ampak ponavlja `assertIsDisplayed`
dokler se prehod ne usede — vozel, ki je na počasnem emulatorju metal naključne napake. CI ob padcu zdaj **najprej** objavi ime padlega testa in njegovo trditev iz Gradlovega
XML — prej so grep-i po dnevniku porabili budget annotacij, »1 test failed« brez imena pa je
neuporaben.

### 6.6 Četrti krog (2026-09-17): tipografija po Appleu, lasje, spekularni rob

Naročilo je bilo: preuči, kako je iOS 26 narejen — efekti, dizajn, steklo, barvne palete — in doseži
ta nivo; posebej berljivost, kontrast, pisava, debelina, velikost. Raziskava je pokazala, da največja
razlika med tem programom in iPhoneom ni v steklu (to je bilo urejeno v prejšnjih krogih), ampak v
**tipografiji**: aplikacija je bila sestavljena iz Materialovih *label* slogov — 12 do 14 sp, Medium
debelina, pozitiven tracking — medtem ko Apple bere pri 15-17 pt, telo postavlja v Regular, poudarek v
Semibold in tracking z velikostjo zmanjšuje.

#### 6.6.1 Kaj pravi Apple (Dynamic Type, privzeta velikost »Large«)

| slog | debelina | velikost | vrstica | sledenje |
|---|---|---|---|---|
| Large Title | Regular¹ | 34 | 41 | −0,026 em |
| Title 1 | Regular | 28 | 34 | −0,022 em |
| Title 2 | Regular | 22 | 28 | −0,020 em |
| Title 3 | Regular | 20 | 25 | −0,018 em |
| Headline | **Semibold** | 17 | 22 | −0,016 em |
| Body | Regular | 17 | 22 | −0,012 em |
| Callout | Regular | 16 | 21 | −0,014 em |
| Subhead | Regular | 15 | 20 | −0,008 em |
| Footnote | Regular | 13 | 18 | −0,002 em |
| Caption 1 | Regular | 12 | 16 | +0,006 em |
| Caption 2 | Regular | 11 | 13 | +0,010 em |

¹ V HIG je Large Title Regular; UIKitov veliki naslov v navigacijski vrstici je Bold, zato je
`displaySmall` tu Bold — to je tisti naslov, ki ga bralec vidi ob vstopu v zaslon.

Tri pravila, ki jih je bilo vredno prevzeti, ne le številk:

* **Telo je Regular, poudarek je Semibold.** Material postavlja `bodySmall` in vse `label*` v Medium;
  pri 12-13 sp na OLED to zapre odprtine črk (counters) in gosto vrstico zmaže v eno piko. Apple pri
  isti velikosti uporabi Regular in šele poudarek dvigne v Semibold — zato Semibold nekaj pomeni.
* **Tracking pada z velikostjo.** SF Pro ima spremenljivo sledenje: nad 15 pt negativno in vse bolj
  negativno, pri 13 pt približno nič, pri 11-12 pt rahlo pozitivno. Materialova privzeta vrednost je
  obrnjena (pozitivna na telesnem besedilu), kar je drugi razlog, da M3 tipografija deluje razlezano
  poleg iOS.
* **Vrstica je 1,21-1,38 velikosti**, ne Materialovih 1,43-1,50. Ista beseda, manj zraka, več strukture.

#### 6.6.2 Kaj je bilo narejeno (`core/designsystem/theme/Type.kt`)

Celotna lestvica je prepisana; imena vlog ostajajo Materialova, ker jih uporablja več kot 300 mest, in
prav zato se sprememba zgodi **povsod naenkrat**:

| vloga | uporaba | Apple | velikost/vrstica | debelina | sledenje |
|---|---|---|---|---|---|
| `displaySmall` | 2 | Large Title | 34/41 | Bold | −0,026 em |
| `headlineMedium` | 1 | Title 1 | 28/34 | SemiBold | −0,022 em |
| `headlineSmall` | 14 | Title 2 | 22/28 | SemiBold | −0,020 em |
| `titleLarge` | 17 | Title 3 | 20/25 | SemiBold | −0,018 em |
| `titleMedium` | 16 | Headline | 17/22 | SemiBold | −0,016 em |
| `titleSmall` | 19 | Callout poudarjen | 16/21 | SemiBold | −0,014 em |
| `bodyLarge` | 2 | Body | 17/22 | Regular | −0,012 em |
| `bodyMedium` | 13 | Subhead | 15/20 | Regular | −0,008 em |
| `bodySmall` | 80 | Footnote | 13/18 | Regular | −0,002 em |
| `labelLarge` | 94 | gumb/čip | 15/20 | SemiBold | −0,008 em |
| `labelMedium` | 22 | Caption 1 | 12/16 | Medium | +0,006 em |
| `labelSmall` | 39 | Caption 1 poudarjen | 12/16 | SemiBold | +0,010 em |

**Optična velikost — in meja, na katero je ta krog naletel.** Roboto Flex je spremenljiva pisava z
osjo `opsz` 8-144 in privzeto vrednostjo 14 — ista zamisel kot SF Pro-jeva dinamična optična velikost:
majhna besedila so risana robustneje in ohlapneje, velika tanjše in tesneje. Ker os ni bila nikoli
nastavljena, je bil vsak slog v aplikaciji, tudi 34 sp naslov, risan z optiko za 14 pt.

Popravek je bil napisan (`FontVariation.opticalSizing(velikost.sp)`, ki upošteva tudi uporabnikovo
merilo pisave) in **se ni izšel**: `TextStyle` in `SpanStyle` v Composeu sploh nimata parametra za
različice pisave. Preverjeno na treh mestih — javni API `ui-text` (`api/current.txt`) ne vsebuje niza
`fontVariationSettings` nikjer, izvorna koda `TextStyle.kt` in `SpanStyle.kt` ga ne vsebuje, gradnja pa
je javila `No parameter with name 'fontVariationSettings' found`. `FontVariation` kot razred obstaja in
ga uporabljata ponudnik Google Fonts ter `PlatformTypefaces`, iz javnega `TextStyle` pa ni dosegljiv.
Zato v Composeu os ostane na privzeti vrednosti, debelina pa pride prek `fontWeight` (to Compose sam
zlije v os `wght`).

**Widget je izjema in je popravljen:** riše se prek `TextView`, ki ima `android:fontVariationSettings`
od API 26, zato vsi štirje postavitvi widgeta nastavljajo `'opsz'` na velikost, pri kateri se rišejo
(12, 15, 20). Isti atribut na API 24-25 preprosto ne obstaja in se ignorira.

Sledenje je zato podano v **sp**, ne v em: Appleove tabele so izmerjene v absolutnih točkah (Large Title
−1,05 px, Body −0,43 px, Caption 1 +0,12 px), sp pa se z uporabnikovim merilom pisave povečuje enako
kot sorazmerna vrednost — razlika je samo v tem, da se pri večjem merilu ne poveča tudi sledenje,
kar je pri Appleu prav tako.

**Dva zavestna odmika od dobesednega prenosa**, oba zaradi gostote in ne okusa: `titleSmall` je Callout
in `bodyMedium` Subhead (ne Body 17), ker se načrtovalnik bere na pogled in v stolpcih — Apple to v
svojih koledarjih počne enako; in `labelSmall` ostaja 12 sp namesto Appleovih 11, ker je 11 sp v
tedenski mreži in urnem žlebu pod tistim, kar OLED zdrži na bralni razdalji (11 sp je bilo že enkrat
poskušeno in ni šlo).

#### 6.6.3 Lasje in spekularni rob

* **Robovi** (`Border` 0,12 → 0,14, `BorderStrong` 0,20 → 0,24, `CardBorder` 0,10 → 0,12): Appleov
  separator v temnem načinu je `rgba(84, 84, 88, 0,6)`, kar na njegovem sekundarnem ozadju znese
  približno `#3D3D41` oziroma **1,47:1**. Naš bel las pri 0,10 je meril 1,34:1 — nad lastnim pragom
  gate-a (1,25), a vidno šibkejši od iPhoneovega. Rob, ki ga mora bralec iskati, ni rob. Ogledalo v
  `res/values/colors.xml` je posodobljeno, gate preverja ujemanje.
* **Spekularni rob** (`RoutineGlass.drawSpecular`): plošča, osvetljena od zgoraj, ujame ob zgornjem
  robu bistveno več svetlobe kot ob stranicah, in prav ta en las je tisto, kar loči steklo od
  obarvane folije — isti blur, isti tint, in panel nenadoma dobi debelino. Appleov lastni vrh je še
  svetlejši (približno 0,85 alfa pri 1,5 px), kar bi bilo na OLED zaslonu, polnem skoraj belega
  besedila, najglasnejša stvar na ekranu, zato je vrh pri 1,9-kratniku moči roba materiala: ~0,57 na
  kromu in ~0,72 na amber kontrolniku, in umre na 18 % vrednosti do 60 % širine. Zgornja vrstica ga
  **nima**: njen zgornji rob je rob zaslona pod statusno vrstico in tam ni ničesar, kar bi lahko ujelo
  svetlobo.

#### 6.6.4 Kaj je ostalo pri Appleu, česar tu ni (in zakaj)

SF Pro (licenca ga ne dovoljuje v Android aplikaciji — zato Roboto Flex z Appleovo *disciplino*, ne z
Appleovo pisavo); zvezna superelipsa za kote (glej §5.9); adaptivne sene, ki se odzivajo na vsebino pod
steklom (glej §5.10); drsni stekleni palec v segmentiranem kontrolniku (prepis komponente, ne
parametra); `Reduce Transparency` in `Increase Contrast` kot sistemski stikali (na Androidu ju ni).

---

## 7. Viri

**Dokumentacija (preverjeno v tej seji):**

* Material 3, gibanje: prostorski in efektni žetoni, trajanja, `MotionScheme` —
  <https://m3.material.io/styles/motion>
* Material 3, temna tema in elevation overlay — <https://m3.material.io/styles/color/dark-theme/overview>
* Predvidljiv nazaj (`enableOnBackInvokedCallback`) —
  <https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture>
* Compose animacije in shared element transitions —
  <https://developer.android.com/develop/ui/compose/animation/shared-elements/transitions>
* WCAG 2.1: 1.4.3 Contrast (Minimum) — <https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html>;
  1.4.11 Non-text Contrast — <https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html>;
  2.3.3 Animation from Interactions — <https://www.w3.org/WAI/WCAG21/Understanding/animation-from-interactions.html>
* Sistemska nastavitev »Remove animations« po platformah — <https://ionic.io/docs/accessibility/motion>
* `backdrop` (kyant0) — API in pravila za steklo na steklu:
  <https://github.com/Kyant0/AndroidLiquidGlass>, <https://kyant.gitbook.io/backdrop>

**Pregled produktov (opažanja, ne citirane številke):** Notion Calendar, Sunsama, Structured,
Things 3, Morgen — vzorci iz razdelka 2.1: datum v naslovu, smer prehoda sledi potovanju, en
pogled = ena naloga.

**Drugi krog — berljivost temnih tem:** Material 3 dark theme (toni `#141218`/`#E6E1E5`/`#CAC4D0`,
elevation overlay) <https://m3.material.io/styles/color/dark-theme/overview>; WCAG 1.4.3 in 1.4.11
(zgornje povezave); priporočila o izogibanju `#000000` ozadju in `#FFFFFF` besedilu zaradi halacije
pri astigmatizmu ter o desaturaciji barv (~20 %) v temnem načinu — pregledani članki o dark-mode
kontrastu in Material/Apple dark-theme smernice.

**Drugi krog — tekoče steklo:** Apple Material/HIG debeline materialov (thin 16–20, regular 28–34,
navigacija 40–48 px), saturacija 140–180 % + ~105 % svetlosti, temna prevleka 12/24/32 %, specularni
rob (1 px prehod 25 % → 5 %), lom ~12/16 dp s kromatsko aberacijo; pravila o glass-on-glass in
»legibility floor« iz dokumentacije kyant0 (`https://kyant.gitbook.io/backdrop/api/backdrop-effects`).
Parametri v kodi so preverjeni proti **izvorni kodi tag-a 1.0.0** (`Lens.kt`, `ColorFilter.kt`,
`Blur.kt`, `BackdropEffectScope.kt`), ker se API v 2.x razlikuje od dokumentacije.

**Drugi krog — nazaj:** predvidljiv nazaj in `PredictiveBackHandler`
<https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture>; veriga
odgovornosti (najgloblji vklopljen callback zmaga; dialogi/listi so v svojem oknu); mentalni model
Navigation 3 »back stack is just state«.

**Tretji krog — iOS otip in gibanje:** Apple HIG, igranje haptike (tri družine generatorjev, kdaj
katera) <https://developer.apple.com/design/human-interface-guidelines/playing-haptics>;
`UIImpactFeedbackGenerator` (light/medium/heavy/soft/rigid), `UISelectionFeedbackGenerator`,
`UINotificationFeedbackGenerator` (success = dva lahka tapa, warning = en srednji, error = trije
naraščajoči) <https://developer.apple.com/documentation/uikit/uiimpactfeedbackgenerator>;
WWDC23 »Animate with springs« (masa 1, `k = (2π/trajanje)²`, `ζ = 1 − bounce`; `.smooth`/`.snappy`/
`.bouncy` = 0,5 s z bounce 0/0,15/0,3; bounce > 0,4 je pretirano)
<https://developer.apple.com/videos/play/wwdc2023/10158/>; Appleovi vzorci za liquid glass
(`.glassEffect(.regular.interactive())`, spuščanje povlečenega stekla na `.spring(response: 0.3,
dampingFraction: 0.6)`, dvig na 1,1 med dragom, `GlassEffectContainer` za morphing, pravilo
»steklo miruje v ustaljenih stanjih« in odsvetovanje glass-on-glass) — pregledani prek referenčne
zbirke <https://github.com/conorluddy/LiquidGlassReference>.

**Tretji krog — Android ekvivalenti:** `VibrationEffect.Composition` in primitivi
(`PRIMITIVE_CLICK`, `PRIMITIVE_TICK`, `PRIMITIVE_QUICK_RISE`, `PRIMITIVE_SLOW_RISE`,
`PRIMITIVE_QUICK_FALL` = API 30; `PRIMITIVE_THUD`, `PRIMITIVE_SPIN`, `PRIMITIVE_LOW_TICK` = API 31;
`addPrimitive(int, float)` = API 30, `addPrimitive(int, float, int, int)` in `DELAY_TYPE_*` = API 36;
javni API **nima** parametra sharpness) <https://developer.android.com/reference/android/os/VibrationEffect.Composition>;
preddefinirani efekti `EFFECT_CLICK`/`EFFECT_DOUBLE_CLICK`/`EFFECT_TICK`/`EFFECT_HEAVY_CLICK` = API 29
<https://developer.android.com/reference/android/os/VibrationEffect>; `arePrimitivesSupported()` =
API 30 <https://developer.android.com/reference/android/os/Vibrator>; `HapticFeedbackConstants`
(`CLOCK_TICK` 21, `CONTEXT_CLICK` 23, `GESTURE_START`/`GESTURE_END`/`SEGMENT_TICK` 30,
`SEGMENT_FREQUENT_TICK` 33, `TOGGLE_ON`/`TOGGLE_OFF` 34)
<https://developer.android.com/reference/android/view/HapticFeedbackConstants>; preslikava iOS → Android
(light → TICK, medium → CLICK, heavy → THUD/`EFFECT_HEAVY_CLICK`, soft → TICK pri 0,3, rigid → CLICK
pri 0,9) iz <https://github.com/mkuczera/react-native-haptic-feedback>; AHAP intenziteta/ostrina
(soft 0,4/0,4; strong 1,0/0,8; naravna frekvenca Taptic Engine 100–250 Hz)
<https://developer.apple.com/documentation/corehaptics>.

**Četrti krog — iOS 26 tipografija in materiali:** Apple HIG, Typography (Dynamic Type za iOS:
velikost in vrstica za vsak slog od xSmall do AX5, privzeto »Large«: Large Title 34/41, Title 1 28/34,
Title 2 22/28, Title 3 20/25, Headline 17/22 Semibold, Body 17/22, Callout 16/21, Subhead 15/20,
Footnote 13/18, Caption 1 12/16, Caption 2 11/13)
<https://developers.apple.com/design/human-interface-guidelines/typography>; lastnosti SF Pro (devet
debelin, spremenljivo sledenje glede na velikost, dinamična optična velikost, Text < 20 pt in
Display ≥ 20 pt) <https://developers.apple.com/design/human-interface-guidelines/typography#Specifications>;
preslikava slogov v sledenje (Large Title −1,05 px, Title 1 −0,8 px, Headline/Body −0,43 px, Callout
−0,32 px, Subhead 0, Footnote +0,03 px, Caption 1 +0,12 px, Caption 2 +0,15 px) in pravilo »višina
vrstice vsaj 1,3× velikosti, optimalna dolžina vrstice 35-50 znakov, vedno levo poravnano«;
Appleove temne sistemske barve in separator (`rgba(84,84,88,0.6)`, neprosojen `#38383A`, ozadja
`#000000`/`#1C1C1E`/`#2C2C2E`/`#3A3A3C`, oznake `#FFFFFF` in `#EBEBF5` pri 60/30/18 %, accenti v
temnem `#0A84FF`, `#30D158`, `#FF453A`, `#FF9F0A`, `#FFD60A`, `#BF5AF2`, `#64D2FF`, `#5E5CE6`) —
Apple objavlja **prilagodljive** vloge, ne zagotovljenih šestnajstiških vrednosti, zato so te izmerjene
v skupnosti in uporabljene kot primerjava, ne kot cilj; iOS 26 liquid glass: leča (upogibanje in
zgoščevanje svetlobe v realnem času, za razliko od blur-a, ki svetlobo razprši), spekularni poudarki,
adaptivne sene, interaktivnost (scale, bounce, shimmer, osvetlitev v točki dotika), materializacija,
morphing, plasti (vsebina → steklo → vibrancy), pravilo »steklo sodi v navigacijsko plast, nikoli na
vsebino«, koncentrični koti (`containerConcentric`, `ConcentricRectangle`, notranji polmer = zunanji −
odmik, `style: .continuous`), dostopnost (Reduce Transparency, Increase Contrast, Reduce Motion,
iOS 26.1+ Tinted mode) in anti-vzorci (glass-on-glass, steklo na vsebinski plasti, tintiranje vsega,
lomljenje koncentričnosti) <https://www.conor.fyi/writing/liquid-glass-reference>,
<https://github.com/conorluddy/LiquidGlassReference>, <https://nilcoalescing.com/blog/ConcentricRectangleInSwiftUI/>;
spekularni rob kot najvišja vrednost za malo denarja (`inset 0 1.5px 0 rgba(255,255,255,.85)` — en
svetel zgornji rob naredi panel steklo namesto folije), omejitev CSS (nobena od desetih funkcij
`backdrop-filter` ne premakne piksla, zato loma ni mogoče ponarediti) in pravila za zmogljivost (eno
steklo na zaslon, ne animirati blur radija) <https://theplusaddons.com/blog/liquid-glass-ui/>;
44 × 44 pt najmanjša ciljna površina, 8 pt mreža s 4 pt podkoraki (konvencija, ne Appleovo pravilo),
rob 12 pt za vnosna polja <https://superdesign.dev/blog/apple-design-system>.

**Izmerjeno v tem repozitoriju:** `tools/check_contrast.py` (141 parov, nivojski minimumi, worst-case
stekla čez `TextPrimary`, halacijski varoval, koraki rampe, ogledalo palete),
`tools/check_presentation.py` (besedilna pogodba, drsniki, steklo, ena paleta),
`docs/audits/UI_TEXT_AND_SCROLL_AUDIT.md` (rezanje besedila), `docs/audits/UI_LAYOUT_CRITIQUE.md`
(prekrivanje in prelom).
