# Pregled arhitekture, palete in Liquid Glass — 20. 9. 2026

## Obseg in metoda

Pregled strukture obeh modulov, virov barv, vseh UI-klicev, XML virov, adapterjev za widget/obvestila,
uvoza podatkov ter gradnje/CI. Statični pregled vključuje vso Kotlin produkcijsko kodo in vse XML
qualifierje; za vizualne odločitve so pregledani konkretni uporabniki barv in alfa-kompozicije.
To ni trditev, da so vse poslovne poti formalno dokazane ali fizično preizkušene.

- `core`: 33 produkcijskih Kotlin datotek, čista domena brez Android/Compose odvisnosti;
  koledar, intervali, zdravje, planiranje, ponavljanje, učenje, izvajanje in pogodbe repozitorijev.
- `app`: 84 produkcijskih Kotlin datotek po tej spremembi; 48 UI-datotek preverja presentation gate.
- `AppGraph` poveže Room, DataStore, repozitorije, scheduler, widget in koordinacijo.
- `RoutineViewModel` je UDF-koordinator, `RoutineApp` je host navigacije/dialogov/sheetov.
- Room ostaja vir podatkov; kanonični resolver se uporablja tudi za obvestila in widget.
- Ni sprememb sheme baze, migracij, identitet receiverjev, applicationId ali algoritmov planiranja.
- Barva shranjenega predmeta je uporabniški podatek, ne hardcoded tema: ni množičnega prebarvanja baze.

Meje arhitekture so smiselne za velikost aplikacije. Dodatnih Gradle modulov samo zaradi palete ne
uvajamo. Večji UI/koordinacijski razredi ostajajo kandidat za ločen refactoring; ta PR jih ne prepisuje
in ne meša vizualne prenove s spremembami poslovnega obnašanja.

## Najdbe pred spremembo

1. `RoutineColors` je že obstajal, toda dokumentacija, imena in dejanske vrednosti so si nasprotovali:
   `Amber` je bil turkizen, fokus je bil primarni accent, `TextSecondary` zelenkast.
2. Material surface vloge so bile zamaknjene: `surface` ni označeval osnovnega ozadja,
   `surfaceContainerLowest` pa ni imel svoje barve. Privzeti tonal overlay je lahko obarval površine.
3. NOW markerji so uporabljali `Crimson`, torej isto semantiko kot brisanje in napake.
4. Steklo je imelo blur 16–24 dp, saturacijo 1,8 in brightness +0,06; komentarji so opisovali druge
   vrednosti. Kontrola je imela ničelni nevtralni overlay. Domnevno »solid« fallback je imel alfa < 1.
5. Launcher background je imel še `#0B0E13`. Prejšnji guard je izvzel vse datoteke `ic_*`.
6. JSON import je imel `#3B82F6` / `0x3B82F6` fallback; RGB zapis ni bil normaliziran v opaque ARGB,
   čeprav validacija predmeta zahteva opaque ARGB. Adapter je imel svojo skrito paleto.
7. Poljubna shranjena barva predmeta se je uporabljala tudi kot tekst na preset chipu.
8. Datumi izven meseca so bili zatemnjeni z alfa 0,4, zaključeni Gantt vnosi z alfa 0,55,
   kar je zatemnilo tudi berljive napise.
9. CI je objavil »zadnji zeleni CI« APK že po build jobu, preden je bil znan izid emulator testov.

## Nova paleta in odgovornosti

`DesignSystem.kt` / `RoutineColors` je edini produkcijski vir barv. Vrednosti so odločitev iz briefa,
ne domnevni uradni Apple ali Google tokeni. Odstranjen je stari, zapleten Apple/Lab derivacijski
program z naknadnim overrideom stare palete. Obstoječi CLI `derive_palette.py` zdaj jasno preverja
16 točnih brand tokenov in generira/checka 22 XML mirror vlog.

| Vloga | Barva |
|---|---|
| Background / Material surface | `#090D16` |
| SurfaceLowest | `#05070B` |
| SurfaceLow | `#0F1422` |
| SurfaceContainer | `#151C2E` |
| SurfaceHigh | `#1D263D` |
| SurfaceHighest / SheetSurface | `#26324F` |
| Primarno / sekundarno besedilo | `#F1F5F9` / `#A8B3C2` |
| Dekorativni tretji nivo | `#718096` |
| Subtilni rob | `#263247` |
| Interakcija / timer / fokus | `#2DD4BF` / `#67E8F9` / `#A78BFA` |
| Uspeh / opozorilo / napaka | `#34D399` / `#FBBF24` / `#FB7185` |

Šola, počitek in sintetični/projektni fokus ohranijo ločljive kategorijske akcente. Semantika uspeha
ni več poimenovana po kategoriji počitka. `RoutineCompletionCheckbox` centralizira zelene checkboxe
za zaključke; nastavitveni checkboxi in stikala ostajajo primarni. Napisi niso odvisni od poljubne
barve predmeta. User barve ostajajo na črtah, pikah, izbirniku in subtilnih 6-% tintih nad opaque površino.

### Matrika pregleda vseh blokov/površin

| Področje | Uporabniki / rezultat |
|---|---|
| Dnevni bloki | `TimelineComponents`, `DailyTimeline`: opaque slate kartice, kategorijska črta, sekundarni časi, cyan NOW, zeleni zaključki, ločene napake in opozorila. |
| Aktivno merjenje | `DailyTimeline`: SurfaceHigh, cyan številka med tekom, sekundarna po ustavitvi; akcije ostanejo teal. |
| Samodejne rutine | `ManagedRoutineCard`, `SleepSettings`: berljive sekundarne oznake, recovery kategorija, enaka hierarhija kartic. |
| Tedenski bloki | `OverviewScreens`: opaque tint namesto nevezanega prosojnega polnila; tudi bel/črn uporabniški accent ne določa kontrasta teksta. |
| Mesec | Violet heatmap za fokus, teal današnji datum, berljivi datumi izven meseca brez alfa na celotni celici. |
| Leto / roki | Skupne surface/text/status vloge; oznake in legende še vedno ločijo pomen z besedilom, ne samo barvo. |
| Cilji / CAS / Gantt | Berljivi zaključeni vnosi brez zatemnitve celotnega teksta, success zaključki, warning bližnji roki, error zamujeni. |
| Opravila | Error zamujena, warning današnja, success checkbox zaključka; predmeti ostanejo označeni s piko. |
| Vnos / predloge / uvoz urnika | Kategorijski accent iz palete za napise; poljubna subject barva ni več barva teksta. Input/track vloge so najnižja površina, selected površine dvignjene. |
| Načrtovanje / teme / predmeti | Skupne Material vloge za obrazce, menije in dialoge; brisanje error, akcije primary. |
| Nastavitve / spanje / dnevi | Enaka semantika in berljivi pomožni teksti; temen kontrasten gumb na vključenem teal stikalu. |
| Sheet header/footer | En renderer in material; ločeno vzorčen backdrop brez self-samplinga. |
| Top bar / Fast add | Slate steklo, teal aktivna navigacija in akcija; odstranjeno dodatno turkizno polnilo Fast add. |
| Widget | Isti tokeni/opaque surfaces; cyan aktivni status in progress; XML tekst/surface mirror. |
| Obvestila | XML semantični accent; analitični fokus violet, ne teal. Končni prikaz nadzira Android. |
| Launcher / prvi frame | Launcher background in foreground sledita paleti; window background že uporablja XML mirror. |
| Backup / demo | Privzeta barva se injicira iz `AppGraph`; demo že prej dobiva seznam swatchev. Uporabniške barve se ohranijo. |

## Backdrop 1.0.0: preverjena API in izvedba

Pregledana dejanska izvorna koda taga `1.0.0` v `Kyant0/AndroidLiquidGlass`:

- `effects/ColorFilter.kt`: `vibrancy()` = saturation 1,5. Ne dodajamo še `colorControls()`.
- `effects/Lens.kt`: parametri `refractionHeight`, `refractionAmount`, `depthEffect`,
  `chromaticAberration`. Ne kopiramo `radius`/`depth` imen iz drugega API.
- `BackdropEffectScope.kt`: izpostavlja `size`, `Density`, `shape`.
- Naš vrstni red je `vibrancy → blur → lens`, ne trditev o univerzalno obveznem vrstnem redu.
- Višina refrakcije se omeji z dejanskimi vidnimi vogali in polovico manjše dimenzije.
- `GlassStyle`/`GlassStyles` sta ločena od palete in rendererja. Card preset 6/18/32 in alpha 0,52;
  Compact 4/14/24 in alpha 0,58. To sta začetna preseta za nadzorovan/lokalen backdrop, ne izgovor
  za dodajanje dragega učinka v vsako vrstico seznama.
- Dejanska vrstica uporablja blur 6, sheet 10, kontrola 4 dp. Rob 0,12–0,14 zgoraj, šibkejši
  levo in spodaj/desno. Omejena tilt/touch osvetlitev, brez dodatnih močnih senc.
- Ozadje je slate z lokalnima blue/violet radialnima svetlobama alpha 0,07 / 0,045.
- API < 31: res opaque fallback. API 31–32: blur brez AGSL lens. API 33+: lens knjižnice.

### Namerno odstopanje od predlagane prosojnosti

Na premikajoči vsebini pod top barom/sheetom je lahko celo bel tekst ali bel uporabniški accent.
Alpha 0,52/0,68 ne zagotavlja AA za `#A8B3C2`. Zato dejanski scrolling Bar/Sheet uporabljata **0,82**,
Control **0,84** (vključno z najmočnejšim touch highlightom). Ni pošteno razglasiti AAA na podlagi
kontrasta samo na temnem ozadju. Lokalne Card/Compact nastavitve ostajajo lahkotnejše.

Primeri iz avtomatskega izračuna (sRGB kompozicija; GPU slika je ločeno preverjanje):

- TextPrimary na Background: **17,74 : 1**.
- TextSecondary na Background: **9,15 : 1**.
- Bar/Sheet nad čisto belim backdropom + maksimalnim tilt highlightom:
  TextPrimary **9,59 : 1**, TextSecondary **4,95 : 1**, Primary **5,64 : 1**.
- Control nad belim + tilt + touch: TextSecondary **4,97 : 1**, Primary **5,67 : 1**.
- `#718096` ni uporabljena za drobne vsebinske napise: na SurfaceHighest je približno **3,17 : 1**.

`check_contrast.py` pokriva 215 kombinacij: vse glavne površine, akcente, šest kategorij, tinted
kartice z belim/črnim user accentom, ambient, heatmap, pulziranje, vse glass role in fallback.
To ni certifikat WCAG za celotno aplikacijo; dekorativni robovi niso interaktivni indikatorji.

## Hardcoded barve in varnost podatkov

- Literali so dovoljeni v `RoutineColors` in generiranem `colors.xml`.
- Edina izjema risbe je bela alpha maska `ic_notification.xml`, ki jo obarva Android.
- `Color(subject.colorHex)` in Color iz izbirnika sta uporabniška podatka; guard ju ne prepoveduje.
- Bitni maski v `SubjectColorCodec` in domenski ARGB validaciji nista barvni odločitvi.
- `SubjectColorCodec` normalizira stare RGB/kratke RGB in aktualne ARGB zapise. Napačni/manjkajoči
  zapisi dobijo injicirano privzeto barvo. Dodani so trije regresijski testi.
- Stari auditi v `docs/audits` so zgodovinski dokumenti, zato njihovih HEX primerov ne prepisujemo.

## Preverjanje in objava

Lokalno brez Android SDK/JDK: palette drift/XML check, 215 contrast kombinacij, presentation guard
in 19 SQLite integrity testov. Dodani so štirje Kotlin testi za Material vloge, semantiko, user
swatche in glass fallback; te izvrši GitHub CI skupaj z obstoječimi JVM/Android testi.

GitHub workflow gradi debug in **R8 release**, izvaja core/app unit teste, lint in API 35 emulator
suite s screenshot artefakti. Interni PR ali ročni workflow objavi `test-<commit>` prerelease + SHA-256 šele po
uspehu **obeh** jobov. Fork PR nima dovoljenja za objavo; interni PR dobi lastno testno izdajo
iz preverjenega merge commita, ne prepisuje globalnega `debug-latest`.

APK uporablja obstoječi testni/debug podpis. To ni produkcijsko podpisana trgovinska izdaja.
Ključ runnerja se lahko spremeni: če posodobitev ni mogoča, najprej izvoz backup, nato odstranitev
in ponovna namestitev; nikoli slepo odstranjevanje aplikacije s podatki.

## Fizični test, ki ostaja potreben

1. Namestitev release APK; obstoječi backup/restore in preverjanje predmetov stare palete.
2. Dnevni/teden/mesec/leto, cilji, opravila, dodajanje, urejanje, vsi settings zavihki in dialogi.
3. Scroll svetle vsebine pod top barom/Fast add in pod sheet header/footer; preveri droben tekst.
4. Aktivni/ustavljeni timer, NOW pulse, zaključki, bližnji/zamujeni roki in napake.
5. Android 7–11 fallback, Android 12 blur in Android 13+ lens; widget na svetlem launcherju.
6. Velika pisava, TalkBack in Remove animations; svetla/temna okolica na OLED napravi.
7. Najšibkejša ciljna fizična naprava: frame timing, scroll, toplota in baterija. Manjši blur sam po
   sebi še ni dokaz boljše zmogljivosti; emulator ne nadomesti GPU meritve.
