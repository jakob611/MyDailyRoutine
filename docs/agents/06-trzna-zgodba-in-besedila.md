# Naloga 06 — Tržna zgodba in pregled besedil (N20, N21)

## Namen

Tržna raziskava je bila edina, ki ni uspela. Vzrok je v promptu, ne v modelu: prejšnja različica je
zahtevala "prvi vtis in ponudbo", ni povedala, kaj je v aplikaciji res, ni prepovedala
presežnikov in ni povedala, kdaj je aplikacija "pripravljena za trgovino". Ta naloga prompt popravi in
hkrati naredi pregled vseh besedil v aplikaciji.

## Del 1 — prompt za tržno zgodbo (N21)

Uporabi `docs/audits/2026-09-22-prompti-za-zunanje-ai.md`, §8.8. Spremembe v promptu pred pošiljanjem
preveri v tem vrstnem redu:

1. **Kaj model vidi:** kontekst iz `KONTEKSTA` + dva posnetka zaslona + 5–8 obstoječih besedil
   aplikacije (naslov dneva, prazno stanje, povzetek, sporočila o zamudi, opis prvega zagona).
   Ne pošiljaj kode, ne imen datotek, ne urnika iz šole.
2. **Kaj model ne sme narediti:** pisati kode, predlagati novih funkcij, primerjati z drugimi
   aplikacijami po imenu, uporabljati presežnikov, navajati številk, ki jih nima, in obljubljati
   rezultatov učenja ("višje ocene", "v 5 minutah boš produktiven").
3. **Kar model mora vrniti:** eno obligacijo v dveh dolžinah (12 besed, 40 besed) v **slovenščini in
   angleščini**; seznam trditev, ki jih je črtal, ker jih aplikacija v prvih petih minutah ne more
   pokazati; odstavek "za koga ta aplikacija ni"; tri trditve, ki so v trženju tega izdelka
   prepovedane; tri pogoje, ki morajo biti izpolnjeni pred trgovino, in tri, ki niso potrebni.
4. **Kar moraš storiti z odgovorom:** vsako trditev preveri proti aplikaciji. Kar ne zdrži, se izloči.
   V poročilu napiši, katera verzija je bila izbrana in **zakaj**.

Če je možno, isti prompt pošlji **trem** različnim modelom in primerjaj. Kar se ponovi v vseh treh, je
verjetno res; kar se pojavi enkrat, je okus.

## Del 2 — pregled besedil (N20)

1. Izpiši vse nize iz `app/src/main/res/values/strings.xml` (in vzporedne iz `values-en`) ter poišči:
   * nize, daljše od 60 znakov, razen tistih, ki so očitno pojasnila (prazna stanja, opozorila),
   * nize, ki zvenijo kot ukaz ("Uskladi", "Dodaj", "Preveri") tam, kjer bi bil dovolj samostalnik,
   * nize, ki se v angleščini razlikujejo po pomenu od slovenskih (ne le po besedah),
   * nize, ki obljubljajo več, kot aplikacija da.
2. Za vsak najden niz napiši predlog (SL + EN) in **kaj se poslabša** s predlogom.
3. Predloge vnesi samo za nize, ki so res problematični (največ 15). Vsak predlog mora imeti
   utemeljitev v enem stavku; brez "zveni lepše".
4. Preveri, da se število nizov v obeh jezikih ne razlikuje in da vsi specifikatorji ostanejo
   (`python3 tools/check_translations.py`), ter da se noben niz ne uporablja v kodi, kjer bi sprememba
   pomenila spremembo pomena stanja.

## Kaj je prepovedano

* Nič v kodi, kar ni niz: brez novih elementov vmesnika, brez novih ikon.
* Brez spremembe pomena nizov, ki jih bere bralnik zaslona ločeno (preveri `contentDescription`).
* Brez novih trženjskih besedil v aplikaciji sami — ta naloga pripravlja besedila **za zunaj**
  (opis, pogovor s sošolcem). V aplikaciji se besedila spreminjajo le, če so res slaba.

## Dokaz

* `python3 tools/check_translations.py` in `python3 tools/check_presentation.py` zelena.
* Poročilo z izbranimi besedili in utemeljitvijo (v `docs/`, npr. `docs/audits/2026-09-22-besedila-in-trzna-zgodba.md`).
* Seznam "kar sem črtal" — to je najbolj uporaben del naloge.

## Poročilo naj vsebuje

1. Izbrano obligacijo (oba jezika, obe dolžini).
2. Tri prepovedane trditve in tri pogoje za trgovino.
3. Največ pet najbolj koristnih sprememb nizov in kaj se z njimi poslabša.
