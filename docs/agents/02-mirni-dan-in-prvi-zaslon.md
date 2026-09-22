# Naloga 02 — Mirni dan in prvi zaslon (N1, N2, N11)

## Namen

Najbolj obremenjen zaslon v aplikaciji je **dnevni povzetek**. Danes, na praznem ali urejenem dnevu,
pokaže tri ničle, "Na voljo: 0 min rezerve", dva gumba ("Uskladi zamudo", "Čakalna vrsta: 0") in
odstavek o zamudi — vse pred časovnico. To je prvi zaslon po namestitvi in v vseh zunanjih študijah
opisano kot najslabše mesto izdelka.

Cilj: **nobena ničla brez konteksta, noben ukaz brez razloga**. Če ni česa uskladiti, tega ne
pokažemo.

## Kaj popraviti

1. **Prazen dan naj ne kaže povzetka.** V `DailyTimeline.kt` (item `key = "summary"`, ~vrstica 112) se
   ob `day.items.isEmpty()` izriše **samo naslov** `day_heading` (naslov je hkrati a11y `heading` in
   ga pričakujejo testi). Ploščice, `DayLoadBar`, vrstica rezerve, gumbi in odstavek se ne izrišejo.
   Kartica praznega dneva (`item(key = "empty")`, ~vrstica 268) že obstaja in ostane edini vsebinski
   element. Ne podvajaj njenega besedila.
2. **Vrstica rezerve samo, kadar je rezerva resnična.** Pokazati jo je treba le, če
   `day.metrics.reserveMinutes > 0` ali če so `day.warnings` neprazni. Vrstica "Na voljo: 0 min
   rezerve" v zeleni barvi je najslabša možna oblika ničle.
3. **Zdravljenje zamude samo, kadar je dan res zamujen.** Gumb "Uskladi zamudo", gumb
   "Čakalna vrsta: N" in odstavek `auto_heal_hint` se izrišejo le, ko velja:
   * dan je današnji in obstaja blok, ki je `!isCompleted && !isSuppressed` in se je v preteklosti
     končal (`endMinute <= nowMinute`), **ali**
   * `backlogCount > 0`.
   Za pretekle dni te vrstice ni (gumb je tam tako ali tako onemogočen).
4. **Ploščice brez kvote in brez ničel.** `MetricTile` (`SummaryComponents.kt:184`) dobi parameter
   za barvo vrednosti. V `DailyTimeline` velja:
   * `Fokus` in `Počitek`: kadar je vrednost 0, pokaži pomišljaj (nov niz `value_none`, v obeh
     jezikih) v `RoutineColors.TextMuted`; sicer običajna barva.
   * `Opravljeno`: niz `completed_count` se spremeni iz `%1$d/%2$d` v "**%1$d od %2$d**"
     (angleško "%1$d of %2$d") — ulomek brez besede je bil najslabši del. Barva: `TextMuted`, kadar
     ni nič opravljeno; `Success`, kadar je opravljeno vse (in `blockCount > 0`); sicer `TextPrimary`.
   * Kadar je `blockCount == 0` (dan ima samo mejnike), tudi `Opravljeno` pokaže pomišljaj.
5. **Barva ploščic ostane tiha.** `MetricTile` ohrani `Surface1` in `CardBorder`; ne dodajaj ikon,
   grafik ali animacij.

## Kaj je prepovedano

* Ne skrivaj povzetka, kadar dan **ima** bloke — tam so številke informacija.
* Ne briši sposobnosti uskladitve: na zamujenem dnevu mora ostati popolnoma enaka pot.
* Ne odpiraj vprašanja prvega zagona (to je že rešeno v `OnboardingScreen.kt`); tukaj gre samo za
  stanje zaslona.
* Ne spreminjaj `TimelineAction.AutoHeal` v modelu.

## Dokaz

* Nov test v `TimelineUiTest.kt` (ali `AccessibilityTest.kt`): na praznem dnevu
  (`R.string.empty_day_title` je viden) **ne** sme biti vozlišča z `R.string.auto_heal` niti z
  `R.string.reserve_remaining`.
* `AccessibilityTest.aNumberIsAnnouncedWithTheLabelItBelongsTo` mora ostati zelen — ploščice še
  vedno povedo svojo vrednost, tudi kadar je ta pomišljaj.
* Obstoječi testi, ki čakajo `day_heading`, morajo ostati zeleni.
* Roka preveritev: na posnetku iz CI na praznem dnevu ni več niti ene ničle v povzetku.

## Poročilo naj vsebuje

1. Koliko vrstic je izginilo s praznega dneva in koliko z urejenega.
2. Kaj se poslabša (na urejenem dnevu ni več stalne bližnjice za uskladitev — tam ni česa
   uskladiti).
3. Ali je `value_none` res v obeh jezikih (pomišljaj je v obeh enak; to je dovoljeno).
