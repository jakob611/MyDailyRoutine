# Prompti za zunanje AI — tretja izdaja (22. 9. 2026)

> **Ta dokument nadomešča §8 v `docs/audits/2026-09-21-recenzija-videz-koda-in-smer.md`.**
> Zakaj nova izdaja: prejšnja je aplikacijo opisala po spominu in na kratko. Zdaj je o njej znano
> več — natančne barve z vrednostmi, kaj je na katerem zaslonu, kje stoji kateri gumb, kako se
> premikaš med obdobji, kaj se zgodi ob dotiku in kaj ob vleku. Prompt brez teh dejstev dobi
> splošne nasvete ("poskrbi za hierarhijo", "povečaj kontrast"); prompt z njimi dobi odgovore, ki
> jih je mogoče uporabiti ali zavrniti.
>
> Pravilo, ki velja za vseh osem: **noben model ne vidi kode in noben ne piše kode.** Vprašanje je
> vedno isto, le z različnih strani: **ali je to primerno dati noter, ali je na pravem mestu, ali
> je psihološko dobro in ali je privlačno.**

## Kako jih uporabljati

1. Za eno nalogo porabi en pogovor. Ne mešaj dveh vprašanj v enem promptu — odgovor se razmaže.
2. Kopiraj **KONTEKST** (spodaj) in za njim **eno** nalogo (8.1–8.8). Naloga sama pove, kaj
   priložiti.
3. Kjer piše **【PRILOGA】**, priloži točno to. Posnetki naj bodo s pravega telefona v temni sobi
   (steklo, sence in OLED črna se na emulatorju vidijo drugače), v slovenščini, ker je to jezik,
   v katerem bo aplikacijo videl prvi uporabnik.
4. Odgovor vzemi kot gradivo za odločitev, ne kot odločitev. Vsak odgovor razdeli na tri kupčke:
   **naredim zdaj** (majhen obseg, jasna korist), **preverim** (potrebuje podatek, ki ga nimam),
   **zavrnem** (pove, zakaj — če ne zna povedati zakaj, ne sodi v noben kupček).
5. Če model ponudi deset predlogov, je uporabnih običajno prvi ali drugi; ostali so variacije.
   Zahtevaj, da jih sam omeji — zato je v vsakem promptu omejitev števila.

---

## KONTEKST (kopiraj pred vsako nalogo)

```
APLIKACIJA: LockIn — dnevni ritem za dijaka mednarodne mature (IB) v Sloveniji, 16–19 let.
Uporabnik: najprej avtor sam, nato sošolci, morda širše.
Platforma: Android, nativno, samo v slovenščini in angleščini. Brez računa, brez omrežja
(dovoljenje za internet je odstranjeno), brez analitike in oglasov; podatki ostanejo na telefonu.
Varnostna kopija v oblaku je načrtovana za pozneje (uporabniško ime + geslo, obnovitev vseh
nastavitev) in je zdaj NI.
Načela: naloži se takoj in je takoj uporabna; v ozadju ne teče nič in ne pije baterije; nikoli ne
sme biti ovira. Cilj je poravnan dnevni ritem, vsakodnevna optimizacija, resničen počitek, nič
frustracije. Ni pritiskov, ni opominov s krivdo, ni tekmovanja s prijatelji, ni serij (streaks),
ni točk in ni nagrad.

VIDEZ: temna tema, prijazna OLED zaslonom: podlaga #090D16, kartice #151C2E, tanke hairline obrobe
(10–14 % bele), mehki vogali. Vse, kar lebdi nad vsebino (zgornja vrstica, gumbi, plošče, spodnji
listi), je steklo: vidi se vsebina pod njim, svetel rob zgoraj in odsev ob nagibu telefona.
Barve poudarkov: primarna turkizna #2DD4BF, ura in časovnik #67E8F9, fokus #A78BFA, uspeh in
počitek #34D399, opozorilo #FBBF24, rok in napaka #FB7185. Besedilo: #F1F5F9 (primarno),
#A8B3C2 (sekundarno), #718096 (tiho). Pisava Roboto Flex z optično velikostjo (naslov zaslona
34 sp → oznaka 12 sp); številke v vrstah so poravnane v stolpce. Vsi odmiki so iz ene lestvice
(4/8/12/16/24 dp), vsaka tarča za dotik je visoka vsaj 48 dp, pogoste 56 dp.

ZASLONI IN POTI: spodaj ni navigacijske vrstice. Zgoraj lebdi zaobljena steklena vrstica: levo
znak aplikacije oziroma ime obdobja, desno štirje stekleni ikonski gumbi — načrtovalnik in
čakalna vrsta, naloge (z rdečo piko, če je kaj zapadlo), cilji (CAS/EE) in nastavitve. Pod njo je
vrstica z datumom: puščica nazaj · širok steklen čip z datumom (dotik odpre koledar) · čip
»Danes« · puščica naprej. Pod njo so štirje enako široki zavihki obdobja — Dan, Teden, Mesec,
Leto — po katerih se poudarek pelje kot ena kapsula. Spodaj desno lebdi turkizna pilula z ikono
plus in napisom »Dodaj blok« (56 dp); dotik jo razširi v spodnji list. Vsi vnosi in vse
nastavitve so spodnji listi s stekleno glavo (naslov, podnaslov, križec) in lepljivo stekleno
nogo z enim polnim in enim obrobljenim gumbom.

VSEBINA ZASLONOV: Dan = dan od 7. do 21. ure: naslov »Prostor za pomembne stvari.«, tri ploščice
povzetka (Fokus, Počitek, Opravljeno 3/9), vrstica obremenitve dneva, nato časovnica s karticami
blokov (barvni rob nosi kategorijo, naslov, vrstica »8:20–9:05 · 45 min«), rdeča črta »zdaj«,
kartica predlogov za ravnotežje (»＋10 min pavze«), dnevni povzetek in preskočeni bloki z gumbom
»Obnovi«. Kartica se ob dotiku razširi; z dolgim pritiskom in vlekom se premakne na drugo uro v
korakih po 15 minut, ob vsakem koraku nežno klikne. Teden = mreža sedmih dni, ki se pomika
vodoravno; dotik stolpca odpre dan. Mesec = mreža dni, kjer močnejša barva pomeni več načrtovanega
fokusa, rdeče pike pomenijo teste in roke, zelenkasta barva pouka prost dan; spodaj je legenda.
Leto = število dni do konca pouka in trije zložljivi razdelki (ravnotežje po mesecih, radar rokov
IB, prostor za počitek). Cilji = vrstica čipov s projekti (CAS, EE, lastni), zavihki Pregled /
Aktivnosti / Mejniki / Napredek, časovnica po mesecih in gumb »Dodaj aktivnost«. Nastavitve = pet
zavihkov: Ritem, Opomniki, Načrt, Pravila, Podatki.

VNOS BLOKA (spodnji list): hitri predlogi (»90 min fokusa«, »Kosilo«, šolska ura), čipi
predmetov, vrsta vnosa (blok / rok / izpit), polji Začetek in Konec (kolo z urama in minutama),
pri šolskih blokih stikalo za odmor med urami, ponavljanje po dnevih tedna, najkrajši čas,
prožnost, prednost, oznaka »fiksno« in gumb »Dodaj v moj dan«.

OBNAŠANJE: nič ne teče v ozadju; opomniki so sistemski alarmi z akcijama »Začni« in »Zabeleži
minute«; vodoravno podrsanje premakne dan/teden/mesec, navpično drsenje ostane drsenju; ob zamudi
aplikacija sama poravna proste bloke, fiksnih obveznosti in počitka pa se ne dotakne; med poukom
so obvestila tiha; zvok, vibracije in gibanje se dajo izklopiti. Prvi zagon ima kratek uvod (ime,
vprašanje o brezplačni varnostni kopiji v oblaku, nekaj nastavitev in hiter pregled, kaj pomeni
kaj) s preskočitvijo.
```

---

## 8.1 Vizualna kritika posnetkov — za model z vidom

**Kaj odloča:** katere tri stvari na zaslonu so videti slučajno in ne namerno; ti popravki so
običajno poceni in jih vidiš takoj.
**Priloga:** 6–10 posnetkov: dan na vrhu seznama in na sredini, teden, mesec, leto, cilji, spodnji
list za vnos, nastavitve.

```
Si art director za mobilne aplikacije, ki je vodil videz izdelkov, za katere ljudje rečejo "to pa
je lepo narejeno", in obenem dovolj strog, da poveš, kaj je videti slučajno.

Priloženi so posnetki zaslona aplikacije LockIn. Kontekst je spodaj; preberi ga, preden gledaš
slike, in ga ne spreminjaj.

[KONTEKST]

NALOGE, v tem vrstnem redu:
1. Prvi vtis v treh stavkih: kaj je ta aplikacija in kako se počuti. Brez vljudnosti.
2. Kar izgleda SLUČAJNO in ne namerno: največ 5 stvari, vsaka s posnetkom in mestom na njem
   ("na 3. posnetku levo zgoraj, v vrstici z datumom"). Za vsako: zakaj izgleda slučajno in kaj
   bi naredilo, da bi izgledalo namerno.
3. Hierarhija: na vsakem posnetku povej, kam gre oko najprej in kam drugič, ter ali je to tisto,
   kar uporabnik takrat potrebuje. Kjer oko obstane na napačnem mestu, povej, kaj bi moralo biti
   tišje, manjše ali nižje.
4. Kar je videti poceni: prevelika pisava, premočna barva, preveč senc, neenaki odmiki, ikone
   različnih debelin, barve, ki se med sabo tepejo. Konkretno, po posnetkih.
5. Ali je to, kar je videti privlačno, tudi psihološko prav: ali barve poudarkov povedo isto, kar
   povedo besede (turkizna = moje, cijan = ura, vijolična = fokus, jantarna = pozor)?
6. Privlačnost: največ 3 predlogi, po katerih bi sošolec rekel "lepa je", in vsak ne sme dodati
   nove funkcije.
7. Kaj bi ODSTRANIL (največ 3 stvari), vsaka z odgovorom, kaj se s tem izboljša.

ZAHTEVE:
- Vsaka trditev se sklicuje na posnetek in mesto na njem. Nobenih splošnih nasvetov o "dobrem
  UX-u", nobenih primerjav z drugimi aplikacijami po spominu.
- Ne predlagaj novih funkcij, novih ikon, novih pisav in ne spreminjaj barv blagovne znamke.
- Ne piši kode in ne omenjaj programskih knjižnic: govori o videzu, teži, barvi in razporeditvi.
- Vsako trditev označi: NAPAKA / OKUS / VPRAŠANJE. Pri OKUSU povej, kdo bi se s tabo ne strinjal.

IZHOD: 1) prvi vtis, 2) tabela slučajno → predlog (največ 5 vrstic), 3) hierarhija po posnetkih,
4) poceni, 5) barve proti besedam, 6) trije predlogi, 7) tri odstranitve.
```

**Kaj naj vrne:** tabelo slučajno→predlog, ki se sklicuje na mesto na posnetku, in največ tri
odstranitve.
**Rdeča zastavica:** če odgovor našteva stvari, ki jih na posnetkih ni (ali jih opisuje napačno),
model ni gledal slik — ponovi z manj posnetki, a z označenimi mesti.

---

## 8.2 Prostor, razporeditev in gibanje palca — za mobilnega UX oblikovalca

**Kaj odloča:** ali človek tisto, kar uporablja večkrat na dan, zadene, ne da bi pogledal; in
koliko spodrsljajev mu pripraviš z vodoravnim podrsanjem.
**Priloga:** ni nujna; koristita dva posnetka dneva (vrh in sredina) in en posnetek tedna.

```
Si oblikovalec mobilnih vmesnikov za telefone, ki jih ljudje držijo v eni roki, med hojo, med
poukom, pod mizo. Tvoje merilo ni lepota, ampak: ali človek to zadene, ne da bi pogledal.

Aplikacija: LockIn (kontekst spodaj; preberi in ga ne spreminjaj). Uporabnik jo odpre večkrat na
dan za nekaj sekund: pogleda, kaj je naslednje, obkljuka, premakne, zapre.

[KONTEKST]

NALOGE:
1. Cona palca: razvrsti tarče, ki jih uporabnik uporablja najpogosteje — zavihke Dan/Teden/Mesec/
   Leto, vrstico z datumom, pilulo »Dodaj blok«, štiri ikonske gumbe v zgornji vrstici in tri
   tarče na kartici bloka — v tri skupine: zlahka dosegljivo / s preprijemom / nedosegljivo, ko
   telefon držiš v eni roki.
2. Je v zgornji tretjini preveč tarč? Če da, kaj bi premaknil dol in kaj bi ostalo zgoraj, in
   zakaj. Upoštevaj, da je zgornja vrstica edini vhod v načrtovalnik, naloge, cilje in nastavitve.
3. Tri tarče na kartici bloka (razširi, zaženi, obkljukaj) in pilula »Dodaj blok«: sta na pravem
   mestu? Če bi jih prestavil, povej, kaj se s tem poslabša (npr. hitrost obkljukanja, doseg).
4. Spodrsljaji: vodoravno podrsanje premakne dan/teden/mesec, navpično drsenje ostane drsenju,
   dolg pritisk na kartici jo vleče v korakih po 15 minut, dotik kartico razširi. Kje bo
   uporabnik po nesreči premaknil obdobje, ko je hotel samo drseti seznam, in kje bo začel vleči
   kartico, ko je hotel drseti? Kako pogosta sta ta dva spodrsljaja in kaj bi ju zmanjšalo brez
   dodajanja gumbov?
5. Gibanje: menjava obdobja drsi v smeri potovanja, menjava merila (dan → teden) se prelije,
   spodnji list se rodi iz pilule, predlog za ravnotežje se prikaže sam. Katero gibanje bi
   opustil, katero okrepil, in kje gibanje laže o tem, od kod je človek prišel?
6. Psihološka presoja položaja: ali je pilula »Dodaj blok« na mestu, ki spodbuja, ali na mestu, ki
   pritiska? Je obvestilo o zapadlem (rdeča pika na ikoni nalog) vidno, a ne vsiljivo?
7. Na koncu: če bi smel spremeniti natanko TRI stvari, katere in zakaj.

ZAHTEVE:
- Vsak predlog mora povedati, kaj se POSLABŠA z njim. Ni brezplačnih izboljšav.
- Ne omenjaj kode, knjižnic in meritev, ki jih ne vidiš. Ne predlagaj novih zaslonov.
- Slovenščina; strokovne izraze lahko dodaš v angleščini v oklepaju.

IZHOD: 1) cone palca (tabela), 2) preveč zgoraj, 3) kartica in pilula, 4) spodrsljaji, 5) gibanje,
6) psihološka presoja, 7) tri spremembe.
```

**Kaj naj vrne:** tabelo con palca in največ tri spremembe, vsako z navedeno ceno.
**Rdeča zastavica:** predlogi, ki zahtevajo nov zaslon ali novo funkcijo, niso odgovor na to
vprašanje — črtaj jih.

---

## 8.3 Prvi zagon: pot do prvega uspeha — za oblikovalca in psihologa navad

**Kaj odloča:** ali dijak v prvih petih minutah dobi dan, v katerem vidi svoj blok in ga obkljuka,
ali pa aplikacijo zapre s praznim dnem in se ne vrne.
**Priloga:** posnetki prvega zagona od začetka do praznega dneva (tudi praznega dne, ker je to
najverjetnejši končni zaslon prvega zagona).

```
Si oblikovalec vstopnih tokov in psiholog navad. Tvoja naloga ni "narediti onboarding", ampak
narediti, da dijak v manj kot petih minutah dobi dan, v katerem vidi svoj prvi blok in ga
obkljuka — in da se ob tem ne počuti, kot da je podpisal pogodbo.

Izdelek: LockIn (kontekst spodaj; ne spreminjaj ga). Stanje ob prvem zagonu: prazno, brez
podatkov. Aplikacija zna pripraviti šolski urnik in zna urnik prebrati iz besedila, prilepljenega
s šolske strani (ManageBac), a je oboje shranjeno v nastavitvah, pod zavihkom Podatki. Prvi zagon
ima kratek uvod: ime, vprašanje o brezplačni varnostni kopiji v oblaku, nekaj nastavitev in hiter
pregled, kaj pomeni kaj — z možnostjo preskoka. Varnostne kopije še NI, načrtovana je za pozneje.

[KONTEKST]

NALOGE:
1. Zasnuj pot prvega zagona: največ 4 koraki, vsak z namenom, besedilom in tem, kaj uporabnik
   vidi. Za vsak korak: kaj mora izvedeti zdaj in kaj sme izvedeti pozneje.
2. Za vsako vprašanje, ki ga želiš postaviti (ime, varnostna kopija, nastavitve):
   - ali je primerno vprašati TAKOJ in ne pozneje,
   - kaj se zgodi, če uporabnik ne odgovori (privzeto mora biti varno in koristno),
   - kako vprašati, da ne zveni kot obrazec.
   Posebej presodi vprašanje o varnostni kopiji: funkcije ni, zato povej, ali jo omeniti zdaj,
   kako in zakaj — ali je bolje molčati, dokler ne dela. Upoštevaj, da je to edino vprašanje, na
   katero odgovor vpliva na to, kar boš nekoč lahko obnovil.
3. Prvi uspeh: kaj naj bo prvi blok, s katerim dijak začne, in koliko dotikov do njega. Povej,
   kaj bi bilo premalo (prazna aplikacija) in kaj preveč (trije zasloni navodil).
4. Kaj naj bo ob prvem zagonu VIDETI, da je aplikacija živa: predlog za dan, prazna kartica s
   klicem k dejanju, primer? Za vsako možnost povej, kaj sporoča in kaj obljublja.
5. Zaključek prvega zagona: kaj naj človek občuti, ko je blok shranjen, in kaj naj vidi, ko se
   vrne čez uro. Ne čestitaj prazno.
6. Kar bi izpustil: največ 3 stvari, ki jih drugi izdelki silijo v prvi zagon in so tu odveč.

ZAHTEVE:
- Vsaka odločitev ima en stavek psihološke utemeljitve (kognitivna obremenitev, občutek izgube,
  privzetek, občutek lastništva, uční pripomoček "majhen prvi korak").
- Brez temnih vzorcev: brez umetne krivde, brez pritiska "povabi prijatelje", brez umetnega
  časovnega roka, brez lažne nujnosti, brez prosjačenja za ocene ali dovoljenja.
- Predlagana besedila napiši v slovenščini in v angleščini, kratko (do 12 besed na vrstico).
- Ne piši kode.

IZHOD: 1) pot s štirimi koraki (tabela), 2) vprašanja (tabela: kdaj / če ni odgovora / kako
vprašati), 3) prvi uspeh, 4) kaj je videti, 5) zaključek, 6) tri izpustitve.
```

**Kaj naj vrne:** štiri korake z besedili in utemeljitvijo za vprašanje o oblaku.
**Rdeča zastavica:** vsak predlog, ki uporabnika ustavi z obrazcem, preden vidi svoj dan, je
napačen odgovor za to aplikacijo — tudi če je psihološko "dokazan".

---

## 8.4 Psihološka presoja funkcij — štiri vprašanja za vsako — za vedenjskega znanstvenika

**Kaj odloča:** katere funkcije so psihološko dobre, katere so le pametne na papirju in katere je
bolje izpustiti.
**Priloga:** seznam funkcij iz KONTEKSTA je dovolj; po potrebi dodaj posnetek dneva.

```
Si raziskovalec vedenjske ekonomije in psihologije navad, ki zna ločiti, kdaj je funkcija v
aplikaciji v pomoč, kdaj je le zgledna in kdaj človeka tiho obremenjuje. Ne pišeš kode in ne
predlagaš novih funkcij: presojaš tiste, ki so.

Izdelek: LockIn (kontekst spodaj; preberi in ne spreminjaj).

FUNKCIJE ZA PRESOJO (vsaka naj dobi svojo vrstico v tabeli):
1. Tri ploščice povzetka dneva: Fokus, Počitek, Opravljeno 3/9.
2. Vrstica obremenitve dneva (koliko dneva je zasedenega).
3. Predlog za ravnotežje (»＋10 min pavze«), ki se pojavi sam.
4. Samodejno poravnavanje prostih blokov ob zamudi (fiksne obveznosti in počitek se ne premakne).
5. Vlečenje kartice v korakih po 15 minut z nežnim klikom ob vsakem koraku.
6. Preskočeni bloki z gumbom »Obnovi« (nič se ne izgubi, le odloži).
7. Radarska slika rokov IB na letnem pogledu in števec dni do konca pouka.
8. Cilji (CAS/EE) z mejniki in časovnico po mesecih.
9. Opomniki kot sistemski alarmi z akcijama »Začni« in »Zabeleži minute«.
10. Tihi način med poukom in možnost, da človek izklopi zvok, vibracije in gibanje.
11. Zapis dejanskega časa: ko blok označiš kot opravljen, se zabeleži, koliko je res trajal.

ZA VSAKO OD NJIH ODGOVORI NA ŠTIRI VPRAŠANJA:
a) Ali je primerno, da je funkcija v aplikaciji te vrste (dijaški dnevni ritem, brez pritiskov)?
b) Ali je na pravem mestu oziroma se pojavi ob pravem trenutku (ne prej, ne pozneje)?
c) Ali je psihološko dobra: kaj človeku pomaga, kaj mu jemlje, in na katero znano težavo ljudi
   se opira? Sklicuj se na raziskave ali mehanizme, ki jih znaš pojasniti v enem stavku.
d) Ali je privlačna: si jo človek želi uporabljati, ali jo uporablja, ker "mora"?

ZAHTEVE:
- Vsaka vrstica tabele ima: funkcija | sodba (obdrži / spremeni / izpusti) | zakaj (en stavek) |
  kaj bi moralo veljati, da bi sodba padla (kaj bi te prepričalo v nasprotno).
- Loči, kar je dokazano, od tega, kar je tvoja presoja. Kjer nisi prepričan, to povej.
- Upoštevaj, da ta aplikacija NE uporablja serij, točk, tekmovanja in krivde — vsak predlog, ki
  temelji na tem, je treba označiti kot neprimeren za ta izdelek.
- Brez novih funkcij; če meniš, da nekaj manjka, povej to v enem ločenem odstavku na koncu.

IZHOD: tabela z enajstimi vrsticami in štirimi stolpci, nato največ tri vrstice "kaj bi izpustil
najprej" in odstavek "kaj manjka (in ne predlagam, da se doda)".
```

**Kaj naj vrne:** eno tabelo z jasno sodbo za vsako od enajstih funkcij.
**Rdeča zastavica:** enaka sodba za vse vrstice ("zelo dobro") ali sklicevanje na raziskave brez
pojasnila pomeni, da model ni presojal, ampak hvalil.

---

## 8.5 Barve, kontrast in barvna slepota — za strokovnjaka za vizualno dostopnost

**Kaj odloča:** ali aplikacijo uporablja tudi tisti, ki rdeče ne razlikuje od zelene, in tisti,
ki ima telefon v soncu.
**Priloga:** posnetki dneva in meseca v Slovenščini, po možnosti tudi posnetek z vklopljenim
načinom visokega kontrasta ali povečane pisave, če ga imaš.

```
Si strokovnjak za dostopnost vida. Presojaš barve, kontrast in razločljivost vmesnika, ki ga
bodo uporabljali tudi ljudje z barvno slepoto in ljudje s telefonom v roki na soncu.

Izdelek: LockIn (kontekst spodaj; barve in pisava so v njem). Posnetki so priloženi.

NALOGE:
1. Kontrast: za vsak par besedilo-podlaga, ki ga vidiš na posnetkih, oceni razmerje kontrasta in
   povej, ali dosega 4,5:1 (navadno besedilo), 3:1 (veliko besedilo) oziroma 3:1 (robovi in
   ikone, ki nosijo pomen). Kjer ne dosega, povej, katero barvo bi moral uporabiti namesto nje.
2. Barvna slepota: aplikacija pomen nosi tudi z barvo — močnejša barva na mesecu pomeni več
   fokusa, rdeče pike pomenijo teste in roke, barvni rob kartice nosi kategorijo, turkizna je
   glavno dejanje. Preveri vsak od teh primerov posebej: kaj vidi človek z deuteranopijo,
   protanopijo in tritanopijo, in ali pomen ostane berljiv tudi brez barve (oblika, znak, beseda,
   debelina, položaj). Kjer ne ostane, predlagaj najmanjšo spremembo, ki pomen ohrani.
3. Temna tema in OLED: kjer je črna podlaga prazna, povej, ali je to prednost ali praznina; kjer
   se steklo usede na temno, preveri, ali besedilo na steklu ostane berljivo tudi takrat, ko je
   pod njim svetla kartica.
4. Pisava: razmerja velikosti med naslovom zaslona (34 sp) in oznako (12 sp) so velika. Presodi,
   ali je najmanjša velikost za človeka, ki bere na telefonu v gibanju, še primerna, in kaj se
   zgodi, ko uporabnik poveča pisavo v sistemu.
5. Privlačnost proti dostopnosti: naštej največ 3 mesta, kjer bi strokovnjak za dostopnost
   zahteval spremembo, ki bi aplikacijo naredila manj privlačno. Za vsako povej, katera stran naj
   popusti in zakaj.

ZAHTEVE:
- Vsaka ocena kontrasta mora imeti zraven svojo številko in par barv, na katerem je izračunana.
  Če številke ne moreš izračunati natančno, povej približek in napiši, da je približek.
- Ne predlagaj spremembe barve, ki je hkrati blagovna znamka (turkizna #2DD4BF), brez pojasnila,
  kaj se s tem izgubi; raje predlagaj drugačno podlago ali debelino.
- Ne piši kode. Ne omenjaj orodij, ki jih ne poznaš.

IZHOD: 1) tabela parov kontrasta z oceno in predlogom, 2) barvna slepota po primerih, 3) OLED in
steklo, 4) pisava, 5) trije spopadi dostopnost-proti-privlačnosti.
```

**Kaj naj vrne:** tabelo kontrastov s številkami in ločeno presojo barvne slepote po primerih.
**Rdeča zastavica:** številke brez navedenega para barv so uganjanje — vprašaj znova, naj pokaže
izračun.

---

## 8.6 Mikrobesedila in ton v dveh jezikih — za pisca besedil in psihologa komunikacije

**Kaj odloča:** ali aplikacija zveni kot nekdo, ki ti pomaga, ali kot nekdo, ki ti meri čas.
**Priloga:** posnetki z besedili (naslov dneva, ploščice povzetka, predlog za ravnotežje, prazno
stanje ciljev, spodnji list za vnos z gumbi, nastavitve) — v slovenščini; angleške izraze lahko
izpišeš iz seznama spodaj.

```
Si pisec vmesniških besedil in psiholog komunikacije. Aplikacija je v slovenščini (prvi jezik
uporabnika) in angleščini. Tvoje delo je presoditi ton, ne prepisati vsega.

Izdelek: LockIn (kontekst spodaj; ne spreminjaj ga). Besedila, ki jih presojaš, so na posnetkih.

NALOGE:
1. Preberi vsa besedila na posnetkih in za vsako povej: kaj sporoča (ne kaj piše), ali je to
   povedano na pravem mestu in ob pravem trenutku, in ali je ton enak povsod (prijazen, miren,
   brez ukazov, brez krivde, brez čestitk, ki jih nič ne zasluži).
2. Poišči največ 5 besedil, ki zvenijo kot ukaz, kot opravilo, kot očitek ali kot marketinška
   fraza. Za vsako napiši, zakaj, in predlagaj novo — največ 12 besed, v slovenščini IN angleščini.
3. Presodi naslov dneva »Prostor za pomembne stvari.« in naslov ciljev »CAS in EE«: ali je prvi
   dovolj konkreten, da veš, kje si, in ali je drugi razumljiv nekomu, ki CAS in EE pozna, a ne
   ve, kaj ima aplikacija z njima?
4. Prazna stanja in trenutki, ko gre kaj narobe: prazna dnevna časovnica, ni ciljev, blok
   preskočen, obvestilo o zapadlem. Za vsakega napiši besedilo (slovensko in angleško), ki pove,
   kaj se je zgodilo in kaj je naslednji majhen korak — brez krivde in brez opravičevanja.
5. Ali je psihološko prav, da aplikacija človeka nikoli ne pohvali ("bravo", "super")? Utemelji z
   mehanizmom notranje proti zunanji motivaciji in povej, kaj naj namesto pohvale naredi vmesnik.
6. Angleščina: povej, kje je treba uporabiti "you" in kje nič ("Start" raje kot "Start now"),
   in kje slovenski besedni red zahteva drugačen naslov kot angleški.

ZAHTEVE:
- Za vsako predlagano besedilo povej, kaj je slaba stran (npr. krajše = hladnejše).
- Ne piši kode. Ne predlagaj novih funkcij in ne spreminjaj pomena.
- Upoštevaj, da aplikacija ne laže in ne obljublja: brez "najboljša", brez "v 5 minutah boš",
  brez "zadnja priložnost".

IZHOD: 1) presoja tona, 2) tabela slabih besedil z novimi (SL + EN), 3) dva naslova, 4) prazna
stanja (SL + EN), 5) pohvala in motivacija, 6) opombe o angleščini.
```

**Kaj naj vrne:** največ pet konkretnih zamenjav v obeh jezikih, ne novega slovarja pojmov.
**Rdeča zastavica:** predlagana besedila, daljša od obstoječih, so v tem vmesniku skoraj vedno
slabša — krajše je bolje, dokler pomen ostane.

---

## 8.7 Navada in motivacija brez pritiska — za oblikovalca vedenjskih sistemov

**Kaj odloča:** ali se aplikacija uporablja vsak dan tudi po tretjem tednu, ne da bi uporabnika
silila, in kaj je tisto, kar ga pripelje nazaj.
**Priloga:** ni nujna.

```
Si oblikovalec vedenjskih sistemov. Vodiš ljudi v navado, ki se vzdrži brez pritiska: brez serij,
brez točk, brez lestvic, brez tekmovanja, brez opominov, ki bi hodili po robu krivde.

Izdelek: LockIn (kontekst spodaj; ne spreminjaj ga). Uporabnik je dijak, ki ima že zdaj preveč
opomnikov in premalo občutka, da dan drži v rokah. Aplikacija nima analitike in ne pošilja
podatkov nikamor, zato ne moreš predlagati ničesar, kar zahteva strežnik ali merjenje uporabnika.

NALOGE:
1. Kaj je v tem izdelku trenutek, po katerem se človek vrne? (namig: kontekst opisuje blok, ki ga
   obkljuka, ravnotežje, ki se samo popravi, in dan, ki ob zamudi ne razpade). Povej, ali je
   dovolj močan, in zakaj.
2. Zasnuj navado, ki se vzdrži: katero vedenje naj postane vsakodnevno, kako majhno mora biti in
   kaj ga sproži (ob kateri priložnosti, ne ob kateri uri). Upoštevaj, da aplikacija ne sme
   opominjati v prostem času med poukom.
3. Kar bi človeka odvrnilo: največ 5 stvari, ki bi ga v prvem mesecu odgnale — pretirano
   prilagajanje, zahteva po popolni evidenci, občutek, da ga nekdo nadzoruje, napor ob vnosu,
   občutek krivde ob preskočenem bloku.
4. Notranja in zunanja motivacija: kaj v tem izdelku ostane notranje in kaj je videti zunanje,
   čeprav ni mišljeno tako (npr. števec opravljenih blokov, barva meseca, ki postane močnejša).
   Za vsako poudari, ali naj jo okrepimo, umirimo ali odstranimo.
5. Dolgoročno: kaj naj aplikacija naredi v tretjem tednu, ko je prvotna radovednost mimo? Predlagaj
   največ dve stvari, ki ne uvajata novih funkcij, ampak drugače uporabita tisto, kar je že
   notri.
6. Presodi pošteno: ali je ta izdelek sploh pravi za navado, ali je bolj orodje, ki ga človek
   uporabi, ko mu gre dobro, in pusti, ko mu gre slabo? Kaj bi moral izdelek narediti za drugo
   polovico uporabnikov, ki jim gre slabo?

ZAHTEVE:
- Vsaka trditev naj pove mehanizem ("ker ... zato ..."), ne le nasveta.
- Sklicuj se na raziskave ali modele, ki jih znaš pojasniti v enem stavku, in povej, kadar gre za
  tvojo presojo.
- Brez serij, točk, lig, lestvic in tekmovanja — tudi če so dokazano učinkoviti: ta izdelek jih
  ne uporablja namenoma. Smeš pa pojasniti, kaj s tem izgubi in kaj pridobi.

IZHOD: 1) trenutek vrnitve, 2) navada (vedenje, velikost, sprožilec), 3) pet odvračal, 4) notranje
proti zunanjemu, 5) tretji teden, 6) poštena presoja za slabe dni.
```

**Kaj naj vrne:** mehanizme, ne nasvetov: "ker ... zato ...", z ločenim odgovorom za slabe dni.
**Rdeča zastavica:** če predlaga serije, točke ali tekmovanje, ni prebral načel izdelka.

---

## 8.8 Prvi vtis in ponudba — za tržnika, ki ne sme pretiravati

**Kaj odloča:** kaj aplikacija obljubi, ko jo človek prvič vidi v živo ali v opisu v trgovini, in
ali ta obljuba zdrži prvih pet minut.
**Priloga:** dva posnetka (dan in mesec) in 4–6 stavkov, ki jih aplikacija uporablja kot svoj opis
— iz Piš I v §2 tega dokumenta, če obstaja. Sicer jih napiši sam iz konteksta.

```
Si tržnik, ki ne sme pretiravati: tvoj uspeh je, da človek po namestitvi ne reče "to ni to".
Pripravljaš prvi vtis in opis aplikacije za slovenski trg (in pozneje angleškega).

Izdelek: LockIn (kontekst spodaj; ne spreminjaj ga). Pomembno: aplikacija še ni v trgovini
z aplikacijami, zato je naloga pripraviti besedila za zdaj (v sami aplikaciji, v opisu, ki ga
pokažeš sošolcu) in za pozneje, ko bo šla v trgovino. Brez "prihaja kmalu", brez lažne nujnosti.

NALOGE:
1. Ena vrstica (do 12 besed) in tri vrstice (do 40 besed): kaj aplikacija je, za koga, in kaj
   naredi, česar drugi ne. V slovenščini in angleščini. Vsaka od obeh dolžin naj bo različica iste
   obljube, ne dve različni zgodbi.
2. Kaj v tej obljubi je res, kar lahko uporabnik v prvih petih minutah vidi (preveri proti
   kontekstu: brez računa, brez omrežja, takoj uporabno, sam popravi zamudo). Kar tega ne zdrži,
   črtaj in povej, kaj si črtal.
3. Presodi, ali je prvih pet zaslonov tisto, kar obljuba obljublja (priložena posnetka). Povej,
   kje se obljuba in resnica razhajata.
4. Za koga ta aplikacija NI: napiši odstavek, ki odvrne napačnega uporabnika (tistega, ki hoče
   seznam opravil s podopravili, tistega, ki hoče ekipno delo, tistega, ki hoče merjenje
   produktivnosti). Odvrnitev je boljša od pretiravanja.
5. Razlogi za namestitev, povedani od uporabnika, ne od izdelka: 5 stavkov v prvi osebi, vsak
   mora izhajati iz vedenja, ki ga kontekst res opisuje.
6. Etika obljube: največ 3 stavki, ki jih je v trženju tega izdelka PREPOVEDANO napisati, in
   zakaj (npr. "zviša ocene", "naredi te produktivnega", "vse v eni aplikaciji"). Utemelji s tem,
   kar aplikacija res počne.
7. Kdaj je pravi trenutek za trgovino: povej, katere tri stvari bi morale biti izdelane, preden
   gre v trgovino s tako obljubo, in katere tri niso potrebne (da se ne odlaša v nedogled).

ZAHTEVE:
- Vsaka trditev mora biti preverljiva v aplikaciji. Če je ne moreš preveriti iz konteksta, napiši
  vprašanje namesto trditve.
- Brez presežnikov ("najboljša", "revolucionarna", "#1"), brez primerjav z drugimi po imenu, brez
  dokazovanja s številkami, ki jih nimaš.
- Ne piši kode in ne predlagaj sprememb vmesnika.

IZHOD: 1) ena in tri vrstice (SL + EN), 2) kaj je res, 3) obljuba proti posnetkom, 4) za koga ni,
5) pet razlogov v prvi osebi, 6) tri prepovedane trditve, 7) trije pogoji za trgovino in trije,
ki niso potrebni.
```

**Kaj naj vrne:** eno obligacijo v dveh dolžinah, seznam črtanega in tri prepovedane trditve.
**Rdeča zastavica:** vsaka trditev, ki je v prvih petih minutah uporabe ne moreš pokazati, je
pretiravanje — tudi če zveni dobro.

---

## Kaj naredim z odgovori

| korak | pravilo |
| --- | --- |
| 1 | Vsak odgovor prepiši v tri kupčke: **zdaj** / **preverim** / **zavrnem**. |
| 2 | Vsak predlog iz "zdaj" mora imeti ceno: kaj se poslabša ali kaj izgine. Če cene ni, predlog ni premisljen. |
| 3 | Predlog, ki zahteva nov zaslon, nov podatek o uporabniku ali strežnik, zavrni ali prestavi v "preverim". |
| 4 | Kar se ponovi v treh neodvisnih odgovorih (npr. isti očitek o hierarhiji), je verjetno res — to dobi prednost pred posameznimi predlogi. |
| 5 | Dve uri po odgovoru preveri, ali ti je žal, da bi kaj zamenjal. Kar se ti zdi dobro takoj in še jutri, je prava sprememba. |

## Kaj pošlji in česa ne

- **Pošlji:** posnetke zaslona, opis iz KONTEKSTA, in vprašanje. To je vse, kar model potrebuje.
- **Ne pošiljaj:** kode, imen razredov, strukture baze, imen datotek, zasebnih podatkov in urnika
  iz šole (tudi posnetek urnika ni potreben — posnetek zaslona brez imen zadostuje).
- **Ne dodajaj:** "prosim, napiši kodo za to", "kako naj to implementiram". Če model to ponudi sam,
  ga ustavi in vprašaj po presoji.
- **Za nepristransko presojo imena in opisa** (8.8) ne povej, da si avtor. Za presojo videza (8.1)
  pa povej: presoja je strožja, če ve, da gre za dijakov izdelek, ki mora zdržati med sošolci.

## Merila, po katerih se odgovor da preveriti

Ta števila so iz raziskovalnega dela tega projekta (poglavja 6 in 9 prejšnjega dokumenta). Uporabi
jih kot merilo: če model trdi nasprotno in ne navede vira, je to njegova presoja, ne ugotovitev.

| trditev | merilo |
| --- | --- |
| "Namer ne pomeni dejanja" | izvedba namer: d = 0,65 (Gollwitzer in Sheeran) — vsak korak, ki blok spremeni v dejanje, ima podlago |
| "Samoregulacija se da naučiti" | meta-analiza samoreguliranega učenja: ES 0,63–0,69 |
| "Ponavljanje po razporedu je boljše od nabijanja" | FSRS je boljši od SM-2; to velja za učenje, ne za dnevni ritem |
| "Povratna informacija spremeni vedenje" | vedenjska intervencija z refleksijo: 11,42 → 16,24 (morso.app), v primerjavi z 11,58 → 11,89 brez nje |
| "Uporabniki se izgubijo v prvem dnevu" | obdržanje: prvi dan ~25 %, sedmi dan ~8 %, trideseti dan ~4–7 % |
| "Slovenija je Android" | Android ~62 % naprav v Sloveniji (statcounter) |

Kar ni v tej tabeli (npr. "serije povečajo obdržanje za X %"), naj model pojasni z mehanizmom ali
pa naj pove, da je to njegova presoja.
