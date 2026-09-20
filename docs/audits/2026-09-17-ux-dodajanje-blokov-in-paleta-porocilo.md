# Končno poročilo z vidika uporabnika: dodajanje blokov in barvna paleta
**Datum:** 2026-09-17 · **Veja:** `arena/01a0a0f9-mydailyroutine` (commit `e53f873`) · **Status:** samo raziskava, brez sprememb kode

Metoda: (1) celoten pregled kode flow-a za dodajanje/urejanje blokov (`EntryEditorSheet.kt`,
`BlockEditorSheet.kt`, `RoutineSheetScaffold`, preseti) in ostalih sheetov; (2) raziskava na internetu
po Apple HIG (liquid glass, vibrancy, dark mode), Material 2/3 (dark theme, tonske palete), raziskave
barvnega kodiranja koledarjev, NN/g in USWDS vzorci obrazcev, Smashing/Eleken o vnosu časa ter
razčlembe profesionalnih temnih dizajnov (Raycast, GitHub Dark, dark-token sistemi). Vsaka trditev
spodaj ima ali referenco v kodi (datoteka:vrstica) ali zunanji vir.

---

## 1. Dodajanje blokov: kaj uporabnik dejansko doživi

### 1.1 Katalog trenutnega lista (EntryEditorSheet, nov blok)
Vrstni red kontrol, kot jih uporabnik vidi od zgoraj navzdol (`EntryEditorSheet.kt:196-410`):

1. **Rail standardnih presetov** (LazyRow čipov: ura, globoko delo, sprehod, malica …) — `:196-203`
2. **Segmentirano stikalo BLOCK / DEADLINE / EXAM** — `:204-215`
3. **Rail predmetov** ("Shranjeni predmeti": Vsi + čipi predmetov + Nov predmet) — `:216-246`
4. **Rail predmetnih presetov** (LazyRow: "Matematika ura", "Matematika test" …) — `:247-262`
5. Polje **Naslov** — `:263`
6. Polje **Datum** (besedilo + ikona koledarja) — `:264-269`
7. Polji **Začetek / Konec** (TimePicker na ikoni) — `:283-291`
8. Namig "trajanje sledi začetku" + **čipi trajanja** (30/45/60/90/trenutno) — `:293-301`
9. Namig o polnoči + **čipi kategorij** (6 z ikonami) — `:302-316`
10. Samo za SCHOOL: **odmor** (checkbox + minutno polje) — `:317-330`
11. **"Več možnosti"** → elasticnost: fixed, minimum, elasticity, priority (suha številčna polja),
    calibrate + velocity preview — `:330-382`
12. **Ponavljanje** (switch) + WeekdayPicker — `:383-392`
13. **Opomnik** (switch) — `:393-397`
14. Footer: vrstica napake + "Shrani in naslednja ura" + "Shrani" — `:160-180`

Skupaj: **14 skupin, ~30 interaktivnih elementov, 3 horizontalni raili čipov, 6 besedilnih polj**,
v listu višine 92 % zaslona z enim samim scrollom.

### 1.2 Kaj je uporabniku težko — z dokazi

**P1. Trije raili čipov pred naslovom.** Preden uporabnik napiše kaj koli, mora prečkati tri
horizontalne vrste čipov, ki se med seboj prekrivajo po funkciji: standardni preseti (1) in
predmetni preseti (4) delata *isto* (`applyPreset`, `:106-127`), rail predmetov (3) pa ob tapu
*poleg* izbire predmeta tudi pokliče preset (`:236-244`). Raziskave obrazcev so enotne: prikaži
samo tisto, kar pomaga pri trenutni odločitvi, ena mikro-tema na zaslon; progresivno razkrivanje
zmanjša kognitivni load do ~40 % in dvigne dokončanje ([USWDS](https://designsystem.digital.gov/patterns/complete-a-complex-form/progress-easily/),
[NN/g](https://www.nngroup.com/articles/4-principles-reduce-cognitive-load/),
[paxform](https://paxform.com/insight/mobile-first-electronic-form-best-practices-explained/)).

**P2. Skriti stranski efekti.** Tap na predmet tiho prepisje uro, kategorijo in naslov (če je prazen)
(`:236-244`); tipkanje datuma tiho nastavi `repeatDays` (`:265-267`); izbira kategorije SCHOOL tiho
vklopi trajanje predmeta in izklopi opomnik (`:311-315`); long-press na predmetu odpre urejanje
predmeta (nepojavljeno, `:222-224`). NN/g: obrazec mora zahtevati *čim manj ugibanja* — vsak
nepojavljen stranski efekt je ugibanje, ki se kaznuje z napačnimi vnosi
([NN/g, 4 principles](https://www.nngroup.com/articles/4-principles-reduce-cognitive-load/)).

**P3. Tri poti do iste stvari (trajanje).** Konec bloka se nastavi (a) s poljem Konec, (b) s čipi
trajanja, (c) s presetom — tri mentalna modela za eno številko, plus namig "trajanje sledi začetku",
ki pojasnjuje, da sta polji spregnuti (`:293-301`). Smashing Magazine: za kratek razpon opcij so
hitre poti (čipi/stepper) odlične *poleg* zanesljivega primarnega vnosa, ne namesto ali podvojeno
([Smashing: Designing The Perfect Date And Time Picker](https://www.smashingmagazine.com/2017/07/designing-perfect-date-time-picker/));
hibrid tipkanje + vizualni picker je najboljša praksa ([Eleken, Time Picker UX 2026](https://www.eleken.co/blog-posts/time-picker-ux)).

**P4. Inženirska številčna polja v "Več možnosti".** `minimum`, `elasticity`, `priority` so suha
besedilna polja z validatorji do 1.000.000 (`:141-149`, `:352-362`). Uporabnik (dijak) nima mentalnega
modela za "elastičnost 1.0". To je konfiguracijski jezik plannerja, ne uporabnikov jezik; takšne
parametre profesio­nalni obrazci bodisi skrijejo za privzete vrednosti bodisi dajo v ločen zaslon z
razlago učinka ([USWDS progressive disclosure](https://designsystem.digital.gov/patterns/complete-a-complex-form/progress-easily/)).

**P5. Ena napaka v footerju po tapu Shrani.** Vse prevere se izpijejo v en string pod gumbom
(`:160-166`, `:131-152`); uporabnik ne ve, *katero* polje je narobe. NN/g: pri razdeljenih korakih so
napake takoj vidne in popravljive na mestu ([NN/g](https://www.nngroup.com/articles/4-principles-reduce-cognitive-load/)).

**P6. Segmentirano stikalo menja cel obrazec.** BLOCK/DEADLINE/EXAM ni lastnost vnosa, ampak *tri
različni formi* v istem listu (različna polja, različni namigi, različni footer gumbi, `:204-215`,
`:398-420`). To je "mode switch" — klasičen vir napak in občutka konfigurátorja.

**P7. Dva urejevalnika istega objekta.** Nov blok ustvari `EntryEditorSheet`, obstoječ blok pa ureja
`BlockEditorSheet` (163 vrstic: samo naslov, čas, allDays/whole) — brez kategorije, predmeta,
ponavljanja in opomnika. Uporabnik, ki želi bloku *popraviti kategorijo ali ponavljanje*, po tihu
ugotovi, da ne more. Nedoslednost med ustvarjanjem in urejanjem je funkcionalna luknja, ne samo UX.

**P8. List 92 % z enim scrollom.** Apple Calendar reši isto nalogo z ~6 vrsticami, kjer vsaka
odpre svoj zaslon (wheel za čas, seznam za repeat, seznam za alert) — "ena odločitev na zaslon"
([NN/g one thing per page](https://www.nngroup.com/articles/4-principles-reduce-cognitive-load/));
naši konkurenti (Google Calendar mobile) držijo creation form na enem zaslonu z *največ 5 vidnimi
polji*, ostalo za vrstico "More options".

### 1.3 Kaj je dobro in naj ostane
- Preseti kot hitra pot (učitelj: "Shrani in naslednja ura" je odličen učiteljski flow).
- Haptika po detentih med dragom, reduce-motion disciplina, testTags za teste.
- TimePicker na ikoni ob ohranjenem tipkanju (hibrid po Eleken/Smashing).
- Validatorji z jasnimi sporočili (samo premakniti jih je inline).

### 1.4 Predlog prenove (specifikacija, brez kode)
**Zaslon 1 (list, ≤ 6 vidnih elementov):** Naslov → vrstica "Dan + začetek–konec" (tap odpre
TimePicker/chipe, kot predlaga uxmovement za ponavljajoče se reže
([uxmovement](https://uxmovement.medium.com/the-best-mobile-ui-for-picking-date-and-time-ca226b719c9)))
→ kategorija (6 čipov z ikonami, en rail) → predmet (chip z barvo predmeta, *brez* stranskih
efektov; preset se ponudi kot čip "Uporabi predmet: Matematika ura" šele po izbiri) → Shrani.
**Vrstice, ki vodijo ven:** Ponavljanje (vrstica z povzetkom "vsak pon, sre"), Opomnik, Odmor,
Napredno (elastičnost kot *slider z opisom učinka*, ne številka). **Inline validacija** pod poljem
ob izgubi fokusa. **Ločena vhoda:** FAB meni "Dodaj blok / Dodaj rok / Dodaj izpit" namesto
segmentiranega stikala — vsak vstop je svoj kratek obrazec (P6). **En urejevalnik:** obstoječ blok
odpre isti obrazec kot ustvarjanje, z izpolnjenimi vrednostmi (P7).

---

## 2. Barvna paleta: zakaj je uporabniku "ogabna"

### 2.1 Diagnoza (koda + viri)

**B1. Vsi akcenti so isti ton T80 in ista nizka kroma (≤ 35.6)** — `derive_palette.py` cap,
`DesignSystem.kt:96-108`. Rezultat je enakomeren pastelni mavrični nabor (#B2C4FF, #EFBC87,
#9BD39D, #FFB2A4, #E1B6F3, #68D2FE), ki na OLED deluje kot *kreda/sladkarije*, ne kot živa barva.
Raziskava barvnega kodiranja koledarjev je eksplicitna: **pasteli so za hitro razločevanje slabši**
("avoid too many pastels … harder to distinguish at a glance",
[Virto calendar color coding 2026](https://www.virtosoftware.com/tasks/why-you-need-to-color-code-your-calendar/)),
kategorijske barve naj bodo *zasičene družine* z jasnimi hue družinami (toplo = nujno, hladno =
rutinsko), 5–7 barv največ ([myshyft color-coded scheduling](https://www.myshyft.com/blog/color-coded-scheduling/),
[Readdle](https://readdle.com/blog/color-code-calendar)); KLM študija barvnega kodiranja dogodkov
priporoča perceptualno razmaknjene, *nasičene* hueje (ΔE ≥ 50 v CIELAB) za funkcijske kategorije
([Google Calendar color-coding efficiency analysis](https://lifetips.alibaba.com/tech-efficiency/google-calendar-adds-color-coding-to-individual-events)).
Material 2 dark theme res priporoča desaturirane akcente za *velike* površine in tekst, a hkrati
dovoljuje "bright (saturated, vivid)" akcente za majhne ključne elemente
([M2 Dark theme](https://m2.material.io/design/color/dark-theme.html)) — naš cap je pasteliziral
*tudi* majhne elemente (stripe, pike, čipe), kjer bi vividnost smela in morala živeti.

**B2. Površine so nevtralne (kroma ≈ 3), brez hue hrbtenice** — `#111317/#1A1C20/…` so skoraj čisti
sivci. Viri: "pure grey lacks hue and saturation, which can result in a dull and lifeless
appearance" — rešitev so *tinted greys* ([dark mode accessible interfaces](https://medium.com/@tundehercules/how-to-design-accessible-dark-mode-interfaces-17f38ecea2e9));
Raycast gradi celoten premium občutek na modro-hladnem kanvu #07080a, ne na nevtralnem sivem
([Raycast design system](https://open-design.ai/plugins/design-system-raycast/),
[DESIGN.md](https://github.com/VoltAgent/awesome-design-md/blob/main/design-md/raycast/DESIGN.md)).
Naš ogljik je *izpeljan nevtralno*, zato app deluje sivo, ne "nočno-indigo".

**B3. Well-i (kontejnerji kategorij) na M3 tonu 30 so blatni.** Temna oranžna = rjava (#654011),
temna zelena = močvirna (#1D5225) — `DesignSystem.kt:136-141`. Apple takšnih globokih tonskih
kontejnerjev v dark mode sploh ne uporablja: selektirane vrstice in čipi so **~12–18 % alpha živih
tintov nad površino** (#007AFF @ 18 % dark), ne ton-30 plošče
([Apple HIG color implementation](https://gist.github.com/eonist/7b5abce6979ce4a272c5de57eb0fb550/),
[Apple glass reference](https://github.com/rohitg00/awesome-claude-design/blob/main/design-md/glass/apple.md)).
Rezultat naše izbire je "well kot blato + pasteln tekst", medtem ko bi alpha-fill dal "barva kot
svetloba na steklu".

**B4. Kromna nedoslednost: Warning #E9C300** (polna Apple kroma, izvzet iz capa) visi sredi
pastelne družine kot tupek — `DesignSystem.kt:107`. Ena sama visoko zasičena barva med kredami
izpade kot napaka, ne kot poudarek; Apple drži yellow kot *vivid tint za majhne elemente*, ne kot
tekstovno barvo na olivnem kontejnerju.

**B5. Primarni/brand akcent je pasteln jantar (#EFBC87)** na FAB in glavnih gumbih — primarna
akcija nima samozavesti. Profesionalni temni sistemi imajo **en samozavesten brand akcent**
(Raycast: bela pilula + rdeča kot punktuacija; GitHub Dark: #58A6FF; Linear: indigo) in ga držijo
majhnega ([Raycast](https://open-design.ai/plugins/design-system-raycast/),
[dark mode palettes guide](https://mypalettetool.com/blog/dark-mode-color-palettes)).

**B6. Steklo nima kaj lomiti.** Liquid glass vzorči vsebino za seboj (vibrancy 180 %); če je
vsebina za steklom siva + pastelna, je steklo sivo. Apple: material *nima lastne barve*, barvo
pobere iz vsebine, zato njihovi chromi živijo pod glassom ([HIG materials/vibrancy povzetek](https://lobehub.com/skills/comeonoliver-skillshub-axiom-hig-ref),
[Apple DESIGN.md](https://superdesign.dev/blog/apple-design-system)). Naša paleta steklu krade
raison d'être.

**B7. Kar je prav in naj ostane:** halation cap (brez #FFF/#000), tonska lestvica površin v M3 pasu,
ΔE pravilo razločnosti kategorij, AAA well-tekst, izpeljava iz objavljenih konstant + CI gate.
Telesna napaka ni sistem, ampak *točke na krivulji*: ton/kroma/alpha izbire.

### 2.2 Smeri, ki jih ponuja raziskava

**Smer A — "Vivid tints, tinted neutrals, alpha wells" (priporočam).**
- Površine: ogljik dobi indigo hue hrbtenico (kroma 4–6 pri tonih 4–22) — tinted greys, Raycast pristop.
- Kategorije: Apple hueji pri **tonu 68–74 in kromi 48–60** za *majhne* elemente (stripe 4 dp, pike,
  ikone čipov, progress); pastelni T80 ostane samo za **tekst na barvnih fillih**.
- Well-i/postavke: **18–24 % alpha tinta nad Surface1** (Apple selected-cell vzorec), tekst na fillu
  = pastelni T80 iste hue družine (AAA se ohrani, gate preveri kompozit).
- Brand: en samozavesten akcent (jantar pri kromi ~70, ton ~75) za FAB/primarno, uporabljen varčno.
- Warning: vivid yellow samo ikona/subtext; kontejner = 15 % alpha fill.
- Ujemanje z liquid glass: vividi pod steklom = vibrancy ima kaj početi; chrome ostane brez lastne
  barve (tint 0.45) — točno Apple model ([HIG liquid glass](https://lobehub.com/skills/comeonoliver-skillshub-axiom-hig-ref)).
- Tveganje: halation na velikih vividi — mitigacija: vivid nikoli kot tekst na črnem in nikoli kot
  velika plošča (desaturacija velja za tekst/velike površine, [halation fix](https://www.rs999.in/blog/halation-bloom-in-dark-mode-graphics-why-your-white-text-vibrates-on-black-and-the-anti-glow-fix-pros-use),
  [UXPin](https://www.uxpin.com/studio/blog/dark-mode-benefits/)); gate dobi pravilo "vivid samo ≤ X dp²".

**Smer B — "Monokromna hrbtenica + duotone kategorije" (Raycast/Linear šola).**
Vse površine indigo-monokrom, kategorije samo 6 huejev kot 4 dp stripe in 12 % filli, ves tekst
nevtralen. Najbolj "premium mirno", a izgubi barvno identiteto well-ov in je za šolski planner
(prebarvan vsakdan) lahko prehladno.

**Smer C — "Ostani M3, dvigni kromo."** Minimalna sprememba (cap 35.6 → ~50, well-i T28). Ceneje,
a ostane v Material-kreda estetiki, ki jo uporabnik že zdaj opisuje kot ogabno; ne naslovi B3/B5/B6.

**Priporočilo: Smer A**, implementirana prek obstoječega `derive_palette.py` (nove ciljne točke:
tone/chroma/alpha pravila), gate razširjen z: (i) kompozit tekst-na-alpha-fill za vseh 6 kategorij,
(ii) pravilo majhnosti vividov, (iii) hue-hrbtenica površin (kroma ≥ 4).

### 2.3 Konkretni ciljni parametri za izpeljavo (predlog, ne koda)
| Vloga | Ton (M3) | Kroma | Opomba |
|---|---|---|---|
| Površine Background→Surface4 | 4 / 6 / 10 / 12 / 17 / 22 | 4–6 (indigo hue) | tinted greys |
| Tekst primary/secondary/muted | 98.5 / 90 / 80 | ≤ 2 | ostane (halation cap) |
| Kategorija — majhni elementi | 70 ± 4 | 48–60 | stripe, pike, ikone, chip border |
| Kategorija — tekst na fillu | 82–86 | 20–30 | AAA na kompozitu filla |
| Kategorija — fill (well) | alpha 0.18–0.24 tinta nad Surface1 | — | Apple selected-cell |
| Brand akcent (FAB/primarno) | 75 | 65–75 | varčno, majhne površine |
| Warning | ikona/subtext vivid; fill 0.15 alpha | — | brez olivnega kontejnerja |

---

## 3. Druge ugotovitve (krajše)
- **Opozorilni banner** (SummaryComponents) uporablja Warning tekst na WarningContainer — po Smeri A
  postane 15 % fill + vivid ikona + TextPrimary naslov.
- **Widget** zrcali samo 21 barv; po Smeri A dobi alpha-fille kategorij (Glance podpira ColorProvider
  z alpha), da bo predal domač izgledal kot app.
- **Settings** so po tabih in pregledni — brez sprememb.
- **Tasks/Planning** sheeti sledijo istemu scaffoldu; prenova obrazcev (1.4) naj se uporabi tudi tam,
  kjer imajo > 8 vidnih kontrol (PlanningSheet ima segmente + čipe + polja).

## 4. Prioritetni načrt (če bo odobreno)
- **P0** Prenova dodajanja blokov po 1.4 (en urejevalnik, ≤ 6 kontrol, inline validacija, vrstice-ven).
- **P0** Paleta Smer A: derive pravila + gate razširitve + widget zrcalo.
- **P1** Opozorilni banner in well-i v alpha-fill vzorec; brand akcent na FAB.
- **P2** Napredne parametre kot sliderji z razlago; presets rail združen v en "Hitri vnosi" rail.

## 5. Viri
1. Apple HIG (materials, vibrancy, liquid glass, dark mode) — povzetki: lobehub.com/skills/comeonoliver-skillshub-axiom-hig-ref; github.com/rohitg00/awesome-claude-design (design-md/glass/apple.md); superdesign.dev/blog/apple-design-system
2. Apple sistemskie barve v praksi (selected cell 18 %, separatorji, tinti) — gist.github.com/eonist/7b5abce6979ce4a272c5de57eb0fb550
3. Material 2 Dark theme — m2.material.io/design/color/dark-theme.html
4. Material 3 tonske palete / HCT — coloracci.ai/blog/material-design-3-color-system; softaai.com/material-3-colorscheme-explained
5. Barvno kodiranje koledarjev — virtosoftware.com/tasks/why-you-need-to-color-code-your-calendar; myshyft.com/blog/color-coded-scheduling; readdle.com/blog/color-code-calendar; lifetips.alibaba.com/tech-efficiency/google-calendar-adds-color-coding-to-individual-events (KLM študija)
6. Obrazci in kognitivni load — designsystem.digital.gov/patterns/complete-a-complex-form/progress-easily; nngroup.com/articles/4-principles-reduce-cognitive-load; paxform.com/insight/mobile-first-electronic-form-best-practices-explained
7. Vnos datuma/časa — smashingmagazine.com/2017/07/designing-perfect-date-time-picker; eleken.co/blog-posts/time-picker-ux; uxmovement.medium.com/the-best-mobile-ui-for-picking-date-and-time-ca226b719c9
8. Temni dizajni in tokeni — open-design.ai/plugins/design-system-raycast; github.com/VoltAgent/awesome-design-md (raycast); mypalettetool.com/blog/dark-mode-color-palettes; superdesign.dev/styles/dark-mode; muz.li/blog/dark-mode-design-systems; rs999.in (halation); uxpin.com/studio/blog/dark-mode-benefits; medium.com/@tundehercules (tinted greys)
