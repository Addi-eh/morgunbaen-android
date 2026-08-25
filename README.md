# Morgunbæn

Vekjaraklukka fyrir Android sem spilar „Morgunbæn og orð dagsins" af Rás 1 —
og valkvætt fréttirnar kl. 07:00 á eftir, eins og í útsendingunni sjálfri.

Appið tekur ekkert upp. Það sækir þáttinn frá RÚV og geymir hann á tækinu, svo
bænin spilast þótt síminn sé án nettengingar þegar vekjarinn hringir.

---

## Að setja upp

Sæktu nýjustu APK-skrána undir [Releases](../../releases) og opnaðu hana í
símanum. Android spyr hvort þú viljir leyfa uppsetningu frá þessari uppsprettu —
það er eðlilegt fyrir öpp sem koma ekki úr Play Store.

**Þrennt þarf að leyfa í fyrstu opnun.** Appið biður um það og sýnir rauða
viðvörun ef eitthvað vantar:

| Heimild | Án hennar |
|---|---|
| Tilkynningar | Vekjarinn birtist alls ekki |
| Birta á læstum skjá | Bænin spilar en enginn skjár kemur upp til að slökkva |
| Ótakmörkuð rafhlöðunotkun | Bænin sækist ekki á nóttunni |

**Ef þú ert á Samsung** þarf eitt í viðbót: Stillingar → Umhirða tækis →
Rafhlaða → Takmörk á bakgrunnsnotkun → **Öpp sem sofa aldrei**. Samsung svæfir
öpp sem hafa ekki verið opnuð í þrjá daga, og vekjari sem hringir aðeins á
virkum dögum er ónotaður yfir helgi. Appið sýnir þessar leiðbeiningar sjálfkrafa.

Ýttu svo á **Prófa vekjarann**, læstu símanum og staðfestu að hann hringi.
Það tekur hálfa mínútu og sparar þér einn morgun.

---

## Hvað appið gerir

- Sækir bæn dagsins sjálfkrafa — leitarglugginn opnast kl. 07:00 og reynir
  á fimm mínútna fresti þar til þátturinn finnst, með sex tíma öryggisneti
  allan sólarhringinn.
- Hringir með `setAlarmClock`, sem kemst í gegnum Doze og orkusparnað.
- Virkar þótt síminn hafi endurræst um nóttina og enginn slegið inn PIN
  (Direct Boot).
- Lætur vita ef síminn hefur stöðvað appið — Android segir ekkert sjálft.
- Fyrri bænir, deiling, blundur, valkvæður helgartími, vaxandi hljóðstyrkur
  og titringur.

Ítarleg lýsing á öllum kerfum er í [LESTU_MIG.md](LESTU_MIG.md), og
breytingasaga í [BREYTINGAR.md](BREYTINGAR.md).

---

## Að byggja

Kotlin og Jetpack Compose. Opnaðu möppuna í Android Studio — veldu **Gradle JVM
21**, nýrri Java ræður Gradle ekki við.

```bash
./gradlew test            # einingapróf á tímareikningi vekjarans
./gradlew assembleDebug   # APK í app/build/outputs/apk/debug/
```

---

## Um efnið frá RÚV

Appið hvorki geymir né dreifir efni RÚV. Það sækir sömu MP3-skrár og
[Spilari RÚV](https://www.ruv.is/utvarp) birtir, í tæki notandans, og deiling
sendir hlekk á ruv.is frekar en hljóðskrána sjálfa.

Viðmótið sem notað er (`spilari.nyr.ruv.is/gql/`) er **óskjalfest og getur
breyst án fyrirvara.** Hætti appið að finna bænina er það fyrsta sem á að
athuga — sjá lið 1 í LESTU_MIG.md.

Allt efni er höfundarréttarvarið RÚV. Þetta verkefni er ótengt RÚV.

---

## Leyfi

GPL-3.0 — sjá [LICENSE](LICENSE). Höfundarréttur © 2026 Adam Einar Hildarson.
