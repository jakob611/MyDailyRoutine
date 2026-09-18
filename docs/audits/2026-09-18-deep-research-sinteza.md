# Sinteza deep research: primerjava z referencami in prednostni načrt (koraka 4 in 5)

Datum: 2026-09-18 · Vhod: hevristična evalvacija, sprehodi opravil, matrika funkcij (istega datuma).
Koda se do potrditve tega načrta ne spreminja.

## 1. Kaj počnejo reference (z viri)

| Referenca | Model | Kaj dokazuje | Vir |
|---|---|---|---|
| Structured | ena vizualna dnevna časovnica; routine = recurring task, ki se sam prikaže vsak dan; inbox za hiter zajem; drag za premik | kanonična časovnica + recurrence kot edini vir resnice je uveljavljen vzorec; zajem ločen od urejanja | daveswift.com/structured, apps.apple.com (Structured), habi.app |
| Google Calendar | events blokirajo čas; tasks/reminders ne blokirajo časa in ostanejo, dokler niso opravljeni | naša delitev block vs naloga vs backlog je pravilna in jo je treba ohraniti kot tri različne vrste, ne jih zlivati | clickup.com/blog/tasks-vs-events-google-calendar |
| Recurrence praksa | shrani vzorec, ne instanc; ob urejanju vprašaj »ta pojavitev ali vsa serija«; ponujene priljubljene izbire pred prosto konfiguracijo | naš dialog z obsegom serije je pravilen; hitri vnosi morajo nositi tudi vzorec (pon–pet …), ne samo časa | stackoverflow.com/q/85699, ux.stackexchange.com/q/123142 |
| Progressive disclosure | ≤ 3 nivoji, ena pot do funkcije, zavihki zmerno; več poti do istega = zmeda | štiri poti do »umesti delo na dan« so kršitev; združitev v en sheet je popravilo, ne okras | ixdf.org/literature/topics/progressive-disclosure, eleken.co/blog-posts/tabs-ux |
| Time-blocking plannerji | prosti sloti se ponudijo sami (Structured predlaga polnjenje prostega časa) | naša domena že zna izračunati proste reže — UI jih mora ponuditi v načrtovanju | daveswift.com/structured |

## 2. Odgovori na vprašanja iz načrta (§1)

1. **Ali funkcije živijo, kjer jih uporabnik pričakuje?** Večinoma da (časovnica = vir resnice, sheeti =
   vnos, zavihki = pregledi). Izjeme: spanje (dnevni ritem, skrit v nastavitvah), uvoz urnika (skrit v
   settings/data), dejanske minute (brez samostojne poti).
2. **Ali so pogoste poti kratke?** Ne vse: ponavljajoča ura 8–10 tapov, spanje 13; premik ure 5 in
   share 2 sta vzorčna. Cilj po prenovi: ponavljajoča ura ≤ 4 (predloga), spanje ≤ 6.
3. **Ali se funkcije prekrivajo?** Da: 4 poti do bloka na dan, 2 urejevalnika, 2 mesta za opomnik
   (upravičeno kot pospeševalnik).
4. **Ali ima vsak zaslon eno glavno akcijo?** Sheeti da; dnevni zaslon ne (6 vhodov).
5. **Ali model podatkov ustreza mentalnemu modelu?** Block/naloga/backlog/rok se ujemajo z referenčno
   delitvijo event/task/reminder; žargon (backlog, celjenje) je edino odstopanje.

## 3. Prednostni načrt popravl

### P0 (uporabnost jedra, brez rušenja arhitekture)
1. **En urejevalnik blokov**: BlockEditorSheet postane način EntryEditorSheet (»pojavitev / serija«),
   en sam jezik polj, en sam back-out. (hevristika 4, resnost 3)
2. **Mešan model shranjevanja → vse takojšnje**: kolo/shranjevanje ob potrditvi, šolsko okno ob
   potrditvi sekcije, health/periodic ob izhodu sekcije; gumbi Shrani se ukinejo, stanje je vedno
   resnica. (hevristika 4b, resnost 3; odpravlja tudi sum »shranjevanje ne dela«)
3. **Obvestila z akcijami**: »Začni«, »Zaključi z NN min«, »+10 min« na obvestilu opomnika; dejanske
   minute dosegljive tudi brez žive izvedbe (long-press kartice → dialog). (sprehod 6, resnost 3)

### P1 (jedrovna opravila pod prag tapov)
4. **Predloge ponavljanja v hitrih vnosih**: čipi »pon–pet«, »vsak dan«, »+ opomnik« kot del quick
   presets (vzorec + opomnik v enem tapu). (sprehod 1 → ≤ 4 tapov)
5. **»V čakalnik« iz brisanja/urejanja**: dialog brisanja ponudi »odstrani in shrani v čakalnik« z
   ohranjenim naslovom/trajanjem. (sprehod 3)
6. **Predlog prostih rež v načrtovanju**: backlog vnos prikaže 2–3 izračunane proste reže kot čipe
   pred ročno izbiro datuma. (sprehod 7; domena že zmore)
7. **Spanje bliže ritmu**: povzetek spanja (tra + cilj) na tedenskem pregledu; nastavitve ostanejo
   urejevalnica; število tapov pade z ukinitvijo ločenega Shrani. (sprehod 4)

### P2 (brus, ne nujno)
8. Združitev nalog + načrtovanje v en »Organiziraj« sheet (manj vhodov na dnevu, 6 → 4).
9. Preimenovanje žargona: backlog → čakalnik, auto-heal → samodejna poravnava, rezerva → nadomestni čas.
10. Podvojitev bloka (pospeševalnik) in onboarding vodnik ob prvem zagonu.

## 4. Merila uspeha (preverljiva v CI in ročno)
- Sprehodi 1, 3, 4, 6, 7 pod pragi: 4 / 3 / 6 / 2 / 3 tapov; brez vračanj in brez prepisovanja.
- Vsaka funkcija natanko eno primarno pot (matrika brez vrstic z > 1 potjo, razen zavestnih
  pospeševalnikov, označenih kot takšni).
- Device-testi: nov test za obvestilne akcije in za en urejevalnik; obstoječi ostanejo zeleni.
- Uporabniška potrditev: isti vprašanji kot pri prenovi (kje je kaj čudno / kaj ne dela).

## 5. Status izvedbe
- **P0 izveden in zelen** (commiti `940d009`, `f9d13fa`, `8621dc9`; CI build + device-testi zeleni):
  en urejevalnik blokov (occurrence-način v EntryEditorSheet, BlockEditorSheet ukinjen),
  shranjevanje brez gumbov Shrani (spanje/kolo/čipi/debounced pragovi), obvestila z akcijama
  »Začni izvedbo« / »Zabeleži minute« ter gumb za dejanske minute v razširjeni kartici.
- P1 odprt: predloge ponavljanja v hitrih vnosih, »v čakalnik« iz brisanja, predlogi prostih rež,
  spanje na tednu.

## 6. Vrstni red izvedbe
P0.1 → P0.2 → P0.3 → P1.4 → P1.5 → P1.6 → P1.7 → P2 po potrditvi. Vsak korak: commit + CI + kratek
chat povzetek; noben korak ne sme zlomiti gateov (contrast, presentation, derive, device-testi).
