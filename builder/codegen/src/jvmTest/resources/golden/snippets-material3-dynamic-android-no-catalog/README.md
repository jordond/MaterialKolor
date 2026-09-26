# AppTheme

Made with MaterialKolor Builder 2.0.0.
The theme builds its colors from the seed at runtime with MaterialKolor 6.0.0, the version the dependency lines below pin.

## Add the theme

The `src` folder is laid out like a module, so copy it into the module that holds your theme. That puts these files in `src/main/kotlin/com/example/theme`, in the package `com.example.theme`.

- `Color.kt`
- `Theme.kt`

## Add the dependencies

Add these lines to the build file of the same module. They are also in `snippets/build.gradle.kts`.

```kotlin
dependencies {
    implementation("com.materialkolor:material-kolor-material3:6.0.0")
    implementation("androidx.compose.material3:material3:1.5.0-alpha28")
}
```

## Open it again

To change the theme later, open it in the builder at <https://materialkolor.com/t/AdllOwAAAAAT>
