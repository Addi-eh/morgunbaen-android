# R8 er slökkt í release-byggingunni sem stendur (sjá app/build.gradle.kts),
# svo þessar reglur eru ekki í notkun enn. Þær eru hér svo kveiking á R8
# verði ein lína frekar en rannsóknarleiðangur.

# kotlinx.serialization býr til þáttara með endurskini. Án þessa hverfa
# þeir í styttingu og hvert einasta svar frá skránni verður MalformedResponse.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class aeh.heimvisir.model.** {
    *** Companion;
}
-keepclasseswithmembers class aeh.heimvisir.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class aeh.heimvisir.data.HistoryItem {
    *** Companion;
}
-keepclasseswithmembers class aeh.heimvisir.data.HistoryItem {
    kotlinx.serialization.KSerializer serializer(...);
}
