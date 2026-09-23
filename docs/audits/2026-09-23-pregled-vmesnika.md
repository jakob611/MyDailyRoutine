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
| "tisti onboarding je malce čuden" | Štirje koraki, od katerih sta bila dva razlaga: ura pouka in legenda barv, zavihkov in kretenj. Legenda je opisovala mesec iz prejšnje različice (rdeča pika za test in rok). | Dva koraka: ime (lahko prazno) in prvi dan (primer IB ali prazen dan). Ura pouka je omenjena v eni vrstici in čaka v nastavitvah pod Ritem. Trinajst nizov, ki so pripadali odvzetima korakoma in puščicama, je izbrisanih. |
| "Pitem cas in ee še vedno ne delata hkrati" | **Nisem mogel preveriti.** V kodi sta CAS in EE ločeni vrsti projekta in vsak dobi svojo vrsto (lane) na Ganttu; EE dobi šest aktivnosti (stopnje) in pet mejnikov že ob izbiri. Kar vidim v kodi, je torej videti prav — zato je vprašanje zastavljeno spodaj, namesto da bi ugibal in popravljal nekaj, kar morda ni pokvarjeno. | Zapisano v §3 kot vprašanje z možnimi odgovori. |

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

1. **EE ob CAS.** Potrebujem eno informacijo: ali EE projekta **ni** (gumb ga ne ustvari), ali je
   projekt **prazen** (obstaja, a brez črt na Ganttu), ali **ne moreš dodati napredka** (gumbi za
   ure/besede/stopenj ne delajo). Vsak od teh treh je druga napaka v drugem delu kode, in brez
   telefona je ugibanje dražje od vprašanja.
2. **Na pravem telefonu ni bilo preverjeno še nič**, kar je bilo narejeno v zadnjih dveh dneh. Vse
   skupaj je bilo preverjeno v CI (gradnja `8bfb941`: 229 enotskih testov, 66 na emulatorju, pet
   statičnih gate-ov) in v osnutku na tem telefonskem zaslonu, ne pa na pravem telefonu.
   Prejšnja gradnja je padla na eni vrstici — besedilo za bralnik zaslona se je bralo znotraj
   `semantics { }`, ki ni @Composable obseg; popravljeno, in gate `check_presentation.py` tako
   branje odslej prepove.
3. **Grafična regresija čaka na prvo referenco.** Korak v CI je pripravljen; reference še ni, ker jo
   mora posejati ročni zagon (`Android verification`, vhod `seed_baseline`), ki ga tokratni žeton ne
   sme sprožiti (HTTP 403).
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
| Nobena barva ne nosi pomena sama | `MonthMarkIcon`, `month_cell_description` | `check_contrast.py`, `AccessibilityTest.theCalendarShapesAreNotInterchangeable` |
| Vsak dotik je 48 × 48 dp | `RoutineMetrics.TouchTarget` | `TouchTargetsTest` (tudi spodnja polovica zaslona, z izpisom meritev) |
