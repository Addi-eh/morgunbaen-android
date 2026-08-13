# Morgunbæn — Android-app

Vekjaraklukka sem spilar nýjustu „Morgunbæn og orð dagsins" af Rás 1.

Kemur í staðinn fyrir Termux + ffmpeg + cron + MacroDroid + Sleep as Android.
Appið tekur ekkert upp — það sækir þáttinn beint frá RÚV.

---

## 1. Staða RÚV-viðmótsins — staðfest

Þetta er prófað og virkar. RÚV skilar venjulegri MP3-skrá, t.d.
`https://ruv-radio.akamaized.net/opid/5495160D0.mp3` — sem þýðir að appið
hleður bæninni niður og geymir hana á tækinu.

**Engin nettenging þarf að vera til staðar þegar vekjarinn hringir.**

Ef appið hættir einhvern tímann að finna bænina, keyrðu þessa skipun í Termux
til að sjá hvort RÚV hafi breytt einhverju:

```bash
curl -s https://spilari.nyr.ruv.is/gql/ \
  -H 'content-type: application/json' \
  -H 'Referer: https://www.ruv.is/utvarp' \
  -H 'Origin: https://www.ruv.is' \
  -d '{"operationName":"getEpisode","variables":{"programID":25329},"query":"query getEpisode($programID: Int!) { Program(id: $programID) { title episodes { title id firstrun file } } }"}' \
  | jq '.data.Program.episodes | sort_by(.firstrun) | last'
```

Þú ættir að fá eitthvað á borð við:

```json
{
  "title": "sr. Benedikt Sigurðsson",
  "id": "bme3rd",
  "firstrun": "2026-08-12 06:55:00",
  "file": "https://..."
}
```

Skili hún engu, eða skili `file` slóð sem endar á `.m3u8` í stað `.mp3`,
þá hefur RÚV breytt viðmótinu og appið þarf uppfærslu. Kóðinn ræður við hvort
tveggja, en `.m3u8` þýðir streymi frekar en niðurhal — og þá þarf nettengingu
á vökutíma.

---

## 2. Að opna verkefnið

1. Sæktu **Android Studio** á tölvuna (ókeypis, frá Google).
2. `File → Open` og veldu þessa möppu.
3. Android Studio segir „Gradle sync" neðst. Fyrsta keyrslan tekur 5–15 mínútur
   því hann sækir öll söfnin. Það er eðlilegt. Bíddu.
4. Ef hann kvartar yfir Android SDK: `Tools → SDK Manager` og settu upp
   **Android 15 (API 35)**.

Til að setja appið í símann: tengdu hann með USB, kveiktu á
**Þróunarstillingum** (ýttu sjö sinnum á „Build number" í Stillingar → Um símann)
og **USB-villuleit**. Síminn birtist þá efst í Android Studio og þú ýtir á græna
▶-takkann.

---

## 3. Hvernig kóðinn hangir saman

```
data/RuvClient.kt          Talar við GraphQL-viðmót RÚV
data/EpisodeRepository.kt  Sækir bænina og geymir hana á tækinu
data/Prefs.kt              Allar stillingar

work/SyncWorker.kt         Bakgrunnsverk — sækir bænina á ~6 klst fresti

alarm/AlarmScheduler.kt    Reiknar og skráir næsta vekjara   ← hjartað
alarm/AlarmReceiver.kt     Tekur við þegar klukkan hringir
alarm/AlarmService.kt      Spilar bænina í forgrunnsþjónustu
alarm/AlarmActivity.kt     Skjárinn sem birtist á læstum skjá
alarm/BootReceiver.kt      Skráir vekjarann aftur eftir endurræsingu

MainActivity.kt            Aðalskjárinn — tími, dagar, staða
```

Ef þú lest bara eina skrá, lestu `AlarmScheduler.kt`. Allt annað má klikka;
ef hún klikkar vaknar enginn.

---

## 4. Hvernig á að prófa vekjarann

Ekki bíða til morguns. Þrjár prófanir, í þessari röð:

**Prófun 1 — sækja bænina.** Opnaðu appið, ýttu á „Sækja núna". Þú átt að sjá
nafn prestsins birtast innan nokkurra sekúndna.

**Prófun 2 — vekjarinn sjálfur.** Stilltu hann á tvær mínútur fram í tímann,
**læstu símanum og slökktu á skjánum.** Bænin á að byrja að spila og skjárinn
að kvikna. Ef ekkert gerist, sjá lið 5.

**Prófun 3 — raunverulegar aðstæður.** Þetta er sú eina sem skiptir máli:
láttu appið vekja þig á morgun. Ekki gefa það út fyrr en það hefur virkað
hjá þér í viku samfleytt.

---

## 4a. Hvenær sækir appið bænina?

Morgunbænin er flutt kl. 06:55–07:00 og birtist í Spilara RÚV skömmu síðar.

**Sóknargluggi.** Kl. 07:00 á virkum morgnum opnast gluggi þar sem appið leitar
á fimm mínútna fresti þangað til þáttur dagsins finnst — í mesta lagi í
klukkutíma. Um leið og hann er kominn hættir það að leita.

Athugaðu að skilyrðið er **þáttur dagsins í dag**, ekki „einhver þáttur". Það
dugar ekki að eiga gærdagsins; þá er ekkert unnið og appið heldur áfram.

**Öryggisnet.** Óháð glugganum keyrir reglubundin sókn á sex tíma fresti, alla
daga vikunnar. Hún grípur það sem glugginn missti af: síminn var slökktur kl. 7,
netlaust, eða RÚV birti þáttinn seint. Að hún keyri líka um helgar skiptir máli
á Samsung — sjá lið 5.

**Ein takmörkun.** Vaknir þú fyrir kl. 07:00 færðu bæn gærdagsins, því þáttur
dagsins er einfaldlega ekki til ennþá þegar vekjarinn hringir. Það er ekki hægt
að leysa: útvarpið er ekki búið að flytja hann. Þeir sem vakna kl. 06:30 fá því
alltaf einn dag á eftir.

---

## 4b. Tvær varnir sem eru ósýnilegar í daglegri notkun

**Direct Boot.** Endurræsist síminn um nóttina — vegna uppfærslu, tómrar
rafhlöðu í hleðslu eða kerfishruns — er geymslan dulkóðuð þar til einhver slær
inn PIN. Venjulegt app gæti hvorki lesið hvenær á að hringja né hvað á að spila,
og vekjarinn þegði.

Appið er því merkt `directBootAware`, hlustar á `LOCKED_BOOT_COMPLETED` og geymir
**bæði stillingarnar og MP3-skrána** í device-protected geymslu. Það síðasta er
auðvelt að gleyma: það dugar ekki að vita hvenær á að hringja ef hljóðskráin er
ólæsileg.

Til að prófa: stilltu vekjarann fram í tímann, endurræstu símann og **ekki slá
inn PIN**. Hann á samt að hringja.

**Heilsuvöktun.** Appið skráir í hvert sinn sem vekjarinn hringir í alvöru. Fari
vekjaratími hjá án þess að hann hafi hringt, segir appið frá því næst þegar það
er opnað. Það sama gildir ef bakgrunnssóknin hefur ekki náð að keyra í meira en
sólarhring.

Android lætur ekki vita þegar það stöðvar app. Þetta er eina leiðin til að
notandinn komist að því — annars heldur hann bara að appið sé ónýtt.

---

## 5. Ef vekjarinn hringir ekki

Nær undantekningarlaust er ástæðan ein af þessum þremur:

**Rafhlöðusparnaður.** Þú ert á Samsung — og Samsung er með þeim allra verstu í
þessu. Farðu í Stillingar → Rafhlaða → Bakgrunnsnotkun → Morgunbæn →
**Ótakmarkað**.

**Samsung svæfir líka öpp sem hafa ekki verið opnuð í þrjá daga.** Þetta er
sértækt vandamál fyrir þetta app: vekjari sem hringir aðeins á virkum dögum er
ónotaður frá föstudagskvöldi til mánudagsmorguns — nákvæmlega þrír dagar. Settu
appið á listann **„Öpp sem sofa aldrei"** í Umhirða tækis → Rafhlaða →
Takmörk á bakgrunnsnotkun.

Appið sýnir Samsung-notendum þessar leiðbeiningar sjálfkrafa við fyrstu opnun.
Þeir munu samt hunsa þær. Þess vegna er heilsuvöktunin til.

**Full-screen intent heimild vantar.** Þetta er algengasta ástæðan fyrir því að
bænin spilar en enginn skjár birtist. Frá Android 14 er `USE_FULL_SCREEN_INTENT`
ekki lengur sjálfvirk — Google veitir hana aðeins öppum sem Play Store hefur
flokkað sem vekjara- eða símtalsöpp, og **hliðarhlaðin APK-skrá fær hana ekki**,
sama hvað stendur í manifest.

Appið varar við þessu efst á forsíðunni og takkinn opnar réttu stillinguna.
Þegar appið kemur í Play Store undir réttum flokki fæst heimildin sjálfkrafa og
notendur þínir sjá þetta aldrei.

Til vara reynir appið líka að opna vekjaraskjáinn beint, og tilkynningin ber
bæði „Slökkva" og „Blunda" takka — svo það er alltaf einhver leið til að
stöðva bænina.

**Tilkynningaheimild vantar.** Án hennar birtist ekkert, hvorki skjár né takki.
Appið biður um hana við fyrstu opnun.

**Heimild fyrir nákvæma vekjara.** Sjaldgæft, því `USE_EXACT_ALARM` í manifest
gefur hana sjálfkrafa — en appið athugar það samt.

---

## 6. Áður en þú setur þetta í Play Store

**Sendu RÚV póst fyrst.** Þetta er mikilvægast. Að taka upp fyrir sjálfan sig er
eitt; að dreifa appi sem sækir efni þeirra fyrir hundruð manns er annað.
Spurðu líka hvort viðmótið sé stöðugt — það er óskjalfest og getur breyst
án fyrirvara, og þá hættir appið að virka hjá öllum í einu.

**`USE_EXACT_ALARM` þarf réttlætingu.** Google leyfir hana fyrir öpp þar sem
vekjari er meginhlutverkið — sem á við hér. Þú þarft samt að fylla út form í
Play Console og útskýra það. Verði því hafnað er varaleiðin `SCHEDULE_EXACT_ALARM`
þar sem notandinn veitir heimildina handvirkt.

**targetSdk.** Stendur í 35. Play Store gerir kröfu um nýjustu útgáfur fyrir ný
öpp — athugaðu hvað er í gildi og hækkaðu töluna í `app/build.gradle.kts`
ef þarf.

**Táknmynd og undirritunarlykill.** Hvorugt er í verkefninu. Android Studio býr
til táknmynd (`File → New → Image Asset`) og undirritunarlykil
(`Build → Generate Signed Bundle`). **Taktu afrit af lyklinum og geymdu hann
vel** — týnir þú honum geturðu aldrei uppfært appið aftur.

---

## 6. Fréttir á eftir bæninni

Valkvæmt í Vakning-spjaldinu. Fréttirnar eru næsti dagskrárliður á eftir
Morgunbæninni, svo röðin speglar útsendinguna sjálfa: **bæn → fréttir →
varahljóð**.

Þær koma úr dagskrárlið 38786 („Fréttir" kl. 07:00), sem er daglegur —
stakur ~5 mínútna þáttur og `firstrun` ber nákvæman útsendingartíma. Appið
sækir nýjasta fréttatíma dagsins.

**Varúð ef þetta þarf einhvern tímann að laga:** RÚV heldur úti mörgum
aðskildum fréttaliðum með svipuðum nöfnum. `39025` heitir líka „Fréttir" en er
vikulegur sunnudagsfréttatími kl. 11:00, og `25233` („Morgunfréttir") er
daglegur en kl. 08:00. Rétta auðkennið er **38786**. Staðfestu það alltaf með
`curl` áður en þú breytir einhverju — sjá lið 1.

**Ein regla er önnur en fyrir bænina: gamlar fréttir eru verri en engar.**
Bæn gærdagsins er í lagi — hún eldist ekki. Fréttatími gærdagsins er beinlínis
villandi. Appið hendir því fréttatíma sem er ekki frá deginum í dag *áður* en
það reynir að sækja nýjan, og ef ekkert næst spilast bænin einfaldlega ein.
Betra að þegja en að ljúga.

Athugaðu að fréttirnar eru sóttar í bakgrunni eins og bænin. Vaknir þú kl. 9
færðu þann fréttatíma sem náðist síðast, ekki endilega þann allra nýjasta —
það er verðið fyrir að virka án nettengingar.

---

## 6a. Fyrri bænir, deiling og helgar

**Fyrri bænir.** Sérstakur skjár sýnir síðustu fjórtán þætti og leyfir að spila
þá aftur. Þeir eru **streymdir, ekki hlaðnir niður** — bæn dagsins er það eina
sem þarf að vera til án nettengingar. Fyrri bænir hlustar fólk á meðvitað, og þá
er síminn hvort eð er í höndunum.

**Deiling** sendir titil, dagsetningu og hlekk í Spilara RÚV. Hljóðskránni
sjálfri er aldrei deilt — hún er efni RÚV. Hlekkurinn sendir fólk til þeirra,
sem er bæði rétta leiðin og sú sem heldur áfram að virka eftir að appið er
löngu gleymt.

**Helgartími.** Dagavalið hefur alltaf leyft að velja laugardag og sunnudag; það
sem bættist við er möguleikinn á **öðrum tíma** um helgar. Morgunbænin er ekki
flutt þá, svo appið spilar síðustu bæn vikunnar — margir vilja sofa lengur án
þess að sleppa henni alveg.

---

## 6b. Hljóð og hljóðstyrkur

Þrennt sem er ósýnilegt en skiptir máli:

**Hljóðfókus.** Appið biður um `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` þegar
vekjarinn fer í gang, svo hlaðvarp eða tónlist sem gleymdist í gangi þagnar
alveg í stað þess að blandast saman við bænina.

**Hljóðstyrknum er skilað.** Sé vekjarastyrkur símans undir 60% hækkar appið
hann tímabundið — og setur hann aftur eins og hann var þegar slökkt er. Áður
sat síminn eftir á hærri styrk en eigandinn hafði valið, án þess að nokkur
áttaði sig á hvers vegna.

**Tímamörk.** Þjónustan stöðvast sjálfkrafa eftir 15 mínútur. Vekjarinn hættir
ekki þegar bænin klárast — þá gæti fólk sofnað aftur — en hann má heldur ekki
spila endalaust ef síminn gleymdist heima.

---

## 7. Það sem vantar enn

Vísvitandi sleppt úr fyrstu útgáfu:

- **Eigið varahljóð.** Núna notar appið sjálfgefið vekjarahljóð símans ef bænin
  næst ekki. Það virkar, en er ekki fallegt.
- **Íslenskun kerfistexta.** Appið er á íslensku, en dagsetningarsnið gætu
  þurft fínstillingu.

Byrjaðu ekki á þessu. Byrjaðu á að láta vekjarann virka hjá þér í viku.
