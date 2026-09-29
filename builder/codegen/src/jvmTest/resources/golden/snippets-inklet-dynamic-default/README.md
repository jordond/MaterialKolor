# AppTheme

Made with MaterialKolor Builder 2.0.0: https://materialkolor.com/t/AdllOwAIAABC
The theme builds its colors from the seed at runtime with MaterialKolor 6.0.0

## Add the theme

The `src` folder is laid out like a module, so copy it into the module that holds your theme. That puts these files in `src/commonMain/kotlin/com/example/theme`, in the package `com.example.theme`.

- `Color.kt`
- `Theme.kt`

The theme gives the Inklet components their Material 3 colors and already wraps its content in `InkletTheme`, so wrap nothing else around them.

## Add the dependencies

Merge `gradle/libs.versions.toml` into the version catalog of your project, then add these lines to the build file of the same module. They are also in `snippets/build.gradle.kts`.

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.materialKolor.material3)
            implementation(libs.compose.material3)
            implementation(libs.inklet)
        }
    }
}
```

Inklet needs Android minSdk 23, and has no JS, macOS or iosX64 target.
