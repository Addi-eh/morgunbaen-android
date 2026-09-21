# Morgunbæn — Android-app

Vekjaraklukka sem spilar „Morgunbæn og orð dagsins" af Rás 1, og valkvætt
fréttirnar kl. 07:00 á eftir.

Staða: **v0.974**.

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

**Athugið:** RÚV getur skráð dagskrárlið *áður* en hann er fluttur — svarið að
ofan getur því innihalda þátt með `firstrun` í framtíðinni. `RuvClient.
fetchLatestEpisode()` sniðgengur slíka þætti áður en „nýjasti" er valinn með
`maxByOrNull`; hrár strengjasamanburður án þeirrar síu hefði getað valið
morgundaginn fram yfir daginn í dag.

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

alarm/TriggerTimes.kt      Hreinn tímareikningur - næst/síðast/gluggi/teljari ← hjartað
alarm/AlarmScheduler.kt    Þunn umbúð um TriggerTimes; skráir vekjara og blund
alarm/AlarmReceiver.kt     Tekur við þegar klukkan hringir
alarm/AlarmService.kt      Spilar bæn → fréttir → varahljóð; vekjarahljóð + hlustun
alarm/AlarmActivity.kt     Skjárinn á læstum skjá: hringing, spurning, hlustun
alarm/BootReceiver.kt      Endurskráir allt eftir ræsingu

MainActivity.kt            Samhæfingarlag: state og hliðarverk fyrir spjöldin
OemBatteryGuide.kt         „Remove permissions if app is unused"
ui/AlarmCard.kt             Vekjaratími, dagar, tími hvers dags, sleppa næstu, prófun
ui/PrayerCard.kt            Staða bænarinnar, sókn, spilun, saga, deiling
ui/WakeSettingsCard.kt      Vakna við, fade-in, titringur, fréttir, vekjarahljóð, blundur
ui/DayStrip.kt              Vikan í sjö reitum: stafur kveikir, tíminn undir stillir
ui/TimePickDialog.kt        Klukkuvalið — Material3, fylgir útliti appsins
ui/Components.kt            Deildar einingar (SettingRow, MinuteStepper, o.fl.)
ui/Theme.kt                 Litir, ljóst/dökkt og AppTheme.mode
HistoryActivity.kt         Fyrri bænir, spilun og deiling
AboutActivity.kt           Um appið, styrkir og leiðir til að hjálpa
data/AlarmSoundStore.kt    Afritar valið vekjarahljóð í device-protected geymslu

test/alarm/TriggerTimesTest.kt   26 próf á tímareikningnum, keyra með `./gradlew test`
test/data/DayTimesParseTest.kt   3 próf á lestri vistaðra tíma hvers dags
test/data/DayTimesLogicTest.kt   6 próf á dálkunum: eigin tími og niðurlagning rofans
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

Þessi athugun (`alarmRingsBeforeNews()` í `MainActivity.kt`) verður að skoða
**hvern valinn dag á sínum eigin tíma** — ekki bara stillta `alarmHour`. Vekjari
kl. 08:00 virka daga en 06:30 á laugardögum (eigin tími dagsins, sjá lið 6)
sagði áður ranglega „ekki of snemmt", því aðeins sjálfgefni tíminn var skoðaður.

**Gamlar fréttir eru verri en engar.** Bæn gærdagsins eldist ekki og er geymd.
Fréttatími gærdagsins er villandi og er hentur *áður* en reynt er að sækja nýjan.
Náist ekkert spilast bænin ein.

---

## 5. Direct Boot

Endurræsist síminn kl. 03:00 er geymslan dulkóðuð þar til einhver slær inn PIN.
Venjulegt app gæti hvorki lesið hvenær á að hringja né hvað á að spila.

Þess vegna eru **bæði stillingarnar og hljóðskrárnar** í device-protected
geymslu — líka vekjarahljóð sem notandinn velur sjálfur. `AlarmSoundStore`
**afritar** það inn, í stað þess að geyma slóðina: bæði `content://media`
(hljóð símans) og skrár úr skráavafranum eru ólæsilegar fyrir aflæsingu,
og afritið lifir líka af að upprunaskránni sé eytt. Appið er
`directBootAware`, og `BootReceiver` hlustar á `LOCKED_BOOT_COMPLETED` sem
berst strax við ræsingu.

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
gildum (dagar, sjálfgefin klukka, eigin tími einstakra daga), engin
`Context` eða `SharedPreferences`.
`AlarmScheduler.nextTriggerTime()`/`previousTriggerTime()` og
`CatchUpScheduler.schedule()` eru þunnar umbúðir sem lesa `Prefs` og kalla
hann. Ástæðan fyrir aðskilnaðinum: `nextWindow()` skal skila `null` þegar
engir dagar eru valdir, ekki varatíma — eldri útgáfa af sóknarglugganum féll
aftur á „núna + 24 klst" í því tilfelli, sem skráði gluggann á tíma sem
færðist með klukkunni dag frá degi í stað þess að hverfa. Sú villa hefði
aldrei komist í gegnum einfaldasta einingapróf, en reikningurinn lá læstur
inni í hlutum sem þurftu `Context` til að keyra yfirleitt. `TriggerTimesTest.kt`
hefur núna 26 próf á honum (`./gradlew test`), þar á meðal nákvæmlega þetta
tilfelli.

**Tími hvers dags** (v0.96) leysti „Annar tími um helgar" af hólmi.
`TriggerTimes` tekur `dayTimes: Map<Int, Int>` — mínútur frá miðnætti fyrir
þá daga sem hafa eigin tíma; aðrir dagar fylgja `alarmHour`/`alarmMinute`.
Kveiktur helgartími úr eldri útgáfu færist yfir á lau/sun **einu sinni** í
`Prefs.migrateWeekendTime()`, sem `Application.onCreate` kallar *á undan*
fyrstu skráningu vekjarans.

**Rofinn „Mismunandi tími eftir dögum" var lagður niður í v0.972**, og það
var hættulegri breyting en hún sýnist. Rofinn **faldi** tíma dagsins, eyddi
honum ekki: sá sem stillti laugardag á 09:30 og slökkti svo á rofanum átti
07:00 á laugardag. Yrði kortið virkt án hreinsunar myndi vekjarinn færast um
tvo tíma — þegjandi, og enginn kæmist að því fyrr en hann vaknaði of seint.
Þetta var ekki fræðilegt: `migrateWeekendTime()` skrifar bæði kortið **og**
kveikir á rofanum, svo hver sem slökkti á honum eftir þá færslu átti enn
laugardag og sunnudag í kortinu.

`Prefs.migrateRetiredPerDaySwitch()` hreinsar því kortið hjá öllum sem höfðu
rofann af, **á undan** `AlarmScheduler.schedule()`. Ákvörðunin sjálf er hreint
fall — `retiredPerDayTimes(enabled, raw)` — svo hún sé prófanleg án `Context`,
eins og `parseDayTimes`. `KEY_PER_DAY_ENABLED` er skrifaður áfram, jafnaður
við kortið í setter `dayTimes`, svo hann geti ekki sagt ósatt.

**Sjálfgefinn tími eyðir lyklinum.** `Prefs.applyPickedTime()` skráir dag
aðeins ef valinn tími **víkur frá** sjálfgefnu; jafngildi eyðir honum. Þess
vegna þarf engan „Sjálfgefið"-hnapp: dagurinn verður daufur um leið og hann
er stilltur á sömu tölu og hinir. Afleiðing sem er vert að vita: breytist
sjálfgefni tíminn síðar fylgir sá dagur með, þótt hann hafi verið stilltur
handvirkt á sömu tölu.

`countdown()` býr líka þarna: biðtíminn fram að næstu hringingu, sundurliðaður
í daga, klukkustundir og mínútur. Mínúturnar eru námundaðar **upp** — annars
stæði teljarinn á „0 mín" heila mínútu áður en vekjarinn hringir, og teljari
sem segir núll en hringir ekki er verri en enginn teljari.

`AlarmScheduler.schedule()` kallar núna `cancel(context)` þegar enginn dagur
er valinn, til samræmis við `CatchUpScheduler` — annars gat gömul skráning
lifað áfram og hringt á degi sem notandinn hafði þegar afvalið.

**Blundur er sjálfstæður vekjari.** Hann notar eigin `PendingIntent` (kóða 1003),
er vistaður í `Prefs` með `commit()` svo hann lifi af ferlisdauða, og
endurskráður í `schedule()`. Áður deildi hann `PendingIntent` með daglega
vekjaranum og **eyddi morgundeginum** um leið og ýtt var á Blunda.

**Langt ýt til að slökkva** — 1,5 sekúndur, með sýnilegri framvindu. Blundur er
venjulegt ýt: það á ekki að vera erfitt að sofna aftur, heldur að slökkva alveg.

**Blundlengdin er frjáls** — teljari frá 1 upp í 60 mínútur (`Prefs.SNOOZE_RANGE`),
ekki fastir kostir. Að halda hnappnum inni telur áfram eftir 400 ms; smellurinn
sem kemur við að sleppa er þá hunsaður, svo talan hoppi ekki um eitt umfram það.

**Full-screen intent.** Frá Android 14 er heimildin ekki sjálfvirk og
hliðarhlaðin APK fær hana ekki. Appið varar við henni efst á forsíðunni. Í
Play Store undir vekjaraflokki fæst heimildin sjálfkrafa.

**Full-screen intent er eina leiðin sem kemur skjánum upp — staðfest með
logcat.** Þrjú próf á Galaxy A54 (Android 14, 2026-09-21), Morgunbæn og Sleep
as Android á sömu mínútu:

- **Bein ræsing** (`launchAlarmScreenDirectly()`) fékk `BAL_BLOCK`, result 102,
  í öll þrjú skiptin. Staða sem forgrunnsþjónusta veitir **ekkert** leyfi til
  bakgrunnsræsingar. Kallið stendur samt, því það kostar ekkert og virkar
  þegar appið er sjálft opið.
- **Full-screen intent** kom skjánum upp í öll skiptin
  (`BAL_ALLOW_PENDING_INTENT`, sent af systemui), 0,65–1,54 sek eftir að
  vekjarinn hringdi.
- **Sá sem ræsir skjáinn síðast lendir efst.** Sleep as Android fær alltaf leyfi
  (`BAL_ALLOW_SAW_PERMISSION` — það hefur „Birta yfir öðrum forritum").
  Tvisvar kom það á undan okkur og við unnum; einu sinni 28 ms á eftir og við
  töpuðum.
- **Tilkynningin er ekki varaleið á One UI.** SystemUI bældi heads-up
  („no Heads up : edgelighting enabled app") og sýndi Edge Lighting í staðinn,
  án hnappa. Tilkynningin var í skúffunni allan tímann og rásin á hæsta
  mikilvægi, en hún fannst ekki þótt skúffan væri opnuð tvisvar.
- **Tapaður skjár kemur ekki aftur.** Hann er í eigin verkefni og
  `excludeFromRecents`, svo þegar hinn vekjarinn er afgreiddur fellur síminn á
  heimaskjáinn. Eina leiðin út var að drepa appið.

**Því reynir `AlarmService` að koma skjánum aftur upp** (v0.974).
`AlarmActivity` setur `AlarmService.screenVisible` í `onResume` og tekur það
niður í `onPause` — **ekki** í `onCreate`, því hulinn skjár er búinn til en
ósýnilegur. Hringi vekjarinn og skjárinn sjáist ekki eftir 2, 5, 10, 20 eða
40 sek, birtir þjónustan **nýja** tilkynningu (auðkenni 44) með full-screen
intent. Tvennt virkar ekki og á ekki að reyna aftur: `startActivity` (fær
`BAL_BLOCK`) og `updateNotification()` á sama auðkenni (uppfærsla ræsir
skjáinn ekki aftur — það gerist á hverjum morgni án þess).

Þrjú smáatriði sem skipta máli:

- Endurtilkynningin hefur **eigið `requestCode`** (4). `PendingIntent` greinir
  ekki á milli intent-fána, svo með sama kóða og aðaltilkynningin myndi
  `FLAG_UPDATE_CURRENT` skila útgáfunni með `CLEAR_TASK`.
- `onPause` segir skjáinn **ekki** ósýnilegan þegar hann er að loka sér
  (`isFinishing`). Ýtt á Slökkva lokar skjánum áður en þjónustan fær
  skipunina, og endurtilraun sem félli þar á milli myndi opna hann aftur.
- Endurtilraunirnar keyra á **eigin `Handler`** og allar stöðvanir hreinsa þær á
  einum stað, `cancelScreenRetries()`. `scheduleScreenRetries()` kallar hana
  ekki, því hún setur `ringing = false`.

Óstaðfest: hvort ný full-screen-tilkynning ræsi skjáinn **ofan á** skjá annars
forrits meðan síminn er í notkun, eða verði aðeins heads-up. Logcat-línan
`Vekjaraskjárinn sést ekki eftir …s` segir hvenær tilraun var gerð.

**Heilsuvöktun.** Appið skráir í hvert sinn sem vekjarinn hringir í alvöru og
ber saman við `lastScheduledTriggerMillis` — tímann sem var *raunverulega*
skráður, ekki endurreiknaðan út frá núverandi stillingum. Sá munur skiptir máli:
færi notandinn 07:00 í 06:30 eftir velheppnaða hringingu leit það áður út eins
og klikkaður vekjari.

Merkið er fryst meðan liðinn óhringdur tími stendur, svo `Application.onCreate`
færi það ekki á morgundaginn áður en viðvörunin næði að birtast — og þítt aftur
þegar notandinn kvittar, svo vöktunin þagni ekki að eilífu eftir fyrsta klikk.

---

## 6b. Vekjaraspjaldið

**Stóra talan er næsta hringing, ekki sjálfgefni tíminn.** Hún svarar þeirri
spurningu sem fólk vaknar með — „hvenær hringir hann næst?" — en ekki þeirri
óbeinu, hver sjálfgefni tíminn sé. `ClockMode` ræður þremur ástandum:

| `ClockMode` | Hvað talan sýnir | Ýt |
|---|---|---|
| `NEXT_RING` | næstu hringingu | stillir **þann dag** |
| `DEFAULT` | sjálfgefna tímann (slökkt, eða enginn dagur valinn) | stillir sjálfgefið |
| `SNOOZE` | blundslok | ekkert — talan er frétt, ekki stilling |

**Ýt á stóru töluna breytir ekki allri vikunni.** Fyrri tillaga var að ýt
stillti sjálfgefna tímann þegar næsti dagur fylgdi honum. Því var hafnað: sá
sem vill sofa út á laugardag heldur að hann sé að stilla morgundaginn og
hreyfir um leið alla daga vikunnar. Ýt á stóru töluna sendir því nákvæmlega
sama atburð og ýt á dálk þess dags. Sjálfgefna línan — „sjálfgefið 07:00" —
er eina leiðin að `alarmHour`/`alarmMinute`, og hún stendur alltaf þegar
stóra talan sýnir eitthvað annað.

**Talan tikkar.** Hún er leidd af `TriggerTimes.next` og verður því að
endurreiknast á þremur stöðum: mínútumótum, `ON_RESUME` og
`persistAndReschedule()`. `refreshAlarmView()` í `MainScreen` er eini
staðurinn sem gerir það — þessar línur voru áður afritaðar á alla þrjá
staðina, sem er nákvæmlega hvernig fjórða afritið gleymist.

**Dálkarnir nota `FlowRow(maxItemsInEachRow = 5)`**, ekki óheft `FlowRow` og
alls ekki `Row` með `weight`. Reitur er 48 dp og bilið 6, svo sjö reitir taka
372 dp — en spjaldið hefur aðeins skjábreidd mínus 80 (20 dp spássía á
`Column` og 20 dp inn í `Card`, báðum megin). Óheft `FlowRow` gefur því **6+1
á Pixel-flokki**, þar sem sunnudagurinn lendir einn á línu, og þakið útilokar
það.

**Þakið er ÞAK, ekki fastur fjöldi** — `FlowRow` brýtur línuna hvort sem er
þegar breiddin klárast. Fimm reitir eru 264 dp, svo virku dagarnir lenda á
fyrri línunni og helgin á þeirri síðari frá 360 dp og upp; á 320 dp skjá
fellur það sjálfkrafa í 4+3. v0.972 hafði þakið á 4 af því að ég ruglaði
þessu tvennu saman og þvingaði þar með versta tilfellið upp á öll tæki.

**Penni, ekki undirstrik.** Stóra talan og „sjálfgefið HH:MM" eru báðar
stillanlegar, og merkið um það er `Icons.Outlined.Edit` við hliðina á tölunni
— 24 dp við þá stóru, 16 dp við þá litlu. Undirstrik var reynt fyrst en las
sem vefhlekkur undir 45 sp tölu. Allur reiturinn, talan og penninn saman, er
einn snertiflötur. Í `SNOOZE` er enginn penni: þá er talan frétt en ekki
stilling og má ekki líta út fyrir að vera stillanleg.

**Hvor snertiflötur er 48 dp.** Stafurinn og tíminn eru tveir aðskildir
snertifletir hvor ofan á öðrum, og mistök þar slökkva á degi þegar átti að
stilla hann — sem þýðir að fólk vaknar ekki. Slökktur dagur hefur engan tíma
að stilla; „—" er skraut og er haldið utan við tab-röðina.

**Klukkuglugginn er `ui/TimePickDialog.kt`**, Material3, ekki
`android.app.TimePickerDialog`. Pallaglugginn las þemað úr stillingu símans
en ekki úr `AppTheme.mode`, svo „Dökkt" í ljósum síma skilaði ljósum glugga.
Undir skífunni stendur hvað tillagan þýðir, reiknað með `TriggerTimes.next` á
henni sjálfri **áður** en hún er staðfest. Áður sást sú tala aðeins eftir á,
þótt hún sé einmitt það sem ákvörðunin snýst um.

**Talan er reiknuð á þeim degi sem verið er að stilla**, ekki á næstu
hringingu. Ætti hún alltaf við næstu hringingu stæði hún kyrr þegar maður
stillir dag sem hringir ekki næst — og glugginn liti út fyrir að bregðast
ekki við skífunni. Þess vegna eru orðin tvenns konar: sé dagurinn sjálfur
næsta hringing er talan **svefn**, sama tala og belgurinn sýnir á eftir;
annars er hún **bið** („Hringir eftir 2 daga 6 klst"). Enginn sefur í tvo
daga, og línan má ekki halda því fram.

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

**Varahljóð** er val notandans: vekjarahljóðið (sjálfgefið kirkjuklukka
Staðarfells í APK-inu, í lykkju) eða Rás 1 í beinni. **Síðasta vörnin er alltaf
innbyggða klukkan** — brjóti streymið, eða sé eigið hljóð skemmt, tekur hún við.
Vekjari sem þegir af því notandinn valdi gallaða skrá er versta mögulega bilunin.

**Vakna við vekjarahljóð, svo bæn.** Margir heyra ekki bæn sem þeir eru að
vakna við. Í þessum ham hringir vekjarahljóðið í lykkju (`Stage.WAKE_SOUND`),
og „Slökkva" sendir `ACTION_AWAKE` í stað `ACTION_DISMISS`. Þá þagnar
vekjarinn, skilar hljóðstyrk og fókus, og `afterWake` ræður:

- **Bæn strax** — `startListening()`, og skjárinn verður spilari.
- **Spyrja mig** — skjárinn spyr. Slökkt úr *tilkynningunni* verður „Seinna",
  því þá er enginn skjár til að spyrja.
- **Seinna** — tilkynning á `morgunbaen_prayer`-rásinni sem spilar bænina;
  hverfur eftir 12 klst.

**Hlustun er ekki vekjari.** `USAGE_MEDIA` (miðlastyrkur; ExoPlayer sér um
fókus og gerir hlé þegar heyrnartól eru tekin úr), engin 15 mín tímamörk, og
þegar bæn og fréttir klárast **hættir hún** í stað þess að fara í varahljóð.
Sé engin bæn á disknum spilast Rás 1. `AlarmService.listeningState` segir
skjánum hvenær hlustun lýkur, svo hann loki sér.

---

## 7b. Ljóst og dökkt

**Þemað er á tveimur stöðum og þau verða að vera samstillt.** Litirnir koma úr
`ui/Theme.kt`, en glugginn sjálfur — það sem sést í fyrsta ramma, áður en
Compose hefur teiknað neitt — kemur úr `res/values/themes.xml` og
`res/values-night/themes.xml`. Lengi vel var aðeins ljósa þemað til, svo hvítur
gluggi blikkaði áður en appið birtist í myrkri. `windowBackground` er nú bundinn
við `@color/window_background`, sem hefur sömu gildi og `LightColors.background`
og `DarkColors.background`. **Breytir þú öðrum staðnum þarftu að breyta hinum.**

**`AppTheme.mode` er `MutableStateFlow`** — sama mynstur og
`AlarmService.listeningState`. Allir fjórir skjáirnir vefja sig í
`MorgunbaenTheme`, svo þeir skipta um ham samstundis, líka vekjaraskjárinn.
Gildið er lesið úr `Prefs` í `MorgunbaenApp.onCreate`, á undan hverjum skjá og
í device-protected geymslu, svo það gildi líka fyrir upplásningu símans.

**Birta táknanna í stöðustikunni er sett í `MorgunbaenTheme`**, ekki í
`enableEdgeToEdge`. Valið útlit getur gengið gegn stillingu símans, og
`SystemBarStyle.auto` les eingöngu stillingu símans.

`AlarmActivity` fær ekki `enableEdgeToEdge`. Innihald hennar er miðjujafnað með
32 dp spássíu og hún birtist á læstum skjá, þar sem minnstu breytingar eru
áhættusamastar.

---

## 8. Ef vekjarinn hringir ekki

**Rafhlöðusparnaður.** Stillingar → Rafhlaða → Bakgrunnsnotkun → Morgunbæn →
**Ótakmarkað**.

**Android tekur heimildir af ónotuðu appi.** Afhakaðu við
„Remove permissions if app is unused" undir Settings → Apps → Morgunbæn.
Vekjari á virkum dögum er ónotaður yfir helgi — nákvæmlega þröskuldurinn.

**Full-screen intent eða tilkynningaheimild vantar.** Appið varar við báðum efst
á forsíðunni með takka beint í réttu stillinguna.

---

## 9. Prófanir

**Fyrst, á tölvunni — engan síma þarf:** `./gradlew test` keyrir **35 próf** —
`TriggerTimesTest.kt` (26 á tímareikningnum), `DayTimesParseTest.kt` (3 á
lestri vistaðra tíma) og `DayTimesLogicTest.kt` (6 á dálkunum: hvenær dagur
fær eigin tíma, og hvaða tímar lifa af að rofinn var lagður niður). Grípur
ekki neitt sem snertir Android sjálft, en grípur allt sem snertir *hvenær*
vekjarinn og sóknarglugginn eiga að fara í gang — ódýrasta og hraðasta
staðfestingin sem til er á verkefninu.

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
7. **Vekjarahljóð, svo bæn.** Veldu hljóð símans, svo eigin skrá, og keyrðu
   „Prófa vekjarann" í hvoru tilviki. Prófaðu svo „Bæn strax", „Spyrja mig" og
   „Seinna" — hlustunin á að hætta þegar bænin klárast, og blundur á að hringja
   aftur með vekjarahljóðinu. Endurtaktu Direct Boot-prófið (5) með eigið hljóð
   valið.
8. **Spila bænina.** Ýttu á „Spila bænina" á forsíðunni og farðu svo úr
   appinu (heim-takkinn) — hljóðið á að þagna. Kom það ekki, er
   `ON_PAUSE`-stöðvunin í `MainActivity.kt` biluð.
9. **Vekjaraspjaldið.** Dagarnir eiga að brotna Má–Fö / La Su. Ýttu á stóru
   töluna og gættu að því að hún stilli **þann dag** en ekki alla vikuna;
   ýttu á „sjálfgefið" til að hreyfa hina alla í einu. Stilltu dag á sama
   tíma og sjálfgefið — hann á að verða daufur aftur. Opnaðu dag sem hringir
   **ekki** næst og snúðu skífunni: talan undir henni á að hreyfast og segja
   „Hringir eftir …"; opnirðu daginn sem hringir næst á hún að segja „Þú færð
   … svefn" og vera sama tala og belgurinn á spjaldinu. Blundaðu og gáðu að
   því að penninn hverfi meðan talan sýnir blundslok.
10. **Raunverulegar aðstæður.** Láttu appið vekja þig í viku samfleytt.

Sú síðasta er sú eina sem sannar eitthvað. Vekjari sem virkar kl. 13:10 meðan þú
horfir á símann sannar ekkert; vekjari sem hringir eftir sjö tíma svefn með
dimmum skjá og Doze í fullum gangi sannar allt. **Hver ný útgáfa endurstillir
teljarann.**

---

## 10. Útgáfa og Play Store

### Undirritun

Útgáfur eru undirritaðar lykli sem býr **hvergi í repo-inu**. `release.yml` les
hann úr fjórum GitHub-leyndarmálum; heimavinnsla les hann úr `keystore.properties`
sem `.gitignore` heldur úti. Finnist hvorugt er enginn `signingConfig` settur — það
er viljandi, svo hver sem er geti klónað verkefnið og keyrt `./gradlew test` og
`assembleDebug` án þess að eiga lykilinn.

| Leyndarmál | Gildi |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 morgunbaen-release.jks` |
| `KEYSTORE_PASSWORD` | store-lykilorðið |
| `KEY_ALIAS` | dulnefni lykilsins |
| `KEY_PASSWORD` | key-lykilorðið |

**Afrit af lyklinum er forsenda þess að appið sé uppfæranlegt.** Android hafnar
uppfærslu sem er undirrituð öðrum lykli en þeim sem fyrir er; týnist hann þarf
hver einasti notandi að fjarlægja appið og setja upp á nýtt — og tapar þá
vekjarastillingum. Geymdu hann í lykilorðageymslu og utan tölvunnar.

v1-undirritun (JAR) er slökkt því `minSdk` er 26; v2 og v3 eru kveiktar **skýrt**
frekar en að treysta sjálfgildum AGP, svo uppfærsla á byggingartólunum breyti ekki
undirritun útgáfa í kyrrþey. v3 er forsenda þess að geta skipt um lykil síðar.

### Að klippa útgáfu

1. Uppfærðu `versionCode`/`versionName` í `app/build.gradle.kts` og bættu kafla
   við `BREYTINGAR.md`. Reglan er: `versionCode` er `versionName` án punkts,
   **með þremur aukastöfum** (0.96 → 960, 0.961 → 961, 1.0 → 1000). Án
   núllsins yrði 0.96 að 96, sem er lægra en 0.952 → 952, og Android neitar
   að uppfæra í lægra `versionCode`.
2. `git tag -a v0.94 -m "..."` og `git push origin v0.94`.
3. `release.yml` keyrir prófin, byggir undirritað APK, staðfestir undirritunina
   með `apksigner` og birtir það undir Releases sem `morgunbaen-v0.94.apk`.

Vinnuflæðið **stöðvar sig með læsilegri villu** vanti `KEYSTORE_BASE64`, frekar en
að byggja óundirritað APK sem enginn getur sett upp. SHA-256 vottorðsins er prentað
í loggið — beri maður það saman milli útgáfna sést strax ef lyklinum var skipt út.

> v0.92 og v0.93 voru **debug-undirritaðar** og því ekki uppfæranlegar í v0.94.
> Sú leið er brotin einu sinni, í eitt skipti; eftir það er hún stöðug.

### Styrkir

Greiðsluupplýsingar eru **hvergi í appinu**. `AboutActivity` opnar vefsíðu
sem höfundurinn hýsir sjálfur (Tailscale Funnel á rpi5-sd); slóðin er eina
gildið sem appið geymir, `BuildConfig.DONATE_URL` í `app/build.gradle.kts`.

Ástæðan er sú sama og með undirritunarlykilinn: það sem fer í opinbert repó
eða í útgefið APK verður ekki tekið til baka. Kennitala og reikningsnúmer á
vefsíðu má hins vegar breyta eða fjarlægja hvenær sem er, án uppfærslu.

**Slóðin má ekki breytast** — hún er í öllum útgefnum APK-skrám. Færist
hýsingin þarf lén sem höfundurinn ræður yfir.

`BuildConfig.SHOW_DONATION` felur „Styrkja“-hnappinn. Reglur Google um
greiðslur takmarka hlekki á styrki utan Play, svo Play-útgáfa setur hann á
`false`. Uppsetning síðunnar sjálfrar er skjöluð utan repósins, í
`Morgunbæn android/styrkja-sida/UPPSETNING.md`.

### Áður en þetta fer í Play Store

**Sendu RÚV póst.** Mikilvægast. Viðmótið er óskjalfest og ein breyting slekkur
á appinu hjá öllum samtímis. Grænt ljós og tengiliður eru meira virði en
nokkur kóði.

**targetSdk 36.** Stendur í 35. Frá 31. ágúst 2026 krefst Google Play að ný öpp
miði á Android 16. Fyrir óútgefið app skapar fresturinn engan flýti — þú þarft
36 hvenær sem þú gefur út — en þetta er **ekki einnar línu breyting**: Android 16
fjarlægir undanþáguna frá edge-to-edge teikningu, sem er raunveruleg
viðmótsvinna.

Annað sem vantar:

- **`USE_EXACT_ALARM`** þarf réttlætingu í Play Console. Vekjari er gild ástæða.
- **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** er takmörkuð heimild. Vekjaraöpp
  eru á lista Google yfir gildar undantekningar.
- **Forgrunnsþjónusta `mediaPlayback`** — Play biður um lýsingu og stundum myndband.
- **Persónuverndarstefna** — krafist þótt appið safni engu.
- **`isMinifyEnabled`** er `false` og `proguard-rules.pro` tóm. R8 er viljandi
  slökkt: Compose, media3, WorkManager og OkHttp reiða sig öll á endurskin að
  einhverju leyti, og klippi R8 of nærri birtist það sem vekjari sem hringir ekki.
  Sú ferð þarf eigin prófun á tæki.

---

## 11. Það sem vantar enn

- **Einingapróf víðar.** Prófin ná yfir tímareikninginn og rökfræði dálkanna
  (`TriggerTimesTest.kt`, `DayTimesParseTest.kt`, `DayTimesLogicTest.kt`), en
  ekkert annað — t.d. `Dates.kt`-þáttun eða `EpisodeRepository`-röklegan gang
  (án nettengingar, með mock-uðum `RuvClient`). Mynstrið er alltaf það sama:
  draga ákvörðunina út í hreint fall á `companion object` og prófa hana á JVM.
  Robolectric er ekki í verkefninu, svo allt sem snertir `Context` er óprófað.
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

**Ein leiðréttingarlota grípur ekki öll tilvik af sömu villu.** Helgarforsendan
kom upp þriðja sinni í `Prefs.weekendTimeEnabled`s eigin skjölun, löngu eftir
að hún átti að heita leiðrétt — vegna þess að fyrri yfirferðin lagaði aðeins
þá staði sem hún vissi af (`alarmDays`-skjölunina, `DEFAULT_DAYS`-athugasemdina),
ekki hvert einasta sjálfstæða heimili villunnar. Og öfugt: að sending standist
byggingu og próf óbreytt (eins og v0.92 gerði) er ekki sönnun þess að ekkert
standi eftir — aðeins að þessi tiltekna sending hafi ekkert nýtt fundið til að
bæta við. Hvort tveggja kallar á sama viðbragð: `grep` breiðar en þrengri
lagfæringar, og hverja nýja sendingu sem sjálfstæða tilraun, ekki lokapunkt.

---

Höfundarréttur © 2026 AEH. GPL-3.0 — sjá [LICENSE](LICENSE).

Varahljóð: eldri klukka Staðarfellskirkju (líklega fyrir 1300), upptaka af
[kirkjuklukkur.is](https://www.kirkjuklukkur.is/vesturlandsprofastsdaemi/stadarfellskirkja/).
