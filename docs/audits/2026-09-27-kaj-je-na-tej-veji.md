# Kaj je na tej veji: vse spremembe, po namenu

Veja `arena/01a0dcdf-mydailyroutine`, 19 commitov nad `master` (`72aef93`), 54 datotek,
+2633 / −2348 vrstic. PR #12.

Dve vrsti dela sta pomešani in ju je pošteno ločiti:

* **Iz patcha prejšnjega agenta** (`01a0d907-….patch`, commitan na master, a nikoli apliciran) —
  seriji 1 in 2. Patch se **ni prevajal**; aplicirati sem ga moral in popraviti šest prevajalnih
  napak, nato pa še sedem logičnih napak, ki jih je nosil.
* **Moje delo** — vse ostalo: dve sesutji, arhitektura stekla, razbitje `RoutineApp`, komponente po
  Kyant0, in audit zmogljivosti.

---

# 1. Dva polna jezika (serija 1)

**Cilj:** aplikacija naj v celoti govori slovensko ali angleško, izbira pa naj bo pri roki na
dveh mestih — v nastavitvah in že v onboardingu.

| Datoteka / funkcija | Namen |
|---|---|
| `domain/model/AppLanguage.kt` | Edino mesto, ki ve, katera dva jezika aplikacija govori. `normalize(tag)` vrne `"sl"`, `"en"` ali `null`; tretja oznaka se bere kot »ni izbire«, nikoli kot jezik, ki ga aplikacija ne zna. |
| `core/platform/SlovenianLocale.kt` → `uiLocaleFor(deviceLanguage, userChoice)` | Čisto pravilo jezika, brez Androida: izbira bralca je prvo pravilo, jezik naprave drugo. Neprevedena naprava dobi slovenščino, ker je to privzeti nabor virov — nemški telefon tako bere en cel jezik namesto mešanice. |
| `SlovenianLocale.kt` → `Context.withRoutineLocale()` | Ovije kontekst v konfiguracijo z razrešenim jezikom. Kliče se iz obeh `attachBaseContext`, iz grafa objektov in iz gradnika — povsod, kjer mora nekaj govoriti pravi jezik. |
| `SlovenianLocale.kt` → `applyLocaleToProcessDefaults()` | Poravna procesne privzetke (`Locale.setDefault`), da oblikovanje številk in datumov ne zdrsne v drug jezik od napisov okoli njih. |
| `SlovenianLocale.kt` → `object RoutineLocale` | Sinhrono zrcalo obeh vhodov pravila, berljivo iz katerekoli niti brez konteksta. Obstaja, ker `attachBaseContext` ne more počakati na korutino. |
| `core/presentation/DisplayFormat.kt` → `interfaceLocale` | **Popravljeno iz `val` v `get()`.** Prej zamrznjeno ob nalaganju razreda; ker se jezik uveljavi s ponovnim ustvarjanjem aktivnosti in ne procesa, bi zamrznjena vrednost oblikovala datume v starem jeziku pod novimi napisi. |
| `designsystem/components/LanguageSelector.kt` | Ena komponenta za izbiro jezika, uporabljena v nastavitvah in v onboardingu, da sta ponudba in vedenje na obeh mestih ista stvar. |
| `OnboardingScreen.kt`, `SettingsSheet.kt` | Vstopni točki za izbiro. Onboarding jo ponudi, preden bralec karkoli vpiše. |
| `RoutineViewModel.kt` → veja `SetAppLanguage` | Zapiše izbiro v zrcalo in v shrambo ter pošlje `RestartForLocale`. Ponovno ustvarjanje aktivnosti je nujno: viri so vezani na kontekst in jih rekompozicija ne osveži. |
| `core/platform/ShareTextParser.kt` → `parse(text, fallbackTitle)` | **Podpis spremenjen.** Razčlenjevalnik deljenega besedila je čisti JVM in ne sme imeti jezika; nadomestni naslov za deljeno povezavo brez besedila zdaj poda klicatelj že razrešen. |
| `res/values/strings.xml`, `values-en/strings.xml` | Vsi uporabniško vidni nizi, oba jezika popolna. |
| `res/xml/locales_config.xml` | Sistemski izbirnik jezika po aplikaciji (Android 13+) ponuja natanko ta seznam. |

---

# 2. Vizualni izbirnik barv in stekleni gumbi (serija 2)

**Cilj:** barva predmeta naj se izbere s prstom, ne z vnosom hex kode; in gumbi naj bodo sami
steklo, ne navadni gumbi na steklenem otočku.

| Datoteka / funkcija | Namen |
|---|---|
| `features/subjects/presentation/SubjectColorPicker.kt` (nova, 264 vrstic) | Barvni krog (odtenek = kot, nasičenost = polmer) in drsnik za jasnoto. **Popolnoma sem ga prepisal**, ker se patchova različica ni prevajala: uporabljala je `Color.toLong()` in lastnost `Color.hsv`, ki v Compose ne obstajata. Prava pot je `android.graphics.Color.colorToHSV` / `HSVToColor`. |
| `SubjectEditorDialog.kt` | Hex polje zamenja krog. |
| `designsystem/theme/DesignSystem.kt` → `RoutineColors.HueWheel*` | Barvni literali kroga so v paleti, ne v komponenti — paletna streha prepoveduje `Color(0x…)` zunaj `DesignSystem.kt`. |
| `RoutineText.kt` → `SheetPrimaryButton`, `SheetSecondaryButton` | Gumb **je** steklena plošča z lastnim blurom, lečo in robom, ne prosojna klikalna površina na steklenem okvirju. Stekleni »otoček« pod gumbom »Dodaj v moj dan« je odstranjen. |

---

# 3. Dve sesutji, ki ju je pokazalo šele izvajanje

**Cilj:** aplikacija se ne sme sesuti. Obe napaki sta bili nevidni v statični analizi.

| # | Kaj | Namen popravka |
|---|---|---|
| **C1** | `SIGSEGV` v `RenderThread`, 15 padlih testov | Patch je sloj lista objavil čez **cel** `SheetShell`, zato je gumb v telesu vzorčil prav tisti sloj, v katerega se riše — vozlišče, ki se riše samo vase, ubije render nit. Popravek: sloj dobi **samo noga**, ki je sosed telesa, ne njegov otrok. |
| **C2** | `IllegalArgumentException` v `LinearGradient.nativeCreate` | `rimBrush` je imel privzeti `end = Offset.Unspecified`, kar je `Offset(NaN, NaN)`. Compose proti velikosti vozlišča razreši samo `Offset.Infinite`. **Predobstoječa napaka:** ta preliv riše fallback pot, ki se vzame na vsem **pod Androidom 12**, `minSdk` pa je 24 — aplikacija bi se tam sesula ob zagonu. CI je ni videl, ker emulator teče na API 36. |

---

# 4. Sedem logičnih napak iz patcha (L1–L7)

**Cilj:** da funkcije, ki jih je patch dodal, res delujejo.

| # | Napaka | Namen popravka |
|---|---|---|
| **L1** | Zbiralnik učinkov je stal **pod** zgodnjim `return` za onboarding | Chip za jezik v onboardingu je izbiro shranil, a učinek `RestartForLocale` ni imel poslušalca — zaslon je ostal v starem jeziku, čeprav napis obljublja »Velja takoj«, aktivnost pa se je nepričakovano ponovno ustvarila šele po onboardingu. Zbiralnik je zdaj nad vejo; sporočila tečejo v svoji korutini, ker med onboardingom še ni gostitelja za snackbar in bi `showSnackbar` zadržal zbiralnik. |
| **L2** | »Kot naprava« se je po prvi ročni izbiri pokvarila | `uiLocale()` je jezik naprave bral iz `LocaleList.getDefault()`, ki ga `applyLocaleToProcessDefaults()` sam piše — aplikacija je spraševala samo sebe. Zdaj `captureDeviceLanguage(base)` zajame jezik v obeh `attachBaseContext`, iz konfiguracije, ki jo Android poda, **preden** aplikacija karkoli prepiše. Ta konfiguracija pozna tudi per-app jezik iz sistemskih nastavitev. |
| **L3** | Zrcalo izbire se je polnilo v `onCreate` | `attachBaseContext` teče prej, zato je bil bazni kontekst same aplikacije vedno razrešen po napravi. Zdaj se polni v `attachBaseContext`, pred `super`. |
| **L4** | Baza je koledar posejala s **prevedenimi** naslovi | Kdor je začel v slovenščini in preklopil v angleščino, je pod angleškim vmesnikom še naprej bral »Jesenske počitnice«. Zdaj `SeedAndIntegrityCallback` shrani **ključ podatkovnega niza**, `Resources.calendarTitle(key)` pa ga prevede ob izrisu. Slovenski nizi so enaki ključem, zato migracija ni potrebna. |
| **L5** | Letni pogled je počitnice iskal po slovenskem podnizu | `title.contains("počitnice")` ni našel ničesar, ko je vmesnik govoril angleško — sekcija »Prostor za oddih« se je izpraznila v natanko enem jeziku. Zdaj vpraša `SlovenianAcademicCalendar.vacationTitles`: podatkovni niz sam pove, katera okna so počitnice. **L4 in L5 sta morala iti skupaj** — prva napaka je drugo maskirala. |
| **L6** | `runBlocking` na glavni niti ob **vsakem** zagonu procesa | Za eno vrednost je glavna nit čakala, da DataStore prebere in deserializira celotno datoteko nastavitev — na kritični poti hladnega zagona, ki ga zbudi tudi alarm ali gradnik. Zdaj to vrednost zrcali enoključni `SharedPreferences`, kjer je sinhrono branje predvidena operacija. |
| **L7** | Barvni krog se je gradil na glavni niti | Rešeno že s prepisom `SubjectColorPicker.kt`. |

**Podpornik L6:** `DataStorePreferencesRepository`

| Funkcija | Namen |
|---|---|
| `readPersistedAppLanguage(context)` | Edino sinhrono branje v aplikaciji, za en trenutek: `attachBaseContext`. Ovito v `runCatching`, ker je to prva vrstica procesa in vsaka izjema tam je sesutje brez zaslona, na katerem bi se izpisalo. |
| `Context.localeMirror()` | Odpre zrcalo prek konteksta, ki ga metoda dobi — **ne** prek `applicationContext`, ki je v `attachBaseContext` še `null`. To me je enkrat stalo sesutja ob zagonu in je popravljeno v naslednjem commitu. |
| `preferences.onEach { localeMirror.writeLanguage(…) }` | Za `distinctUntilChanged`: zrcalo sledi vsaki vrednosti, ki jo shramba kdaj drži, tudi taki, ki ni prišla skozi ta proces. |

---

# 5. Arhitektura stekla

**Cilj:** steklo naj sledi Applovim pravilom in naj se nikoli ne razbije v obrobnem primeru.

| Datoteka / funkcija | Namen |
|---|---|
| `RoutineGlass.kt` → `GlassRole` | Ena vrstica na vlogo (Bar, Sheet, Chip, Control) fiksira blur, lečo, globino, rob, senco in tint. Brez tega si vsak zaslon izmisli svoj blur. |
| `RoutineGlass.kt` → `Modifier.routineGlass(...)` | Celotna steklena obdelava v pravem vrstnem redu, ki ga knjižnica zahteva (barvni filter ⇒ blur ⇒ leča), s polno ploskvijo kot fallback, kadar ni ozadja za vzorčenje ali platforma učinkov ne zna. |
| `RoutineGlass.kt` → `LocalRoutineBackdrop` / `LocalSheetBackdrop` | Kateri sloj kdo vzorči. List je svoje okno s svojim slojem; sloj lista dobi **samo kroma**, ker telo ta sloj nosi. |
| `RoutineGlass.kt` → `LocalInsideGlass` **(novo)** | **Steklo ne gnezdi.** Vrhnja vrstica je bila steklena plošča s štirimi steklenimi gumbi v sebi — oba sta brala isti backdrop, zato je gumb pokazal vsebino brez tinta vrstice in deloval kot luknja skozi kromo. `RoutineGlassSurface` postavi ta lokal, `GlassIconButton` nanj odgovori z izpustom plošče. Pravilo zdaj ni stvar spomina. |
| `RoutineGlass.kt` → `rememberReduceTransparency()` **(novo)** | Android nima stikala »reduce transparency«; ustreznik je visok kontrast besedila (od Androida 14 `UiModeManager.getContrast()`). Ko je vklopljen, `RoutineBackdropProvider` **ne objavi ničesar** → vse steklo pade na polno ploskev po isti poti kot na Androidu 11. En fallback, ne dva. |
| `RoutineGlass.kt` → `routineGlassTouch(touch, shape)` | Stisk in sij na točki dotika. Ločen modifier od plošče, zato gumb brez stekla odgovori na prst natanko enako. |

---

# 6. Razbitje `RoutineApp` (621 → 446 vrstic)

**Cilj:** edini pravi God Object v aplikaciji. Ven so šle tri enote, ki s postavitvijo zaslona
nimajo zveze.

| Datoteka / funkcija | Namen |
|---|---|
| `app/presentation/AppFeedback.kt` → `rememberFeedbackAction(vm, haptics, sounds)` | Preslikava »kaj dejanje **pomeni**« v haptiko in zvok, preden dejanje doseže view model. Applovi generatorji so semantični — izbira za korak, udarec za trk, obvestilo za izid — in prav ta preslikava je večina tega, zakaj se iPhone zdi natančen. Ovito na enem mestu, da eno dejanje nikoli ne odgovori dvakrat. |
| `AppFeedback.kt` → `RoutineEffects(vm, haptics, sounds, snackbars)` | Zbira enkratne učinke: trenutek dokončanja, sporočila, ponovni zagon ob menjavi jezika. Sestavljen **nad** onboarding vejo (glej L1). |
| `app/presentation/AppOverlays.kt` → `RoutineOverlays(...)` | Vseh sedem listov in šest dialogov. So sosedje zaslona, ne njegov del: vsak je svoje okno ali dialog, vsakega poganja izključno `state.panels`, in noben ne more vplivati na postavitev pod sabo. |

Naprej namenoma nisem šel: naslednji kandidat je vrhnja vrstica, ta pa zajame dvanajst lokalnih
spremenljivk in bi eno slabost zamenjala za drugo.

---

# 7. Komponente po Kyant0 (F1–F3)

**Ugotovitev raziskave:** knjižnica **ne** ponuja gotovih komponent — README to pove dobesedno.
Kar je na posnetkih, je demo »Backdrop Catalog«. Različica, ki jo že imamo (1.0.0), pa ima **vse
API-je**, ki jih te komponente rabijo. Nadgradnja ni potrebna.

| Faza | Datoteka / funkcija | Namen |
|---|---|---|
| **F1** | `routineGlass` → parametra `highlight` in `shadow` | `drawBackdrop` ta dva **privzeto poda**, mi pa ju nismo — in smo ju ves čas dobivali. Ker sta tiho delovala, je koda čez knjižnično lučko risala **še svoj rob**: vsak element je imel dva robova drug na drugem in enotno 24 dp senco ne glede na velikost. Zdaj rob riše knjižnica enkrat, s senčilnikom, ki pozna dejanske radije, senca pa je stolpec v tabeli vlog (16/20/8/10 dp). |
| **F2** | `LiquidControls.kt` → `RoutineSwitch` | Gumb je bil polna bela ploskev. Zdaj proga objavi svoj sloj in gumb ga vzorči kot **sosed**. Vzorči samo progo, namenoma ne okna: 12 od 14 stikal živi v listih, ti pa so svoja okna. Pod prstom blur odstopi leči in turkizna se ukrivi skozi disk. |
| **F3** | `LiquidControls.kt` → `LiquidSlider` | Isto za palec: proga in polnilo v en sloj, zato palec na eni strani lomi turkizno, ki jo je prevozil, na drugi prazno progo. |
| **F1–F3** | `LiquidControls.kt` → `Modifier.liquidDisc(...)` | Gumb stikala in palec drsnika sta postala isti blok, prepisan dvakrat. Zdaj obstaja enkrat. `RoutineSwitch` 151→126 vrstic, `LiquidSlider` 111→94, obe kontroli pa sta **zagotovljeno** usklajeni. |
| | `SwitchKnobClearance` 0,55 / `SliderThumbClearance` 0,30 | Bistrenje ni skupna vrednost, ker geometrija ni ista: proga stikala pokriva **100 %** gumba, proga drsnika le **30 %** palca. Palec, ki bi se zbistril enako, bi večinoma kazal zaslon za sabo. |
| **F4** | — | **Blokirano.** POM `io.github.kyant0:shapes:1.0.0` zahteva `kotlin-stdlib 2.3.0`, projekt je na 2.2.10. Novejši metapodatki niso opozorilo, ampak napaka prevajanja. |

---

# 8. Audit zmogljivosti — vzrok zatikanja

**Cilj:** ugotoviti, zakaj se aplikacija zatika na S24 Ultra. Če se zatika tam, ni kriva naprava.

| Kaj | Namen popravka |
|---|---|
| `LocalPulse` | Nosil je `Float` iz neskončne animacije, prebran **med kompozicijo**, in bil deljen prek `staticCompositionLocalOf` — vrste lokala, ki namenoma **ne sledi bralcem** in ob spremembi rekomponira **celotno vsebino ponudnika**. Ponudnik je ovijal vso aplikacijo. Rezultat: celotno drevo se je rekomponiralo **120-krat na sekundo, ves čas**, zaradi treh pik, ki utripajo. Zdaj lokal nosi `State<Float>` — referenca je stabilna, lokal se nikoli ne spremeni. |
| `Modifier.pulsing(pulse)` **(novo)** | Prebere `.value` v **izrisni fazi**, kjer spremenjena številka stane prerisan en gradnik namesto rekompozicije drevesa. |
| `LocalGlassTilt` | Ista past, sprožena z roko: nagib iz senzorja pri UI hitrosti je ob vsakem premiku rekomponiral vso aplikacijo. Izmeril sem, kaj nariše — **2,2–3,2 % bele**, na OLED pod pragom vidnosti. **Odstranjeno v celoti**, z njim senzor, `GlassTilt`, `LocalGlassTilt`, `rememberGlassTilt`, parameter `tilt` na osmih mestih in žeton `GlassTiltGlow`. |
| `SheetShell` → `SubcomposeLayout` | Telo lista se je podlagalo z `headerHeight`, ki je začel pri **nič** in se zapisal šele iz `onSizeChanged` glave: prvi frame je vsebino narisal pod glavo, drugi jo je spustil na mesto. Viden skok ob **vsakem** odprtju lista. Merilna faza ne more prebrati vrednosti, ki še ni zapisana — podkompozicija po vrsti pa lahko. |

**Ugotovitev o animacijah:** prehodi **obstajajo** in so dobro napisani — `AnimatedContent` z drsenjem
po skupni osi ob pomiku po času in fade-through z rahlim `scale` ob menjavi merila. Niso manjkali;
niso imeli frame budgeta, da bi tekli. Vtis »ni animacij« in »zatika se« sta bila **en in isti
problem**.

---

# 9. Strehe in testi, ki sem jih dodal

| Kaj | Namen |
|---|---|
| `tools/check_presentation.py` — koledarska streha | Vsak literal v podatkovnem nizu, ki ni datum, mora imeti prevod (29 ključev). Ključ brez preslikave pade skozi na svoj slovenski jaz in se tiho pojavi neprevedn v angleškem vmesniku. Preverja se **samo ena smer** — preslikava mora biti nadmnožica, ker naslovi iz prejšnjih šolskih let še živijo v obstoječih bazah. |
| `CalendarAndValidationTest` — nov test | Zaklene, kaj je počitniško okno in kaj zgolj dvodnevni praznik. |
| `SlovenianAcademicCalendar.vacationTitles` | Podatkovni niz sam pove, katera okna so počitnice — deklarirano enkrat in uporabljeno dvakrat, da se ne moreta razhajati. |
| `SeedAndIntegrityCallback()` brez parametra | Baza ne rabi več lokaliziranega konteksta, ker ne prevaja. 9 testnih klicnih mest posodobljenih. |

---

# 10. Dokumenti

| Datoteka | Vsebina |
|---|---|
| `docs/audits/2026-09-26-pregled-patcha-in-nacrt.md` | Pregled patcha: kaj je narejeno, kaj ne, P1–P6, L1–L7, tri napake, ki jih je pokazalo šele izvajanje, in načrt faz. |
| `docs/audits/2026-09-26-kyant0-komponente-raziskava.md` | Raziskava: ali knjižnica ponuja komponente (ne), kaj 1.0.0 zna, zakaj nadgradnja ni potrebna, načrt F1–F4 in izid. |
| `docs/audits/2026-09-27-audit-zmogljivosti.md` | Audit zatikanja, glitchev, animacij in stekla. |
| `docs/audits/2026-09-27-kaj-je-na-tej-veji.md` | Ta dokument. |

---

# 11. Kar ostaja odprto

| | Kaj | Zakaj |
|---|---|---|
| **A** | 14 stikal v nastavitvah ustvari 14 slojev | Površine so drobne (52×32 dp) in napravni testi ne kažejo padcev. Izmeriti je treba, ne ugibati. |
| **B** | Steklo v mirovanju računa blur pod neprosojnim diskom | Isto dela katalog. Pravi test je `dumpsys gfxinfo`, ne sklepanje. |
| **C** | Mikroanimacije, ki jih res ni: zamik vrstic seznama, odziv kartic na pritisk izven stekla | Smiselno šele, ko je frame budget zdrav. Po popravku iz §8 je. |
| **D** | Zvezne zaobljenosti (squircle) | Blokirano na Kotlinu 2.3 — glej §7, F4. |
| **E** | `RoutineApp` (446 vrstic) in `GoalsScreen.kt` (1622) | Predobstoječa God Objecta. `GoalsScreen` se ga nisem dotaknil. |

---

# 12. Stanje ob zaključku

* CI zelen na `89975a1`: `build` ✅ + `device-tests` ✅.
* Vseh pet statičnih streh zelenih: paleta, SQLite, prevodi, predstavitev, kontrast (109 preverjanj).
* Nobene viseče reference na odstranjene simbole, nobenega uvoza, ki bi ga pokvarila ta veja.
* PR #12, 19 commitov, `MERGEABLE`.
