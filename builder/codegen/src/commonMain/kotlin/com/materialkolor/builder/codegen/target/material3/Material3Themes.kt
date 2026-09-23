package com.materialkolor.builder.codegen.target.material3

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.BodyScope
import com.materialkolor.builder.codegen.dsl.Expression
import com.materialkolor.builder.codegen.dsl.call
import com.materialkolor.builder.codegen.dsl.ifElse
import com.materialkolor.builder.codegen.dsl.infix
import com.materialkolor.builder.codegen.dsl.member
import com.materialkolor.builder.codegen.dsl.ref
import com.materialkolor.builder.codegen.symbol.Symbols
import com.materialkolor.builder.codegen.target.CONTENT_PARAMETER
import com.materialkolor.builder.codegen.target.DYNAMIC_COLOR_PARAMETER
import com.materialkolor.builder.codegen.target.IS_DARK_PARAMETER
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.persist.ExportTarget

// The theme calls the two Material 3 exports share. The frozen export calls `MaterialTheme` on its
// literal colors, and either export calls it on the wallpaper colors when an Android export asks
// for them.

private const val CONTEXT = "context"

/**
 * Whether the theme function offers the wallpaper colors. Only an Android project can reach them,
 * so a multiplatform export never does, whatever the option says.
 */
internal val ExportInput.writesAndroidDynamicColor: Boolean
    get() = !prefs.multiplatform && prefs.androidDynamicColor

/** `val context = LocalContext.current`, which the wallpaper schemes are read from. */
internal fun BodyScope.assignContext() {
    assign(CONTEXT, ref(Symbols.LocalContext).member("current"))
}

/**
 * `if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)` the theme on the wallpaper
 * colors, else [otherwise].
 *
 * The wallpaper schemes only exist from Android 12, and where they do they hide the exported colors,
 * which is why the option is off unless someone turns it on.
 */
internal fun androidDynamicColorBranch(
    input: ExportInput,
    otherwise: Expression,
): Expression {
    val build = ref(Symbols.Build)
    val sdkAtLeastS = infix(build.member("VERSION").member("SDK_INT"), ">=", build.member("VERSION_CODES").member("S"))
    val context = ref(CONTEXT)
    val wallpaper = ifElse(
        condition = ref(IS_DARK_PARAMETER),
        whenTrue = call(Symbols.DynamicDarkColorScheme) { argument(context) },
        whenFalse = call(Symbols.DynamicLightColorScheme) { argument(context) },
    )

    return ifElse(
        condition = infix(ref(DYNAMIC_COLOR_PARAMETER), "&&", sdkAtLeastS),
        whenTrue = materialThemeCall(input, colorScheme = wallpaper),
        whenFalse = otherwise,
    )
}

/**
 * `MaterialTheme(...)`, or `MaterialExpressiveTheme(...)` with the document's motion scheme, on
 * [colorScheme]. The frozen theme and the wallpaper branch of either mode call it.
 */
internal fun materialThemeCall(
    input: ExportInput,
    colorScheme: Expression,
): Expression {
    val content = ref(CONTENT_PARAMETER)

    return if (input.target == ExportTarget.Material3Expressive) {
        call(Symbols.MaterialExpressiveTheme, multiline = true) {
            argument("colorScheme", colorScheme)
            argument("motionScheme", motionSchemeExpression(input.document.motionScheme))
            argument(CONTENT_PARAMETER, content)
        }
    } else {
        call(Symbols.MaterialTheme, multiline = true) {
            argument("colorScheme", colorScheme)
            argument(CONTENT_PARAMETER, content)
        }
    }
}

/** `MotionScheme.standard()` or `MotionScheme.expressive()`. */
internal fun motionSchemeExpression(choice: MotionSchemeChoice): Expression {
    val motionScheme = ref(Symbols.MotionScheme)

    return when (choice) {
        MotionSchemeChoice.Standard -> motionScheme.call("standard")
        MotionSchemeChoice.Expressive -> motionScheme.call("expressive")
    }
}
