import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeCmf
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.fluent.toFluentColors

/**
 * Touches the published public API from outside the project. Nothing calls this. Compiling it is
 * the whole point, so every reference here has to resolve against the artifacts alone.
 */
fun publicApi(): Int {
    val seed = Hct.fromInt(0xff6750a4.toInt()).withTone(50f)
    val single: DynamicScheme = SchemeTonalSpot(seed, false, 0.0)
    val multiple = SchemeCmf(listOf(seed, Hct.fromInt(0xff3498db.toInt())), false, 0.0, SpecVersion.SPEC_2026)
    val fluent = single.toFluentColors()
    return single.primary xor multiple.secondary xor PaletteStyle.TonalSpot.name.length xor
        fluent.shades.base.hashCode()
}
