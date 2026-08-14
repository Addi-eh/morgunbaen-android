# Morgunbæn — Android-app

Vekjaraklukka sem spilar „Morgunbæn og orð dagsins" af Rás 1, og valkvætt
fréttirnar kl. 07:00 á eftir.

Kemur í staðinn fyrir Termux + ffmpeg + cron + MacroDroid + Sleep as Android.
**Appið tekur ekkert upp** — það sækir tilbúna MP3-skrá frá RÚV.

Staða: **v0.91** (`0b159a7`).

---

## 1. RÚV-viðmótið — og gildrurnar í því

Appið talar við óskjalfest GraphQL-viðmót á `https://spilari.nyr.ruv.is/gql/`.
Það virkar, en getur breyst án fyrirvara. Bilanaleit byrjar alltaf hér.

### Dagskrárliðirnir tveir

| Auðkenni | Hvað | Tími |
|---|---|---|
| **25329** | Morgunbæn og orð dagsins | 06:55, daglega |
| **38786** | Fréttir | 07:00, daglega |

**Gildran:** RÚV heldur úti mörgum fréttaliðum með nánast sama nafni. Tveir sem
líta rétt út en eru það ekki:

- `39025` „Fréttir" — **vikulegur** sunnudagsfréttatími kl. 11:00
- `25233` „Morgunfréttir" — daglegur, en kl. **08:00**

Appið notaði `39025` um tíma. Það fann aldrei neitt frá deginum í dag og sagði
réttilega frá því — villan var í auðkenninu, ekki rökvísinni. Staðfestu alltaf
með `curl` áður en þú breytir auðkenni.

### Staðfestingarskipun

Skiptu `25329` út fyrir `38786` til að prófa fréttirnar.

```bash
curl -s https://spilari.nyr.ruv.is/gql/ \
  -H 'content-type: application/json' \
  -H 'Referer: https://www.ruv.is/utvarp' \
  -H 'Origin: https://www.ruv.is' \
  -d '{"operationName":"getEpisode","variables":{"programID":25329},"query":"query getEpisode($programID: Int!) { Program(id: $programID) { title episodes { title id firstrun file } } }"}' \
  | jq '.data.Program.episodes | sort_by(.firstrun) | reverse | .[0:3]'
```

Þrennt á að stemma:

1. **Nýjasti þáttur er frá í dag eða gær.** Sé hann vikugamall er auðkennið rangt.
2. **`firstrun` er ISO með T**: `2026-08-13T06:55:00`. Kóðinn ræður líka við bil
   í stað T, en snið sem er hvorugt brýtur dagsetningarlestur.
3. **`file` endar á `.mp3`.** Endi hún á `.m3u8` er þetta streymi: appið spilar
   það en getur ekki geymt það, og þá þarf nettengingu á vökutíma.

---

## 2. Að opna verkefnið

1. `File → Open` í Android Studio, veldu **möppuna sem inniheldur
   `settings.gradle.kts`** — ekki yfirmöppuna.
2. Biðji hann um Gradle JVM, veldu **21**. Nýrri Java ræður Gradle ekki við.
3. Fyrsta samstilling tekur 5–15 mínútur. Rauðar undirstrikanir á meðan eru
   eðlilegar.

---

## 3. Kóðinn

```
data/RuvClient.kt          GraphQL-viðmót RÚV, dagskrárauðkennin
data/EpisodeRepository.kt  Sækir bæn og fréttir, geymir á tækinu
data/Prefs.kt              Allar stillingar (device-protected geymsla)
data/DeviceStorage.kt      Aðgangur að geymslu sem virkar fyrir PIN
data/Dates.kt              Allur lestur á RÚV-tímastimplum, á einum stað

work/SyncWorker.kt         Sóknargluggi + 6 klst öryggisnet
work/CatchUpScheduler.kt   Opnar gluggann kl. 07:00, ræður við læstan síma

alarm/TriggerTimes.kt      Hreinn tímareikningur - næst/síðast/gluggi   ← hjartað
alarm/AlarmScheduler.kt    Þunn umbúð um TriggerTimes; skráir vekjara og blund
alarm/AlarmReceiver.kt     Tekur við þegar klukkan hringir
alarm/AlarmService.kt      Spilar bæn → fréttir → varahljóð
alarm/AlarmActivity.kt     Skjárinn á læstum skjá, langt ýt til að slökkva
alarm/BootReceiver.kt      Endurskráir allt eftir ræsingu

MainActivity.kt            Samhæfingarlag: state og hliðarverk fyrir spjöldin
ui/AlarmCard.kt             Vekjaratími, dagar, helgartími, prófunarhnappur
ui/PrayerCard.kt            Staða bænarinnar, sókn, spilun, saga, deiling
ui/WakeSettingsCard.kt      Fade-in, titringur, fréttir, blundur
ui/Components.kt            Deildar einingar (DayPicker, WarningCard, o.fl.)
HistoryActivity.kt         Fyrri bænir, spilun og deiling

test/alarm/TriggerTimesTest.kt   12 próf á tímareikningnum, keyra með `./gradlew test`
```

Lestu `TriggerTimes.kt` fyrst — hreinn tímareikningur, engin Android-tenging,
og hjartað í bæði vekjaranum og sóknarglugganum. `AlarmScheduler.kt` og
`CatchUpScheduler.kt` eru þunnar umbúðir utan um það. Allt annað má klikka;
klikki tímareikningurinn vaknar enginn — eða hann vaknar á vitlausum tíma,
sem er verra því ekkert segir frá því.

---

## 4. Hvenær efnið er sótt

Morgunbænin er **dagleg** — dagskrá RÚV sýnir hana kl. 06:55 alla sjö daga
vikunnar, líka laugardaga og sunnudaga, og fréttirnar kl. 07:00 sömuleiðis.
Sóknargluggi og sjálfgefnir vekjaradagar byggja á þessu (sjá gildruna í lið 1
og töfluna þar).

**Sóknargluggi.** Kl. 07:00 opnast gluggi þá daga sem vekjarinn er stilltur á
— ekki bara virka daga — og leitar á fimm mínútna fresti í allt að tvo tíma.
Hann lokast þegar **allt efni dagsins** er komið — bæn, og fréttir líka ef
notandinn hefur valið þær. Skilyrðið er *dagurinn í dag*; gærdagurinn dugar
ekki. (Eldri útgáfa hafði helgar harðkóðaðar úti, byggt á rangri forsendu um
að bænin væri bara flutt virka daga — notandi með sunnudagsvekjara fékk þá
aldrei glugga þann morgun og vaknaði við bæn gærdagsins án viðvörunar.)

**Öryggisnet.** Óháð glugganum keyrir sókn á sex tíma fresti, alla daga. Að hún
keyri líka um helgar skiptir máli á Samsung (sjá lið 8).

**Læstur sími.** `CatchUpReceiver` er `directBootAware` og keyrir kl. 07:00 þótt
enginn hafi slegið inn PIN. WorkManager getur það ekki — hann þarf
credential-geymslu — svo viðtakandinn greinir læstan síma og reynir aftur á
fimm mínútna fresti í stað þess að tapa deginum. `BootReceiver` opnar gluggann
strax ef síminn kemur upp ólæstur innan hans.

**Ein takmörkun.** Vaknir þú fyrir kl. 07:00 færðu bæn gærdagsins og engar
fréttir. Þátturinn er einfaldlega ekki til — útvarpið er ekki búið að flytja
hann. Appið segir frá þessu í stað þess að láta þig bíða.

**Gamlar fréttir eru verri en engar.** Bæn gærdagsins eldist ekki og er geymd.
Fréttatími gærdagsins er villandi og er hentur *áður* en reynt er að sækja nýjan.
Náist ekkert spilast bænin ein.

---

## 5. Direct Boot

Endurræsist síminn kl. 03:00 er geymslan dulkóðuð þar til einhver slær inn PIN.
Venjulegt app gæti hvorki lesið hvenær á að hringja né hvað á að spila.

Þess vegna eru **bæði stillingarnar og hljóðskrárnar** í device-protected
geymslu, appið er `directBootAware`, og `BootReceiver` hlustar á
`LOCKED_BOOT_COMPLETED` sem berst strax við ræsingu.

Það síðasta er auðvelt að gleyma: það dugar ekki að vita hvenær á að hringja ef
MP3-skráin er ólæsileg.

**Prófun:** stilltu vekjara fram í tímann, endurræstu símann og **ekki slá inn
PIN**. Hann á samt að hringja.

---

## 6. Vekjarinn

**`setAlarmClock`** er sterkasta tímasetningin sem Android býður og kemst í
gegnum Doze. `AlarmReceiver` skráir næsta dag um leið og hann hringir — algengasta
villan í heimasmíðuðum vekjurum er að gleyma því.

**Tímareikningurinn sjálfur býr í `TriggerTimes.kt`** — hreint fall af
gildum (dagar, klukka, helgartími), engin `Context` eða `SharedPreferences`.
`AlarmScheduler.nextTriggerTime()`/`previousTriggerTime()` og
`CatchUpScheduler.schedule()` eru þunnar umbúðir sem lesa `Prefs` og kalla
hann. Ástæðan fyrir aðskilnaðinum: `nextWindow()` skal skila `null` þegar
engir dagar eru valdir, ekki varatíma — eldri útgáfa af sóknarglugganum féll
aftur á „núna + 24 klst" í því tilfelli, sem skráði gluggann á tíma sem
færðist með klukkunni dag frá degi í stað þess að hverfa. Sú villa hefði
aldrei komist í gegnum einfaldasta einingapróf, en reikningurinn lá læstur
inni í hlutum sem þurftu `Context` til að keyra yfirleitt. `TriggerTimesTest.kt`
hefur núna 12 próf á honum (`./gradlew test`), þar á meðal nákvæmlega þetta
tilfelli.

**Blundur er sjálfstæður vekjari.** Hann notar eigin `PendingIntent` (kóða 1003),
er vistaður í `Prefs` með `commit()` svo hann lifi af ferlisdauða, og
endurskráður í `schedule()`. Áður deildi hann `PendingIntent` með daglega
vekjaranum og **eyddi morgundeginum** um leið og ýtt var á Blunda.

**Langt ýt til að slökkva** — 1,5 sekúndur, með sýnilegri framvindu. Blundur er
venjulegt ýt: það á ekki að vera erfitt að sofna aftur, heldur að slökkva alveg.

**Full-screen intent.** Frá Android 14 er heimildin ekki sjálfvirk og
hliðarhlaðin APK fær hana ekki. Appið varar við og býður þrjár varaleiðir:
bein ræsing skjásins, tilkynning með Slökkva/Blunda, og ýt á tilkynninguna.
Í Play Store undir vekjaraflokki fæst heimildin sjálfkrafa.

**Heilsuvöktun.** Appið skráir í hvert sinn sem vekjarinn hringir í alvöru og
ber saman við `lastScheduledTriggerMillis` — tímann sem var *raunverulega*
skráður, ekki endurreiknaðan út frá núverandi stillingum. Sá munur skiptir máli:
færi notandinn 07:00 í 06:30 eftir velheppnaða hringingu leit það áður út eins
og klikkaður vekjari.

Merkið er fryst meðan liðinn óhringdur tími stendur, svo `Application.onCreate`
færi það ekki á morgundaginn áður en viðvörunin næði að birtast — og þítt aftur
þegar notandinn kvittar, svo vöktunin þagni ekki að eilífu eftir fyrsta klikk.

---

## 7. Hljóð

**Röðin er bæn → fréttir → varahljóð.** Vekjarinn stöðvast *aldrei* þegar efni
klárast — þá gæti fólk sofnað aftur — heldur færist á næsta stig. Varahljóðið
spilar í lykkju þar til slökkt er, en þjónustan hættir sjálfkrafa eftir
**15 mínútur** ef enginn er heima.

**Hljóðfókus** er `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` með
`setWillPauseWhenDucked(false)`. Hlaðvarp sem gleymdist í gangi þagnar alveg, og
vekjarinn gefur sjálfur aldrei eftir.

**Hljóðstyrknum er skilað.** Sé vekjarastyrkur undir 60% hækkar appið hann
tímabundið og setur hann aftur eins og hann var. Áður sat síminn eftir á hærri
styrk en eigandinn valdi.

**Vaxandi hljóðstyrkur og titringur eru sjálfgefið AF.** Bænin er talað mál —
fyrstu setningarnar hverfa ef styrkurinn er enn að hækka, og titringur keppir
við rödd prestsins. Hvort tveggja er í boði fyrir þá sem vilja.

---

## 8. Ef vekjarinn hringir ekki

**Rafhlöðusparnaður.** Stillingar → Rafhlaða → Bakgrunnsnotkun → Morgunbæn →
**Ótakmarkað**.

**Samsung svæfir öpp sem hafa ekki verið opnuð í þrjá daga.** Þetta er sértækt
vandamál fyrir þetta app: vekjari sem hringir aðeins á virkum dögum er ónotaður
frá föstudagskvöldi til mánudagsmorguns — nákvæmlega þrír dagar. Settu appið á
listann **„Öpp sem sofa aldrei"** í Umhirða tækis → Rafhlaða. Appið sýnir
Samsung-notendum þessar leiðbeiningar sjálfkrafa; þeir munu samt hunsa þær, og
þess vegna er heilsuvöktunin til.

**Full-screen intent eða tilkynningaheimild vantar.** Appið varar við báðum efst
á forsíðunni með takka beint í réttu stillinguna.

---

## 9. Prófanir

**Fyrst, á tölvunni — engan síma þarf:** `./gradlew test` keyrir
`TriggerTimesTest.kt`, 12 próf á tímareikningnum. Grípur ekki neitt sem
snertir Android sjálft, en grípur allt sem snertir *hvenær* vekjarinn og
sóknarglugginn eiga að fara í gang — ódýrasta og hraðasta staðfestingin sem
til er á verkefninu.

Því næst, á símanum:

1. **Sækja.** „Sækja núna" → nafn prestsins birtist. Kveiktu á fréttum → tími
   fréttatímans birtist.
2. **Prófunarhnappur.** Ýttu á „Prófa vekjarann", læstu símanum og slökktu á
   skjánum — full hringing eftir 30 sek með skjá, bæn og slökkvitakka.
   Athugaðu að „Næst:" sýni enn réttan morgun á eftir, og að
   prófunartextinn sjálfur sé horfinn þegar þú kemur til baka í appið.
3. **Vekjari.** Tvær mínútur fram í tímann, **læstu símanum og slökktu á
   skjánum**.
4. **Hljóðfókus.** Kveiktu á tónlist og láttu vekjarann hringja ofan í hana.
   Þessi bilar aðeins þegar eitthvað annað er í gangi — sem er sjaldan þegar
   maður prófar.
5. **Direct Boot.** Endurræstu, ekki slá inn PIN.
6. **Blundur.** Blundaðu, slökktu svo á blundinum, og athugaðu að
   „Næst:" sýni enn morgundaginn.
7. **Spila bænina.** Ýttu á „Spila bænina" á forsíðunni og farðu svo úr
   appinu (heim-takkinn) — hljóðið á að þagna. Kom það ekki, er
   `ON_PAUSE`-stöðvunin í `MainActivity.kt` biluð.
8. **Raunverulegar aðstæður.** Láttu appið vekja þig í viku samfleytt.

Sú síðasta er sú eina sem sannar eitthvað. Vekjari sem virkar kl. 13:10 meðan þú
horfir á símann sannar ekkert; vekjari sem hringir eftir sjö tíma svefn með
dimmum skjá og Doze í fullum gangi sannar allt. **Hver ný útgáfa endurstillir
teljarann.**

---

## 10. Áður en þetta fer í Play Store

**Sendu RÚV póst.** Mikilvægast. Viðmótið er óskjalfest og ein breyting slekkur
á appinu hjá öllum samtímis. Grænt ljós og tengiliður eru meira virði en
nokkur kóði.

**targetSdk 36.** Stendur í 35. Frá 31. ágúst 2026 krefst Google Play að ný öpp
miði á Android 16. Fyrir óútgefið app skapar fresturinn engan flýti — þú þarft
36 hvenær sem þú gefur út — en þetta er **ekki einnar línu breyting**: Android 16
fjarlægir undanþáguna frá edge-to-edge teikningu, sem er raunveruleg
viðmótsvinna.

Annað sem vantar:

- **Undirritunarlykill.** Ekki til. Búðu hann til, taktu afrit, geymdu utan
  tölvunnar. Týnist hann geturðu aldrei uppfært appið.
- **`USE_EXACT_ALARM`** þarf réttlætingu í Play Console. Vekjari er gild ástæða.
- **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** er takmörkuð heimild. Vekjaraöpp
  eru á lista Google yfir gildar undantekningar.
- **Forgrunnsþjónusta `mediaPlayback`** — Play biður um lýsingu og stundum myndband.
- **Persónuverndarstefna** — krafist þótt appið safni engu.
- **`isMinifyEnabled`** er `false` og `proguard-rules.pro` tóm.
- **`versionCode`/`versionName`** standa í 1 / 1.0.

---

## 11. Það sem vantar enn

- **Eigið varahljóð.** Sjálfgefið vekjarahljóð símans er kalt vakningarúrræði.
- **Fleiri framleiðendur en Samsung.** Xiaomi, Huawei, Oppo og OnePlus drepa
  bakgrunn jafn mikið.
- **Einingapróf víðar.** `TriggerTimesTest.kt` nær yfir tímareikninginn; ekkert
  annað í verkefninu er prófað enn — t.d. `Dates.kt`-þáttun eða
  `EpisodeRepository`-röklegan gang (án nettengingar, með mock-uðum `RuvClient`).
- **Kvöldstöðutékk.** Lítil hljóðlát tilkynning kl. 21 sem staðfestir að bæn
  morgundagsins hafi náðst, eða varar við ef eitthvað vantar heimild.
  Vekjaraklukkur bila á nóttunni; þetta er eina tækifærið til að segja frá
  fyrir skaðann.

---

## 12. Vinnulag — lærdómur sem kostaði

Verkefnið hefur ítrekað lent í sömu villunni: **kóði sem kemur utan frá er
byggður á eldra grunnsniði og vekur upp lagfæringar sem voru löngu gerðar.**
Þrjár villur fóru þannig hring: `mutableStateOf`-innflutningur, `newsDir`, og
`hour` gegn `currentHour()`.

Fernt sem verður að halda:

**Git-repóið er eina uppspretta sannleikans.** Samhliða vinnutré — hvort sem það
heitir `Grok/`, `morgunbaen-verkefni vX.Y/` eða annað — verða að hverfa um leið
og innihald þeirra er staðfest komið inn.

**Berðu saman skrá fyrir skrá, alltaf.** Fylgiskjal sem segir hvað breyttist er
ekki sönnun. Sending sem fullyrti „ein breyting" bar í reynd breytingar á tólf
skrám, þar á meðal hrunvillu sem hafði leynst frá fyrstu útgáfu.

**Byggðu áður en þú commit-ar — og keyrðu prófin, ekki bara `assembleDebug`.**
En mundu að hvorki Gradle né `git apply --check` grípa allt. `hour` gegn
`currentHour()` þýddist fullkomlega í öll þrjú skiptin sem hún kom aftur —
það var rökvilla, ekki þýðingarvilla. Og `git apply --check`, sem ber saman
blob-kennitölur en ekki bara texta og er því nákvæmari staðfesting en nokkur
skrá-fyrir-skrá samanburður, staðfestir samt aðeins að bútarnir passi við
skrána — ekki að útkomandi Kotlin þýðist. Patch sem stóðst þá athugun bar samt
`private private fun` (tvítekið lykilorð, leif eftir handvirka endurheimt).
Aðeins bygging fann hana.

**Sumar villur eru hvorki í kóðanum né í fylgiskjalinu um hann — heldur í
forsendu sem báðir aðilar deila.** Sú staðreynd að Morgunbænin er flutt alla
daga vikunnar, ekki bara virka daga, var ranghermd í kóða, athugasemdum og
notendatexta samtímis frá fyrstu útgáfu — ekkert `diff` grípur það, því allt
var samstiga um sömu röngu niðurstöðuna. Fannst aðeins þegar einhver bar
fullyrðinguna saman við frumgögn (dagskrá RÚV) sem lægju utan kóðans
sjálfs.
