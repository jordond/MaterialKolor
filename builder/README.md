# MaterialKolor Builder

The Compose Multiplatform app behind [materialkolor.com](https://materialkolor.com) — an
interactive playground for generating and exporting Material 3 color schemes with
[MaterialKolor](https://github.com/jordond/MaterialKolor).

It lives in this repo as two Gradle modules:

- `:builder:shared` — the Compose Multiplatform app (Android, iOS, JVM/desktop, Web/wasmJs + JS).
- `:builder:android` — the thin Android application wrapper around `:builder:shared`.

## Running

```bash
# Web (wasmJs) in the browser
./gradlew :builder:shared:wasmJsBrowserRun

# Desktop (JVM)
./gradlew :builder:shared:run

# Android (installs the debug app on a connected device/emulator)
./gradlew :builder:android:installDebug
```

iOS runs from the Xcode project under `ios/`.

## MaterialKolor source: local vs published

By default the builder depends on the **local** `:material-kolor` and
`:material-color-utilities` modules in this repo, so changes to the library are picked up
immediately. To build against the **published** `com.materialkolor` artifacts instead, set
the `materialkolor.useLocal` Gradle property to `false`:

```bash
./gradlew :builder:shared:compileKotlinJvm -Pmaterialkolor.useLocal=false
```

## Building release artifacts

The `scripts/build` helper produces versioned artifacts under `dist/`:

```bash
./scripts/build web 1.0.4
./scripts/build android 1.0.4
./scripts/build desktop 1.0.4
```
