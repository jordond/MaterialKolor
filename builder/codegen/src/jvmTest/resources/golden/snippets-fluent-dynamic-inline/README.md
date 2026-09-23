# AppTheme

Made with MaterialKolor Builder 2.0.0.
The theme builds its colors from the seed at runtime with MaterialKolor 6.0.0, the version the dependency lines below pin.

## Add the theme

The `src` folder is laid out like a module, so copy it into the module that holds your theme. That puts these files in `src/commonMain/kotlin/com/example/theme`, in the package `com.example.theme`.

- `Color.kt`
- `Theme.kt`

## Add the dependencies

Merge `gradle/libs.versions.toml` into the version catalog of your project, then add these lines to the build file of the same module. They are also in `snippets/build.gradle.kts`.

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.materialKolor.core)
            implementation(libs.fluent)
        }
    }
}
```

Compose Fluent needs JVM 17 and has no macOS native target.

## Open it again

To change the theme later, open it in the builder at <https://materialkolor.com/t/AdllOwACAADF>
