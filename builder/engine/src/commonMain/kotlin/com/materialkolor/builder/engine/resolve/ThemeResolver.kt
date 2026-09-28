package com.materialkolor.builder.engine.resolve

import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.mapping.toCore
import com.materialkolor.builder.engine.mapping.toPaletteStyle
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.DynamicScheme

/**
 * The only place in the builder that generates a scheme.
 *
 * Two caches sit in front of the library. The last eight resolved documents are kept, so undo and
 * redo come straight back. The last 64 schemes are kept by their [SchemeInputs] and mode, so a
 * library switch, a rename or any other edit that leaves generation alone builds its new result
 * around schemes that already exist. Style chips and project thumbnails go through [scheme] and
 * share that second cache.
 *
 * The same inputs always give back the same scheme instance while it is cached. The resolver, and
 * every [ThemeResult] and [DynamicScheme] it hands out, belong to the thread that created them,
 * which is the UI thread in the app.
 */
public class ThemeResolver {
    private val results = LruCache<ThemeDocument, ThemeResult>(RESULT_CACHE_SIZE)
    private val schemes = LruCache<SchemeKey, DynamicScheme>(SCHEME_CACHE_SIZE)

    /**
     * The resolved theme for [document], built on the first request and cached after that.
     */
    public fun resolve(document: ThemeDocument): ThemeResult = results.getOrPut(document) { build(document) }

    /**
     * The scheme [inputs] generate in the mode [isDark] picks.
     */
    public fun scheme(
        inputs: SchemeInputs,
        isDark: Boolean,
    ): DynamicScheme = schemes.getOrPut(SchemeKey(inputs, isDark)) { generate(inputs, isDark) }

    private fun build(document: ThemeDocument): ThemeResult {
        val inputs = SchemeInputs.from(document)
        val light = scheme(inputs, isDark = false)
        val dark = scheme(inputs, isDark = true)
        val chromeInputs = inputs.forChrome()
        val sameChrome = chromeInputs == inputs
        return ThemeResult(
            document = document,
            light = light,
            dark = dark,
            chromeLight = if (sameChrome) light else scheme(chromeInputs, isDark = false),
            chromeDark = if (sameChrome) dark else scheme(chromeInputs, isDark = true),
        )
    }

    /**
     * Generate through the library's public factory, passing the seed and every key color exactly
     * as the document holds them. A primary key color then pins only the primary palette and the
     * seed keeps driving the others.
     */
    private fun generate(
        inputs: SchemeInputs,
        isDark: Boolean,
    ): DynamicScheme =
        DynamicScheme(
            seedColor = inputs.seed.toColor(),
            isDark = isDark,
            primary = inputs.keyColors.primary?.toColor(),
            secondary = inputs.keyColors.secondary?.toColor(),
            tertiary = inputs.keyColors.tertiary?.toColor(),
            neutral = inputs.keyColors.neutral?.toColor(),
            neutralVariant = inputs.keyColors.neutralVariant?.toColor(),
            error = inputs.keyColors.error?.toColor(),
            style = inputs.style.toPaletteStyle(inputs.cmfTertiarySeed),
            contrastLevel = inputs.contrast.toDouble(),
            specVersion = inputs.spec.toCore(),
            platform = inputs.platform.toCore(),
        )

    /**
     * What the scheme cache is keyed by, the inputs and the mode.
     */
    private data class SchemeKey(
        val inputs: SchemeInputs,
        val isDark: Boolean,
    )

    private companion object {
        /**
         * How many resolved documents are kept.
         */
        const val RESULT_CACHE_SIZE = 8

        /**
         * How many generated schemes are kept, across both modes.
         */
        const val SCHEME_CACHE_SIZE = 64
    }
}
