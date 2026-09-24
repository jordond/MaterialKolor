package com.materialkolor.builder.domain.persist

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals

class WireNamesTest {
    @Test
    fun records_keys_stayWhereStoredRecordsExpectThem() {
        val wireNames = listOf(
            ProjectRecord.serializer().descriptor to listOf("id", "name", "document", "revision", "writerTab"),
            ProjectIndex.serializer().descriptor to listOf("projects"),
            ProjectMeta.serializer().descriptor to
                listOf("id", "name", "createdAt", "updatedAt", "library", "expressive", "previewColors"),
            HistoryRecord.serializer().descriptor to listOf("entries"),
            ProjectViewState.serializer().descriptor to
                listOf("tab", "mode", "splitFraction", "deviceWidth", "openFineTuneRows"),
            Preferences.serializer().descriptor to listOf(
                "appearance",
                "motion",
                "hueLock",
                "styleLock",
                "seedLock",
                "dismissedHints",
                "firstExportDone",
                "posterCollapsed",
                "lastProjectId",
                "persistRequested",
                "exportPrefs",
                "singleKeyShortcuts", // b-315
            ),
            ExportPrefs.serializer().descriptor to listOf(
                "packageName",
                "multiplatform",
                "versionCatalog",
                "mode",
                "animate",
                "animationDurationMs",
                "frozenVariants",
                "androidDynamicColor",
            ),
            Envelope.serializer().descriptor to listOf("schema", "data"),
        )

        wireNames.forEach { (descriptor, names) -> assertEquals(names, elementNames(descriptor)) }
    }

    @Test
    fun enums_entries_stayWhereStoredRecordsExpectThem() {
        val wireNames = listOf(
            PreviewTab.serializer().descriptor to listOf("App", "Components", "Roles", "Palettes", "Contrast"),
            PreviewMode.serializer().descriptor to listOf("Light", "Split", "Dark"),
            DeviceWidth.serializer().descriptor to listOf("Phone", "Tablet", "Desktop"),
            FineTuneRow.serializer().descriptor to listOf("CoreColors", "SpecExtras"),
            Appearance.serializer().descriptor to listOf("System", "Light", "Dark"),
            MotionOverride.serializer().descriptor to listOf("System", "Reduce", "Full"),
            ExportTarget.serializer().descriptor to
                listOf("Material3", "Material3Expressive", "Unstyled", "Fluent", "Custom"),
            ExportMode.serializer().descriptor to listOf("Dynamic", "Frozen"),
            FrozenVariants.serializer().descriptor to listOf("StandardOnly", "AllContrasts"),
        )

        wireNames.forEach { (descriptor, names) -> assertEquals(names, elementNames(descriptor)) }
    }

    private fun elementNames(descriptor: SerialDescriptor): List<String> =
        List(descriptor.elementsCount) { index -> descriptor.getElementName(index) }
}
