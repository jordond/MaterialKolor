package com.materialkolor.builder.codegen.symbol

/**
 * Every symbol a generated file refers to, in one place.
 *
 * The export targets name what they call through this table and never spell a package out
 * themselves, so a rename in the library shows up here and in `LibrarySymbolsTest` rather than
 * in a file someone downloaded. The MaterialKolor entries are held to the library's ABI dumps by
 * that test.
 */
public object Symbols {
    // Kotlin. These are in scope without an import and only ever appear as types.

    public val Boolean: Symbol = Symbol("kotlin", "Boolean", SymbolKind.Class)
    public val Double: Symbol = Symbol("kotlin", "Double", SymbolKind.Class)
    public val Int: Symbol = Symbol("kotlin", "Int", SymbolKind.Class)
    public val String: Symbol = Symbol("kotlin", "String", SymbolKind.Class)
    public val Unit: Symbol = Symbol("kotlin", "Unit", SymbolKind.Class)
    public val OptIn: Symbol = Symbol("kotlin", "OptIn", SymbolKind.Annotation)

    public val Map: Symbol = Symbol("kotlin.collections", "Map", SymbolKind.Class)
    public val MapOf: Symbol = Symbol("kotlin.collections", "mapOf", SymbolKind.Function)

    // Compose runtime, foundation, graphics and animation.

    public val Composable: Symbol = Symbol(COMPOSE_RUNTIME, "Composable", SymbolKind.Annotation)
    public val Immutable: Symbol = Symbol(COMPOSE_RUNTIME, "Immutable", SymbolKind.Annotation)
    public val Remember: Symbol = Symbol(COMPOSE_RUNTIME, "remember", SymbolKind.Function)
    public val StaticCompositionLocalOf: Symbol =
        Symbol(COMPOSE_RUNTIME, "staticCompositionLocalOf", SymbolKind.Function)
    public val CompositionLocalProvider: Symbol =
        Symbol(COMPOSE_RUNTIME, "CompositionLocalProvider", SymbolKind.Function)
    public val Color: Symbol = Symbol("androidx.compose.ui.graphics", "Color", SymbolKind.Class)
    public val IsSystemInDarkTheme: Symbol =
        Symbol("androidx.compose.foundation", "isSystemInDarkTheme", SymbolKind.Function)
    public val Tween: Symbol = Symbol("androidx.compose.animation.core", "tween", SymbolKind.Function)

    // Compose Material 3.

    public val MaterialTheme: Symbol = Symbol(MATERIAL3, "MaterialTheme", SymbolKind.Function)
    public val MaterialExpressiveTheme: Symbol = Symbol(MATERIAL3, "MaterialExpressiveTheme", SymbolKind.Function)
    public val ColorScheme: Symbol = Symbol(MATERIAL3, "ColorScheme", SymbolKind.Class)
    public val LightColorScheme: Symbol = Symbol(MATERIAL3, "lightColorScheme", SymbolKind.Function)
    public val DarkColorScheme: Symbol = Symbol(MATERIAL3, "darkColorScheme", SymbolKind.Function)
    public val MotionScheme: Symbol = Symbol(MATERIAL3, "MotionScheme", SymbolKind.Class)
    public val ExperimentalMaterial3ExpressiveApi: Symbol =
        Symbol(MATERIAL3, "ExperimentalMaterial3ExpressiveApi", SymbolKind.Annotation)

    // Android only, for the wallpaper colors an Android export can switch to.

    public val Build: Symbol = Symbol("android.os", "Build", SymbolKind.Class)
    public val LocalContext: Symbol = Symbol("androidx.compose.ui.platform", "LocalContext", SymbolKind.Property)
    public val DynamicDarkColorScheme: Symbol = Symbol(MATERIAL3, "dynamicDarkColorScheme", SymbolKind.Function)
    public val DynamicLightColorScheme: Symbol = Symbol(MATERIAL3, "dynamicLightColorScheme", SymbolKind.Function)

    // MaterialKolor core and the color utilities it ships with.

    public val PaletteStyle: Symbol = Symbol(KOLOR, "PaletteStyle", SymbolKind.Class)
    public val ColorSpec: Symbol = Symbol(KOLOR_DYNAMIC_COLOR, "ColorSpec", SymbolKind.Class)
    public val DynamicScheme: Symbol = Symbol(KOLOR_DYNAMIC_COLOR, "DynamicScheme", SymbolKind.Class)
    public val TonalPalette: Symbol = Symbol("com.materialkolor.palettes", "TonalPalette", SymbolKind.Class)
    public val Harmonize: Symbol = Symbol(KOLOR_KTX, "harmonize", SymbolKind.Function)
    public val RememberTonalPalette: Symbol = Symbol(KOLOR_KTX, "rememberTonalPalette", SymbolKind.Function)
    public val OnTone: Symbol = Symbol(KOLOR_KTX, "onTone", SymbolKind.Function)
    public val ToneColor: Symbol = Symbol(KOLOR_KTX, "toneColor", SymbolKind.Function)
    public val ContrastThreshold: Symbol = Symbol(KOLOR_KTX, "ContrastThreshold", SymbolKind.Class)
    public val RememberDynamicScheme: Symbol = Symbol(KOLOR_KTX, "rememberDynamicScheme", SymbolKind.Function)

    public val MaterialKolors: Symbol = Symbol(KOLOR, "MaterialKolors", SymbolKind.Class)

    // MaterialKolor Material 3.

    public val DynamicMaterialTheme: Symbol = Symbol(KOLOR_MATERIAL3, "DynamicMaterialTheme", SymbolKind.Function)
    public val DynamicMaterialExpressiveTheme: Symbol =
        Symbol(KOLOR_MATERIAL3, "DynamicMaterialExpressiveTheme", SymbolKind.Function)
    public val DynamicMaterialThemeState: Symbol =
        Symbol(KOLOR_MATERIAL3, "DynamicMaterialThemeState", SymbolKind.Class)
    public val RememberDynamicMaterialThemeState: Symbol =
        Symbol(KOLOR_MATERIAL3, "rememberDynamicMaterialThemeState", SymbolKind.Function)

    // MaterialKolor Unstyled, and the Compose Unstyled theming it plugs into.

    public val DynamicColorSchemes: Symbol = Symbol(KOLOR_UNSTYLED, "dynamicColorSchemes", SymbolKind.Function)
    public val DynamicColors: Symbol = Symbol(KOLOR_UNSTYLED, "dynamicColors", SymbolKind.Function)
    public val ThemeValues: Symbol = Symbol(KOLOR_UNSTYLED, "themeValues", SymbolKind.Function)
    public val ToThemeValues: Symbol = Symbol(KOLOR_UNSTYLED, "toThemeValues", SymbolKind.Function)
    public val MaterialKolorTokens: Symbol = Symbol(KOLOR_UNSTYLED, "MaterialKolorTokens", SymbolKind.Class)
    public val BuildThemeV2: Symbol = Symbol(UNSTYLED_THEME, "buildThemeV2", SymbolKind.Function)
    public val ThemeToken: Symbol = Symbol(UNSTYLED_THEME, "ThemeToken", SymbolKind.Class)

    public val ThemeProperty: Symbol = Symbol(UNSTYLED_THEME, "ThemeProperty", SymbolKind.Class)
    public val UnstyledColorScheme: Symbol = Symbol(UNSTYLED_THEME, "ColorScheme", SymbolKind.Class)

    // MaterialKolor Fluent, and Compose Fluent itself.

    public val RememberFluentColors: Symbol = Symbol(KOLOR_FLUENT, "rememberFluentColors", SymbolKind.Function)
    public val AnimateFluentColors: Symbol = Symbol(KOLOR_FLUENT, "animateFluentColors", SymbolKind.Function)
    public val FluentTheme: Symbol = Symbol(FLUENT, "FluentTheme", SymbolKind.Function)
    public val FluentColors: Symbol = Symbol(FLUENT, "Colors", SymbolKind.Class)
    public val FluentShades: Symbol = Symbol(FLUENT, "Shades", SymbolKind.Class)
}

private const val COMPOSE_RUNTIME = "androidx.compose.runtime"
private const val MATERIAL3 = "androidx.compose.material3"
private const val KOLOR = "com.materialkolor"
private const val KOLOR_DYNAMIC_COLOR = "com.materialkolor.dynamiccolor"
private const val KOLOR_KTX = "com.materialkolor.ktx"
private const val KOLOR_MATERIAL3 = "com.materialkolor.material3"
private const val KOLOR_UNSTYLED = "com.materialkolor.unstyled"
private const val KOLOR_FLUENT = "com.materialkolor.fluent"
private const val UNSTYLED_THEME = "com.composeunstyled.theme"
private const val FLUENT = "io.github.composefluent"
