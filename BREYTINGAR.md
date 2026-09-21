# Breytingar

Stutt útgáfusaga — sjá `LESTU_MIG.md` fyrir hvernig hlutirnir hanga saman og
af hverju, `git log` fyrir fullar commit-lýsingar.

## v0.974

- **Vekjaraskjárinn gleymist ekki lengur á bak við annan vekjara.** Hringdu
  Morgunbæn og annað vekjaraforrit á sömu sekúndu gat hitt lagst ofan á
  skjáinn okkar — og þá kom hann aldrei aftur. Bænin spilaði og ekkert var
  hægt að gera nema drepa appið. Nú athugar Morgunbæn hvort skjárinn sést, og
  reynir að koma honum upp aftur nokkrum sinnum fyrstu fjörutíu sekúndurnar —
  líka eftir að hinn vekjarinn hefur verið afgreiddur.

## v0.973

- **Virku dagarnir eru komnir á sömu línu.** Föstudagurinn lenti með helginni
  þótt nóg pláss væri fyrir hann uppi. Nú standa Má–Fö saman og helgin fyrir
  neðan, á öllum venjulegum símum.
- **Penni í stað undirstriks.** Stóra talan og „sjálfgefið“ eru merktar með
  litlum penna frekar en striki undir — strikið las eins og vefhlekkur.
- **Svefnlínan hreyfist með skífunni.** Stilltirðu dag sem hringir ekki á
  morgun stóð talan kyrr og glugginn virtist ekki bregðast við. Nú telur hún
  að þeim degi sem þú ert að stilla — og segir „Hringir eftir …“ í stað
  „svefn“ þegar það á lengra í land en eina nótt.

## v0.972

- **Vekjaratíminn er ekki lengur falinn á þremur stöðum.** Stóra talan sýnir
  núna **næstu hringingu** — þann tíma sem þú vaknar við — og vikudaginn undir
  henni. Ýttu á hana til að breyta þeim degi.
- **Vikan er komin í dálka.** Hver dagur hefur staf og tíma: stafurinn kveikir
  eða slekkur, talan undir honum stillir þann dag. Rofinn „Mismunandi tími
  eftir dögum“, „Stilla daga“ og blaðið neðan frá eru öll farin — það þarf
  ekki lengur að kveikja á neinu til að láta laugardaginn sofa út. Dagur sem
  er stilltur á sama tíma og hinir fylgir þeim sjálfkrafa aftur.
- **Nýtt klukkuval.** Gamli kerfisglugginn er farinn og í staðinn kemur
  klukka í útliti appsins. Þú getur slegið tímann inn í stað þess að snúa
  skífu, og **undir henni stendur hve langur svefninn verður**, áður en þú
  staðfestir.
- **Þú velur blundlengdina sjálf(ur).** Áður voru fastir kostir — 5, 9, 10, 15
  og 20 mínútur — og ekkert þar á milli. Nú er teljari: frá einni mínútu upp í
  sextíu, og það gengur hraðar að halda hnappnum inni.
- **Ljóst og dökkt fylgir símanum.** Litirnir gerðu það áður, en glugginn ekki,
  svo hvítur skjár blikkaði þegar appið var opnað í myrkri og táknin í
  stöðustikunni gátu horfið. Hvort tveggja er lagað — líka á vekjaraskjánum.
- **„Útlit“ neðst á forsíðunni.** Viljirðu appið dökkt þótt síminn sé ljós, eða
  öfugt, þá velurðu það sjálf(ur): Kerfið, Ljóst eða Dökkt.
- **Falinn helgartími getur ekki lengur vaknað til lífsins.** Hafðir þú stillt
  einstaka daga og slökkt svo á rofanum voru tímarnir enn vistaðir, óvirkir.
  Þeir eru hreinsaðir við uppfærslu, svo vekjarinn þinn haldist nákvæmlega
  þar sem hann var.

## v0.97

- **Dagarnir taka ekki lengur yfir vekjaraspjaldið.** Listinn með tíma hvers
  dags ýtti „Næst:“ og „Sleppa næstu“ allt að sjö línur niður. Nú stendur
  samantekt undir rofanum — „Fös 08:15 · Lau 09:30“ — og „Stilla daga“ opnar
  blað sem rennur upp neðan frá. Breytingar vistast strax, eins og áður.
- **„Styrkja Morgunbæn“ neðst á forsíðunni** opnar nýjan skjá: stutt um af
  hverju appið varð til, leiðir til að styrkja, og leiðir sem kosta ekkert —
  að deila appinu, gefa stjörnu eða tilkynna villu. Styrkur opnar ekkert í
  appinu; vekjarinn hegðar sér eins fyrir alla.
- **Lokagæsalappir birtust ekki.** Leiðbeiningin um „Remove permissions if app
  is unused“ hefur vantað lokagæsalappir síðan v0.951, því ó-hjúpaðar
  gæsalappir eru fjarlægðar úr strengjaskrám við byggingu.

## v0.96

- **Mismunandi tími eftir dögum.** „Annar tími um helgar" var of þröngur: sumir
  vilja fara fyrr á fætur á föstudögum, aðrir sofa lengur bara á sunnudögum.
  Rofinn heitir nú „Mismunandi tími eftir dögum" og opnar lista yfir valda
  daga, hver með klukku sem má ýta á. Dagar án eigin tíma fylgja stóru
  klukkunni. Helgartími sem var stilltur færist sjálfkrafa yfir á laugardag
  og sunnudag — ekkert glatast við uppfærsluna.
- **Vakna við vekjarahljóð, hlusta á bænina á eftir.** Margir heyra ekki bæn
  sem þeir eru að vakna við. Undir Vakning er nú „Vakna við": bænina sjálfa,
  eins og áður, eða vekjarahljóð fyrst. Eftir að slökkt er á hljóðinu byrjar
  bænin strax, skjárinn spyr hvort eigi að hlusta, eða tilkynning bíður þar
  til hentar. Bænin spilast þá á miðlastyrk og hættir þegar hún er búin.
- **Eigið vekjarahljóð.** Hljóð símans eða eigin hljóðskrá, í stað
  kirkjuklukkunnar. Hljóðið er afritað inn í appið svo það virki líka eftir
  endurræsingu áður en síminn er opnaður. Klikki hljóðið tekur kirkjuklukkan
  við — vekjarinn má aldrei þegja.

## v0.952

- **„Sleppa næstu" aftur eins og í v0.95.** Stóri ramminn ofar á spjaldinu
  í v0.951 var of áberandi. Takkinn er aftur textahnappur undir „Næst:",
  eftir helgarrofanum, aðeins þegar vekjarinn er kveiktur og dagar eru
  valdir.
- **Obtainium-leiðbeiningar** í README: slóðin á GitHub-repóið og smellanlegur
  `obtainium://add/…`-hlekkur, svo uppfærslur komi án Play Store. GitHub
  Releases lesa héðan í frá kaflann fyrir taggið í stað autogenerated
  compare-tengils sem sagði notandanum ekkert.

## v0.951

- **„Sleppa næstu" fannst ekki.** Takkinn sat neðarlega sem daufur textahnappur
  og birtist aðeins með kveiktum vekjara. Hann er nú áberandi hnappur beint
  undir „Næst:", sýnilegur um leið og vekjarinn er kveiktur — án þess að
  skruna framhjá helgarrofanum.
- **Helgartíminn er jafn stór og aðalklukkan** þegar „Annar tími um helgar"
  er kveikt, og smellanlegur eins og hún. Áður var hann aðeins strengur í
  lýsingunni undir rofanum; fyrsta hreyfing margra er að ýta á töluna.
- **Varahljóð er val, ekki sjálfvirk röð.** Undir Vakning velur maður
  kirkjuklukku Staðarfells eða Rás 1. Gildir þegar bænin er búin, eða vantar
  á diskinn. Brjóti streymið tekur klukkan við — hún má ekki þegja.
- **Ein heimildaleiðbeining, með nafninu sem stendur í símanum.** Xiaomi-
  og Samsung-slóðirnar í v0.95 pössuðu ekki við enska stillingaskjáinn.
  Nú stendur: afhakaðu við „Remove permissions if app is unused" undir
  Settings → Apps → Morgunbæn. Vekjari á virkum dögum er ónotaður yfir
  helgi — nákvæmlega þröskuldurinn sem Android notar til að taka
  heimildir af appinu.
- Upprunasaga um Termux + ffmpeg + MacroDroid tekin úr `LESTU_MIG.md` —
  hún lýsti hvernig verkefnið byrjaði, ekki því sem notandinn þarf.

## v0.95

- **Sleppa næstu hringingu** — einn takki, enginn auka vekjari. Fyrir
  þjóðhátíð og veikindi. Hægt að hætta við. Prófunarhnappurinn er óháður
  sleppingunni svo maður geti enn staðfest Samsung-stillingar án þess að
  eyða morgundeginum.
- **Eigið varahljóð, ekki kerfisvekjari.** Eldri klukka Staðarfellskirkju
  (líklega fyrir 1300, sprunginn hljómur; upptaka af kirkjuklukkur.is) er
  í APK-inu. Sjálfgefinn vekjaratónn símans getur vantað — þá þagði appið
  áður, með skjá og titring sem eina varaleið.
- **Rás 1 sem varaleið** þegar engin bæn er á disknum. Beint HLS-streymi
  af útsendingunni sjálfri; brjóti netið tekur kirkjuklukkan við, og
  fréttir dagsins fá að spilast ef þær náðust. (Í v0.951 varð þetta val
  notandans, ekki sjálfvirk röð.)
- **Repóið er public** á GitHub. Útgáfur og APK eru sýnileg án innskráningar;
  undirritunarleyndarmálin haldast leynd.

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
