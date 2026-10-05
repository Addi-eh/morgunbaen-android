# Morgunbæn

Vekjaraklukka fyrir Android sem spilar „Morgunbæn og orð dagsins“ af Rás 1 —
og fréttirnar kl. 07:00 á eftir ef vill, eins og í útsendingunni sjálfri.

Appið tekur ekkert upp. Það sækir þáttinn frá RÚV og geymir hann á tækinu, svo að
bænin spilist þótt síminn sé án nettengingar þegar vekjarinn hringir.

---

## Að setja upp

Sæktu nýjustu APK-skrána undir [Releases](../../releases) og opnaðu hana í
símanum. Android spyr hvort þú viljir leyfa uppsetningu frá þessum uppruna —
svo er um öll öpp sem koma ekki úr Play Store.

**Þrennt þarf að leyfa við fyrstu opnun.** Appið biður um það og sýnir rauða
viðvörun ef eitthvað vantar:

| Heimild | Án hennar |
|---|---|
| Tilkynningar | Vekjarinn birtist alls ekki |
| Birta á læstum skjá | Bænin hljómar en enginn skjár kemur upp til að slökkva á henni |
| Ótakmörkuð rafhlöðunotkun | Bænin næst ekki á nóttunni |

**Eitt í viðbót:** taktu hakið úr **Remove permissions if app is unused**
(Settings → Apps → Morgunbæn). Android tekur annars heimildir af appinu
yfir helgi ef vekjarinn hringir aðeins á virkum dögum.

Ýttu svo á **Prófa vekjarann**, læstu símanum og staðfestu að hann hringi.
Það tekur hálfa mínútu og sparar þér einn morgun.

### Obtainium — uppfærslur án Play Store

[Obtainium](https://github.com/ImranR98/Obtainium/releases) sækir nýjar
útgáfur beint héðan af GitHub og lætur vita (eða setur upp) þegar næsta
útgáfa kemur.

1. Settu Obtainium upp (APK undir Releases þar).
2. Opnaðu Obtainium → **Add app**.
3. Límdu þessa slóð og ýttu á **Add**:

   `https://github.com/Addi-eh/morgunbaen-android`

Ef Obtainium er þegar uppsett má [bæta Morgunbæn við með einum smelli](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Addi-eh/morgunbaen-android).

Obtainium á að sjá um uppfærslur héðan í frá. APK með annarri undirritun
uppfærir ekki þetta app — það þyrfti að fjarlægja appið fyrst.

---

## Hvað appið gerir

- Sækir bæn dagsins sjálfkrafa — sóknarglugginn opnast kl. 07:00 og appið
  leitar á fimm mínútna fresti þar til þátturinn finnst, með sex tíma öryggisneti
  allan sólarhringinn.
- Hringir með `setAlarmClock`, sem kemst í gegnum Doze og orkusparnað.
- Virkar þótt síminn hafi endurræst sig um nóttina og enginn slegið inn PIN
  (Direct Boot).
- Lætur vita ef síminn hefur stöðvað appið — Android segir ekkert sjálft.
- Vikan í dálkum: hver dagur hefur staf og tíma. Sofa lengur á laugardögum,
  fara fyrr á fætur á föstudögum — og fá samt bæn þess dags. Stóra talan á
  forsíðunni sýnir næstu hringingu, ekki abstrakt sjálfgefinn tíma.
- Klukkuval sem segir hve langur svefninn verður **áður** en þú staðfestir.
- Vakna við bænina sjálfa, eða við vekjarahljóð og hlusta á bænina á eftir:
  strax, eftir spurningu á skjánum, eða úr tilkynningu þegar hentar.
- Eigið vekjarahljóð: hljóð símans eða eigin hljóðskrá. Sjálfgefið er
  kirkjuklukka Staðarfells.
- Fyrri bænir, deiling, vaxandi hljóðstyrkur og titringur.
- Blundur að eigin vali, frá einni mínútu upp í sextíu.
- Ljóst eða dökkt eftir stillingu símans — eða þitt eigið val, ef þig langar
  að hafa appið dökkt þótt síminn sé ljós.
- Sleppa næstu hringingu (þjóðhátíð, veikindi) án þess að slökkva á vekjaranum.
- Varahljóð: vekjarahljóðið eða Rás 1 í beinni.
- „Um Morgunbæn“-skjár með upplýsingum um verkefnið og leiðum til að styrkja
  það. Appið er ókeypis og styrkur opnar ekkert í því.

Ítarleg lýsing á öllum kerfum er í [LESTU_MIG.md](LESTU_MIG.md) og
breytingasaga í [BREYTINGAR.md](BREYTINGAR.md).

---

## Að byggja

Kotlin og Jetpack Compose. Opnaðu möppuna í Android Studio — veldu **Gradle JVM
21**; Gradle ræður ekki við nýrri útgáfur af Java.

```bash
./gradlew test            # einingapróf á tímareikningi vekjarans
./gradlew assembleDebug   # APK í app/build/outputs/apk/debug/
```

---

## Um efnið frá RÚV

Appið geymir hvorki né dreifir efni RÚV. Það sækir sömu MP3-skrár og
[Spilari RÚV](https://www.ruv.is/utvarp) birtir, beint í tæki notandans, og deiling
sendir tengil á ruv.is en ekki hljóðskrána sjálfa.

Viðmótið sem appið notar (`spilari.nyr.ruv.is/gql/`) er **óskjalfest og getur
breyst án fyrirvara**. Hætti appið að finna bænina er það fyrsta sem á að
athuga — sjá lið 1 í LESTU_MIG.md.

Allt efni er í höfundarrétti RÚV. Þetta verkefni er ótengt RÚV.

---

## Leyfi

GPL-3.0 — sjá [LICENSE](LICENSE). Höfundarréttur © 2026 AEH.

Varahljóð: eldri klukka Staðarfellskirkju, upptaka af
[kirkjuklukkur.is](https://www.kirkjuklukkur.is/vesturlandsprofastsdaemi/stadarfellskirkja/).
