# Paket nalog za agente — LockIn

Ta mapa vsebuje šest samostojnih nalog, pripravljenih tako, da jih lahko izvaja drug agent (ali ti v
novem pogovoru) brez dodatnega konteksta. Vsaka naloga se sklicuje na `docs/audits/2026-09-22-kriticna-analiza-studij-in-nacrt.md`,
kjer je utemeljitev in cena vsake spremembe.

## Pravila, ki veljajo za vse naloge

1. **Ena naloga = ena veja = en commit = en krog CI.** Krog traja ~9 minut; če CI pade, se popravi
   pred naslednjo nalogo.
2. **Nobene nove odvisnosti.** Brez novih knjižnic, brez novih dovoljenj, brez omrežja, brez
   analitike. Aplikacija mora ostati "nič ne teče v ozadju".
3. **Preden karkoli narediš, zaženi statične gate** (če imaš JDK, tudi `./gradlew`):
   ```
   python3 tools/derive_palette.py --check && python3 tools/check_sqlite_integrity.py && \
   python3 tools/check_translations.py && python3 tools/check_presentation.py && python3 tools/check_contrast.py
   ```
   Če JDK-ja ni, se zanašaj na CI: vsak padec napiše natančno vrstico z napako kot CI-opombo.
4. **Brez prevajanja zunaj nabora.** Slovenski nizi so vir resnice; angleški nizi v
   `app/src/main/res/values-en/strings.xml` morajo imeti **ista imena in iste specifikatorje**
   (`check_translations.py` to preveri in pade brez pravih razlogov).
5. **Komentarji so del specifikacije.** V tem repozitoriju vsaka sprememba pove *zakaj*; komentar, ki
   samo opisuje, kaj koda dela, se ne piše.
6. **Dokaz je del naloge.** Vsaka naloga navaja, kako se preveri, da je narejena (test, gate, posnetek
   iz CI-opomb). Brez dokaza naloga ni končana.
7. **Cena mora biti zapisana.** Vsaka sprememba v opisu commita pove, kaj se poslabša ali izgubi.
8. Jezik commit sporočil in komentarjev: **slovenščina**, brez žargonov, brez angleških oklepajev,
   razen imen, ki so v kodi angleška.

## Naloge in njihovo zaporedje

| Datoteka | Naloga | Obseg | Kar se dotakne |
| --- | --- | --- | --- |
| `01-barve-brez-izgube-znacaja.md` | N9: kontrastna popravljanja, ki ne spremenijo znamke | S | `DesignSystem.kt`, `colors.xml`, `docs/PALETTE.md`, `tools/derive_palette.py`, `tools/check_contrast.py` |
| `02-mirni-dan-in-prvi-zaslon.md` | N1, N2, N11: prazen in urejen dan nehata govoriti v ničlah | S | `DailyTimeline.kt`, `SummaryComponents.kt`, nizi |
| `03-mesec-brez-tekmovanja.md` | N7, N8: količina berljiva brez barve, oblike za roke | M | `OverviewScreens.kt`, `SummaryComponents.kt`, nizi |
| `04-kompaktna-glava-in-pisava.md` | N5, N10, N12, N13, N14: datum v eni vrstici, zavihki brez 3+2, teden brez navodil, nastavitve brez manifesta, pisava 13 sp | M | `OverviewScreens.kt`, `RoutineText.kt`, `SummaryComponents.kt`, `DisplayFormat.kt`, `SettingsSheet.kt`, nizi |
| `05-vedenjski-protokol.md` | N15, N6: protokol minimalnega stanja, nevtralna odložitev | M | `RoutineViewModel.kt`, `DailyTimeline.kt`, `RoutineState.kt`, nizi |
| `06-trzna-zgodba-in-besedila.md` | N20, N21: pregled vseh besedil + tržna zgodba (nadomestilo za neuspeli prompt) | S | `docs/`, brez kode |

**Vrstni red zaradi konfliktov:** `02` in `04` se oba dotakneta istega območja (`DailyTimeline.kt` /
`SummaryComponents.kt` / `RoutineApp.kt`). Izvedi `02`, počakaj, da je CI zelen, nato `04`. `01` je
neodvisen in se lahko izvede hkrati s `03`. `05` se dotakne modela in naj pride zadnji.

## Kako nalogo oddaš

1. Veja: `arena/01a0c42e-mydailyroutine` (ali nova veja, ki se od nje odcepi; nikoli `master`).
2. Preden začneš: `git pull` in stanje gate-ov.
3. Po spremembi: `bash -n` na vse skripte, ki jih spremeniš; `python3 tools/*.py` po potrebi.
4. Commit v slovenščini, v opis napiši: kaj se izboljša, kaj se poslabša, kako se preveri.
5. Push in počakaj na zelen CI (`build` in `device-tests`). Če pade, preberi opombo z imenom testa.
6. V poročilu napiši: kaj si spremenil, kaj je ostalo odprto, in ali je kakšna trditev iz študije
   izpadla kot napačna (to je koristen izid, ne neuspeh).
