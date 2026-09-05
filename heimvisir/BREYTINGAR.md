# Breytingar

## v0.1 — fyrsta útgáfa

Fyrsta smíð. Appið flettir upp örmerkjum og eyrnamerkjum í skrá
Dýraauðkennis, beint úr símanum.

**Virkni**

- Leit að merki með sjálfvirkri hreinsun á bilum og bandstrikum.
- Niðurstaða með dýrinu og **öllum** skráðum eigendum.
- Áberandi merking og dagsetning þegar dýr er skráð týnt.
- Símanúmer og netföng opnast í símtals- og póstforriti.
- Leitarsaga í tækinu, 20 færslur, með hreinsun.
- Íslenskt viðmót í gegn, Material 3 með Material You frá Android 12.

**Persónuvernd**

- Sagan geymir merki, nafn dýrs, tegund og tímastimpil. Ekkert um fólk.
- Skýjaafrit og flutningur milli tækja slökkt.
- Netlagið skráir svarkóða og ekkert annað.
- Tvær heimildir: `INTERNET` og `ACCESS_NETWORK_STATE`.

**Undirstaða**

Þetta app er endurskrifað frá grunni upp úr vefútgáfu sem Grok Build
smíðaði. Sjö gallar úr þeirri útgáfu voru útilokaðir viljandi, ekki
fluttir með:

1. Svarið frá skránni var lesið með óstaðfestu `as`-casti. Hér þáttar
   `kotlinx.serialization` það gegn ströngum týpum og ólæsilegt svar
   verður `MalformedResponse` í stað auðs skjás.
2. Engin tímamörk voru á ytri beiðninni. Hér: 10 s tenging, 15 s lestur,
   20 s heildarþak.
3. Vistun leitarsögunnar gat fellt uppflettingu sem hafði tekist.
   Vistun er nú í `runCatching`, utan við niðurstöðuflæðið.
4. Netvillur voru skráðar í söguna sem „fannst ekki". Nú rata aðeins
   raunveruleg svör þangað.
5. Aðeins fyrsti eigandi var birtur. Nú allir.
6. Ógilt innslegið merki skilaði auðum skjá án skýringar. Nú fær það
   sýnilega ástæðu.
7. Reiturinn „Geldur" birtist alltaf, líka þegar gildið vantaði. Nú
   felur hann sig eins og allir aðrir reitir.

**Óklárað**

- Táknmyndin er bráðabirgðamynd, teiknuð í vektor. Á að víkja fyrir
  almennilegri mynd.
- Ekkert `release.yml`. Undirritun þarf sinn eigin lykil og sína eigin
  ferð — að blanda undirritunarlyklum milli forrita brýtur
  uppfærsluleiðina fyrir notendur.
