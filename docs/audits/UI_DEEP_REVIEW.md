# Globok pregled: videz, gibanje in dostopnost cele aplikacije

Datum: 2026-09-16 · Veja: `arena/01a0a0f9-mydailyroutine` · Obseg: **celotna aplikacija** —
29 presentation/component datotek (8.920 vrstic), `core/designsystem`, `res/` (widget, tema,
barve), `AndroidManifest.xml`, CI gate-i.

Pregled ni narejen »na oko«. Vsaka trditev spodaj je bodisi izmerjena (skripte v `tools/`),
bodisi prebrana v dokumentaciji/virih (razdelek 6), bodisi preverjena v CI. Kjer je bila stvar
zavrnjena kot nepotrebna, je to zapisano v razdelku 5.

> **Stanje: implementirano.** Popravki so v kodi, ne samo v tem dokumentu. Vsak popravek ima
> v razdelku 3 svojo vrstico: težava → praksa → kje je popravljeno → kako je zavarovano v CI.

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

---

## 4. Kaj zdaj preverja CI

| Gate | Kaj | Stanje |
|---|---|---|
| `tools/check_sqlite_integrity.py` | shema, FK, migracije, indeksi | 19 testov OK |
| `tools/check_presentation.py` | slovenski nizi, pisava, tabularne številke, en domen model, merjene postavitve, en vir datuma, en backdrop okna, zasloni pod stekleno vrstico | 701 nizov, 43 datotek, 5 zaslonov |
| `tools/check_contrast.py` (**nov**) | 137 parov WCAG, worst-case stekla, koraki rampe, bordi, ogledalo palete v XML, proste hex barve | exit 0 |
| `:core:test`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug` | prevajanje, enote, lint (`abortOnError = true`) | — |
| `:app:connectedDebugAndroidTest` | 9 UI testov na napravi + posnetki | — |

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
   gate-u — strojna prepoved (npr. lint pravilo ali parameter tipa) bi bila močnejša, a bi zahtevala
   spremembo podpisov `RoutineText`/`RoutineLabel`.

---

## 6. Viri

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

**Izmerjeno v tem repozitoriju:** `tools/check_contrast.py` (137 parov, worst-case stekla, koraki
rampe, ogledalo palete), `tools/check_presentation.py` (besedilna pogodba, drsniki, steklo),
`docs/audits/UI_TEXT_AND_SCROLL_AUDIT.md` (rezanje besedila), `docs/audits/UI_LAYOUT_CRITIQUE.md`
(prekrivanje in prelom).
