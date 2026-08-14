# Breytingar

Stutt útgáfusaga — sjá `LESTU_MIG.md` fyrir hvernig hlutirnir hanga saman og
af hverju, `git log` fyrir fullar commit-lýsingar.

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
