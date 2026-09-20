# Heimvísir

Android-forrit sem flettir upp örmerki eða eyrnamerki gæludýrs í skrá
Dýraauðkennis — svo dýr sem finnst rati heim.

Fyrirspurnin fer beint úr símanum í skrána. Enginn netþjónn er á milli,
engin gögn fara annað, og appið geymir engar upplýsingar um eigendur.

---

## Að setja upp

Verkefnið er ekki komið í útgáfu. Byggðu það sjálf/ur:

```bash
./gradlew test            # 27 einingapróf: merkjahreinsun, þáttun, netlag
./gradlew assembleDebug   # APK í app/build/outputs/apk/debug/
```

Kotlin og Jetpack Compose. Opnaðu möppuna í Android Studio, eða byggðu
frá skipanalínu með Java 21 og `ANDROID_HOME` stillt.

| | |
|---|---|
| Lágmark | Android 8.0 (API 26) |
| Byggt gegn | API 37 |
| Auðkenni | `is.aeh.heimvisir` |

---

## Hvað appið gerir

- **Leit** — sláðu inn merki og fáðu dýrið og skráða eigendur. Bil og
  bandstrik eru hreinsuð sjálfkrafa, því merki eru oft skrifuð með þeim.
- **Saga** — nýlegar leitir, geymdar í tækinu. Ýttu á færslu til að
  fletta upp aftur.
- **Um** — skýringar og fyrirvarar.

Dýr sem er skráð týnt fær áberandi merkingu og dagsetningu hvarfsins.

---

## Persónuvernd

Þetta er ekki formsatriði heldur ástæðan fyrir nokkrum ákvörðunum í
kóðanum:

- **Ekkert um eigendur er geymt.** Leitarsagan geymir merkið, nafn dýrs,
  tegund og tímastimpil — ekkert um fólk. Sjá `HistoryItem` í
  [`HistoryStore.kt`](app/src/main/java/aeh/heimvisir/data/HistoryStore.kt).
- **Engin skýjaafrit.** `allowBackup="false"` og
  [`data_extraction_rules.xml`](app/src/main/res/xml/data_extraction_rules.xml)
  halda sögunni í tækinu.
- **Ekkert í loggana.** Netlagið skráir svarkóða og ekkert annað — aldrei
  merki, aldrei eigendagögn.
- **Engir milliliðir.** Fyrirspurnin fer beint í `dyraaudkenni.is`. Engin
  greiningartól, engin auglýsinganet, engin þjónusta þriðja aðila.
- **Tvær heimildir.** `INTERNET` og `ACCESS_NETWORK_STATE`. Ekkert annað.

---

## Fyrirvarar

**Appið er ótengt Dýraauðkenni.** Skráning dýra, eigendaskipti og
innskráning fara fram á [dyraaudkenni.is](https://dyraaudkenni.is).
Fyrirspurnir um skrána sjálfa fara á hallo@dyraaudkenni.is.

**Viðmótið sem appið notar (`dyraaudkenni.is/api/MicroTags`) er
óskjalfað og getur breyst án fyrirvara.** Hætti appið að finna dýr er það
fyrsta sem á að athuga. Þess vegna er `MalformedResponse` sérstakt
ástand í [`LookupResult`](app/src/main/java/aeh/heimvisir/model/LookupResult.kt):
notandinn á að fá að vita að viðmótið hafi breyst, ekki bara „villa kom
upp".

**Uppflettingar innihalda persónuupplýsingar annars fólks.** Dýraauðkenni
birtir þær sjálft hverjum þeim sem hefur merkið undir höndum, og appið
sýnir ekkert umfram það. Þær eru til þess að koma dýri heim, ekki til
annars.

---

## Uppbygging

```
app/src/main/java/aeh/heimvisir/
  MainActivity.kt          ein Activity, þrír áfangastaðir
  core/TagNumber.kt        hreinsun og athugun á merki
  model/                   Pet, Owner, LookupResult
  net/DyraAudkenniApi.kt   OkHttp, skýr tímamörk, kastar aldrei
  data/HistoryStore.kt     leitarsagan á DataStore
  ui/                      ViewModel, skjáir, hlutir, þema
```

Netlagið kastar aldrei undantekningu upp í viðmótið. Hvert kall skilar
[`LookupResult`](app/src/main/java/aeh/heimvisir/model/LookupResult.kt),
og fimm ástönd þess — fundið, ekki til, ekkert samband, ólæsilegt svar,
villa frá þjóni — hafa hvert sinn texta á skjánum.

Namespace kóðans (`aeh.heimvisir`) og auðkenni appsins
(`is.aeh.heimvisir`) eru viljandi ekki eins: `is` er lykilorð í Kotlin.
Skýringin er í [`app/build.gradle.kts`](app/build.gradle.kts).

---

## Leyfi

GPL-3.0 — sjá [LICENSE](LICENSE). Höfundarréttur © 2026 AEH.
