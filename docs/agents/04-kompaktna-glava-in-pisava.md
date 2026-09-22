# Naloga 04 — Kompaktna glava, zavihki in pisava (N5, N10, N12, N13, N14)

## Namen

Pet majhnih popravkov, ki skupaj vrnejo prostor in red tam, kjer ga študije najbolj pogrešajo:
datum se lomi v siroto, zavihki se lomijo 3+2, tedenski pogled ima dve vrstici navodil, nastavitve
se odpirajo s manifestom o zasebnosti, najmanjša pisava pa je 12 sp — premalo za branje na soncu.

## Kaj popraviti

1. **N5 — datum v eni vrstici.** `RoutineDate.range` (`DisplayFormat.kt:157`) naj za razpon **v istem
   mesecu** vrne kratko obliko: "21.–27. sep" (nov niz `date_range_same_month` v obeh jezikih;
   angleško "21–27 Sep"). Če meseca ali leta nista ista, ostane obstoječi `date_range`.
   Cilj: naslov v vrstici z datumom se pri širini 360 dp in pisavi 16 sp **ne prelomi v dve vrstici**.
2. **N10 — zavihki brez lomljenja 3+2.** `CategoryTabs` (`RoutineText.kt:345`) naj namesto `FlowRow`
   uporabi **eno vodoravno drsno vrstico** čipov z mehkim prelivom na robu (preliv se izriše le, kadar
   je kaj za robom). Zadnji zavihek sme biti skrit za robom, a mora biti to videti (preliv), ne
   izgledati kot odrezan čip. Testne oznake (`tagPrefix-...`) in vrstni red ostanejo nespremenjeni.
   Velja za nastavitve, cilje in načrtovalnik.
3. **N12 — teden brez stalnih navodil.** Vrstica `week_hint` se umakne iz `OverviewScreens.kt:115-121`.
   Namesto nje se ob **prvem** obisku tedenskega pogleda v isti vrstici, kjer je bil povzetek, pokaže
   eno vrstico ("Mrežo pomakneš vodoravno." / "Swipe the grid sideways."), ki ob naslednjem obisku
   izgine. Shranjevanje "prvega obiska" naj bo v obstoječih nastavitvah (`DataStorePreferencesRepository`),
   ne v novi podatkovni bazi.
4. **N13 — nastavitve brez manifesta.** V `SettingsSheet.kt:127` je podnaslov lista `privacy_summary`
   ("Brez povezave. Brez računa…"). Ta stavek se preseli v zavihek Podatki (tam je že smiselno mesto),
   glava lista pa obdrži eno samo vrstico podnaslova ali nobene. Cilj: zavihki so vidni brez drsenja
   na zaslonu višine 640 dp.
5. **N14 — pisava 13 sp.** Vsi časovni nizi (ura v stolpcu tedna, `TimeGutter`, vrstica
   "8:20–9:05 · 45 min") dobijo spodnjo mejo **13 sp** in težo **Medium 500**. Uporabi obstoječi
   `RoutineLabel` s samodejnim krčenjem (ne uvajaj nove pisave), spodnjo mejo pa nastavi
   `RoutineTextDefaults.MinLabelSize` ali posebej za ta mesta (odločitev utemelji v komentarju).
   `LargeFontUiTest` (1,45×) mora ostati zelen.

## Kaj je prepovedano

* Ne skrivaj funkcionalnosti za "manj kaosa": če se nekaj umakne iz vidnega polja (npr. zasebnost),
  mora biti še vedno dosegljivo — samo ne na prvem zaslonu.
* Ne uvajaj horizontalnega drsenja za celotno stran in ne skrivaj zavihkov za meni "več".
* Ne dodajaj animacije za preliv roba; statični gradient zadostuje.
* Ne spreminjaj razmerij pisave (naslov 34 sp ostane).

## Dokaz

* Enotni test za `RoutineDate.range` (isti mesec / različna meseca / različni leti) v
  `app/src/test` — isti vzorec kot obstoječi testi `DisplayFormat`.
* `python3 tools/check_translations.py` — nova niza v obeh jezikih z istimi specifikatorji.
* Roka preveritev na posnetku iz CI: vrstica z datumom je enovrstična; zavihki nastavitev so v eni
  vrstici; teden se začne z mrežo, ne z navodili.
* `LargeFontUiTest` še zelen (pisava 13 sp ne sme povzročiti prekrivanja).

## Poročilo naj vsebuje

1. Koliko dp višine je pridobil tedenski pogled in koliko mesečni/dnevni.
2. Kako je shranjen "prvi obisk" tedna in kaj se zgodi po ponovni namestitvi.
3. Kaj se poslabša (večja pisava pomeni manj časa v stolpcu tedna; skrit zadnji zavihek je manj
   očiten, dokler se vrstica ne drsne).
