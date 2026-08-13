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

## 5. Ef vekjarinn hringir ekki

Nær undantekningarlaust er ástæðan ein af þessum þremur:

**Rafhlöðusparnaður.** Þú ert á Samsung — og Samsung er með þeim allra verstu í
þessu. Farðu í Stillingar → Rafhlaða → Bakgrunnsnotkun → Morgunbæn →
**Ótakmarkað**. Slökktu líka á „Setja ónotuð öpp í dvala" í
Stillingar → Umhirða tækis → Rafhlaða.

Appið sjálft varar þig við þessu á forsíðunni, en notendur þínir munu samt
hunsa það. Gerðu ráð fyrir því.

**Tilkynningaheimild vantar.** Án hennar birtist vekjarinn ekki á læstum skjá.
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

## 7. Það sem vantar enn

Vísvitandi sleppt úr fyrstu útgáfu:

- **Eigið varahljóð.** Núna notar appið sjálfgefið vekjarahljóð símans ef bænin
  næst ekki. Það virkar, en er ekki fallegt.
- **Vaxandi hljóðstyrkur.** Byrjar lágt, hækkar á 30 sekúndum. Mun mýkri vakning.
- **Saga.** Að geta hlustað á bænir fyrri daga — þær hverfa ekki hjá RÚV.
- **Íslenskun kerfistexta.** Appið er á íslensku, en dagsetningarsnið gætu
  þurft fínstillingu.

Byrjaðu ekki á þessu. Byrjaðu á að láta vekjarann virka hjá þér í viku.
