# Naloga 05 — Vedenjski protokol: minimalno stanje in nevtralna odložitev (N15, N6)

## Namen

Dve spremembi, ki ju predlaga vedenjska študija, a z zavestno popravljenim pravilom: aplikacija
**nikoli sama ne briše uporabnikovih načrtov**, lahko pa preneha pritiskati.

Prva: kadar dan razpade (dva ali več blokov zaporedoma preskočenih, ura je pozna), sistem **ne**
porine vsega neopravljenega v večer — tam bi nastalo kopičenje, ki uporabnika odžene. Druga: preskočen
blok ni kazen in ne sme biti obarvan kot opozorilo.

## Kaj popraviti

1. **Protokol minimalnega stanja** (N15). V `RoutineViewModel` (ali tam, kjer se sestavi dnevni plan)
   se doda stanje `minimalMode`, ki se vključi, ko velja **oboje**:
   * vsaj **dva zaporedna** bloka, ki sta se v preteklosti končala in nista bila opravljena niti
     preskočena z dotikom, in
   * ura je **po 18. uri**.

   Ob vklopljenem stanju:
   * prožni (`elasticity`) in ne-nujni bloki za ta dan se **skrijejo** s časovnice (ne premaknejo),
   * časovnica večera ostane mirna, v povzetek pa se doda **ena** vrstica: "Za danes je to dovolj."
     z dejanjem "Pokaži vse", ki stanje razveljavi (in to odločitev uporabnika **zapomni za tisti dan**),
   * **fiksne obveznosti in načrtovani počitek se nikoli ne dotaknejo**,
   * če je jutri test ali rok, sistem ohrani **eno** sidro (največ 30 minut) ob uri, ki je zanj
     običajna (uporabi obstoječe podatke o zgodovini, ne ugibaj).
2. **Nevtralna odložitev** (N6). Preskočen blok:
   * nima barve `Warning` ali `Error`; besedilo je "Odloženo" (`skipped_*` nizi),
   * gumb ostane "Obnovi",
   * po 20. uri se v dnevnem povzetku ponudi **ena vrstica** z možnostjo "Skrij za danes" — nikoli
     samodejni izbris, nikoli opozorilo.
3. **Brez novih podatkov.** Stanje naj se izpelje iz obstoječih podatkov (bloki, njihov status, ura).
   Ne dodajaj tabele, ne dodajaj poizvedbe, ne vodi zgodovine.

## Kaj je prepovedano

* Samodejno brisanje, samodejno premeščanje v večer, samodejno skrivanje brez možnosti vrnitve.
* Nova podatkovna tabela ali novo polje v bazi (Room shema je zaklenjena; `check_sqlite_integrity.py`
  bi padel).
* Kakršno koli besedilo, ki omenja neuspeh, krivdo ali zamudo v slabi luči.
* Obvestila: protokol ne pošilja ničesar.

## Dokaz

* Enotni test v `app/src/test` (`RoutineViewModel` ali čista funkcija odločitve): dan z dvema
  preskočenima blokoma ob 19. uri → `minimalMode` vklopljen in seznam vsebuje samo ne-nujne bloke;
  fiksni bloki ostanejo; ob 15. uri se ne vklopi; z enim preskočenim blokom se ne vklopi.
* Test za "Pokaži vse": po razveljavitvi ostane stanje razveljavljeno tudi po ponovnem izračunu.
* Zaslonski test: preskočen blok nima barve `Warning`/`Error` (primerjaj barvo z drugimi bloki).
* Roka preveritev: posnetek večernega dneva iz CI — časovnica je videti mirna, ena vrstica pojasni.

## Poročilo naj vsebuje

1. Kje se stanje izračuna in kako se shrani odločitev "Pokaži vse" za tisti dan.
2. Kaj se poslabša (uporabnik lahko "izgubi" prožne bloke iz pogleda, če ne pritisne "Pokaži vse";
   zato je vrstica obvezna).
3. Zakaj protokol ne sme teči ponoči in ne v prihodnjih dneh.
