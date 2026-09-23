# Pregled vmesnika po 23. 9. 2026 — kaj je smiselno, kaj je bilo popravljeno

Ta dokument je odgovor na vprašanje "ali je vse smiselno narejeno". Ni seznam želja: vsaka vrstica je
bila preverjena v kodi, vsak popravek ima vzrok in ceno, vsaka stvar, ki je nisem mogel preveriti, je
zapisana kot taka.

---

## 1. Kaj je bilo videti narobe in kaj je zdaj drugače

| Kar si opazil | Vzrok v kodi | Popravek |
| --- | --- | --- |
| "marsikje odmiki, velikosti in oblike še vedno niso pravilne in enake povsod" | Tri različne vodoravne mere za isto stvar: zaslon 16 dp, list 24 dp, vsebina kartice 12 ali 16 dp. Dve različni obrobi kartic (15 mest je uporabljalo `Border` 0,14, dvajset `CardBorder` 0,10). Dve zaobljenosti za isto celico (mesec 12 dp, teden 6 dp). Ikone v treh velikostih (16, 18, 20 dp). | `ScreenPadding` (16) velja za zaslone **in** liste; `CardPadding` (12) za vsebino kartice; `ListBottomInset` (108) na dnu vsakega seznama; `CardBorder` za vsako kartico; `RoutineShapes.Cell` (12) za celico v mesecu in v tednu; ikone samo 16 ali 20 dp, pika 6 dp, gumb v listu 52 dp. Gate `check_presentation.py` zdaj to preveri. |
| "nekatere stvari občasno skrite pod drugimi" | Številka na gumbu nalog je rasla čez ikono (18 dp kvadratek na kotu 40 dp stekla). Glava lista je imela podnaslov, ki je porinil zavihke pod rob (to je bilo popravljeno že prej, a je bila ista napaka še v drugem smislu: opozorilo nad seznamom je lahko prekrilo prvo vrstico). | Številka je postala 8 dp pika na kotu (rdeča za rok danes ali jutri, siva za zamujeno), število pa prebere bralnik zaslona; glava lista je ena vrstica; vsak seznam ima `ListBottomInset`, da zadnja vrstica ne pade pod lebdeči gumb. |
| "tista zgornja vrstica verjetno ne rabi imeti puščic levo in desno" | Datum je bil stisnjen med dve puščici, ki sta ponavljali podrsanje. | Puščici odstranjeni. Naslov je edini element v vrstici, zato se dolg datum zlomi v dve vrstici namesto v elipso, in podrsanje dela tudi po naslovu samem. |
| "ko je na danes tega gumba ne rabi biti" | Gumb "Danes" je stal zmeraj. | Izriše se samo, kadar prikazano obdobje ni tisto, v katerem bralec živi (dan, teden, mesec ali šolsko leto). |
| "zgornji floating island kadar ni skrčen čisto na vrhu prekriva besedilo" | Višina otoka se je merila **znotraj** odmika za statusno vrstico in presledka nad njim; `onSizeChanged` je bil zadnji v verigi, zato je sporočil samo višino stekla. Na vsakem zaslonu je bila zato prva vrstica vsebine za 48–56 dp previsoko in prva beseda dneva je bila pod otokom. | Merjenje je prestavljeno **pred** odmike, tako da je mera celotna površina otoka (statusna vrstica + presledek + steklo). Preizkus na napravi zdaj zahteva, da je prva vrstica dneva **pod** spodnjim robom odprtega otoka — to je trditev, ki pade, če se merjenje vrne nazaj. |
| "še mnogo drugih podobnih težav" — zadnja vrstica pod gumbom | Spodnji odmik seznama je bil konstanta (108 dp), gumb za dodajanje pa lebdi nad **sistemskim** navigacijskim pasom, ki je 0, 24 ali 48 dp — odvisno od telefona in od tega, ali se uporabnik premika s kretnjami ali z gumbi. | Odmik je izmerjen: gumb sporoči, koliko prostora zaseda od spodnjega roba okna navzgor, seznam pa dobi toliko. Privzeto ostane 108 dp, če meritve še ni. |
| Dnevnik napredka je za pol ure pisal "0 h" | Napredek se shranjuje v urah z decimalkami, vrstice pa so ga brale z `toInt()`: 30 minut je postalo 0 h, 45 minut ni bilo videti nikoli. Niz "Zabeležene ure" je zahteval celo število, vanj pa je šel `Double` (in bi ob 1,5 ure padel). | Obe branji gresta skozi eno mesto (`formatHoursRounded`, `goalsMinutesLabel`): pol ure je "30 min", ura in pol "1,5 h" — z vejico v slovenščini in piko v angleščini. Štirje enotski testi to prikujejo. |
| Črte na Ganttu so se dotikale | Višina vrste v pasu (28 dp) in višina črte (20 dp + 4 dp) sta bili dve različni resnici: spodnji rob črte je padel **točno** na zgornji rob naslednje, zato sta bili dve aktivnosti v istem pasu videti kot ena stvar, prva črta naslednjega pasu pa se je dotikala prejšnjega. | Ena geometrija (črta 20 + presledek 6) velja za pas in za črte; črta, ki ne bi šla v svoj pas, se ne nariše namesto da bi se narisala čez sosednjo. |
| "tisti onboarding je malce čuden" | Štirje koraki, od katerih sta bila dva razlaga: ura pouka in legenda barv, zavihkov in kretenj. Legenda je opisovala mesec iz prejšnje različice (rdeča pika za test in rok). | Dva koraka: ime (lahko prazno) in prvi dan (primer IB ali prazen dan). Ura pouka je omenjena v eni vrstici in čaka v nastavitvah pod Ritem. Trinajst nizov, ki so pripadali odvzetima korakoma in puščicama, je izbrisanih. |
| "Pitem cas in ee še vedno ne delata hkrati" | Vrstica s projekti je ponujala izbiro **ali** CAS **ali** EE, vsak seznam (aktivnosti, mejniki, dnevnik napredka, kartica stanja) pa je bil filtriran na izbrani projekt. Dve stvari, ki v resnici tečeta vzporedno dve leti, sta bili v aplikaciji ločeni zid. Gantt je lane risal za oba, a je bil edini. | Chip **Vsi projekti** (prikaže se od dveh načrtov naprej in je privzet): kartica stanja za vsak načrt, oba seznama združena, vsaka vrstica pove, iz katerega načrta je. Nov obrazec vpraša, v kateri načrt gre. |

---

## 2. Kaj sem pregledal in je v redu (da se ne popravlja v nedogled)

* **Vodoravna poravnava.** Vsi štirje pogledi (dan, teden, mesec, leto), seznam ciljev in vse liste
  zdaj začnejo na isti navpičnici; preverjeno z gate-om, ne z očesom.
* **Navpični ritem.** Trije razmiki, ki so vsak v svoji družini: 8 dp med vrsticami časovnice, 12 dp
  med vrsticami v listu in v ciljih, 16 dp med razdelki. To je namerno: dnevna časovnica je gosta,
  list z nastavitvami pa ni.
* **Barve in obrobe.** Kartica `CardBorder`, ločnica `Border`, obroba polja `FieldOutline`, tir
  stikala `SwitchOn` — vsaka ima svojo vlogo, vse izmerjene (`check_contrast.py`, 109 preverb).
* **Štiri stanja vsakega zaslona.** Prazen dan (kartica z vabilom), dan z vsebino (povzetek),
  napaka (sporočilo z gumbom Poskusi znova), prvič (prvi zagon v dveh korakih).
* **En dotik do pomena.** Dan/naslednji blok/kljukica/dodaj so vsi v enem dotiku; "3 od 9" je v
  povzetku, teden in mesec sta dva dotika stran.
* **Kar je mogoče razveljaviti, je razveljavljivo.** Preskočen blok (Obnovi), premaknjen dan
  (Ponastavi), izbris (potrditev), večerni protokol (Pokaži vse).
* **Nič ne teče v ozadju.** Preverjeno v manifestu: internet je izrecno odstranjen, opomniki so
  alarmi sistema.

---

## 3. Kar ostaja odprto (in kdo to lahko zapre)

1. **Vse iz tega kroga je bilo zeleno le v CI, ne na tvojem telefonu.** Zadnja zelena gradnja
   (`088f8c2`) je pognala 234 enotskih testov, 68 preizkusov na emulatorju in pet statičnih gate-ov. Dve gradnji pred tem sta padli in obe napaki sta popravljeni: branje besedila znotraj
   `semantics { }` (ni @Composable obseg) in manjkajoči uvoz razširitve `getOrNull`. Emulator ni
   telefon: kaj vidiš ti, je edino merilo, ki šteje.
2. **Grafična regresija čaka na prvo referenco.** Korak v CI je pripravljen; reference še ni, ker jo
   mora posejati ročni zagon (`Android verification`, vhod `seed_baseline`), ki ga tokratni žeton ne
   sme sprožiti (HTTP 403).
3. **Pregled ni popoln.** Preveril sem tisto, kar je bilo videti v kodi in kar je opisal bralec:
   otok nad vsebino, spodnji odmik, števila, Gantt, pogled obeh načrtov. Ostajajo zasloni, ki jih
   v tem krogu nisem odprl (uvoz urnika, varnostna kopija, pripomoček na domačem zaslonu); če je
   težava tam, jo je treba videti.
4. **Tržna zgodba čaka na tri modele.** Obljuba in seznam črtanega sta v
   `docs/audits/2026-09-23-besedila-in-trzna-zgodba.md`.

---

## 4. Pravila, ki so zdaj zapisana v kodi (in jih gate brani)

| Pravilo | Kje je zapisano | Kaj ga preveri |
| --- | --- | --- |
| Vodoravni odmik zaslona in lista je 16 dp | `RoutineMetrics.ScreenPadding` | `check_presentation.py` (dnevni, tedenski, mesečni, letni pogled, cilji, skelet lista) |
| Vsebina kartice je 12 dp | `RoutineMetrics.CardPadding` | `check_presentation.py` (pregledi) |
| Dno seznama je 108 dp | `RoutineMetrics.ListBottomInset` | `check_presentation.py` |
| Vsaka kartica ima `CardBorder` | `RoutineColors.CardBorder` | `check_presentation.py` (prepove `BorderStroke(1.dp, RoutineColors.Border)`) |
| Zagon zaobljenosti je v žetonih | `RoutineShapes` | `check_presentation.py` (prepove `RoundedCornerShape(<število>` v UI-razredih) |
| Velikost ikone ali pike je v žetonih | `RoutineMetrics.IconSmall/IconSize/DotSize` | `check_presentation.py` (prepove `.size(<število>.dp)`) |
| Oba načrta sta vidna hkrati | chip `goal-project-all`, `projectNameOf` | `check_presentation.py`, `TimelineUiTest.bothPlansAreVisibleAtOnce` |
| Nobena barva ne nosi pomena sama | `MonthMarkIcon`, `month_cell_description` | `check_contrast.py`, `AccessibilityTest.theCalendarShapesAreNotInterchangeable` |
| Vsak dotik je 48 × 48 dp | `RoutineMetrics.TouchTarget` | `TouchTargetsTest` (tudi spodnja polovica zaslona, z izpisom meritev) |
