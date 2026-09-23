# Naloga 01 — Barve: kontrast, ki ne odvzame značaja (N9)

## Namen

Trije pari v vmesniku so pod mejo, ki jo zahteva WCAG, in vsi trije se popravijo z vrednostjo, ne z
novo komponento. Zunanja študija dostopnosti je te številke navedla; izračun je bil ponovljen v
`docs/audits/2026-09-22-kriticna-analiza-studij-in-nacrt.md` (§3) in drži.

Cilj: **nobena sprememba naj ne spremeni videza znamke** (turkizna ostane `#2DD4BF`), le tri
podlage/lestvice, ki so danes premalo ločljive.

## Kaj popraviti

1. **Tiho besedilo.** `TextMuted` `#718096` → **`#8391A7`** (`DesignSystem.kt:34`).
   Danes 4,23:1 na kartici `#151C2E` (meja 4,5:1); po spremembi 5,31:1 na kartici in 6,08:1 na
   podlagi. Vtišina ostane, berljivost se vrne. Posledično se rahlo dvignejo tudi polnila, ki se
   izpeljejo iz te barve (`Fill…`, alfa 0,36/0,32/0,24/0,18) — preveri, da obrobe ostanejo mehke.
2. **Kartica proti podlagi.** `Surface1` `#151C2E` → **`#1A2238`** (`DesignSystem.kt:26`).
   Danes 1,14:1 (kartica se zlije s podlago), po spremembi 1,23:1; primarno besedilo 14,41:1,
   sekundarno 7,44:1. Vrednost mora ostati v koraku lestvice površin (preveri `SurfaceLow`,
   `Surface2`, `Surface3` in meje v `check_contrast.py`, ki zahtevajo stopničenje 1,04–1,20 med
   zaporednimi površinami).
3. **Meje aktivnih vnosnih polj.** V vseh spodnjih listih, kjer je polje interaktivno (vnos besedila,
   ure, minute), obroba naj bo **34 % bele** (`#656975` na kartici, ~3,10:1). Pasivne kartice in
   plošče obdržijo 10 %. Uporabi obstoječi `BorderStrong`, če že ima pravo vrednost; če ne, ga
   prilagodi in poskrbi, da se `CardBorder` (10 %) ne uporablja več na vnosnih poljih
   (`RoutineTimeField`, `OutlinedTextField` v `EntryEditorSheet`, `GoalsScreen` obrazci).
4. **Stikalo.** Beli drsnik na turkiznem tiru je 1,86:1 (meja 3:1). Najcenejša rešitev: tir v
   vklopljenem stanju **`#0D9488`** (`checkedTrackColor`), drsnik ostane bel (~3,1:1). Poišči, kje
   se v tem projektu nastavljajo barve stikal; če jih ni, jih določi **na enem mestu** (tema) in ne
   v vsaki uporabi posebej. Ne spreminjaj `Primary` — ta barva je znamka in je na temni podlagi
   odlična (10,44:1).

## Kaj je prepovedano

* Ne spreminjaj `Primary`, `Timer`, `FocusAccent`, `Success`, `Warning`, `Error` (znamka in
  pomenska lestvica).
* Ne uvajaj nove teme, novega stikala ali barvnega filtra.
* Ne popravljaj kontrasta z "malo večjo pisavo" — to je obhod, ne rešitev.

## Dokaz

* `python3 tools/derive_palette.py --check` — uskladi pin v `EXPECTED` (to je namerno: paleta je
  zaklenjena, da se ne "izboljša" po nesreči) in XML zrcalo `app/src/main/res/values/colors.xml`.
* `python3 tools/check_contrast.py` — dvigni mejo za `TextMuted` s 3,0 na **4,5** in dodaj tri
  nova pravila: tir stikala (3,0), meja aktivnega polja (3,0), kartica proti podlagi (1,2).
  Gate mora povedati, koliko preverjanj je opravil.
* `docs/PALETTE.md` — posodobi vrednosti in dodaj eno vrstico utemeljitve za vsako.
* Posnetki iz CI (opombe `UI screenshot`) — vizualna potrditev, da sprememba ni videti kot nova tema.

## Poročilo naj vsebuje

1. Stare in nove vrednosti ter izmerjena razmerja (pred/po).
2. Koliko mest je uporabljalo `CardBorder` na vnosnih poljih in kaj si zamenjal.
3. Kaj se je rahlo poslabšalo (npr. kartica je svetlejša, tiho besedilo je videti močnejše).
