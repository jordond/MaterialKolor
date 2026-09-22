# Keep all public top-level Kotlin functions (generated *Kt classes) so that
# R8/ProGuard does not strip them in release builds of consumer apps.
# See: https://github.com/jordond/MaterialKolor/issues/493
-keep public class com.materialkolor.*Kt { public *; }
-keep public class com.materialkolor.ktx.*Kt { public *; }
