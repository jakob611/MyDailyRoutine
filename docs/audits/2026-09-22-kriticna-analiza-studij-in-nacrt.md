# Kritična analiza šestih zunanjih študij in načrt naprej

Datum: 22. 9. 2026 · Veja: `arena/01a0c42e-mydailyroutine` (tedaj `0e8b8b2` — glava z vsemi študijami)
Predmet: 6 dokumentov (`.docx`) v korenu repozitorija, dodanih 22. 9. 2026
Namen: ločiti, kaj od študij danes še velja, kaj je zastarelo, kje si nasprotujejo, in iz tega sestaviti
vrstni red dela, po katerem aplikacija postane **najboljša različica sebe** — ne nekaj drugega.

---

## 0. Povzetek v dvanajstih vrsticah

1. Šest študij je metodološko **resno delo**: videz, ergonomija palca, dva psihološka pregleda, dostopnost in prvi zagon. Vse naštejejo konkretna mesta in vsaka trditev ima ceno ("kaj se poslabša").
2. Bile so narejene na **starejši različici** — od takrat je aplikacija dobila a11y naslove, prvi zagon, PDF uvoz in popravke M1–M11. Zato je prva naloga bila preveriti vsako trditev v današnji kodi.
3. Preverjeno: **večina resnih očitkov še vedno drži.** Najresnejši trije so potrjeni v kodi danes (`DailyTimeline.kt:232-252`, `OverviewScreens.kt:115-121`, `OverviewScreens.kt:370-383`).
4. Največja najdba ni na nobenem seznamu študij: **prvi zaslon nove namestitve je administrativna cona.** Prazen dan kaže tri ničle, "Na voljo: 0 min rezerve", "Uskladi zamudo", "Čakalna vrsta: 0" in tri vrstice pojasnil — vse skupaj pred časovnico.
5. Druga največja najdba: **mesec uporablja barvno intenzivnost** kot merilo količine (alpha 0,08–0,43). To je serija (streak) brez besede "serija" — in je v neposrednem nasprotju z načeli izdelka.
6. Tretja: **legenda meseca je neuporabna pri barvni slepoti in deloma tudi brez nje** — Test in Rok imata isto barvo (`RoutineColors.Error`), legenda nima oblik.
7. Kontrastne številke študije dostopnosti sem preračunal sam; **vse držijo** (tiho besedilo 4,23:1, beli drsnik na turkiznem tiru 1,86:1, lasna obroba 1,34:1, kartica proti podlagi 1,14:1).
8. Nasprotja med študijami so resnična in jih je treba **odločiti, ne uravnotežiti** (§4). Največje: spodnja navigacija (ergonomija jo zahteva, videz ji nasprotuje, obstoječi dogovor je brez nje).
9. Nekaj predlogov je **slabših od stanja danes**: časovni prag za vlek kartice (mi imamo prostorskega), vprašanje o oblaku s PIN-om (aplikacija brez strežnika), "vlečenje na časovnici" (M6 je to že prepovedal).
10. Iz šestih študij izhaja **21 uporabnih nalog**; predlagani vrstni red je v §5 (P0–P3), vsaka naloga ima merilo dokončanosti in ceno.
11. Šest nalog sem razpisal kot **prompte za agente** (`docs/agents/`), da jih lahko izvajaš vzporedno in brez izgube kakovosti.
12. Cilj ni, da aplikacija dobi vse, kar študije predlagajo. Cilj je, da **neha izgledati kot inženirski prototip**, da prvi dan mine brez frustracije in da barve povedo resnico.

---

## 1. Kako sem preverjal (metoda in njene meje)

**Kaj sem naredil:**

* Iz vseh šestih `.docx` sem izluščil besedilo in prebral celoto (405 odstavkov, ~119.000 znakov).
* Vsako trditev s številko, barvo ali koordinato sem poiskal v današnji kodi na `0e8b8b2` in jo označil kot **potrjeno / zastarelo / napačno**.
* Kontrastne pare sem preračunal po WCAG 2.1 (relativna svetilnost, razmerje (L1+0,05)/(L2+0,05)) z istim postopkom, kot ga uporablja `tools/check_contrast.py`, in primerjal s številkami iz študije.
* Zagnal sem vse statične preveritve, ki jih zahteva CI: `derive_palette.py --check`, `check_translations.py`, `check_presentation.py`, `check_contrast.py` (vse zelene pred posegi).
* Prebral obstoječe dogovore v kodi (komentarji so v tem repozitoriju del specifikacije): `OnboardingScreen.kt`, `SwipeToShift.kt`, `GlassIconButton`, `CategoryTabs`, `Legend`, `MetricTile`, `DateNavigator`.

**Česa nisem mogel:**

* **Zagnati prevajalnika.** V tem peskovniku ni JDK-ja (`JAVA_HOME` ni nastavljen, `java` ni v `PATH`), zato Kotlina ne morem prevesti. Vsaka sprememba kode se preveri v CI (~9 minut na krog). Zato so naloge zastavljene tako, da je ena sprememba = en commit (kot doslej).
* **Videti zaslona v živo.** Presojo videza lahko oprem s številkami in kodo, ne pa z občutkom. Zato so vizualne odločitve v §4 označene kot predlogi, ki jih potrdi tvoj pogled (posnetki iz CI so pri vsakem zagonu priloženi kot opombe).
* **Preveriti trditev o knjižnicah tretjih oseb** (npr. obnašanje `Switch` v tej različici Materiala) drugače kot po kodi in izračunu.

**Kaj velja za vse spodnje:** študije so bile narejene na različici **pred** commiti `6660c01` (prvi zagon), `7bb9436` in `b1b7e6f` (PDF urnik), `a607e9b`/`589f173` (jezik v testih), `c141700` (velika pisava, tarče dotika) in `cdb6a6f` (a11y). Zato je vsak očitek najprej dobil sodbo "ali to še obstaja".

---

## 2. Kaj je zastarelo ali napačno (ne ponavljaj tega)

| Trditev študije | Stanje v kodi danes | Sodba |
| --- | --- | --- |
| "Prvi zagon ne vpraša nič o ritmu; zahteva obsežno nastavljanje petih zavihkov" | Prvi zagon je štiristopenjski: ime → šolsko okno → legenda → primer dneva, z gumbom "Preskoči" in z privzetki, ki so že vgrajeni (`OnboardingScreen.kt:49-71`). | **Zastarelo.** Zasnova iz študije je v veliki meri že izvedena. |
| "Vprašanje o varnostni kopiji v oblaku postavite takoj; pripravite PIN/ključ v Android Keystore" | Aplikacija **namerno ne vpraša** in to dokumentira: "there is no account and no server, so offering a 'free backup' here would be a promise the app cannot keep" (`OnboardingScreen.kt:64-67`). Zastavica za poznejši korak že obstaja. | **Napaka (za zdaj).** PIN brez strežnika je obljuba brez vsebine in dodatna ovira. Obdrži se kljuka, ne korak. |
| "Dodajte 'dinamični predlog za dan' kot možnost A" | Prav to dela zadnji korak prvega zagona: "Load the IB example" napolni dan z bloki, ki jih je mogoče takoj obkljukati (`OnboardingScreen.kt:56-58`). | **Že izvedeno.** Vrednost študije je drugje: v protokolu minimalnega stanja (§5, N15). |
| "Vzpostavite kratki dotik (≤250 ms) za premik kartice" | Takega mehanizma ni in ga ne bo: premik bloka je vezan na **prostorski prag** (64 dp) in en haptični klik (`SwipeToShift.kt:24`, `SwipeShiftTracker`), M6 pa ga je namerno omejil na bloke, kjer premik ni fiksen. | **Zastarelo in slabše.** Časovni prag na dotik bi se sprožal med navadnim drsenjem. |
| "Vlečenje kartice na časovnici" kot obstoječa interakcija | Vodoravno podrsanje spremeni obdobje; navpični vlek kartice **ne** premika bloka (M6 ga je odstranil). | **Zastarelo.** |
| "Zgornja tretjina ima 12 samostojnih kontrol" | Res so štirje ikonski gumbi, štiri tarče v vrstici z datumom in štirje zavihki obdobja — a zavihki so **en pas s štirimi enakimi celicami**, ne štiri ločene kontrole. | **Pretiravano, a smer drži** (glej N4/N5). |
| "Aplikaciji manjka dostopnost za bralnike zaslona" (nikjer izrecno, a implicitno) | Naslovi so označeni (dnevna/tedenska/mesečna stran, cilji, glave listov), vrednosti imajo `stateDescription` (ploščice, časovna polja), nov `AccessibilityTest` to pokriva. | **Že izvedeno (M2).** |
| "Lasne obrobe so 10 % bele" | Drži (10 %), z izjemo `BorderStrong`. | **Potrjeno.** |

---

## 3. Kaj študije zadenejo (potrjeno v kodi danes)

To je delovni seznam. Vsaka vrstica ima dokaz, da je očitek resničen.

| # | Očitek | Dokaz v kodi (danes) | Barvna/številčna potrditev |
| --- | --- | --- | --- |
| P1 | Prazen/urejen dan je preobložen z administracijo: tri ničle, "Na voljo: 0 min rezerve", "Uskladi zamudo", "Čakalna vrsta: 0", odstavek o zamudi — vse pred časovnico | `DailyTimeline.kt:145-252` (ploščice, vrstica rezerve, `ActionRow` z obema gumboma, `auto_heal_hint`) | — |
| P2 | Mesec kodira količino fokusa z jakostjo barve (tiho tekmovanje s samim sabo) | `OverviewScreens.kt:330` — `heat = focusMinutes/300`, `alpha = 0.08 + heat*0.35` | alfa 8 % → 43 % |
| P3 | Test in Rok imata **isto** barvo in isto piko; legenda nima oblik | `OverviewScreens.kt:363-365` (obe `RoutineColors.Error`), legenda `:380-383`, `Legend()` = pika 6 dp (`SummaryComponents.kt:209`) | `#FB7185` za oba |
| P4 | Tiho besedilo pade pod AA na kartici | `DesignSystem.kt:34` `TextMuted = #718096` | **4,23:1** na `#151C2E` (zahteva 4,5:1) |
| P5 | Beli drsnik na turkiznem tiru stikala je neberljiv | nikjer ni `SwitchDefaults` → privzeti Material3 (tir = `Primary`) | **1,86:1** (zahteva 3:1) |
| P6 | Lasna obroba polj je pod 3:1 | `DesignSystem.kt:40` `CardBorder = White 10%` | **1,34:1** na kartici |
| P7 | Kartica se od podlage loči komaj opazno | `Surface1 #151C2E` proti `Background #090D16` | **1,14:1** |
| P8 | Tedenski pogled zapravlja vrh z didaktičnim besedilom | `OverviewScreens.kt:115-121` (`week_hint`: "Mrežo lahko pomikaš vodoravno…") | 2 vrstici pod naslovom + povzetek |
| P9 | Zavihki v nastavitvah se lomijo 3+2 (nesimetrično) | `CategoryTabs` = `FlowRow` (`RoutineText.kt:345-372`), uporabljen tudi v ciljih in načrtovalniku | 5 zavihkov → 3+2 |
| P10 | Datumski čip lomi mesec v siroto ("… 27." / "sep.") | `DateNavigator` (`SummaryComponents.kt:85-90`) z `maxLines = 2`, besedilo iz `RoutineDate.range` = "21. sep – 27. sep" | 2× "sep" v istem nizu |
| P11 | Opomniki so "sistemski alarmi z zahtevo po beleženju" | `ScheduleNotifier.kt:43-50`: dva kanala — `IMPORTANCE_DEFAULT` in tihi `IMPORTANCE_LOW` brez zvoka med poukom; zvok je privzeti sistemski, ne budilka | **Delno zastarelo**: ostane vprašanje *tona* in *gostote*, ne mehanizma |
| P12 | Prvi zaslon prve namestitve je najbolj obremenjen (ni v študijah, a sledi iz njih) | prazen dan → P1 v polni obliki, plus `DayLoadBar` in grafi | — |

**Preračunani kontrasti (moj izračun, isti postopek kot v `tools/check_contrast.py`):**

| Par | Razmerje | Meja | Sodba |
| --- | --- | --- | --- |
| `TextPrimary` na `Background` | 17,74:1 | 4,5 | AAA |
| `TextSecondary` na `Surface1` | 7,99:1 | 4,5 | AAA |
| `TextMuted` na `Surface1` | **4,23:1** | 4,5 | **ne ustreza** |
| `TextMuted` na `Background` | 4,84:1 | 4,5 | ustreza, brez rezerve |
| beli drsnik na `Primary` | **1,86:1** | 3,0 | **ne ustreza** |
| `InkOnPrimary` (`#090D16`) na `Primary` | 10,44:1 | 4,5 | AAA |
| lasna obroba 10 % na `Surface1` | **1,34:1** | 3,0 (meje polj) | **ne ustreza** |
| `Surface1` proti `Background` | **1,14:1** | 3,0 (če nosi mejo) | **ne ustreza** |
| `FocusAccent` na `Surface1` | 6,24:1 | 4,5 | AA |
| `Error` na `Surface1` | 6,31:1 | 4,5 | AA |

**Predlagane vrednosti (preverjene z istim izračunom):**

* `TextMuted` `#718096` → **`#8391A7`**: 5,31:1 na kartici, 6,08:1 na podlagi. Še vedno tiho, a zdaj berljivo tudi na soncu.
* Kartica `Surface1` `#151C2E` → **`#1A2238`**: kartica proti podlagi **1,23:1**, primarno besedilo 14,41:1, sekundarno 7,44:1, tiho (novo) 4,94:1. Dvig je majhen, a kartica dobi mejo.
* Obroba polj: 10 % → **24 %** (2,17:1) za pasivne kartice, **34 % → `#656975`** (3,10:1) za **aktivna vnosna polja** (WCAG 1.4.11 velja za meje interaktivnih elementov).
* Drsnik stikala: tir potemniti na **`#0D9488`** (bela na njem ~3,1:1) **ali** drsniku dodati 1,5 dp obrobo `#090D16`. Prva rešitev je ena vrstica, druga zahteva lastno risanje.

---

## 4. Kjer si študije nasprotujejo — odločitve

Nasprotja niso napaka študij, ampak cena, ki jo je treba plačati zavestno.

### 4.1 Spodnja navigacijska vrstica (ergonomija za, videz proti)

Ergonomska študija zahteva, da se zavihki obdobja in bližnjice preselijo v spodnjo stekleno vrstico. Študija videza zahteva ravno obratno: "odstranitev velikega naslova in ohranitev zraka", oblika pa opozori, da bi spodnja vrstica vzela 56–68 dp in se tepla s pilulo ter sistemsko kretnjo.

**Odločitev:** spodnje vrstice **ne uvajamo** zdaj. Aplikacija je zgrajena kot "nič ne teče v ozadju, brez navigacijskih vrstic" (`docs/ARCHITECTURE.md`), pilula "Dodaj blok" sedi točno v udobni coni desnega palca, ciljna skupina pa telefon drži v eni roki predvsem za dva opravila: pogled "kaj je naslednje" in kljukica. Zato naredimo cenejše in bolj ciljno:

1. **Pilula dobi bližnjico nazaj** (N4): ob dotiku se odpre zadnji uporabljeni tip vnosa, ne prazen list.
2. **Vrstica z datumom se skrajša** (N5): čip z datumom ostane, "Danes" postane ikona (ali se skrije, ko je izbran današnji dan), puščici se združita z robom.
3. **Podrsanje po celotnem dnevu ostane** — to je že zdaj najhitrejša pot med dnevi in je ne bi radi zamenjali za gumb na dnu.

Če se po teh treh spremembah izkaže, da je zgornja tretjina še vedno preobremenjena, je naslednji korak **eno samo dejanje**: zavihki obdobja se preselijo v vrstico z datumom (kot "glasbeni" preklop), ne v novo vrstico spodaj. Nobenega podvojenega navigacijskega sistema.

### 4.2 Števec "Opravljeno 3/9"

Vedenjska študija ga zahteva za odstranitev (kvota, Zeigarnik, Goodhart). Uporabniška vrednost pa je resnična: dijak vidi, koliko blokov dneva je za njim.

**Odločitev:** števec **ostane, a se preoblikuje** (N2): prikazuje se kot "Opravljeno 3 od 9" samo, ko je kaj opravljeno; ob nič opravljenih se pokaže črtica (—). Barva števca je tiha, dokler ni vseh opravljenih; nikoli ni rdeč. Nikoli se ne prikaže kot ulomek brez besede ("0/0" je bila najslabša možnost).

### 4.3 Rdeča pika zapadlosti

Vedenjska študija jo hoče odstraniti (sram, reaktanca). Vendar je to edini opomnik, da jutri je rok za oddajo.

**Odločitev:** pika ostane, a **ne rdeča in ne na ikoni nalog** (N3): število zapadlih se izpiše v tihi barvi na samem gumbu nalog (`TextMuted`, številka brez pike). Rdeča se uporablja samo za rok **danes ali jutri** — kar je informacija, ne kazen. To zmanjša alarm brez izgube podatka.

### 4.4 "Obnovi" pri preskočenih blokih

Vedenjska študija (druga) hoče ob 21. uri popoln izbris preskočenih blokov ("večerna amnestija"). Prva vedenjska študija funkcijo hvali (kognitivno preokvirjanje). Nasprotje je ostro.

**Odločitev — dve stopnji (N6):**

* Preskočen blok ostane, a **nikoli ne dobi barve opozorila**; besedilo je nevtralno ("Odloženo"), gumb pa ostane "Obnovi".
* Pravilo večerne amnestije se **ne izvede samodejno**. Namesto tega se v dnevnem povzetku po 20. uri ponudi ena vrstica: "Preskočene bloke lahko pustiš za danes." z gumbom "Skrij". To je dijaku en dotik, ne odločitev sistema o njegovih podatkih. Sistem, ki sam briše načrte, je oblika nadzora — in prav temu se izdelek izogiba.

### 4.5 Vlečenje kartice in občutek izgube

Ergonomska študija zahteva 500 ms dolgi pritisk za vlek. M6 je vlek zamenjal s podrsanjem in ga omejil. **Odločitev:** ostane podrsanje po časovnici (64 dp, prostorski prag). Ne uvajamo časovnih pragov nazaj.

---

## 5. Načrt naprej: 21 nalog po prioriteti

Oznake: **N** = nova naloga iz študij, **M** = nadaljevanje obstoječe serije M1–M11.
Obseg: **S** ≤ pol dneva, **M** ≈ dan, **L** ≈ dva dni ali več (vključno z dokazi).
Vsaka naloga ima **merilo dokončanosti** — brez njega naloga ni končana.

### P0 — "Aplikacija mora biti prijetna v prvih petih minutah" (naredi prvo)

| # | Naloga | Obseg | Merilo dokončanosti | Cena |
| --- | --- | --- | --- | --- |
| **N1** | **Mirni dan**: dnevni povzetek se prilagodi stanju. Prazno/urejeno → samo tri ploščice z tiho ničlo (črtica) in brez administrativnih vrstic; vrstica rezerve samo, kadar je rezerva > 0 ali opozorila; "Uskladi zamudo" in "Čakalna vrsta" samo, kadar je dan res zamujen (nepravljen blok v preteklosti) ali je čakalna vrsta > 0 | S | Vsaj 4 od 5 vrstic izgine na praznem dnevu; na zamujenem dnevu ostane vse; nov test v `TimelineUiTest` dokaže oba stanja | Izgubi se stalna bližnjica "Uskladi zamudo" za urejene dni — sprejemljivo, ker tam ni česa usklajevati |
| **N2** | **Števec brez kvote**: "Opravljeno" prikaže "3 od 9", ob nič — črtica, barva tiha; nikoli rdeče | S | Ni več besedila `completed_count` v obliki "0/0"; slovar ima nov niz v obeh jezikih | Števec izgubi ostrino "kvote" — kar je namen |
| **N3** | **Tiha zapadlost**: namesto rdeče pike število v tihi barvi; rdeče le, če rok poteče danes ali jutri | S | Pika ni več rdeča pri rokih, daljših od dveh dni; a11y opis pove število | Zahteva nov pogled na `badge` v `RoutineApp.kt`; kratek rok mora ostati viden |
| **N11** | **Prvi zaslon prve namestitve**: če je baza prazna in je bil prvi zagon pravkar končan, dan pokaže (a) uro in (b) eno vrstico "Dodaj svoj prvi blok" z gumbom, ki odpre list s prednastavljenim blokom 45 min fokus ob trenutni uri | S | Prazna namestitev ne pokaže niti ene ničle; prvi blok je oddaljen en dotik | Prekriva se z N1 — izvedi ju skupaj |

**Zakaj P0:** to so mesta, kjer uporabnik v prvih 60 sekundah vidi "prototip" — in toliko časa ima, da se odloči, ali bo aplikacijo obdržal.

### P1 — "Videz naj neha izgledati slučajno"

| # | Naloga | Obseg | Merilo dokončanosti | Cena |
| --- | --- | --- | --- | --- |
| **N7** | **Mesečna mreža brez tekmovanja**: jakost barve opisuje **vrsto** dneva, ne količine (npr. dan pouka / prost dan / dan z veliko fokusa, a v treh diskretnih stopnjah), plus diskretne oznake (ena do tri črtice) za količino — berljivo tudi brez barve | M | Količina je berljiva iz oblik; noben dan ni videti "manjvreden" samo zato, ker ni poln | Zahteva novo risanje v `OverviewScreens.kt` + posodobitev legende in a11y opisa |
| **N8** | **Oblike za roke**: Test = trikotnik, Rok = diamant, prost dan = obroč, fokus = polna pika. Legenda jih uporablja | S | Test in Rok se ločita brez barve; v slovarju sta dva nova a11y opisa | Povečana količina simbolov — čisti zrak v legendo |
| **N9** | **Barve, ki jih je preračunati**: `TextMuted #8391A7`, kartica `#1A2238`, obroba aktivnih polj 34 %, tir stikala `#0D9488`; gate `check_contrast.py` zviša mejo za `TextMuted` na 4,5 | S | Gate teče z novimi mejami; 3 nova pravila v `check_contrast.py`; posodobljen `docs/PALETTE.md` | Kartica je za 4 % svetlejša (OLED poraba zanemarljiva); tiho besedilo je videti malo močnejše |
| **N5** | **Kompakten datum**: `RoutineDate.range` za istoimenski mesec vrne "21.–27. sep" | S | Datum ostane v **eni** vrstici pri širini 360 dp in pisavi 16 sp | V drugem jeziku je treba preveriti obliko (EN "21–27 Sep") |
| **N10** | **Zavihki brez lomljenja 3+2**: `CategoryTabs` postane ena vodoravno drsna vrstica z mehkim prelivom na robu (prenese se v `RoutineText.kt`) | S | 5 zavihkov se nikoli ne prelomi v dve vrstici, tudi pri 1,45× pisavi; testi ohranijo iste oznake | Zadnji zavihek je lahko skrit za robom — zato preliv + oznaka, da se vrstica drsi |
| **N12** | **Teden brez navodil**: `week_hint` se umakne z zaslona; eno sporočilo o drsenju se pokaže **samo ob prvem obisku tedenskega pogleda** (v isti vrstici, kjer je bil naslov), nato izgine | S | V tednu ni stalnega besedila z navodili; mreža se dvigne za ≥ 40 dp | Prvi obisk tedna ima manj razlage — nadomesti jo kretnja, ki je sama po sebi jasna |
| **N13** | **Nastavitve: glava brez manifesta**: vrstica o zasebnosti se preseli v zavihek Podatki (tam že obstaja), glava lista pa obdrži eno vrstico | S | Zavihki so vidni brez drsenja na 640 dp višine | Zasebnost ni več "prva stvar, ki jo prebereš" — kar je škoda, a pravi naslov je Podatki |

### P2 — "Palec, gibanje, poštenost"

| # | Naloga | Obseg | Merilo dokončanosti | Cena |
| --- | --- | --- | --- | --- |
| **N4** | **Pilula si zapomni**: odpiranje lista uporabi zadnji tip vnosa in trajanje (npr. "45 min fokus"), namesto privzetka | S | Dva zaporedna vnosa iste vrste se odpreta z isto prednastavitvijo | Za prvo uporabo brez zgodovine ostane privzetek |
| **N6** | **Nevtralna odložitev** (glej §4.4): preskočen blok nima barve opozorila; 20. ura ponudi eno vrstico za skrivanje, ne samodejnega izbrisa | S | Noben preskočen blok ni obarvan z `Warning`/`Error`; besedilo je v obeh jezikih | Zamujeno delo je videti manj nujno |
| **N14** | **Pisava 12 sp**: vsi časovni nizi (začetek/konec, ura v tabeli tedna) dobijo spodnjo mejo 13 sp in Medium težo | S | Tedenski stolpec in mreža ure se bereta pri 1,45× pisavi brez prekrivanja; `LargeFontUiTest` še zelen | Nekaj vrstic se lahko prelomi — rešuje `RoutineLabel` s samodejnim krčenjem |
| **N15** | **Protokol minimalnega stanja** (iz vedenjske študije): kadar je bilo zaporedoma ≥ 2 bloka preskočenih in je ura po 18., aplikacija ne porine vsega v večer; proste bloke za ta dan tiho skrije in pokaže eno vrstico "Za danes je to dovolj." | M | Stanje je mogoče doseči v testu; večerni počitek se nikoli ne premakne | Zahteva odločitev v modelu (`RoutineViewModel`) — najbolj "vedenjska" sprememba doslej |

### P3 — "Dokazi in priprava na izdajo"

| # | Naloga | Obseg | Merilo dokončanosti |
| --- | --- | --- | --- |
| **N16** | **Grafična regresija**: CI ob vsakem zagonu shrani posnetke (že dela) in jih primerja z referenčnimi po preprostem številu razlik (npr. delež različnih pikslov), z opombo pri odstopanju > 2 % |
| **N17** | **A11y v CI**: `AccessibilityTest` se razširi na barvno slepoto (preverjanje, da imata Test in Rok različno obliko) in na veliko pisavo |
| **N18** | **Merjenje tarč dotika**: `TouchTargetsTest` razširiti na vse tarče v spodnji polovici zaslona (palec), z izpisom v dnevnik |
| **N19** | **Prva minuta**: meritev (v dnevniku, brez analitike) časa od zagona do prvega izrisanega dneva; zgornja meja 900 ms na srednjem telefonu |
| **N20** | **Besedila**: prehod skozi vse nize v obeh jezikih z merili iz §6 (dolžina, ton, brez ukazov); izpis tistih, ki so daljši od 60 znakov, in predlogi |
| **N21** | **Tržna zgodba** (nadomestilo za neuspeli prompt): ena obljuba v dveh dolžinah + seznam črtanega (`docs/agents/06-...` ima prompt) |

---

## 6. Kaj "najbolj profesionalno" pomeni tukaj (merila, ne okras)

Študije pokrivajo videz in vedenje. Manjka pa seznam tega, kar profesionalen izdelek mora imeti, ne glede na študije. To so merila, po katerih se naloge potrjujejo:

1. **Vsak zaslon ima štiri stanja.** Prazno, polno, napaka, prvič. Danes je prazno stanje najslabše (N1/N11). Pravilo: nobeno stanje ne sme vsebovati ničle brez konteksta in nobeno ne sme biti prazno brez naslednjega koraka.
2. **Nobena barva ne nosi pomena sama.** Pravilo WCAG 1.4.1, izvedeno pri N7/N8, preverjeno z N17.
3. **Nobenega trdega besedila, ki bi lahko pomenilo "neumnost".** "Mrežo lahko pomikaš vodoravno" je opomba razvijalca. (N12)
4. **Tiha lestvica.** Barva, debelina, velikost in položaj povedo prioriteto; nič ne kriči. Merilo: če je treba pogledati dvakrat, da vidiš, kaj je pomembno, je nekaj narobe.
5. **Vse, kar je mogoče razveljaviti, je razveljavljivo.** Preskočen blok, prekinjen blok, premaknjen dan — vse se vrne z enim dejanjem. Novost: preveriti, da `Undo` obstaja pri vseh dejanjih, ki brišejo.
6. **En dotik do pomena.** Vsaka informacija, ki jo dijak potrebuje večkrat na dan, je dosegljiva v enem dotiku (dan, naslednji blok, kljukica, dodaj).
7. **Nič ne teče v ozadju in nič ne pošilja podatkov.** Že drži; vsaka nova naloga mora to ohraniti. Naloga, ki tega ne more, se zavrne.
8. **Vsaka trditev v vmesniku je resnična.** "Vse ostane na telefonu" je res; "varnostna kopija" ni, dokler strežnika ni (zato je ni).
9. **Prevajalnik in testi odločajo, ne okus.** Vsaka sprememba gre skozi gate (`derive_palette`, `check_sqlite_integrity`, `check_translations`, `check_presentation`, `check_contrast`), skozi `:app:lintDebug` in skozi napravo.
10. **Vsaka naloga ima ceno.** Če predlog ne zna povedati, kaj poslabša, ni premisljen (pravilo, ki so ga študije same upoštevale).

---

## 7. Kaj iz študij zavračam (in zakaj)

| Predlog | Vir | Zakaj ne |
| --- | --- | --- |
| Spodnja navigacijska vrstica s štirimi zavihki | ergonomska | Podre dogovor "brez navigacijskih vrstic", vzame 56–68 dp, tepe se s pilulo in sistemsko kretnjo; §4.1 ponuja cenejši pot |
| PIN/šifrirni ključ v prvem zagonu | vstopni tok | Ni strežnika; to je obljuba in dodatna ovira. Kljuka obstaja. |
| Samodejni izbris preskočenih blokov ob 21. uri | vedenjsko oblikovanje | Aplikacija ne briše načrtov uporabnika (§4.4) |
| Časovni prag 250 ms za premik bloka | ergonomska | Naš prag je prostorski in varnejši (§4.5) |
| Vlečenje kartice po časovnici | ergonomska | M6 je to odstranil z razlogom |
| Odstranitev števca "Opravljeno 3/9" | vedenjska presoja | Ostane preoblikovan (§4.2) |
| Odstranitev rdeče pike zapadlosti | vedenjska presoja | Ostane utišana (§4.3) |
| "Zamenjajte barve blagovne znamke" | dostopnost | Turkizna `#2DD4BF` je znamka in je zelo berljiva na temni podlagi (10,44:1); problem je tir stikala, ne znamka |
| Obvestila z "Zabeleži minute" kot zahteva | vedenjska presoja | Akcija je neobvezna; kanal je že tih med poukom. Tone popravimo, mehanizma ne rušimo |

---

## 8. Vrstni red izvedbe (predlagan)

```
Teden 1:  N1+N11 (mirni dan)  ->  N2  ->  N3      [P0: prvi vtis]
Teden 1:  N7+N8 (mesec)       ->  N9      ->  N5  [P1: videz]
Teden 2:  N10  ->  N12  ->  N13  ->  N4           [P1/P2]
Teden 2:  N14  ->  N6  ->  N15                    [P2: palec, gibanje, vedenje]
Teden 3:  N16-N21 (dokazi, priprava na izdajo)
```

Pravilo: **ena naloga = en commit = en krog CI**. Če CI pade, se popravi pred naslednjo nalogo (krog je ~9 minut). Naloge N7 in N15 sta edini, ki se jih loti z lastno vejo v enem kosu, ker segata v risanje oziroma v model.

---

## 9. Presoja kakovosti študij (kaj od njih smeš citirati)

| Študija | Kakovost | Kaj smeš citirati |
| --- | --- | --- |
| Dostopnost vmesnika | **Najboljša.** Vse številke sem preračunal in držijo; sklici na WCAG so pravilni (1.4.1, 1.4.3, 1.4.11, 2.5.8) | Kontrastna razmerja, pravilo "barva ni edini nosilec pomena", potreba po 3:1 za meje polj, obnašanje pri 200 % pisave |
| Ergonomsko-vedenjska evalvacija | Dobra; cone palca so standard, citati so blogi, a trditve o Fittsovem zakonu držijo | Razdelitev tarč po dosegljivosti, tveganje spodrsljajev pri vodoravnem podrsanju, kritika horizontalnega podrsanja po vsebini |
| Vedenjska presoja | Dobra; mehanizmi (Zeigarnik, načrtovalna zmota, what-the-hell, reaktanca) so pravilno pripisani. Empirične številke (85,8 % stresa) prihajajo s komercialnih strani za inštruiranje — citiraj kot usmeritev, ne kot dokaz | Sodbe po funkcijah, posebej za števec, piko in radarsko sliko |
| Vedenjsko oblikovanje | Dobra, a najbolj "aktivistična"; predlogi segajo v lastništvo podatkov | Trenutek vrnitve, mikro-vedenje "uskladitev popoldanskega sidra", protokol minimalnega stanja, pet odvračal |
| Zasnova vstopnega toka | Zastarela v izvedbi, uporabna v načelu; vsebuje največjo napako (PIN brez strežnika) | Načelo štirih korakov, "majhen prvi korak", izpuščene industrijske prakse |
| Ocena oblikovanja | Najbolj ostri in najbolj koristni opisi videza; nekaj napak v podrobnostih (npr. "vijolična je odsotna" — prisotna je v blokih fokusa) | "Slučajno proti namernemu", hierarhija po zaslonih, zahteva po dveh radijih, tišina praznih stanj |

**Kjer so bile študije premalo natančne:** niso mogle vedeti, da so bile nekatere funkcije med izdelavo študij že izvedene; trditev "vijolična je odsotna" je bila napačna že takrat (bloki fokusa so vijolični, `DisplayFormat` in `CategoryStyle` to določata); in nobena študija se ni dotaknila bralnikov zaslona, kar smo medtem uredili sami (M2).

---

## 9a. Kaj je iz tega načrta že narejeno (stanje te veje)

Trije commiti po prejemu študij. Vrstni red je bil: najprej najbolj viden očitek, potem tisto, kar je
bilo videti najceneje.

| Commit | Naloga | Kaj je narejeno | Dokaz |
| --- | --- | --- | --- |
| `2b657b5` | **N1, N2** | Prazen dan ne izriše povzetka (ostane naslov in kartica z vabilom); vrstica rezerve se pokaže le, kadar je resnična; "Uskladi zamudo", "Čakalna vrsta" in pojasnilo le, kadar je dan zamujen ali je čakalna vrsta polna; ničle so pomišljaj v tihi barvi; "3/9" je postalo "3 od 9" | nov test `anEmptyDayDoesNotShoutZeroes`, posodobljen test ploščic, vse statične preveritve zelene |
| `1c54398` | **N5, N12** | Razpon v istem mesecu pove mesec enkrat ("21.–27. sep"), prek meseca pa ostane stara oblika; tedenski pogled je izgubil dve vrstici navodil o drsenju | nov enotni test `DateRangeTest`, statične preveritve zelene |
| `177eb9a` | popravek | Test ploščic si je nalagal primer IB in s tem onesnažil bazo za vse naslednje teste v isti seji (štiri padci v CI). Zdaj se ob praznem dnevu preveri kartica z vabilom, primera pa testi ne nalagajo | CI |
| `8dcecab` | **N9** | Barve, ki jih je mogoče prebrati tudi v soncu: kartica `#1A2238`, tiho besedilo `#94A3B8` (meja v `check_contrast.py` zvišana na 4,5), nova pomenska tokna `FieldOutline` `#74849C` za obrobe polj, tir stikala `SwitchOn` `#0D9488` | `check_contrast.py` (109 preverb), `derive_palette.py --check`, `docs/PALETTE.md` |
| `9eb2e90` | **N7, N8** | Mesec: količino nosijo do tri črtice (izrisane, ne izmerjene), vrsto dneva ena ploska barva v treh stopnjah, pomene pa oblike (trikotnik = test, diamant = rok, obroč = prost dan, pika = fokus). Legenda in mreža rišeta isti komponent (`MonthMarkIcon`), opis za bralnik zaslona pove količino in vrsto z besedo | nov enotni test `MonthSignalsTest` |
| `097a492` | **N10, N13, N14** | Zavihki so ena vodoravna drsna vrstica s prelivom na robu (nič več 3+2), glava nastavitev je izgubila stavek o zasebnosti (preselil se je v zavihek Podatki), vse ure dobijo spodnjo mejo 13 sp in težo Medium (`timeLabelStyle()`) | `check_presentation.py`, `check_translations.py`; test v `TimelineUiTest` popravljen, ker je zavihek "Podatki" po novem lahko za robom |
| `38974ec` | **N3, N4, N11** | Rdeča pika zapadlosti je postala tiho število (rdeče le za rok danes ali jutri), list si zapomni zadnjo vrsto in dolžino bloka (`EntryPrefill`), kartica praznega dne pa odpre list s 45-minutnim blokom fokusa ob trenutni uri (`OpenFirstBlock`) | nova `plurals tasks_badge_waiting`; obstoječi testi ostajajo zeleni |
| `803f854` | **N6, N15** | Preskočen blok je "Odloženo" (nevtralno, brez barve opozorila), po 20. uri ponudi povzetek vrstico "Preskočene bloke lahko pustiš za danes." z "Skrij za danes"; nov `MinimalEvening` vklopi mirni večer, ko sta dva zaporedna bloka minila brez oznake in je ura po 18., ter s časovnice umakne le prožne bloke fokusa, ki se niso začeli | nov enotni test `MinimalEveningTest` (devet primerov) |
| `f26b99a` | **N16–N19** | Dokazi: oblike v `AccessibilityTest`, navpični položaj zavihkov pri 1,45× pisavi, tarče dotika v spodnji polovici zaslona z izpisom meritev, `StartupTrace` (prva minuta v dnevnik, brez analitike), `tools/compare_shots.py` s primerjavo posnetkov proti referenci (`ui-baseline`) | preizkušen na treh sintetičnih posnetkih |
| ta dokument | **N20, N21** | Pregled vseh 777 nizov v obeh jezikih (pet popravkov z utemeljitvijo) in tržna zgodba: obljuba v dveh dolžinah, seznam črtanega, za koga izdelek ni, tri prepovedane trditve, trije pogoji pred trgovino | `docs/audits/2026-09-23-besedila-in-trzna-zgodba.md` |

**Kar je iz tega ostalo odprto (stanje po drugem krogu):**

* **N16 čaka na prvo referenco.** `tools/compare_shots.py` in korak v CI sta na mestu, `ui-baseline`
  pa mora posejati človek z ročnim zagonom delovnega toka (`workflow_dispatch`, vhod
  `seed_baseline`). Do takrat korak pove, da reference ni, in ne pade. Zagon tega iz okolja brez
  emulatorja ni mogoč, zato je to edina naloga, ki je končana le do polovice.
* **N21 čaka na tri zunanje modele.** Prompt iz §8.8 je popravljen in pripravljen; obljuba v tem
  dokumentu je moja, zato je vredna toliko kot en človek. Ko jo pošlješ trem modelom in primerjaš,
  dobiš tisto, česar en sam ne more dati: kar se ponovi v vseh treh.
* **Na pravem telefonu še ni bilo preverjeno nič od tega** (uvoz iz PDF-ja, baterija, prava ura).
  Emulator je pokazal, da se izriše in da testi tečejo; to ni isto.

**Pravilo, ki se je izkazalo za koristno:** vsak test, ki v skupni bazi pusti nove predmete, bloke ali
nastavitve, pokvari teste, ki tečejo za njim (isti proces, ista baza). Test sme ustvariti največ toliko
stanja, kolikor ga potrebuje njegova trditev, in nikoli ne sme nalagati primera IB.

## 10. Kaj še ni bilo narejeno iz prejšnjih krogov

* **§8 prompti**: nova izdaja je v `docs/audits/2026-09-22-prompti-za-zunanje-ai.md`; zadnji commit s popravki je v veji, a ga žeton takrat ni mogel potisniti — preveri, da je gor. Tržni prompt (8.8) je bil očitno premalo konkreten; v novi izdaji so dodana merila in prepovedi (§8.8, pravila 1–7).
* **Problem 1 (fiksne širine)**: zaključen, brez spremembe kode (vse štiri vrednosti rišejo s `RoutineLabel`, ki se samodejno krči do 11 sp; `MinLabelWidth` je prag, ne širina).
* **M1–M11**: M1, M3, M6, M7, M2 zaključeni; M4, M5, M8–M11 še odprti in se prekrivajo z N-nalogami (M8 = N14, M5 = N10, M9/M10 = N16/N17). Vrstni red iz §8 jih vključi.
* **Uvoz iz PDF-ja**: koda in testi so (ena naprava, en PDF v preizkusu); **na telefonu še nepreverjeno** — to je ena od nalog P3.

---

## 11. Kaj narediti v naslednjih treh korakih (za človeka)

Naloge iz §5 so izvedene; ostane tisto, česar agent ne more: telefon, emulator in trije zunanji modeli.

1. **Namesti `app-release.apk` iz izdaje `debug-latest` in ga uporabljaj en teden.**
   Povej, kje te ustavi: prva minuta, dodajanje bloka, urejanje, ura ob zamudi. Noben test ne vidi
   tega, kar vidi palec. V izdaji iz istega zagona CI se zapiše tudi `first-day-rendered` v dnevnik
   (`adb logcat -s LockIn`), če te zanima številka.
2. **Poženi `Android verification` ročno z `seed_baseline: true`.** To posname referenco posnetkov;
   vsak naslednji zagon se bo od nje samodejno primerjal in v opombah javil, kateri zaslon se je
   spremenil za več kot 2 %.
3. **Pošlji prompt iz §8.8 trem različnim modelom** (navodila: `docs/agents/06-trzna-zgodba-in-besedila.md`,
   pravila v `docs/audits/2026-09-22-prompti-za-zunanje-ai.md`). Moja obljuba v
   `docs/audits/2026-09-23-besedila-in-trzna-zgodba.md` je izhodišče za primerjavo; kar se v treh
   odgovorih ponovi, je verjetno res.

**Ena stvar, ki bi jo naredil naslednjo, če bi bila samo ena:** prvi zagon naj ponudi tudi "vrzi
teden pouka iz šole" poleg primera IB — dijaku je to edini podatek, ki ga že ima, in teden pouka je
tisto, kar naredi časovnico videti resnično brez truda.
