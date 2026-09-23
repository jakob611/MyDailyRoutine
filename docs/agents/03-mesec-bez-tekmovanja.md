# Naloga 03 — Mesec: količina, berljiva brez barve (N7, N8)

## Namen

Mesečni pogled danes kodira **količino** načrtovanega fokusa z jakostjo barve
(`OverviewScreens.kt:330`: `alpha = 0.08 + heat * 0.35`). To je serija (streak) brez besede "serija":
dan z manj načrtovanega dela je videti manjvreden. Prav tako sta **Test in Rok označena z isto
barvo in isto piko** (`OverviewScreens.kt:363-365`) — nedvoumna napaka tudi za tiste z običajnim
vidom, pri barvni slepoti pa oba izgineta.

Cilj: količino povedo **oblike** (berljive brez barve), barva pa pomen (vrsta dneva).

## Kaj popraviti

1. **Tri diskretne stopnje namesto zvezne jakosti.** Barva celice naj opisuje vrsto dneva
   (pouk / prost dan / dan z načrtovanim fokusom), količino pa naj nosijo do tri vodoravne črtice
   pod številko datuma (0, 1, 2, 3), kjer vsaka črtica pomeni do ~1 ure načrtovanega fokusa
   (prilagodi meje, da se ujemajo z lestvico, ki jo že uporablja legenda).
   Pravilo: **noben dan ni obarvan močneje samo zato, ker je bolj poln.**
2. **Oblike za pomene.** V mreži in v legendi:
   * Test = polni trikotnik,
   * Rok = polni diamant,
   * prost dan = obroč (prazen krogec),
   * dan z načrtovanim fokusom = polna pika.
   Barve ostanejo (`Error` za test in rok, `Success` za prost dan, `Primary` za fokus), a **nikoli
   same**.
3. **Legenda sledi obliki.** `Legend()` v `SummaryComponents.kt:209` dobi parameter za obliko
   (pika/trikotnik/diamant/obroč), da mreža in legenda ne moreta več govoriti različno.
4. **Opis za bralnik zaslona** (`month_cell_description`) mora povedati tudi količino in vrsto, ne
   samo števil (npr. "22. september, 2 uri fokusa, test"). Pravilo: kar je vidno v obliki, mora biti
   povedano v besedi.
5. **Pravica do praznega dne.** Dnevi brez vsebine ostanejo vizualno mirni — ne dobijo obrobe, sence
   ali opozorilne barve.

## Kaj je prepovedano

* Ne dodajaj novih barv; uporabi obstoječe pomenske (`Error`, `Success`, `Primary`, `Warning`).
* Ne uvajaj interaktivnega filtra ali preklopa "pokaži količino" — pravilo mora veljati zmeraj.
* Ne spreminjaj velikosti celice ali razporeditve tednov (`aspectRatioCell`, `Row` s 7 stolpci).
* Ne krepaj animacij; mesec je pregled, ne igra.

## Dokaz

* Nov test v `AccessibilityTest.kt` ali `TimelineUiTest.kt`: dva dneva z **različno** količino fokusa
  imata **enako** barvo ozadja, razlikujeta pa se v številu oznak (berljivo brez barve).
* Test, da test in rok nista opisana z isto obliko (npr. število različnih oblik v mreži je vsaj 2,
  ko sta prisotna oba).
* Roka preveritev: posnetek meseca iz CI — oko naj ne bi več iskalo "najtemnejšega" dneva.
* Brez regresije pri barvni slepoti: opis za bralnik zaslona vsebuje količino in vrsto.

## Poročilo naj vsebuje

1. Kakšna je nova lestvica stopenj in kako se ujema z mejo "300 minut" iz prejšnje različice.
2. Kako sta oblike narisani (Compose `Canvas` ali `Icon`), in koliko dp so velike.
3. Kaj se poslabša (npr. mreža je videti bolj "tehnična"; zvezna jakost je bila na prvi pogled
   hitrejša za branje).
