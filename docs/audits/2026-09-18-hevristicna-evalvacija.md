# Hevristična evalvacija (korak 1 deep research)

Datum: 2026-09-18 · Metoda: Nielsenovih 10 hevristik + 3 mobilna pravila (44 pt tarče, ena primarna
akcija na zaslon, berljivost nad efektom) + Apple HIG / Material 3 smernice · Pregledane površine:
časovnica (4 lestvice), list dodajanja/urejanja, urejevalnik pojavitve, načrtovanje, cilji, naloge,
nastavitve (5 zavihkov), izvedba, widget, obvestila.
Resnost: 0 = ni težave, 1 = kozmetika, 2 = manjša uporabnostna težava, 3 = resna težava, 4 = blokada.

## Ugotovitve po hevristikah

| # | Hevristika | Zaslon / površina | Ugotovitev (dejstva iz kode) | Resnost |
|---|---|---|---|---|
| 1 | Vidnost stanja sistema | celotna app | `isSaving` onemogoči kontrole med shranjevanjem; NowBand pulse kaže živo uro; snackbar za izide; med samodejnim celjenjem ni vidnega indikatorja (slepo čakanje) | 1 |
| 2 | Match z realnim svetom | načrtovanje, nastavitve | žargon: »backlog«, »avtomatsko celjenje«, »rezerva«, »buffer« — dijaški mentalni model pozna »čakalnik«, »premik«, »nadomestna ura«; kategorije (šola/fokus/okrevanje) pa so dobro poimenovane | 2 |
| 3 | Nadzor in svoboda | celotna app | back sklad lestvic + goals + sheeti; brisanja z potrditvenim dialogom; Skip/Restore/ResetOverride za pojavitve; **ni razveljavitve po brisanju** (samo potrditev pred) | 1 |
| 4 | Konsistentnost | urejevalnika blokov | **dva različna urejevalnika istega objekta**: EntryEditorSheet (osnutek: kategorija, vzorec, opomnik, advanced) in BlockEditorSheet (pojavitev: samo naslov + čas + obseg serije). Ista stvar, dva jezika. | 3 |
| 4b | Konsistentnost | nastavitve | **mešan model shranjevanja**: stikala se shranijo takoj, skupine polj (spanje, šolsko okno, zdravje, periodični odmor, privzete ure) pa zahtebajo ločen gumb Shrani. Uporabnik ne ve, kdaj je kaj zavezujoče — verjeten izvor pritožbe »shranjevanje ne dela«. | 3 |
| 5 | Preprečevanje napak | vnosi | kolo za uro ne more proizvesti neveljavne ure; inline validacija pod polji; čipi za jutranjo rezervo; brisanja s potrditvijo | 0 |
| 6 | Prepoznavanje namesto spominjanja | dodajanje bloka | hitri vnosi (standardni + predmetni) ✓; ikone kategorij ✓; **vzorci (repeat + opomnik) niso del hitrih vnosov**, zato se ponavljajoča ura sestavlja ročno | 2 |
| 7 | Fleksibilnost in pospeševalniki | celotna app | FAB, widget z deep-linkom na dan, share-sheet za nalogo, timetable-import za urnik; **ni podvojitve bloka** (»še ena enaka ura drugje«) | 1 |
| 8 | Estetski minimum | dan, nastavitve | dnevna časovnica je strukturirana (spine, welli, zdaj-pass); nastavitve imajo **5 zavihkov z dolgimi scrolli** — meje priporočila »omeji število zavihkov na mobilnem« | 2 |
| 9 | Prepoznavanje in okrevanje iz napak | vnosi | inline napake + sporočila (error_values, error_save, conflict reasoni); haptični warning; kolo je napako tipkanja odpravilo v celoti | 0 |
| 10 | Pomoč in dokumentacija | celotna app | hinti pod skoraj vsako sekcijo (sleep_hint, month_hint, week_hint …); ni onboarding vodnika (sprejemljivo za osebni planer) | 1 |
| M1 | 44 pt tarče | celotna app | RoutineMetrics + chip/vrstice ≥ 40 dp, gumbi 56 dp; wheel vrstice 44 dp | 0 |
| M2 | Ena primarna akcija na zaslon | dan, sheeti | dnevni zaslon: FAB + 4 ikone v vrstici (nastavitve, načrtovanje, naloge, cilji) — pet konkurirajočih vhodov; sheeti imajo jasen footer z eno primarno akcijo ✓ | 2 |
| M3 | Berljivost nad efektom | steklo | gate 197 parov ≥ 4.5:1; tint 0.45/0.80; wash nad refrakcijo (FAB popravljen po povratni informaciji) | 0 |

## Povzetek
- 0 blokad, 3 resne težave (dva urejevalnika, mešan model shranjevanja, žargon + število vhodov na dnevu),
  ostalo manjše ali kozmetika.
- Najmočnejši strani: preprečevanje napak (kolo, inline validacija), konsistentna barvna in steklena
  govorica, deep-linki (widget, share), hinti.
- Najšibkejši strani: **konsistentnost modelov** (urejevalnika, shranjevanje) in **gostota vhodov**
  na dnevnem zaslonu.

Nadaljevanje: `2026-09-18-sprehodi-opravil.md` (korak 2).
