# Kyant0 Liquid Glass: ali obstajajo že narejene komponente?

Raziskava, 26. 9. 2026. Vprašanje: na posnetkih zaslona so vidni lepi stekleni gumbi in on/off
stikala — ali jih knjižnica ponuja, da jih lahko uporabimo namesto sedanjih?

---

## 1. Kratek odgovor

**Knjižnica jih ne ponuja. Njen README to pove v eni vrstici:**

> *The library does not include any high-level components; you will need to create your own.
> Below are some example components: LiquidButton, LiquidToggle, LiquidSlider, LiquidBottomTabs.*

Kar si videl na posnetkih, je **demo aplikacija „Backdrop Catalog“**, ki je del istega
repozitorija. Njene komponente obstajajo kot **izvorna koda pod licenco Apache 2.0** — torej jih
smemo prevzeti, z navedbo avtorja.

**Dobra novica, ki je vredna več od vprašanja:** različica, ki jo že imamo v projektu
(`io.github.kyant0:backdrop:1.0.0`), ima **vse API-je, ki jih te komponente potrebujejo**. Ne
rabimo nadgradnje. Rabimo pa jih začeti uporabljati — trenutno jih ne uporabljamo nobenega.

---

## 2. Kaj točno obstaja

`app/src/commonMain/kotlin/com/kyant/backdrop/catalog/` v repozitoriju
[Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass):

| Datoteka | Kaj je | Ali nam koristi |
|---|---|---|
| `components/LiquidToggle.kt` | on/off stikalo; **gumb je steklena leča**, ki lomi zeleno progo pod sabo | **Da — to je tisto, kar si videl** |
| `components/LiquidButton.kt` | kapsula, ki se pod prstom stisne in *zdrsne* proti dotiku | Delno; naš `SheetPrimaryButton` to že zna |
| `components/LiquidSlider.kt` | drsnik, ki se med vlečenjem raztegne | Da, imamo 2 drsnika |
| `components/LiquidBottomTabs.kt` + `LiquidBottomTab.kt` | zavihki z vlečljivo izbirno pilulo | Nimamo spodnje navigacije — ne |
| `utils/DampedDragAnimation.kt` | skupna fizika: vrednost, hitrost, stisk, raztezek ob hitrosti | Da, srce vseh treh |
| `utils/InteractiveHighlight.kt` | sij pod prstom prek AGSL senčilnika | Ne — naš `routineGlassTouch` to že dela |

---

## 3. Odločilna združljivost: 1.0.0 (kar imamo) proti 2.0.1 (zadnja)

V `gradle/libs.versions.toml` je pin že opremljen z opozorilom:

> *1.0.0 je izdaja, zgrajena proti Kotlin 2.2.x in androidx.compose 1.9.x, kar je natanko ta
> projekt; 1.0.3+ zahteva Kotlin 2.3 in compose 1.10, 2.x pa je Compose Multiplatform na Kotlin 2.4.*

Naš projekt: **Kotlin 2.2.10**, Compose BOM **2025.09.01**, KSP `2.2.10-2.0.2`, AGP 8.13.2,
minSdk 24, compileSdk 36. Nadgradnja na 2.0.1 torej **ni bump odvisnosti, ampak migracija
orodjarne** — Compose compiler, KSP, Room, Glance gredo vsi zraven.

Preveril sem izvorno kodo pri oznaki `1.0.0`. Rezultat:

| Kar komponente potrebujejo | Je v 1.0.0? |
|---|---|
| `drawBackdrop(backdrop, shape, effects, highlight, shadow, innerShadow, layerBlock, …)` | ✅ **točno ta podpis** |
| `com.kyant.backdrop.highlight.Highlight` (`Default`, `Ambient`) | ✅ |
| `com.kyant.backdrop.shadow.Shadow` in `InnerShadow` | ✅ |
| `rememberCombinedBackdrop` (gumb lomi progo *in* ozadje) | ✅ `backdrops/CombinedBackdrop.kt` |
| `rememberBackdrop`, `rememberCanvasBackdrop`, `emptyBackdrop` | ✅ |
| `lens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)` | ✅ isti podpis |
| `com.kyant.shapes.Capsule` | ❌ druga knjižnica — a imamo `RoutineShapes.Pill` |
| `com.kyant.backdrop.RuntimeShader` / `isRuntimeShaderSupported` | ❌ samo 2.x (abstrakcija za KMP) |
| `kotlin.time.Clock`, `awaitFrame`, `inspectDragGestures` | ❌ drobne zamenjave |

Zadnji dve vrstici zadevata **samo** `InteractiveHighlight`, ki ga tako ali tako ne rabimo, in dve
vrstici v `DampedDragAnimation`.

**Sklep: nadgradnja ni potrebna in je ne priporočam.**

---

## 4. Kje smo danes — in kaj to pojasni

### 4.1 Uporabljamo približno polovico knjižnice

`RoutineGlass.kt` kliče `drawBackdrop` s štirimi argumenti: `backdrop`, `shape`, `effects`,
`onDrawSurface`. Parametri **`highlight`, `shadow` in `innerShadow` so neuporabljeni** — čeprav so
v naši različici na voljo. Namesto njih:

* rob risemo ročno (`rimBrush` + `drawOutline`),
* odsev risemo ročno (`drawSpecular`),
* **sence ni nobene** — kar je natanko vrzel, ki jo je našla revizija smernic.

### 4.2 Naše „liquid“ kontrole niso steklene

| | Naš `RoutineSwitch` | Kyant0 `LiquidToggle` |
|---|---|---|
| Proga | polna barva, lerp siva → turkizna | isto |
| **Gumb** | **polna bela ploskev + 1 dp rob** | **steklena leča** z `drawBackdrop` |
| Kaj gumb lomi | nič | `rememberCombinedBackdrop(okno, proga)` — progo pod sabo **in** aplikacijo za njo |
| Ob pritisku | `scaleX` 1,18 | blur 8 dp → `lens` z aberacijo, `InnerShadow`, `Highlight.Ambient` |
| Senca | ni | `Shadow(4 dp, črna 5 %)` |
| Fizika | `Animatable` + spring | `DampedDragAnimation`: raztezek po **hitrosti** |

Isto velja za naš `LiquidSlider` — njegov KDoc pošteno pravi „the catalog's LiquidSlider **reduced
to what the entry editor needs**“. Prejšnji agent je obliko posnel, stekla pa ni prenesel.

**To je odgovor na „zakaj je vse tako basic“:** ni v knjižnici in ni v nadgradnji, ampak v tem, da
sta stikalo in drsnik narisana kot polni ploskvi, medtem ko je vse ostalo steklo.

---

## 5. Načrt

### F1 — začni uporabljati, kar že imamo *(največji učinek, najmanjše tveganje)*

Rob in odsev iz ročnega risanja preseli v knjižnična parametra, in dodaj senco:

```kotlin
drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = { vibrancy(); blur(role.blur.toPx()); lens(...) },
    highlight = { Highlight.Ambient.copy(alpha = role.rim) },   // namesto rimBrush + drawSpecular
    shadow = { Shadow(radius = role.shadow, color = Black.copy(alpha = 0.05f)) },
    innerShadow = { InnerShadow(radius = 4.dp * press, alpha = press) },
    onDrawSurface = { /* samo še barvni wash */ },
)
```

* Zapre vrzel „mehka, nizka senca“ iz revizije smernic.
* Odstrani ~40 vrstic našega risanja robov.
* **Pozor:** `rimBrush` mora ostati za fallback pot (brez ozadja / Android 11 / visok kontrast).
* Vrednosti gredo v `GlassRole`, da ostanejo vloge enotne.

*Obseg: 1 datoteka. Tveganje: nizko — vizualno se spremeni rob in pribudi senca.*

### F2 — pravo stekleno stikalo *(to, kar si videl na posnetkih)*

Prenesi `LiquidToggle` v `RoutineSwitch`, na naš 1.0.0 API:

1. Prenesi `DampedDragAnimation` (zamenjaj `kotlin.time.Clock` → `withFrameMillis`,
   `inspectDragGestures` → naš `detectHorizontalDragGestures` z začetno pozicijo).
2. Proga dobi `layerBackdrop(trackBackdrop)`.
3. Gumb: `drawBackdrop(rememberCombinedBackdrop(LocalRoutineBackdrop.current, trackBackdrop), …)`
   s `Capsule` → `RoutineShapes.Pill`, `Highlight.Ambient`, `Shadow`, `InnerShadow`.
4. Ohrani, kar imamo in česar katalog nima: `Role.Switch` + `toggleable` semantiko,
   `LocalReduceMotion`, 48 dp tarčo dotika, `enabled` stanje.
5. **Fallback:** brez ozadja (Android ≤ 11, visok kontrast) gumb ostane sedanja polna ploskev.

*Učinek: **14 stikal** v nastavitvah, urejevalniku in spanju naenkrat.*
*Obseg: 1 nova util datoteka + prepis ~70 vrstic. Tveganje: srednje — nova risalna pot.*

### F3 — drsnik po istem vzorcu

Isti prijem za `LiquidSlider` (2 mesti). Poceni, ko F2 obstaja, ker deli `DampedDragAnimation`.

### F4 — `io.github.kyant0:shapes` *(neobvezno)*

Zvezne zaobljenosti (Applov „squircle“) namesto `RoundedCornerShape`. Vizualno opazno na vsem.
**Najprej preveri**, katero različico Kotlina/Compose zahteva — velja ista past kot pri `backdrop`.

### Česa ne priporočam

* **Nadgradnje na backdrop 2.x** — migracija orodjarne brez nove zmožnosti.
* **`LiquidBottomTabs`** — nimamo spodnje navigacije; uvajati jo zaradi videza bi bilo narobe.
* **`InteractiveHighlight`** — naš `routineGlassTouch` to že dela in deluje pod Androidom 13.

---

## 6. Kar je treba upoštevati pri vsaki fazi

* **Pravilo brez gnezdenja** velja naprej: stikalo v steklenem listu ali vrstici mora ostati polno
  (`LocalInsideGlass`).
* **Vsaka nova steklena koda mora biti preverjena tudi v fallback različici** — API 24–30, brez
  ozadja, visok kontrast.
* `lens` zahteva `CornerBasedShape` in Android 13+; `refractionHeight` ≤ najmanjši radij oblike.
* Kontrastna streha mora po F1 in F2 še vedno teči zeleno.
* Licenca: Apache 2.0 — v vsaki prevzeti datoteki navedi izvor (`Kyant0/AndroidLiquidGlass`,
  katalog, Apache-2.0).

---

## 7. Priporočilo

**F1, nato F2.** F1 je ena datoteka in zapre vrzel iz prejšnje revizije. F2 je tisto, kar si
pravzaprav prosil — in zadene 14 stikal hkrati, kar je največ vidnega učinka na uro dela v celotnem
projektu. F3 sledi skoraj zastonj. F4 posebej, po preverbi različic.
