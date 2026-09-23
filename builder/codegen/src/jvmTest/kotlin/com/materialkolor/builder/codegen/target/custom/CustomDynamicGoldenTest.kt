package com.materialkolor.builder.codegen.target.custom

import com.materialkolor.builder.codegen.GoldenHarness
import com.materialkolor.builder.codegen.symbol.DefaultArguments
import com.materialkolor.builder.codegen.target.material3.propertyName
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.SlotResolution
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomDynamicGoldenTest {
    private val materialKolors: String = File(GoldenHarness.repoRoot(), MATERIAL_KOLORS_SOURCE).readText()

    @Test
    fun customDynamic_everyCase_matchesTheCheckedInFiles() {
        CustomDynamicCases.all.keys.forEach { case ->
            GoldenHarness.Default.verify(case, CustomDynamicCases.files(case))
        }
    }

    @Test
    fun customDynamic_everyRoleASlotReads_isAMaterialKolorsFunction() {
        val missing = CustomSlot.entries
            .mapNotNull { slot -> (slot.resolution as? SlotResolution.FromRole)?.role?.propertyName }
            .filterNot { name -> "public fun $name(): Color" in materialKolors }

        assertEquals(emptyList(), missing, "MaterialKolors has no function for these roles")
    }

    @Test
    fun customDynamic_isAmoledDefault_matchesTheMaterialKolorsConstructor() {
        val default = DefaultArguments.MaterialKolorsIsAmoled
        val parameter = "private val ${default.parameter}: Boolean = ${default.source},"

        assertTrue(parameter in materialKolors, "MaterialKolors no longer declares $parameter")
    }
}

private const val MATERIAL_KOLORS_SOURCE = "material-kolor-core/src/commonMain/kotlin/com/materialkolor/" +
    "MaterialKolors.kt"
