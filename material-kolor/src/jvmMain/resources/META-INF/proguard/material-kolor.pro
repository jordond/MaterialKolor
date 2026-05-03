# Keep all public top-level Kotlin functions (generated *Kt classes) so that
# R8/ProGuard does not strip them in release builds of consumer apps.
# See: https://github.com/jordond/MaterialKolor/issues/493
#
# Auto-discovered by R8/ProGuard from META-INF/proguard/ in jvm jar deps.
# Mirror of material-kolor/consumer-rules.pro (Android consumer rules).
-keep public class com.materialkolor.*Kt { public *; }
-keep public class com.materialkolor.ktx.*Kt { public *; }
