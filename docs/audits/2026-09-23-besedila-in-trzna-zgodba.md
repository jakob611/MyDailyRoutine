# Besedila in tržna zgodba (N20, N21)

Datum: 23. 9. 2026. Naloga iz `docs/agents/06-trzna-zgodba-in-besedila.md`; merila iz §6 dokumenta
`docs/audits/2026-09-22-kriticna-analiza-studij-in-nacrt.md`. Vse trditve spodaj so preverjene proti
kodi in nizom v tem repozitoriju, ne proti vtisu.

---

## Del 1 — Tržna zgodba

### 1.1 Ena obljuba v dveh dolžinah

Ista obljuba, dvakrat povedana; daljša različica ne dodaja ničesar, česar krajša ne bi vsebovala.

**Kratka (11 besed, SL)**
> Tvoj dan na enem zaslonu. Brez računa, brez omrežja, brez čakanja.

**Kratka (12 besed, EN)**
> Your day on one screen. No account, no network, no waiting.

**Dolga (38 besed, SL)**
> LockIn je urnik za dijaka, ki zdrži tudi dan, ki ne gre po načrtu. Pouk, učenje in počitek so na
> eni časovnici; blok označiš, ko ga opraviš, zamujeno delo pa se prestavi, ne izbriše. Vse ostane na
> telefonu.

**Dolga (37 besed, EN)**
> LockIn is a timetable for a student, and it survives a day that does not go to plan. Lessons,
> study and rest share one timeline; you mark a block when you finish it, and late work is moved, not
> deleted. Everything stays on the phone.

### 1.2 Kaj od tega je res v prvih petih minutah

| Trditev | Kje jo človek vidi | Drži? |
| --- | --- | --- |
| Brez računa | Aplikacija se odpre na dnevu, prvi zagon vpraša samo ime (lahko prazno) in urnik pouka | drži |
| Brez omrežja | V `AndroidManifest.xml` sta `INTERNET` in `ACCESS_NETWORK_STATE` izrecno odstranjena (`tools:node="remove"`), tudi če bi ju knjižnica prinesla s sabo | drži |
| Takoj uporabno | Prazna namestitev pokaže kartico z vabilom; "Dodaj svoj prvi blok" odpre list s 45-minutnim blokom fokusa ob trenutni uri (N11) | drži |
| Zamujeno delo se prestavi, ne izbriše | Gumb "Uskladi zamudo" je viden le, kadar dan res zamuja (N1); preskočen blok ostane v razdelku "Odloženo" in se vrne z "Obnovi" (N6) | drži |
| Vse ostane na telefonu | Izvoz/varnostna kopija je datoteka JSON, ki jo shraniš sam; v oblaku ni ničesar | drži |
| Nič ne teče v ozadju | Ni storitve v ospredju, ni periodičnega preverjanja; opomniki so alarmi sistema (`AlarmManager` in `SCHEDULE_EXACT_ALARM`), torej ni procesa, ki bi tekel stalno | drži |

### 1.3 Kar sem črtal (najbolj uporaben del naloge)

Vsaka od teh trditev se sliši dobro in vsaka bi se v prvih petih minutah razkrila kot pretiravanje:

| Črtano | Zakaj ne zdrži |
| --- | --- |
| "Višje ocene" / "boljši uspeh v šoli" | Aplikacija meri tvoj načrt, ne tvojega znanja. Nobene povezave med uporabo in oceno ne moremo pokazati. |
| "V petih minutah boš produktiven" | Lahko obljubimo le, da je načrt na zaslonu; produktivnost ni naša lastnost. |
| "Vse v eni aplikaciji" | Ni e-pošte, koledarja, klepeta, skupinskih projektov. To je namerno in je prednost, ne pomanjkljivost. |
| "Umetna inteligenca ti sestavi urnik" | Aplikacija uporablja pravila, ki jih lahko prebereš v nastavitvah (elastičnost, prioriteta, meje dnevnega fokusa). Nobenega modela ni. |
| "Sinhronizira se med napravami" | Ni strežnika, ni računa, ni sinhronizacije. Varnostna kopija je datoteka, ki jo preneseš sam. |
| "Spremlja tvojo produktivnost" | Aplikacija nima analitike in ne pošilja podatkov. "Spremljanje" bi bilo laganje o tem, kar dela. |
| "Nadomesti učitelja / nadzor staršev" | Aplikacija je za dijaka samega. Nobenega poročanja tretjim osebam ni. |
| "Najboljša aplikacija za dijake" | Presežnik brez merila. Tudi z najboljšim namenom je to trditev, ki je ni mogoče preveriti. |
| "Deluje povsod" | Android, od različice 7 (API 24). iOS in splet ne obstajata. |

### 1.4 Za koga ta aplikacija NI

Za tistega, ki hoče seznam opravil s podopravili, podprojekti in ekipo: LockIn ima eno časovnico dneva in
naloge z rokom, ne drevesa. Za tistega, ki hoče skupinski koledar: ni računa in ni deljenja, zato tudi
ni vabila za sošolce. Za tistega, ki hoče merjenje produktivnosti in tedenske grafe svojega truda: v
aplikaciji ni analitike in je ne bo, ker ni omrežja. In za tistega, ki je na iPhonu ali hoče urejati
urnik na računalniku: obstaja samo Android, in to namerno — ena naprava je tisto, kar obljubo
"vse ostane pri tebi" naredi resnično.

### 1.5 Pet razlogov za namestitev, v prvi osebi

1. "Ko se zbudim, vidim, kaj je danes, in ne sprašujem, kje je tisti papir."
2. "Ko se ura premakne, blok premaknem z dvema dotikoma, namesto da bi pisal nov seznam."
3. "Za vsak blok, ki ga opravim, dobim kljukico — to je edina statistika, ki jo potrebujem."
4. "Vem, da noben podatek ne gre z mojega telefona, zato ga lahko uporabljam tudi za stvari, ki jih ne
   bi zaupal aplikaciji z računom."
5. "Ko dan propade, ga aplikacija ne posuje z ničlami — mirno mi pokaže, kaj je ostalo."

### 1.6 Tri trditve, ki so v trženju tega izdelka prepovedane

1. **"Zviša ocene" (ali katerikoli rezultat učenja).** Prepovedano zato, ker je edino, kar lahko
   dokažemo, to, da je načrt na zaslonu. Rezultat je odvisen od človeka.
2. **"Vse v enem" / "najboljša" / "#1".** Prepovedano zato, ker je aplikacija namerno ozka, presežnik
   pa je trditev brez merila, kar §6.8 izrecno prepoveduje.
3. **"Tvoji podatki so varni" brez besede "na tvojem telefonu".** Prepovedano zato, ker v oblaku ni
   varnostne kopije: če telefon izgubiš, urnika ne moremo vrniti. Stavek mora povedati tudi to ceno
   (glej `privacy_delete_warning`).

### 1.7 Pred trgovino: tri stvari, ki morajo biti narejene

1. **Podpis z izdajnim ključem.** Trenutni `app-release.apk` je podpisan z razvojnim ključem (tako ga
   je mogoče namestiti iz GitHub izdaje). Za trgovino potrebujeta ključ in `versionCode` svojo pot.
2. **Vsaj en teden na pravem telefonu.** Vse do zdaj je bilo preverjeno na emulatorju; naloga 05 v
   dokumentu analize zato ostaja odprta. Brez tega ne vemo, kako se obnaša baterija na pravem
   sistemu, ki aplikacije ustavlja drugače kot emulator.
3. **Zaslon za prvi zagon, ki pove ceno.** "Vse ostane na telefonu" in "če telefon izgubiš, urnika ne
   moremo vrniti" morata stati v istem stavku, preden človek vnese teden.

### 1.8 In tri stvari, ki pred trgovino NISO potrebne

1. **Sinhronizacija ali račun.** Brez njiju je obljuba o zasebnosti resnična in preverljiva.
2. **iOS ali spletna različica.** Ena naprava je lastnost izdelka, ne pomanjkljivost.
3. **Oblikovanje trženjskih materialov (logotip, animacije, posnetki).** Opis lahko napišeš v enem
   popoldnevu; dokler aplikacija ni preizkušena na pravem telefonu, je vsak posnetek zastarel.

---

## Del 2 — Pregled besedil (N20)

### 2.1 Kaj sem pregledal

* 777 nizov v `app/src/main/res/values/strings.xml`, vzporedno z `values-en`. Od teh jih je 100
  daljših od 60 znakov; **velika večina so pojasnila, opozorila in prazna stanja**, ki so po merilih
  §6 izvzeta (dolžina je tam cena za to, da ni ničle brez konteksta).
* Ločeno sem pregledal: naslove zaslonov in razdelkov, gumbe, sporočila po dejanju, opozorila o
  ravnotežju, prazna stanja in vse `contentDescription`, ki jih bere bralnik zaslona.
* Preverjeno je bilo tudi, da se število nizov v obeh jezikih ne razlikuje in da vsi specifikatorji
  ostajajo: `python3 tools/check_translations.py` (783 vira, oba jezika, enaka imena in oblike).

### 2.2 Pet sprememb, ki so bile res potrebne

| Niz | Prej | Zdaj | Zakaj (in kaj se poslabša) |
| --- | --- | --- | --- |
| `message_skipped` | "Odloženo. Obnoviš **jo** lahko pod časovnico." | "Odloženo. Obnoviš **ga** lahko pod časovnico." | Zaimka ni imel več predhodnika, ko je stavek nehal imenovati "pojavitev"; "blok" je moškega spola. Slovnica, ne okus. Cena: nič. |
| `tasks_clear_date` | "odstrani" / "remove" | "Odstrani datum" / "Remove the date" | To je `contentDescription` ikone X ob polju z datumom; bralnik zaslona je prebral "odstrani" brez povedka, kaj se odstrani. Cena: daljši niz, ki ga bralnik prebere v enem dihu. |
| `warning_daily` | "Uravnotežimo preostanek dneva" / "Let us balance the rest of the day" | "Dan je poln" / "A full day" | Štirje od petih naslovov opozoril o ravnotežju so samostalniške fraze ("Prostor za obnovo", "Čas za kratek odmor", "Priložnost za miren odmor", "Malo gibanja bo dobrodošlo"); ta eden je bil vzpodbuda "uravnotežimo". Zdaj je med njimi enak. Cena: manj spodbuden, bolj stvaren. |
| `warning_physical_body` | "…**Vstani, poglej v daljavo ali pojdi na kratek sprehod.**" | "…Kratek sprehod ali pogled v daljavo pomaga." | Trije velelniki v enem stavku so navodilo, ne predlog; vedenjska študija (in §6.4) hočeta mirno lestvico. Trditev pove isto, ne ukazuje. Cena: manj neposredno. |
| `warning_concentration_body` | "…**Poskusi** 10–15 minut mirnega odmora." | "…10–15 minut mirnega odmora običajno zadošča." | Isti razlog; poleg tega je "poskusi" nakazovalo, da bo odmor morda premalo — stavbo, ki jo povzroči neuspeh, aplikacija ne piše. Cena: izgubi se vabilo k poskusu. |

### 2.3 Kar sem namenoma pustil pri miru

| Niz | Zakaj ostane |
| --- | --- |
| `fast_add_title` = "Ustvari malo prostora." | Ponuja se popravek v samostalnik ("Nov blok"), a ta list je edini korak, kjer aplikacija govori v nedovršni obliki; tam je glas del izdelka. Ni ukaz, ni prazna fraza, zato ostane. |
| `warning_daily_body` (122 znakov) | Pojasnilo, izvzeto po merilih; vsebuje "Premisli o krajšanju ali prestavitvi dela" — blag predlog znotraj razlage, ne naslov. |
| `message_milestone_saved` (95 znakov) | Sporočilo po dejanju mora povedati, kaj se je zgodilo z opozorilom; krajšanje bi skrilo, da je priprava lahko v čakalni vrsti. |
| `settings_health_body` (229 znakov) | Najdaljši niz v aplikaciji in hkrati tisti, ki pojasni, od kod meje — brez njega bi bila števila v nastavitvah videti kot mnenje. |
| `timetable_import_hint` (210 znakov) | Navodilo za uvoz; dolžina je cena za to, da uporabnik ne prevzame napačnega vira. |
| `health_explanation`, `privacy_delete_warning`, `battery_note` | Vse tri so pojasnila ali opozorila; §6 jih izvzema, vsaka pa pove nekaj, kar bi bilo brez nje videti kot obljuba. |

### 2.4 Kaj je bilo videti kot problem, pa ni

* **Velelniki na gumbih** ("Dodaj", "Shrani", "Uskladi zamudo", "Obnovi", "Skrij za danes") — na gumbu
  je velelnik pravilen in ga merila §6 ne prepovedujejo; prepoved se nanaša na ukaze tam, kjer bi bil
  dovolj samostalnik (naslovi, stanja).
* **Ničle** — po N1/N2 jih ni več: prazna vrednost je pomišljaj v tihi barvi, "Opravljeno" pa "3 od 9".
* **Angleški nizi, ki pomenijo drugo kot slovenski** — pregledanih je bilo vseh pet spremenjenih nizov
  in noben se ne razhaja po pomenu; to preverjata `check_translations.py` in ročni pregled parov.

### 2.5 Preverjeno

* `python3 tools/check_translations.py` — zeleno (783 vira, enaka imena, enaki specifikatorji).
* `python3 tools/check_presentation.py` — zeleno (nobeno besedilo ni zapisano neposredno v kodi).
* Besedila iz tega pregleda so vključena v `values/` in `values-en/`; aplikacija sama ni dobila
  nobenega novega trženjskega besedila.
