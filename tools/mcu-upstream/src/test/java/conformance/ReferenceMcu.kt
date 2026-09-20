package conformance

import java.lang.reflect.InvocationTargetException

/**
 * Accesses each implementation through its own classes. No adaptation logic is shared.
 */
internal class ReferenceMcu(
    private val namespace: String,
) {
    // The empty namespace is the original Java project, whose call shapes differ in several places.
    private val isJava = namespace.isEmpty()

    private fun type(name: String): Class<*> = Class.forName("$namespace$name")

    private val schemeType = type("dynamiccolor.DynamicScheme")
    private val specType = type("dynamiccolor.ColorSpec\$SpecVersion")
    private val platformType = type("dynamiccolor.DynamicScheme\$Platform")
    private val colors = type("dynamiccolor.MaterialDynamicColors").getConstructor().newInstance()
    private val getArgb = type("dynamiccolor.DynamicColor").getMethod("getArgb", schemeType)
    private val getters = ROLE_NAMES.associateWith { role ->
        colors.javaClass.getMethod(if (isJava) role else "get${role.replaceFirstChar { it.uppercase() }}")
    }

    fun hct(argb: Int): Any = factory("hct.Hct", "fromInt", Int::class.javaPrimitiveType!!, argb)

    fun scheme(case: SchemeCase): Any {
        val constructor = type("scheme.Scheme${case.variant}").getConstructor(
            List::class.java,
            Boolean::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
            specType,
            platformType,
        )
        return unwrap {
            constructor.newInstance(
                case.seeds.map(::hct),
                case.dark,
                case.contrast,
                specType.enumConstants.single { it.toString() == case.spec },
                platformType.enumConstants.single { it.toString() == case.platform },
            )
        }
    }

    fun customScheme(case: SchemeCase): Any {
        val variantType = type("dynamiccolor.Variant")
        val paletteType = type("palettes.TonalPalette")
        val constructor = schemeType.getConstructor(
            List::class.java,
            variantType,
            Boolean::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
            platformType,
            specType,
            paletteType,
            paletteType,
            paletteType,
            paletteType,
            paletteType,
            if (isJava) java.util.Optional::class.java else paletteType,
        )
        val palettes = listOf(0xfff08080, 0xff008080, 0xff663399, 0xff708090, 0xff778899, 0xffff8c00).map {
            factory("palettes.TonalPalette", "fromInt", Int::class.javaPrimitiveType!!, it.toInt())
        }
        return unwrap {
            constructor.newInstance(
                case.seeds.map(::hct),
                variantType.enumConstants.single { it.toString() == "TONAL_SPOT" },
                case.dark,
                case.contrast,
                platformType.enumConstants.single { it.toString() == case.platform },
                specType.enumConstants.single { it.toString() == case.spec },
                palettes[0],
                palettes[1],
                palettes[2],
                palettes[3],
                palettes[4],
                if (isJava) java.util.Optional.of(palettes[5]) else palettes[5],
            )
        }
    }

    fun roles(scheme: Any): Map<String, Int?> =
        getters.mapValues { (_, getter) ->
            val color = getter.invoke(colors)
            color?.let { getArgb.invoke(it, scheme) as Int }
        }

    fun effectiveSpec(scheme: Any): String =
        if (isJava) {
            schemeType.getField("specVersion").get(scheme).toString()
        } else {
            schemeType.getMethod("getSpecVersion").invoke(scheme).toString()
        }

    private fun factory(
        name: String,
        method: String,
        argumentType: Class<*>,
        argument: Any,
    ): Any {
        val clazz = type(name)
        if (isJava) return clazz.getMethod(method, argumentType).invoke(null, argument)
        val companion = clazz.getField("Companion").get(null)
        return companion.javaClass.getMethod(method, argumentType).invoke(companion, argument)
    }
}

internal fun <T> unwrap(action: () -> T): T =
    try {
        action()
    } catch (exception: InvocationTargetException) {
        throw exception.targetException
    }

internal data class SchemeCase(
    val variant: String,
    val spec: String,
    val dark: Boolean,
    val contrast: Double,
    val platform: String,
    val seeds: List<Int>,
) {
    override fun toString(): String =
        "$variant/$spec/$platform/dark=$dark/contrast=$contrast/seeds=${seeds.map { it.toUInt().toString(16) }}"
}

internal val VARIANT_NAMES = listOf(
    "TonalSpot",
    "Neutral",
    "Vibrant",
    "Expressive",
    "Fidelity",
    "Content",
    "Rainbow",
    "FruitSalad",
    "Monochrome",
    "Cmf",
)

// Duplicated, deliberately, by roleArgb() in
// material-color-utilities/src/commonTest/kotlin/com/materialkolor/conformance/UpstreamRoleGoldenTest.kt.
// The two modules cannot share a source set, and the second copy is what catches fixture inventory
// drift, so keep both lists identical, same names, same order, same count.
internal val ROLE_NAMES = listOf(
    "primaryPaletteKeyColor",
    "secondaryPaletteKeyColor",
    "tertiaryPaletteKeyColor",
    "neutralPaletteKeyColor",
    "neutralVariantPaletteKeyColor",
    "errorPaletteKeyColor",
    "background",
    "onBackground",
    "surface",
    "surfaceDim",
    "surfaceBright",
    "surfaceContainerLowest",
    "surfaceContainerLow",
    "surfaceContainer",
    "surfaceContainerHigh",
    "surfaceContainerHighest",
    "onSurface",
    "surfaceVariant",
    "onSurfaceVariant",
    "inverseSurface",
    "inverseOnSurface",
    "outline",
    "outlineVariant",
    "shadow",
    "scrim",
    "surfaceTint",
    "primary",
    "primaryDim",
    "onPrimary",
    "primaryContainer",
    "onPrimaryContainer",
    "inversePrimary",
    "primaryFixed",
    "primaryFixedDim",
    "onPrimaryFixed",
    "onPrimaryFixedVariant",
    "secondary",
    "secondaryDim",
    "onSecondary",
    "secondaryContainer",
    "onSecondaryContainer",
    "secondaryFixed",
    "secondaryFixedDim",
    "onSecondaryFixed",
    "onSecondaryFixedVariant",
    "tertiary",
    "tertiaryDim",
    "onTertiary",
    "tertiaryContainer",
    "onTertiaryContainer",
    "tertiaryFixed",
    "tertiaryFixedDim",
    "onTertiaryFixed",
    "onTertiaryFixedVariant",
    "error",
    "errorDim",
    "onError",
    "errorContainer",
    "onErrorContainer",
)
