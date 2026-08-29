# Breytingar

Stutt útgáfusaga — sjá `LESTU_MIG.md` fyrir hvernig hlutirnir hanga saman og
af hverju, `git log` fyrir fullar commit-lýsingar.

## v0.95

- **Sleppa næstu hringingu** — einn takki á vekjaraspjaldinu, enginn auka
  vekjari. Fyrir þjóðhátíð og veikindi. Hægt að hætta við. Prófunarhnappurinn
  er óháður sleppingunni.
- **Bakgrunnssvefn: Xiaomi/HyperOS, Huawei, Oppo og OnePlus** fá sömu
  viðvörun og Samsung, með leiðbeiningum og óskjalfestum leið inn í
  stillingarnar (fellur á app-upplýsingar ef intentið bregst).
- **Eigið varahljóð:** eldri klukka Staðarfellskirkju (upptaka af
  kirkjuklukkur.is) er í APK-inu og spilast í lykkju þegar bæn/fréttir
  klárast eða klikka. Kerfisvekjari getur vantað; þá þagði appið áður.
- **Rás 1 sem varaleið** þegar engin bæn er á disknum. Beint HLS-streymi;
  brjóti netið tekur kirkjuklukkan (og fréttir dagsins, séu þær til) við.

## v0.94

- **Útgáfur eru nú undirritaðar alvöru lykli.** `release.yml` byggði áður
  `assembleDebug` og birti debug-undirritað APK, svo v0.92 og v0.93 voru
  undirritaðar debug-lykli hlauparans. Android hafnar uppfærslu sem er
  undirrituð öðrum lykli en þeim sem fyrir er — fólk þurfti því að fjarlægja
  appið, og tapa vekjarastillingum, við hverja uppfærslu. Nú byggir hún
  `assembleRelease` með lykli úr GitHub-leyndarmálum, staðfestir undirritunina
  með `apksigner` og prentar SHA-256 vottorðsins í loggið. Lykillinn býr hvergi
  í repo-inu; `signingConfig` er einfaldlega ekki settur finnist hann ekki, svo
  hver sem er getur áfram klónað og keyrt próf og debug-byggingu án hans.
  v1-undirritun slökkt (minSdk 26), v2 og v3 kveiktar skýrt frekar en að treysta
  sjálfgildum AGP. Release-APK er um leið 31% minna en debug-APK (15,0 MB í stað
  21,9), eingöngu af því debug-upplýsingar falla brott — R8 er áfram slökkt.
  **Leiðin úr debug-undirritaðri v0.93 í næstu útgáfu krefst þess að notendur
  fjarlægi appið einu sinni enn.** Eftir það er hún stöðug.

## v0.93

- **Teljari að næstu hringingu** birtist undir vekjaraklukkunni — „2 klst
  7 mín" — svo klukkan á skjánum svari líka spurningunni sem raunverulega er
  spurt á kvöldin: hve lengi má ég enn sofa? Reikningurinn er hreint fall í
  `TriggerTimes.countdown()` með eigin prófum; mínúturnar eru námundaðar upp
  svo teljarinn standi ekki á núlli heila mínútu áður en hringt er. Hann
  tikkar á mínútumótum meðan skjárinn er opinn og telur niður að blundslokum
  þegar blundað er, til samræmis við textann undir honum.
- **Titillinn er í miðju** á báðum skjám (`CenterAlignedTopAppBar`); til-baka-örin
  á „Fyrri bænir" situr áfram vinstra megin.
- **Klukkan sjálf opnar tímavalið** — fyrsta hreyfing margra er að ýta á töluna.
  „Breyta tíma"-hnappurinn stendur áfram fyrir þá sem giska ekki á það.
- **Textinn undir „Fréttir" lýsir eiginleikanum** í stað sóknarstöðu:
  „Fréttayfirlit RÚV á eftir bæninni". Áður stóð þar „Fréttir dagsins koma kl.
  07:00" á þeim tíma sólarhringsins sem fréttatíminn var ekki kominn út —
  staða sem svarar ekki spurningunni sem raunverulega er spurð, hvað gerist ef
  ég kveiki. Viðvörunin um vekjara sem hringir fyrir kl. 07:00 er óbreytt.
- **Teljarinn fluttur til hægri**, á sömu línu og „Breyta tíma". Þá stendur
  ekkert á milli klukkunnar og hnappsins sem breytir henni, og spjaldið styttist
  um eina línu. Belgurinn minnkaði lítillega (12dp innskot, 18dp tákn) svo
  lengsti teljaratextinn — „2 dagar 19 klst" — rúmist við hlið hnappsins á
  360dp skjá.
- **„Þú ert með nýjustu bænina"** í stað „Nýjasta bænin var þegar til staðar"
  þegar ýtt er á „Sækja núna" og ekkert nýtt er að hafa.
- **Skýrari texti á helgarrofanum þegar slökkt er á honum**: „Sami tími alla
  daga — kveiktu til að sofa lengur um helgar" í stað „Hrein tímastilling —
  bæn dagsins næst alla daga", sem sagði frá forsendu hönnunarinnar en ekki
  frá því hvað rofinn gerir.

## v0.92

- **Þriðja birtingarmynd helgarforsendunnar fjarlægð** — í
  `Prefs.weekendTimeEnabled`s eigin skjölun, sem fyrri leiðréttingarlota
  missti af. Samræmt orðalag lagað í `CatchUpScheduler`, `SyncWorker` og
  `AndroidManifest.xml`.
- **`AlarmScheduler.schedule()` afskráir sig núna á tómum dögum**, til
  samræmis við `CatchUpScheduler` — gömul skráning gat áður lifað áfram á
  degi sem var afvalinn.
- **Varið gegn `null` úr `RingtoneManager`**: sjálfgefinn vekjaratónn getur
  vantað á tækinu. Fellur á tilkynningartón, og á titring ef hvorugt er til,
  í stað þess að hrynja.
- **Framtíðarskráðir þættir sniðgengnir** við val á „nýjasta" þætti — RÚV
  getur skráð dagskrárlið áður en hann er fluttur.
- **Fréttatíma-„of snemmt"-athugunin er núna helgar-meðvituð**: metur virkan
  dag og helgardag (með sínum eigin tíma) hvorn í sínu lagi í stað þess að
  horfa aðeins á virka tímann.
- **`HistoryActivity`**: spilunartáknið núllstillist núna líka við
  `ON_STOP`, ekki bara þegar lagið klárast — annars gat það sýnt „Stopp"
  eftir að skjárinn hafði þagað spilunina.
- **Aðgengi**: `contentDescription` á deila/spila/stöðva/til-baka-táknum í
  `HistoryActivity`, sem höfðu `null` áður.

## v0.91

- **Fjórar villur lagaðar:** úrelt helgarathugasemd í MainActivity fjarlægð
  (stangaðist á við strenginn við hliðina); „Spila bænina" stöðvast nú þegar
  farið er úr appinu (ON_PAUSE — áður hélt hún áfram án nokkurs sýnilegs
  stöðvunartakka); tómir vekjaradagar afskrá sóknargluggann í stað þess að
  setja hann á rek um klukkuna; prófunartextinn hverfur þegar komið er til
  baka eftir hringinguna.
- **Helgartímarofi án helgardaga varar nú við** því að hann hafi engin áhrif.
- **MainActivity klofin:** útlitið býr í ui/AlarmCard, ui/PrayerCard,
  ui/WakeSettingsCard og ui/Components; MainActivity er samhæfingarlag með
  state og hlidarverkum.
- **Fyrstu einingaprófin:** TriggerTimesTest nær yfir hjarta appsins —
  þar á meðal nákvæmlega tilfellið sem olli reki gluggans (tómir dagar).
  JUnit bætt í build.gradle.kts; keyrist með `./gradlew test`.

## v0.9

- **Sóknargluggi fylgir vekjaradögum notandans**, ekki lengur harðkóðaðir
  virkir dagar. Leiðrétting á rangri forsendu frá fyrstu útgáfu: dagskrá RÚV
  staðfestir að Morgunbænin er flutt kl. 06:55 alla sjö daga vikunnar, líka
  um helgar.
- **Blundur skráir sig hjá heilsuvöktuninni** — drepi síminn appið á meðan
  blundað er, greinist það núna sem klikkaður vekjari í stað þagnar.
- **Prófunarhnappur**: hringir eftir 30 sek, sömu leið og alvöru vekjarinn
  (AlarmManager → AlarmReceiver → AlarmService → AlarmActivity) — engin þörf
  á að bíða til morguns til að sannreyna Samsung-stillingar.
- **„Spila bænina"** á forsíðunni, léttur miðlaspilari óháður vekjaranum.
- **User-Agent** á öllum netköllum til RÚV og niðurhals.
- **Heilsuvöktun** sýnir núna báðar viðvaranir samtímis (klikkaður vekjari og
  stöðnuð sókn koma oftast saman).

## v0.8

- **Rafhlöðuheimild sem vantaði** (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
  löguð — hafði valdið hruni frá fyrstu útgáfu þegar ýtt var á
  rafhlöðuviðvörunartakkann.
- **`CatchUpReceiver` gert `directBootAware`** — sóknarglugginn virkar núna
  líka áður en PIN er slegið inn.
- **Blundur fékk eigin `PendingIntent`** — deildi áður auðkenni með daglega
  vekjaranum og gat skrifað yfir hann.
- **Heilsuvöktunin hætt að frjósa** eftir fyrstu viðvörun.
- Varnir gegn tvöfaldri ræsingu þjónustunnar, `renameTo`-fallback fyrir
  niðurhal, sögulisti hættir að festast á „stopp".

## v0.72

Tvískipt „of snemmt"-ástand fyrir fréttir: stilltur vekjaratími og raunveruleg
klukka voru ranglega notuð til skiptis í sama skilyrðinu — leyst með tveimur
aðskildum strengjum.

## v0.71

Staðfesting á að þekktar villur (`mutableStateOf`-innflutningur, `newsDir`)
væru lagaðar; tvö ný atriði löguð til viðbótar.

## v0.7

**Rangt dagskrárauðkenni fundið og lagað**: fréttaliðurinn sem appið notaði
(39025) reyndist vikulegur sunnudagsþáttur, ekki daglegur. Rétt auðkenni er
38786.

## v0.65

Fréttir sóttar strax þegar kveikt er á rofanum, ekki beðið eftir næstu
bakgrunnssókn. Réttari stöðutexti.

## v0.6

Sögulisti (fyrri bænir, streymdar), deiling, valfrjáls helgartími,
hold-to-dismiss-takki, hljóðfókus, sjálfvirk stöðvun eftir 15 mín.

## v0.61

Valkvæmar fréttir eftir bænina. Vaxandi hljóðstyrkur og titringur breytt í
sjálfgefið af.

## v0.5

Nýtt forritstákn (adaptive icon).

## v0.4

Viðvörun ef tilkynningaleyfi eða full-screen-intent vantar, með beinni
ræsingu á vekjaraskjá sem varaleið.

## v0.3

Direct Boot-stuðningur, Samsung-leiðbeiningar, sóknargluggi kl. 07:00, valkvæð
fade-in/titringur.

## v0.2

Endurskoðar kerfisviðvaranir þegar skjárinn kemur í forgrunn, lagar dagavalið,
íslenskt dagsetningarsnið.
