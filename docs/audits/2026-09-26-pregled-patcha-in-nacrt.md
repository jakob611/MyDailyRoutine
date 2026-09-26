# Pregled patcha prejšnjega agenta in načrt za nadaljevanje

*26. 9. 2026 · predmet pregleda: `01a0d907-2b73-7e70-a6c5-703418029691.patch` (30 datotek, 1778 vrstic diffa,
27 spremenjenih + 3 nove datoteke, +514 / −204)*

---

## 1. Povzetek v petih stavkih

1. **Patch ni apliciran.** Na `master` je commitana samo *datoteka* patcha; delovno drevo je v stanju pred njim.
2. **Nič ni bilo pushano.** Veja `arena/01a0d907-mydailyroutine` na GitHubu obstaja, a stoji na `13f2695` = merge PR #11,
   torej na starem masterju. Edini nosilec dela je res ta patch.
3. **Patch se čisto aplicira** (`git apply --check` → 0) in **vse štiri statične strehe gredo skozi**
   (prevodi, predstavitev, kontrast, SQLite).
4. **Vendar se ne prevede.** Našel sem **6 prevajalnih napak v 3 datotekah** — CI bi padel v prvem koraku
   `Build, unit tests and lint`. Nobena od njih ni bila ujeta, ker prejšnji agent (kot tudi jaz) nima JDK/Android SDK.
5. Poleg tega je **7 logičnih napak**, od katerih dve neposredno lomita funkcijo, ki jo patch oglašuje
   (»jezik velja takoj«) in eno, ki se pokaže šele v angleščini (»Prostor za oddih« ostane prazen).

Serija 1 (jeziki) in serija 2 (barvni krog, stekleni gumbi) sta **vsebinsko dobro zastavljeni in ~90 % narejeni**.
Serija 3 (oblika gumbov pri počitnicah, stekleni spodnji listi z rahlim blurom) **ni začeta**.

---

## 2. Kaj sem preveril in česa nisem mogel

| Preverjeno | Kako | Rezultat |
|---|---|---|
| Aplikabilnost patcha | `git apply --check` | ✅ čisto |
| Statične strehe | vse 4 skripte na apliciranem drevesu v `/tmp` | ✅ 781 virov, 775 nizov, 109 kontrastnih preverb, 19 SQLite testov |
| Odstranjeni importi, ki so še v rabi | lasten skript čez cel patch | ❌ 1 najdba |
| Obstoj vsakega simbola, ki ga nova koda kliče | `grep` po repozitoriju + preverba Compose API na spletu | ❌ 4 najdbe |
| Tipi v novi kodi | ročno branje | ❌ 1 najdba |
| Sintaksa | ročno branje | ❌ 1 najdba |
| Klicna mesta spremenjenih podpisov | `grep` (`parse(`, `OnboardingScreen(`, `PreferencesRepository`) | ✅ vsa posodobljena |
| Vedenje ob preklopu jezika | branje toka `Action → Effect → recreate()` | ❌ 3 najdbe |
| **Prevajanje** | **ni mogoče** — v peskovniku ni `java`, `gradle`, `ANDROID_HOME` | ⛔ |

Zadnja vrstica je bistvena: **edini prevajalnik tega projekta je GitHub CI.** Vsak obrat, ki se konča brez pusha,
se konča brez dokaza, da se koda prevede. To je sistemski vzrok za vseh 6 napak spodaj.

---

## 3. Kaj je narejeno (in dobro)

### Serija 1 — dva polna jezika in preklop

| Del | Stanje |
|---|---|
| `AppLanguage` v domeni (`sl`/`en`/`normalize`) | ✅ čisto, validacija na robu domene |
| `SchedulePreferences.appLanguage` + `PreferencesRepository.setAppLanguage` | ✅ vključno s testnim dvojnikom v `ConnectedFeaturesTest` |
| DataStore ključ + `readPersistedAppLanguage` | ✅ (glej pa L6 spodaj) |
| `RoutineLocale.userChoice` (`@Volatile` zrcalo za `attachBaseContext`) | ✅ zamisel je pravilna |
| `uiLocaleFor(deviceLanguage, userChoice)` | ✅ pravilo »izbira > naprava > slovenščina« |
| `AppGraph` gradi bazo/sejalnik/obvestila na `withRoutineLocale()` | ✅ dobro opažena past |
| `LanguageSelector` (3 chipi, oznake v svojem jeziku) | ✅ prava konvencija |
| Vgradnja v nastavitve (vrh zavihka Ritem) in onboarding | ✅ postavitev, ❌ vedenje v onboardingu (L1) |
| `DisplayFormat` z živim `interfaceLocale` + `FormatCache` | ✅ prava rešitev za menjavo jezika brez restarta procesa |
| `ShareTextParser.parse(text, fallbackTitle)` | ✅ parser ostane čist JVM, niz pride od klicatelja |
| `RoomPlanningRepository` uporablja `uiLocale()` namesto trdega `sl` | ✅ |
| Novi nizi sl + en, `check_translations.py` | ✅ pariteta drži |
| README | ✅ pošteno in natančno opisano |

### Serija 2 — vizualni izbirnik barv in stekleni gumbi

| Del | Stanje |
|---|---|
| `SubjectColorPicker` (HSV krog + slider za jasnoto + predogled + hex odčitek) | ⚠️ zasnova ✅, izvedba se **ne prevede** (P3–P6) |
| Krog kot **enkrat izrisan bitmap** namesto Canvasa na okvir | ✅ prava odločitev (a glej L7) |
| Hex polje odstranjeno iz `SubjectEditorDialog` | ✅ |
| Stekleni »otoček« pod gumbi odstranjen (`GlassSheetFooter` izbrisan, nič ne ostane viseče) | ✅ dosledno |
| `SheetPrimaryButton` / `SheetSecondaryButton` sta **sama** steklo | ✅ velja za vseh 26 klicnih mest v 9 listih |
| `LocalSheetBackdrop` — gumb v listu vzorči list, ne okna za njim | ✅ pravi popravek pravega problema |
| `quickPresets` zavit v `remember` | ✅ (majhen, a pravi prispevek k tekočnosti) |

Kontrast novega primarnega gumba sem preračunal ročno: turkiz `#2DD4BF` pri 68 % nad temnim ozadjem da
≈ `rgb(34,148,137)`, proti temnemu inku `#090D16` je to **5,2 : 1** — nad pragom 4,5 : 1. Streha to potrjuje.

---

## 4. Kaj NI narejeno

* **Serija 3 v celoti.** V patchu ni niti ene vrstice v `OverviewScreens.kt` (gumbi pri počitnicah v letnem pogledu)
  niti v `RoutineSheet.kt` / drugih listih (liquid glass + rahel blur ozadja).
* **Push, PR, CI, link do release variante.** Nič od tega. Zadnji zeleni build je še iz PR #11.
* **Testi za novi funkciji.** `settings-language-*` in `onboarding-language-*` testne oznake obstajajo, a jih
  noben test ne uporablja. Barvni krog nima ne testa ne posnetka.
* **Razbitje na dva commita** (serija 1 / serija 2), ki ga je prejšnji agent načel, a ni dokončal.

---

## 5. Napake

### 5.1 Prevajalne — build pade (P1–P6)

> Vse te so v patchu, ne v obstoječi kodi. Vsaka sama zase ustavi `:app:compileDebugKotlin` ali `:app:compileDebugUnitTestKotlin`.

**P1 — `RoutineText.kt`: odstranjen import, ki je še v rabi**

Patch briše `import androidx.compose.animation.core.animateFloatAsState`, ker je pospravljal uvoze za stare
`Button`/`OutlinedButton`. Funkcija pa je še vedno uporabljena v `CollapsibleSection` (vrstica 319, rotacija ševrona).

```
Unresolved reference: animateFloatAsState
```

*Popravek:* import vrni. (Ostale štiri odstranitve — `MutableInteractionSource`, `collectIsPressedAsState`,
`AppleMotion`, `glassTouchSpec` — so pravilne; preveril sem vsako.)

---

**P2 — `LocaleChoiceTest.kt:25`: sintaktična napaka**

```kotlin
// patch:
assertEquals("sl", uiLocaleFor("")).language)   // ← oklepaj se je premaknil
// pravilno:
assertEquals("sl", uiLocaleFor("").language)
```

Izvorni set `test` se ne prevede → `./gradlew test` pade, preden sploh kaj požene.

---

**P3 — `SubjectColorPicker.kt:82, 187`: `Color.hsv` kot lastnost ne obstaja**

```kotlin
val hsv = remember(customColor) { Color(customColor.toInt()).hsv }
```

`androidx.compose.ui.graphics.Color` ima **tovarno** `Color.hsv(h, s, v)` (companion), nima pa lastnosti `.hsv`.
Pretvorba v drugo smer je v Androidu `android.graphics.Color.colorToHSV(argb, FloatArray(3))`.

*Popravek:*

```kotlin
import android.graphics.Color as AndroidColor

/** HSV [hue, saturation, value] barve, shranjene kot ARGB long. */
private fun hsvOf(argb: Long): FloatArray =
    FloatArray(3).also { AndroidColor.colorToHSV(argb.toInt(), it) }
```

---

**P4 — `SubjectColorPicker.kt:130, 198`: `Color.toLong()` ne obstaja**

```kotlin
onColor(Color.hsv(hue, saturation, value).toLong())
```

`Color` je `value class` nad `ULong`; javni API pozna `value: ULong`, `toArgb(): Int` in `toColorLong(): Long`.
Zadnji je **drug format** (Androidov 64-bitni ColorLong s half-float komponentami), ne `0xAARRGGBB`, ki ga
hrani ta aplikacija. Tudi če bi se prevedlo, bi shranilo napačno število.

*Popravek* — in maska je tu bistvena, ker so vse barve v `SubjectPalette.swatches` **pozitivni** longi (`0xFF67E8F9L`),
`toArgb().toLong()` pa da negativnega:

```kotlin
/** Barva v isti obliki, kot jo hrani baza in polica: pozitiven 0xAARRGGBB. */
private fun Color.toColorValue(): Long = toArgb().toLong() and 0xFFFFFFFFL
```

Brez maske bi primerjava `color !in RoutineColors.subjectSwatches` in shranjena vrednost v bazi zdrsnili narazen.

---

**P5 — `SubjectColorPicker.kt:152`: tip `android.graphics.ImageBitmap` ne obstaja**

```kotlin
private fun buildWheelBitmap(): android.graphics.ImageBitmap
```

`ImageBitmap` je `androidx.compose.ui.graphics.ImageBitmap`; v `android.graphics` živi `Bitmap`.
Telo funkcije vrača pravilno stvar (`asImageBitmap()`), le deklaracija je napačna.

---

**P6 — `SubjectColorPicker.kt:233–234`: `Double` v `Float`**

```kotlin
val angle = Math.toRadians(hsv[0].toDouble())     // Double
translationX = center + radius * cos(angle) - thumbPx / 2   // Float * Double → Double
```

`GraphicsLayerScope.translationX` je `Float`.

*Popravek:* `val angle = Math.toRadians(hsv[0].toDouble()).toFloat()` (in `thumbPx / 2f`) — `kotlin.math.cos(Float)`
vrne `Float`, tip se izide.

---

### 5.2 Logične — prevede se, obnaša pa se narobe (L1–L7)

**L1 — Preklop jezika v onboardingu ne naredi ničesar vidnega** *(resno: lomi obljubo iz lastnega niza)*

`RoutineApp` ima onboarding v zgodnji veji:

```kotlin
if (!state.preferences.onboardingDone) { OnboardingScreen(...); return }   // vrstica 236–246
...
LaunchedEffect(...) { viewModel.effects.collect { ... } }                   // vrstica 254 ← za return
```

Med onboardingom torej **nihče ne posluša učinkov**. `TimelineEffect.RestartForLocale` gre v
`Channel(Channel.BUFFERED)` in tam čaka. Posledici:

* chip v onboardingu shrani izbiro, jezik zaslona pa se **ne spremeni** — niz pa pravi
  »Velja takoj« / »It applies at once«;
* ko uporabnik onboarding konča, se zbiralnik zažene, prevzame odloženi učinek in **nepričakovano
  recreate-a aktivnost** sredi prvega vtisa.

*Popravek:* zbiralnik učinkov premakni **nad** zgodnji `return` (ali onboarding zavij v isto vejo, ki ima
zbiralnik). Najmanj invazivno: `LaunchedEffect` za učinke naj bo prvi v `RoutineApp`, pred kakršnimkoli `return`.

---

**L2 — »Kot naprava« se po prvi ročni izbiri pokvari**

```kotlin
fun uiLocale(device: LocaleList = LocaleList.getDefault()) = uiLocaleFor(device[0].language)

fun applyLocaleToProcessDefaults() {
    val locale = uiLocale()
    Locale.setDefault(locale)
    LocaleList.setDefault(LocaleList(locale))   // ← prepiše prav tisti vir, ki ga zgornja vrstica bere
}
```

Scenarij, ki ni eksotičen: angleški telefon → uporabnik izbere slovenščino → `setDefault(sl)` →
uporabnik se premisli in izbere »Kot naprava« → `userChoice = null` → `uiLocale()` prebere
`LocaleList.getDefault()`, ki je zdaj `[sl]` → aplikacija ostane **slovenska**, dokler se proces ne ubije.

*Popravek:* jezik naprave beri iz vira, ki ga aplikacijski `setDefault` ne doseže:

```kotlin
/** Jezik naprave, neodvisen od tega, kaj je aplikacija nastavila kot proces-privzeto. */
private val deviceLocales: LocaleList get() = Resources.getSystem().configuration.locales

fun uiLocale(device: LocaleList = deviceLocales): Locale = uiLocaleFor(device[0].language)
```

---

**L3 — Application sam teče v napačnem jeziku**

`RoutineApplication.attachBaseContext` se izvede **pred** `onCreate`, zrcalo `RoutineLocale.userChoice` pa se
napolni šele v `onCreate`. Bazni kontekst aplikacije je torej vedno razrešen po napravi, ne po izbiri.
Za zdaj to ne pokvari ničesar, ker `AppGraph` vsem storitvam podaja svoj `localized` kontekst — je pa past:
prvi `applicationContext.getString(...)`, ki ga kdo napiše, bo v napačnem jeziku, in razlog bo neviden.

*Popravek:* zrcalo napolni v `attachBaseContext` (pred `super`), ne v `onCreate`; `onCreate` naj samo še
`applyLocaleToProcessDefaults()`.

---

**L4 — Imena počitnic ostanejo v jeziku, v katerem je bila baza posejana**

`RoutineDatabase.create(localized)` ob prvem zagonu **shrani prevedena imena** (`resources.calendarTitle(key)`)
v tabelo koledarja. Menjava jezika pozneje teh vrstic ne prevede. Kdor je aplikacijo prvič odprl v slovenščini in
nato preklopil na angleščino, bo med angleškim vmesnikom bral »Jesenske počitnice«.

*Popravek (pravi):* v bazi hrani **ključ** (`"Jesenske počitnice"` kot dataset key ali bolje enum), prevajaj ob
izrisu. *Popravek (hitri):* ob `RestartForLocale` ponovno zapiši naslove koledarskih vrstic.

---

**L5 — »Prostor za oddih« v letnem pogledu je v angleščini prazen** *(in to je ista sekcija, ki jo zahteva serija 3)*

`OverviewScreens.kt:549`:

```kotlin
val vacations = content.calendar.filter {
    it.isWorkFreeDay && (it.title.contains("počitnice") || it.title.contains("oddih"))
}
```

Filtriranje po **slovenskem podnizu prikaznega besedila**. Če je bila baza posejana v angleščini
(»Autumn holidays«, »New Year break«), ne ujame ničesar → sekcija pokaže »Za to šolsko leto ni priloženih počitnic.«
Trenutno to napako maskira napaka L4 (naslovi so pogosto slovenski ne glede na jezik) — kar pomeni, da bo
popravek L4 **razbil** to sekcijo, če se ne popravita skupaj.

*Popravek:* filtriraj po lastnosti modela (npr. `kind == CalendarKind.VACATION` ali obstoječi `isWorkFreeDay`
+ nov `isMultiDay`), nikoli po besedilu.

---

**L6 — `runBlocking` na glavni niti ob vsakem zagonu procesa**

```kotlin
fun readPersistedAppLanguage(context: Context): String? =
    runBlocking { context.applicationContext.scheduleDataStore.data.first()[appLanguageKey] }
```

Razlog je legitimen (`attachBaseContext` ne more počakati na korutino), cena pa je na kritični poti hladnega
zagona — in proces lahko zbudi tudi alarm ali widget. DataStore mora pri tem prebrati in deserializirati
**celotno** `schedule_preferences` datoteko, ne samo tega ključa.

*Popravek:* za to eno vrednost uporabi `SharedPreferences` (sinhrono branje je njen namen) kot zrcalo,
DataStore pa ostane vir resnice; ali pa zmeri z `StartupTrace` in šele potem odloči.

---

**L7 — Barvni krog: dve manjši, a vidni težavi**

* `remember { buildWheelBitmap() }` požene **65 536 iteracij** `Color.hsv(...).toArgb()` in alocira 256 KB
  `IntArray` + 256 KB bitmap — **na glavni niti, med kompozicijo**, ob vsakem odprtju urejevalnika predmeta.
  To je natanko tista vrsta zatika, ki jo je patch hotel odpraviti. Krog se nikoli ne spremeni:
  naj bo `private val wheel by lazy { ... }` na ravni datoteke, zgrajen na `Dispatchers.Default`
  (`produceState`), z `AndroidColor.HSVToColor(hsv)` namesto Compose poti.
* `Modifier.pointerInput(enabled, value)` — ključ vsebuje `value` (jasnota). Ko se jasnota spremeni
  (tudi za 1/255 zaradi zaokroževanja pri HSV↔RGB), se gesture-detektor **ponovno zažene in prekine
  poteg v teku**. Uporabi `rememberUpdatedState(value)` in ključ samo `enabled`. Hkrati združi
  `detectTapGestures` in `detectDragGestures` v **en** `pointerInput`; dva vzporedna tekmujeta za isti kazalec.

---

### 5.3 Manjše (kakovost, ne napake)

* `SettingRow(title = …, description = …, control = {})` — komponenta za vrstico z gumbom je uporabljena
  s praznim gumbom samo zato, da dobimo naslov + opis. Boljše: lasten `SettingHeading`.
* `LanguageSelector.chip(...)` je `@Composable` z malo začetnico → Compose lint `ComposableNaming` (opozorilo)
  in odstopanje od konvencije datoteke.
* `RoutineLocale.userChoice` se nastavi **pred** zapisom v DataStore; če zapis vrže izjemo, sta zrcalo in
  shramba do konca procesa narazen.
* Patch je en sam kup — serija 1 in serija 2 morata biti dva commita (in najbrž dva PR-ja).
* Datoteka `01a0d907-…​.patch` (1778 vrstic) je commitana v repozitorij; ko bo apliciran, naj gre ven.
* Sistemski izbirnik jezika na Androidu 13+ (`android:localeConfig` je že prijavljen) in nova izbira v aplikaciji
  sta **dva vira resnice**, ki se lahko razideta: kar uporabnik nastavi v Nastavitvah sistema, aplikacija tiho
  povozi. Ker je `minSdk = 24`, `LocaleManager` sam ne zadošča — zato predlagam: na API ≥ 33 piši **in** beri
  `LocaleManager.applicationLocales`, pod tem pa obstoječe zrcalo. To je edina izvedba, ki jo Google šteje za pravilno.

---

## 6. Načrt

### Faza 0 — spravi delo v CI (danes, ~1 obrat)

1. Patch apliciraj na `arena/01a0dcdf-mydailyroutine`.
2. Popravi **P1–P6** (to je ~20 vrstic; brez njih je vse ostalo neopazljivo).
3. Razbij na dva commita: `Dva polna jezika in preklop` / `Barvni krog in stekleni gumbi listov`.
4. Izbriši `.patch` datoteko iz repozitorija.
5. Poženi statične strehe lokalno, potisni, odpri PR, počakaj CI.
6. **Rezultat, ki ga rabiš ti:** link do `app-release.apk` iz rolling releasa `debug-latest`.

> Definicija »končano«: CI zelen, APK naložen. Šele takrat vemo, da P1–P6 niso bili edini.

### Faza 1 — popravki vedenja iz patcha (naslednji obrat)

* **L1** zbiralnik učinkov nad zgodnji `return` (jezik v onboardingu res velja takoj).
* **L2** `Resources.getSystem()` kot vir jezika naprave.
* **L3** zrcalo v `attachBaseContext`.
* **L7** krog: lazy + off-main-thread gradnja, en `pointerInput`, `rememberUpdatedState`.
* Device test: `settings-language-en` → preveri, da se `activity.resources.configuration.locales[0]` spremeni;
  `onboarding-language-en` → preveri, da se naslov koraka prevede **brez** zapuščanja onboardinga.
* Potisni, CI, nov APK.

### Faza 2 — serija 3a: počitnice v letnem pogledu

* **L4 + L5 skupaj** (eno brez drugega naredi škodo): koledarska vrstica dobi tipizirano lastnost,
  filtriranje po njej, prevod ob izrisu.
* Oblika: trenutno je to gol Material `ListItem` z `headlineContent`/`supportingContent`/`trailingContent` —
  edini v tem zaslonu, zato štrli. Zamenjaj z obstoječim vzorcem vrstice tega zaslona (isti radij, ista višina
  dotika, isti razmiki, števec kot obstoječi žeton), da je sekcija videti kot del »Velike slike«, ne kot tujek.
* Posnetki pred/po v `tools/compare_shots.py`.

### Faza 3 — serija 3b: stekleni spodnji listi z rahlim blurom

Tu je **pomemben tehnični popravek prejšnje ugotovitve.** Prejšnji agent je sklenil, da je za blur ozadja
potreben prepis vseh listov v »in-window overlay«, ker je `ModalBottomSheet` svoje okno in ne more vzorčiti
ozadja aplikacijskega okna. Prvi del drži, sklep pa ne: **Android 12+ ima za točno ta primer javni API.**

`WindowManager.LayoutParams.FLAG_BLUR_BEHIND` + `setBlurBehindRadius(px)` zabrise **vse za oknom**, na GPU,
v SurfaceFlingerju — aplikacija tega ne plača z risanjem. Okno lista je dosegljivo iz njegove kompozicije:

```kotlin
val window = (LocalView.current.parent as? DialogWindowProvider)?.window
LaunchedEffect(window, radius) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply { blurBehindRadius = radiusPx }
    }
}
```

Načrt:

1. En sam `Modifier`/efekt `sheetBackdropBlur()` v `RoutineSheet.kt`, ki ga dobijo **vsi** listi hkrati
   (Načrt / Naloge / Cilji / Nastavitve / Dodaj blok / uvoz urnika / urejevalnik teme).
2. Radij **zelo majhen** — izhodišče 8 dp (≈ 20 px na xhdpi), ne več; tvoja zahteva je bila
   »čisto čisto rahel, da ne bo moteče«. Skupaj z njim znižaj `scrimAlpha`, ker blur sam že ustvari globino
   (Googlova lastna smernica: z blurom nižji dim, brez bluma višji).
3. **Fallback je obvezen, ne kozmetičen.** `WindowManager.isCrossWindowBlurEnabled` je lahko `false` zaradi
   varčevanja z baterijo ali ker proizvajalec (npr. One UI) sploh ne omogoča `supports_background_blur`.
   Registriraj `addCrossWindowBlurEnabledListener` in ob `false` dvigni motnost ploskve lista in scrim —
   isto, kar priporoča Androidova dokumentacija. Isti fallback pokriva »reduce transparency«.
4. Ploskev lista postane steklena po obstoječem `GlassRole.Sheet` (tint `GlassTintStrongAlpha = 0.68`,
   ki kot edini prestane kontrastni prag; 0,50–0,55 pade pod 4,5 : 1 — to je bilo že izračunano).
5. Pravilo »največ 1–2 glavna steklena elementa na zaslon« ostane: list dobi steklo, gumbi v njem ga
   **že imajo** (serija 2), zato je treba paziti, da se ne naberejo trije sloji — verjetno bo treba
   gumbom v steklenem listu znižati rob/highlight.
6. Preveri na treh ozadjih (prazen dan, gost dan, letni pogled) + na napravi z izklopljenim blurom.

### Faza 4 — tekočnost, izmerjeno

Šele ko je zgornje zeleno: `dumpsys gfxinfo … framestats` pred/po na istem scenariju (odpiranje lista,
poteg po krogu, drsenje letnega pogleda), plus Perfetto sled za en poteg. Če želiš, dodam jank-check
v device teste CI, da se regresija ujame sama.

---

## 7. Odgovor na vprašanje, ki ga boš verjetno postavil

> *Zakaj je bilo v patchu šest napak, ki bi jih prevajalnik ujel v treh sekundah?*

Ker v tem peskovniku ni prevajalnika. Ni `java`, ni `gradle`, ni Android SDK; `JAVA_HOME` in `ANDROID_HOME`
sta prazna. Vsa lokalna verifikacija je Python + branje. Iz tega sledita dve pravili, ki ju predlagam kot stalni:

1. **Nobena serija se ne konča brez pusha.** CI je edini prevajalnik; obrat brez pusha je obrat brez dokaza.
2. Pri vsakem uvozu novega API-ja (Compose, Android) se simbol **preveri v dokumentaciji**, ne po spominu.
   Štiri od šestih napak (`.hsv`, `.toLong()`, `android.graphics.ImageBitmap`, `Double`→`Float`) so natanko
   »zvenelo je, kot da obstaja«.
