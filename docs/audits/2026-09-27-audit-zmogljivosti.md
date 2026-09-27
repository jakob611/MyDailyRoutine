# Audit zmogljivosti, glitchev, animacij in stekla

27. 9. 2026. Povod: aplikacija se zatika na Galaxy S24 Ultra, v menijih se pojavljajo glitchi,
prehodov skoraj ni, steklo v listih dela čudne stvari.

Če se zatika na S24 Ultra, ni kriva naprava. Iskal sem strukturni vzrok in ga našel.

---

## 1. Vzrok zatikanja: celotna aplikacija se je rekomponirala vsak frame

Dve vrednosti, ki se **stalno spreminjata**, sta bili deljeni prek `staticCompositionLocalOf`.

Ta vrsta lokala namenoma **ne sledi bralcem**. To je njena prednost, kadar se vrednost nikoli ne
spremeni (tema, haptika), in njena past, kadar se: ob vsaki spremembi Compose ne rekomponira
bralcev, ampak **celotno vsebino ponudnika**. Oba ponudnika sta ovijala vso aplikacijo.

### 1.1 `LocalPulse` — 120 rekompozicij vsega na sekundo

```kotlin
// prej
val LocalPulse = staticCompositionLocalOf { 1f }
fun rememberAppPulse(...): Float = transition.animateFloat(...).value   // <- .value
// v RoutineApp:
CompositionLocalProvider(LocalPulse provides rememberAppPulse(reduceMotion)) { /* vsa aplikacija */ }
```

`rememberInfiniteTransition` se spremeni vsak frame. Prebran je bil **kot vrednost, med
kompozicijo**, v `RoutineApp` — torej se je `RoutineApp` rekomponiral vsak frame, in ker lokal ne
sledi bralcem, se je z njim rekomponiralo **celotno drevo**. Na 120 Hz zaslonu 120-krat na
sekundo, ves čas, za tri pike, ki utripajo.

**Popravek.** Lokal nosi `State<Float>`. Referenca je stabilna, torej se lokal nikoli ne spremeni
in nič se ne rekomponira. Okvirji pristanejo v `.value`, ki ga nov `Modifier.pulsing` prebere v
**izrisni fazi**, kjer spremenjena številka stane prerisan en gradnik.

### 1.2 `LocalGlassTilt` — ista past, sprožena z roko

Nagib telefona iz senzorja rotacijskega vektorja pri `SENSOR_DELAY_UI`, kvantiziran na 0,05, spet
prebran kot vrednost in spet deljen statično čez vso aplikacijo. **Vsak premik roke je
rekomponiral celotno aplikacijo.**

Tu se popravek ni splačal — izmeril sem, kaj ta nagib sploh nariše:

| vloga | alpha sija |
|---|---|
| Bar | 0,0288 → **2,9 % bele** |
| Sheet | 0,0324 → **3,2 %** |
| Chip | 0,0216 → **2,2 %** |
| Control | 0,0252 → **2,5 %** |

Na OLED podlagi je to na pragu nevidnosti. Za ta sij je aplikacija držala prižgan senzor,
poslušalca, ki je pisal stanje ob vsakem vzorcu, in invalidacijo vsakega steklenega elementa.
**Odstranjeno v celoti** — z njim `GlassTilt`, `LocalGlassTilt`, `rememberGlassTilt`, parameter
`tilt` na osmih klicnih mestih, žeton `GlassTiltGlow` in osem uvozov.

---

## 2. Glitch v menijih: glava lista se je merila prek stanja

`SheetShell` je telo lista podlagal z `headerHeight`, ki je **začel pri nič** in se je zapisal šele
iz `onSizeChanged` glave:

```
frame 1:  headerHeight = 0    -> vsebina se nariše pod glavo
frame 2:  glava izmerjena     -> vsebina poskoči navzdol
```

Skok ob **vsakem** odprtju lista, plus polna rekompozicija telesa zraven. Merilna faza ne more
prebrati vrednosti, ki še ni zapisana — podkompozicija po vrsti pa lahko. `SheetShell` zdaj
najprej izmeri glavo in šele nato sestavi telo, ki že ve, koliko prostora mu ostane, v enem
prehodu. Brez stanja, brez skoka, brez rekompozicije.

---

## 3. Animacije: obstajajo in so dobre, le teka niso imele

To je bila najbolj presenetljiva ugotovitev. Preklop med Dan/Teden/Mesec/Leto **ni** gol `when` —
je `AnimatedContent` s premišljenim `transitionSpec`:

* pomik po času → drsenje po skupni vodoravni osi v smeri potovanja,
* menjava merila (dan → teden) → fade-through z rahlim `scale`, ker novi pogled ne deli geometrije
  s starim,
* pod sistemsko nastavitvijo »odstrani animacije« oboje postane `snap`.

Ključ `contentKey = { it.date to it.mode }` je pravilen, `backProgress` predictive-back geste se
bere v `graphicsLayer` (izrisna faza), ne v kompoziciji.

**Animacije torej niso manjkale — niso imele frame budgeta, da bi tekle.** Drevo, ki se
rekomponira 120-krat na sekundo, spusti prav tiste okvirje, iz katerih je narejen prehod. Zato je
bil vtis »ni animacij« in »zatika se« en in isti problem.

---

## 4. Kaj sem preveril in je v redu

* **Gnezdenje stekla** — nobenega. Vseh 8 mest, ki nanašajo steklo neposredno, ne vsebuje steklene
  kontrole; gumb za zapiranje v glavi lista je navaden Material.
* **Android 12** — rob ne izgine brez senčilnika: `HighlightNode.configurePaint` barvo nastavi
  vedno, senčilnik je le dodatek.
* **`AddBlockMorph`** se med 420 ms morfa rekomponira vsak frame, a je to ena majhna komponenta z
  lastnim obsegom. Ne splača se popravljati.
* **`collapsed`** se preklopi ob spremembi smeri drsenja, ne vsak frame.
* **Kontrastna streha** ostaja zelena: 109 preverjanj.

---

## 5. Kar ostaja odprto

| | Kaj | Zakaj ni v tem obratu |
|---|---|---|
| **A** | Vsak `RoutineSwitch` in `LiquidSlider` ustvari svoj `rememberLayerBackdrop`. Na zaslonu z nastavitvami je to 14 dodatnih slojev. | Površine so majhne (52×32 dp) in napravni testi ne kažejo padcev. Meriti je treba na napravi, preden se karkoli spremeni. |
| **B** | Steklo v mirovanju izračuna blur pod neprosojnim diskom, ki ga skrije. Isto dela katalog. | Površine so drobne; pravi test je `dumpsys gfxinfo`, ne ugibanje. |
| **C** | Mikroanimacije, ki jih res ni: vrstice seznama se ne pojavljajo z zamikom, kartice nimajo odziva na pritisk izven stekla. | Šele ko je frame budget zdrav, je smiselno dodajati. Zdaj je. |
| **D** | Zvezne zaobljenosti (squircle) — blokirano na Kotlinu 2.3. | Glej `2026-09-26-kyant0-komponente-raziskava.md`, §5.1. |

**Naslednji korak, ki ga priporočam:** aplikacijo preizkusi s to gradnjo. Če je zatikanje odpravljeno,
sta A in B najbrž nepotrebna in je vredno delati na C. Če ne, je naslednji korak `dumpsys gfxinfo
… framestats` pred/po na istem scenariju in Perfetto sled za en poteg — to je edini način, da se
naslednji popravek meri namesto ugiba.
