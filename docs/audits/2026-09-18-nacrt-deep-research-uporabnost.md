# Načrt poglobljene raziskave: razporeditev funkcij in praktičnost uporabe

Datum: 2026-09-18 · Veja: `arena/01a0a0f9-mydailyroutine` · Status: **načrt odobren za izvedo v naslednjem koraku**

Prenova dodajanja blokov in palete (poročilo z 2026-09-17) je popravila *posamezna* mesta.
To poročilo načrtuje sistematičen odgovor na vprašanje, ki ostaja odprto: **ali je razporeditev
funkcij po zaslonih sploh pravilna in ali je aplikacija v celoti praktična za vsakdanjo uporabo** —
ne samo lepa in kontrastno pravilna.

## 1. Vprašanja, na katera mora raziskava odgovoriti

1. Ali vsaka funkcija živi tam, kjer jo uporabnik *pričakuje* (informacijska arhitektura), ali tam,
   kjer je pač pristala med razvojem?
2. Ali so pogoste poti kratke (število tapov do cilja), redke pa najdljive, ne skrite?
3. Ali se funkcije prekrivajo (dve poti do istega izida) in ali je katera od njih odveč?
4. Ali je vsak zaslon berljiv v eni temi: ena glavna akcija, jasna hierarhija, brez odločitvenih
   preobratov sredi opravila?
5. Ali model podatkov (blok / rok / izpit / vzorec / spanje / cilji / naloge) ustreza mentalnemu
   modelu dijaka, ali ga sili v žargon aplikacije?

## 2. Obseg (vseh 5 zaslonov + vsi listi + widget + obvestila)

| Zaslon / površina | Ključne funkcije danes | Kandidati za premik združitve |
|---|---|---|
| Časovnica (dan/teden/mesec/leto) | pregled, drill-down, zdaj-pass, FAB dodaj | filtri, prikaz kategorij |
| List dodajanja/urejanja bloka | naslov, vrsta, čas, kategorija, predmet, vzorec, opomnik, advanced | urejanje pojavitve (BlockEditorSheet) |
| Načrtovanje (Planning) | backlog, auto-razpored, teme | odnos do tedenskega pogleda |
| Cilji (CAS/EE/tedni) | dejavnosti, napredek, radar | odnos do časovnice |
| Naloge (checklist) | dnevne naloge, deljeno dodajanje | odnos do blokov |
| Nastavitve (4 zavihki) | ritem, spanje, opomniki, pravila, podatki | kaj spada v profil uporabe |
| Izvedba (merjenje ure) | zagon/konec, dejanske minute | dostopnost iz obvestila |
| Widget + obvestila | agenda, hitro dodajanje, opomniki | skladnost z zasloni |

## 3. Metode (po vrstnem redu izvedbe)

### 3.1 Hevristična evalvacija (Nielsen + HIG + Material)
Dva neodvisna pregleda vseh površin po 10 Nielsenovih hevristikah, dodana tri mobilna pravila:
*vidnost stanja sistema, match z realnim svetom, konsistentnost, prepoznavanje namesto
spominjanja, flexibilnost in pospeševalniki, estetski minimum, recovery iz napak, help in
documentation* + Apple HIG (44 pt tarče, ena primarna akcija na zaslon, vibrancy/varnost branja)
+ Material 3 (state layer, hierarchical vs tonal, progressive disclosure).
Izid: tabela `hevristika × zaslon × resnost (0–4)`.

### 3.2 Kognitivni sprehod skozi 8 jedrovnih opravil
Za vsako opravilo: zapis korakov po pričakovanju uporabnika (goal → action → gulf of execution),
dejanskih korakov v aplikaciji, razlike in točk, kjer se uporabnik mora *spomniti* namesto
*prepoznati*:
1. Dodaj uro za ponavljajoči predmet z opomnikom (pon–pet).
2. Premakni jutrišnjo uro za 30 min, ker je odpovedana.
3. Odpovej izpit in prenesi učenje v backlog.
4. Nastavi spanje za med tednom in vikend.
5. Preveri, koliko ura je pretekla prejšnji teden (mesec).
6. Zabeleži dejanske minute po končani uri iz obvestila.
7. Načrtuj backlog v prosti večer.
8. Deljaj nalogo iz druge naprave in jo zapri.

### 3.3 Matrika razporeditve funkcij (card-sort po obstoječem stanju)
Vsaka funkcija dobi: zaslon, kjer živi; število tapov od zagona; ali je podvojena; ali je
dosegljiva brez scrolla; ali jo uporabnik z imenom funkcije *najde* (predvidljivost imena/ikone).
Pragovi: jedrovna opravila ≤ 3 tapovi; secondary ≤ 4; podvojitve se združijo ali odstranijo.

### 3.4 Primerjalna analiza referenc
Apple Calendar (iOS 26), Google Calendar, Structured, Notion Calendar, Things 3, Raycast:
kako razporejajo (a) ponavljajoče dogodke, (b) opravila brez ure, (c) meritve časa, (d) spanje/ritem.
Vir: uradne HIG/Material strani, posnetki zaslona, javne opombe oblikovalskih ekip.
Izid: seznam *dokazanih* vzorcev z viri (brez ugibanja).

### 3.5 Merljivost
Za vsako jedrovno opravilo se zabeleži: tapovi, preklopi zaslonov, vračanja (back), napake pri
vnosu. Merjenje z obstoječimi device-testi (compose semantics) + ročni pregled posnetkov CI.
Cilj po prenovi: vsako jedrovno opravilo brez vračanja in brez napake vnosa.

## 4. Viri, ki jih bo raziskava uporabila (zbrani pred pisanjem sklepov)
- Nielsen Norman Group: 10 heuristics; cognitive walkthrough; mobile IA študije.
- Apple HIG: Navigation, Modality, Entering data, Date and time, Layout, Dark mode.
- Material 3: Navigation, Sheets, Component hierarchy, Usability guidance.
- ISO 9241-110 (dialog principles) in ISO 25010 (usability characteristics) za uteži resnosti.
- Objavljene IA študije koledarjev (KLM, card sorting) in prakse oblikovalskih ekip referenc.

## 5. Izdelki
1. `docs/audits/YYYY-MM-DD-hevristicna-evalvacija.md` — tabela najdb z resnostjo in zasloni.
2. `docs/audits/YYYY-MM-DD-sprehodi-opravil.md` — 8 opravil, pričakovano vs dejansko, razlike.
3. `docs/audits/YYYY-MM-DD-matrika-funkcij.md` — tapovi, podvojitve, predlogi premikov.
4. Skupno poročilo s prednostnim načrtom popravl (P0/P1/P2) in merili uspeha.

## 6. Vrstni red izvedbe
1. Hevristike (brez kode, samo pregled) → 2. sprehodi → 3. matrika → 4. reference → 5. sinteza.
Vsak korak se konča z commitom dokumenta; koda se ne spreminja, dokler sinteza ni potrjena.

## 7. Omejitve in poštenost
Brez uporabniških testov z ljudmi (ni panela): hevristike in sprehodi so strokovna napoved, ne
merjenje. Zato vsaka najdba navede pravilo ali vir, iz katerega izhaja, in stopnjo zaupanja.
