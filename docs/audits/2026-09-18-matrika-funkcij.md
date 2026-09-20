# Matrika razporeditve funkcij (korak 3 deep research)

Datum: 2026-09-18 · Metoda: inventura vseh uporabniških funkcij iz akcij (`TimelineAction`, 60+),
z dodelitvijo zaslona, števila tapov od zagona, števila različnih poti do istega izida in dosegljivosti
brez scrolla. Pragovi iz načrta: jedrovno ≤ 3 tapovi; vsaka funkcija natanko ena primarna pot;
podvojitve se združijo ali ukinejo.

## Jedrovne funkcije

| Funkcija | Primarni zaslon | Tapov | Alternativne poti do istega izida | Dosegljivost brez scrolla |
|---|---|---|---|---|
| Pregled dneva / zdaj | časovnica DAN | 0 | teden→drill (2) | da |
| Ustvari časovni blok | FAB → EntryEditor | 2 | TaskToSchedule (3), ScheduleBacklog (4), PlanMilestone (4) | da (FAB) |
| Uredi pojavitev | tap kartice → BlockEditor | 3 | — (EntryEditor urejuje osnutek, ne pojavitve) | delno (scroll dneva) |
| Uredi vzorec/serijo | EntryEditor (edit) | 3–4 | — | da |
| Ponavljanje (vzorec) | EntryEditor repeat sekcija | +2 | — | ne (scroll sheeta) |
| Opomnik na block | EntryEditor / kartica (SetReminder) | 1–2 | 2 poti (sheet + kartica) | delno |
| Izvedba (start/finish) | kartica dneva | 1–2 | — | delno |
| Dejanske minute | ActualCompletionDialog | samo skozi finish | **ni druge poti** | ne |
| Backlog → na dan | Načrtovanje/backlog | 4 | ročno čez FAB (2, a prepis) | ne (ikona v vrstici) |
| Naloga (checklist) | Naloge sheet | 2 | share prefill (2) | ne (ikona) |
| Naloga → blok | TasksSheet (TaskToSchedule) | 3 | ročno čez FAB | ne |
| Cilji CAS/EE | Goals (4 zavihki) | 2 | — | ne (ikona) |
| Spanje | Nastavitve/Ritem | 2 + 11 vnosov | — | ne |
| Šolsko okno / privzete ure | Nastavitve/Ritem+Opomniki | 2–3 | — | ne |
| Zdravje/kognitivne meje | Nastavitve/Pravila | 3 | — | ne |
| Uvoz urnika | TimetableImport | 3 | — | ne (skrito v settings/data) |
| Widget agenda | namizje | 0 | deep-link dan (1) | — |

## Podvojitve in prekrivanja

1. **Štiri poti do »delo na časovnico«**: FAB (ročno), TaskToSchedule (iz naloge), ScheduleBacklog
   (iz čakalnika), PlanMilestone (iz roka). Vsaka ima svoj dialog in svoj nabor polj; izid je enak
   (block na dan). IXDF progressive-disclosure načelo: *avoid multiple access paths* — ena pot je
   boljša za uporabnost. Predlog: en sam »umesti na dan« sheet, ki sprejme vir (ročno/naloga/backlog/
   rok) kot parameter, ne štirih ločenih UI-jev.
2. **Dva urejevalnika blokov** (EntryEditor osnutek vs BlockEditor pojavitev): uporabnik ne more
   vedeti, kateri se bo odprl; polja se razlikujejo brez vidnega pravila. Predlog: en urejevalnik z
   načinoma »ta pojavitev / vsa serija« (obseg že obstaja v BlockEditorSheet).
3. **Mešan model shranjevanja** (stikala takoj vs skupine z gumbom Shrani): dve pravili v isti app =
   uporabnik ne ve, ali je shranjeno. Predlog: vse takojšnje (kolo potrdi = shrani; šolsko okno ob
   izgubi fokusa; health/periodic ob potrditvi sekcije), gumbi Shrani se ukinejo.
4. **Opomnik na dveh mestih** (kartica + sheet): edina upravičena podvojitev (hitra akcija na kartici
   je pospeševalnik), ohrani se, a sheet ostane edini vir resnice za podrobnosti.

## Gostota vhodov na dnevnem zaslonu
Vrstica: datumski navigator + 4 ikone (naloge, načrtovanje, cilji, nastavitve) + FAB = 6 vhodov.
Priporočilo M2: ena primarna akcija + največ 2–3 sekundarna. Predlog: naloge in načrtovanje združi v
en »organiziraj« sheet z zavihki (backlog + checklist sta oba »neuresničeno delo«), cilji ostanejo
ikona, nastavitve ostanejo ikona → 4 vhodi vključno z FAB.

## Zaključek o razporeditvi
- **Da, osnovna razporeditev je pravilna**: ena kanonična časovnica (Structured model), sheeti za
  vnos, zavihki za pregledne načine — to se ujema z referencami in z mentalnim modelom »dan je vir
  resnice«.
- **Ne, tri področja odstopajo**: (a) multi-path načrtovanje dela, (b) dvojni urejevalnik, (c) mešan
  model shranjevanja. Vsa tri so popravljiva brez rušenja arhitekture.
- Nastavitve z 5 zavihki so na meji; spanje in šolsko okno (dnevni ritem) bi po pomenu spadala
  bliže časovnici (povzetek ritma na tednu), nastavitve naj ostaneta samo uradna urejevalnica.

Nadaljevanje: `2026-09-18-deep-research-sinteza.md` (korak 4 + 5: reference in prioritete).
