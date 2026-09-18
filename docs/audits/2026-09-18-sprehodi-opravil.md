# Kognitivni sprehodi skozi 8 jedrovnih opravil (korak 2 deep research)

Datum: 2026-09-18 · Metoda: za vsako opravilo pričakovana pot uporabnika (goal → action) proti
dejanski poti v aplikaciji, izpeljani iz kode (akcije, sheeti, dialogi). Štetje tapov od zagona app.
Prag iz načrta: jedrovno opravilo ≤ 3 tapovi, secondary ≤ 4; vračanja (back) in napake vnosa = 0.

## 1. Dodaj ponavljajočo uro z opomnikom (pon–pet)
Pričakovano: »nova ura« → predmet → »vsak delovnik« → shrani (4).
Dejansko: FAB (1) → naslov (2) → kategorija ŠOLA (3) → predmet (4) → repeat-weekly (5) → delovniki (6) →
opomnik vklop (7, privzeto izklop za SCHOOL) → shrani (8). Čas je privzeto pravilen samo, če ustreza
privzeti uri; sicer +2 tapa kolesa.
**8–10 tapov.** Vrnitev: 0. Napake: 0.
Vrzel: hitri vnos ne nosi vzorca ponavljanja in opomnika — ponavljajoča ura (najpogostejše opravilo
dijaka!) je ročno sestavljanje. Resnost 3.

## 2. Premakni jutrišnjo uro za 30 min
Pričakovano: dan → ura → premakni → shrani (4).
Dejansko: premik na jutri (1) → tap na kartico (2) → kolo začetek: odpri (3), potrdi (4) → shrani (5);
dialog ponudi obseg (samo ta dan / vsa serija) ✓ točno pravilno vprašanje za recur rence.
**5 tapov.** Vrnitev: 0. Napake: 0. **Najboljše opravilo v naboru.**

## 3. Odpovej izpit in prenesi učenje v backlog
Pričakovano: izpit → »prekliči in shrani v čakalnik« (2–3).
Dejansko: najdi izpit (1–2) → tap (3) → brisanje (4) → potrdi (5) → odpri načrtovanje (6) → zavihek
backlog (7) → ročno ustvari temo/backlog vnos z enakim naslovom (8–10).
**8–10 tapov, prepisovanje naslova ročno.** Vrzel: ni akcije »v čakalnik« iz dialoga brisanja/urejanja;
prekinitev izgubi kontekst (naslov, trajanje). Resnost 3.

## 4. Nastavi spanje za med tednom in vikend
Pričakovano: ritem → spanje → časi → shrani (5–6).
Dejansko: nastavitve (1) → zavihek Ritem (2) → enable (3) → ležanje kolo (4–5) → bujenje kolo (6–7) →
vikend vklop (8) → 2× kolo (9–12) → shrani (13).
**13 tapov.** Vrzel: ločen gumb Shrani po že potrjenih kolesih (mešan model); spanje je skrito v
nastavitvah, čeprav je dnevni ritem — povzetka spanja ni nikjer na časovnici. Resnost 2.

## 5. Preveri ure prejšnjega tedna/meseca
Pričakovano: teden/mesec → številke (1–2).
Dejansko: zavihek Teden (1) → `week_totals` povzetek ✓; Mesec (2) → markerji + cycle; Leto (3) → bilanca.
**1–3 tapovi.** Vrnitev: back po lestvici ✓. **Zadovoljeno.**

## 6. Zabeleži dejanske minute po končani uri iz obvestila
Pričakovano: obvestilo → »zaključi z NN min« (1–2).
Dejansko: obvestilo nima akcijskih gumbov (`ScheduleNotifier` ima samo contentIntent) → tap odpre dan (1)
→ najdi kartico (2) → če je bila izvedba zagnana: zaključi izvedbo → dialog dejanskih minut (3–4);
**če izvedba ni bila zagnana, zapisa dejanskih minut ni mogoče vpisati** (RecordActual dosegljiv samo
skozi FinishExecution).
**2–4 tapovi ali nemogoče.** Vrzel: obvestila brez akcij; merjenje vezano izključno na živо izvedbo.
Resnost 3.

## 7. Načrtuj backlog v prosti večer
Pričakovano: prost večer → predlogi → potrdi (2–3).
Dejansko: ikona načrtovanje (1) → zavihek backlog (2) → vnos → izbira datuma (3) → potrdi (4).
**4 tapovi.** Vrzel: ni predloga prostih rež (app ve, kje je prosto — `planning.backlog` + prosti sloti
obstojejo v domeni, a UI jih ne ponudi). Resnost 2.

## 8. Deljaj nalogo iz druge naprave in jo zapri
Pričakovano: share → shrani (1–2).
Dejansko: share-sheet (1) → TasksSheet odprt s predizpolnjenim naslovom/rokom → shrani (2).
**2 tapova.** **Zadovoljeno.**

## Povzetek vrzeli (po resnosti)
1. **Opravilo 6**: obvestila brez akcij + dejanske minute samo skozi izvedbo (3).
2. **Opravilo 1**: ponavljajoča ura brez predloge/vzorca v hitrih vnosih (3).
3. **Opravilo 3**: brisanje ne ponudi »v čakalnik«, kontekst se izgubi (3).
4. **Opravilo 4**: spanje 13 tapov + ločen Shrani po kolesih (2).
5. **Opravilo 7**: ni predlogov prostih rež, čeprav domena to zmore (2).
Najboljši vzorci za kopiranje: opravilo 2 (dialog z obsegom serije) in opravilo 8 (share prefill).

Nadaljevanje: `2026-09-18-matrika-funkcij.md` (korak 3).
