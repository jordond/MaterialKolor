# AppTheme

Made with MaterialKolor Builder 2.0.0.
Every color is written out as it is, so the files need no MaterialKolor. The colors come from MaterialKolor 6.0.0.

## Add the theme

The `src` folder is laid out like a module, so copy it into the module that holds your theme. That puts these files in `src/main/kotlin/com/example/theme`, in the package `com.example.theme`.

- `Theme.kt`

## Add the dependencies

Merge `gradle/libs.versions.toml` into the version catalog of your project, then add these lines to the build file of the same module. They are also in `snippets/build.gradle.kts`.

```kotlin
dependencies {
    implementation(libs.composeFluent)
}
```

Compose Fluent needs JVM 17 and has no macOS native target.

## Open it again

To change the theme later, open it in the builder at <https://materialkolor.com/t/AdllOwACAADF>
