package com.materialkolor.builder.domain.persist

import com.materialkolor.builder.domain.DocumentArb
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.history.History
import com.materialkolor.builder.domain.history.HistoryEntry
import com.materialkolor.builder.domain.model.Library
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Records with every field moved off its default, so a round trip notices a field the format drops.
 */
class PersistFixtures(
    private val arb: DocumentArb = DocumentArb(),
) {
    fun project(): ProjectRecord =
        ProjectRecord(
            id = "p7",
            name = "Cactus",
            document = arb.nextDocument(),
            revision = 4_294_967_296L,
            writerTab = "tab-2",
        )

    fun meta(index: Int): ProjectMeta =
        ProjectMeta(
            id = "p$index",
            name = "Project $index",
            createdAt = 1_758_000_000_000L + index,
            updatedAt = 1_758_500_000_000L + index,
            library = Library.entries[index % Library.entries.size],
            expressive = index % 2 == 0,
            previewColors = List(ProjectMeta.PREVIEW_COLORS) { arb.nextArgb() },
        )

    fun index(): ProjectIndex = ProjectIndex(projects = List(6) { index -> meta(index) })

    fun history(size: Int = History.PERSISTED): HistoryRecord =
        HistoryRecord(entries = List(size) { index -> entry(index) })

    fun entry(index: Int): HistoryEntry {
        val kind = ChangeKind.entries[index % ChangeKind.entries.size]
        val label = if (index % 2 == 0) ChangeLabel(kind) else ChangeLabel(kind, detail = kind.name)
        val at = 1_758_000_000_000L + index
        return HistoryEntry(before = arb.nextDocument(), after = arb.nextDocument(), label = label, at = at)
    }

    fun viewState(): ProjectViewState =
        ProjectViewState(
            tab = PreviewTab.Palettes,
            mode = PreviewMode.Dark,
            splitFraction = 0.25f,
            deviceWidth = DeviceWidth.Desktop,
            openFineTuneRows = FineTuneRow.entries.toSet(),
        )

    fun preferences(): Preferences =
        Preferences(
            appearance = Appearance.Dark,
            motion = MotionOverride.Reduce,
            hueLock = true,
            styleLock = false,
            seedLock = true,
            dismissedHints = setOf("shuffle", "export"),
            firstExportDone = true,
            posterCollapsed = true,
            lastProjectId = "p7",
            persistRequested = true,
            exportPrefs = ExportTarget.entries.associateWith { target -> exportPrefs(target) },
        )

    fun exportPrefs(target: ExportTarget): ExportPrefs =
        ExportPrefs(
            packageName = "com.cactus.${target.name.lowercase()}",
            multiplatform = false,
            versionCatalog = false,
            mode = ExportMode.Frozen,
            animate = true,
            animationDurationMs = 150 + target.ordinal,
            frozenVariants = FrozenVariants.AllContrasts,
            androidDynamicColor = true,
        )
}

/**
 * Asserts [outcome] read cleanly and hands back what it read.
 */
fun <T> assertOk(outcome: DecodeOutcome<T>): T = assertIs<DecodeOutcome.Ok<T>>(outcome).value

/**
 * Asserts [outcome] was quarantined for [reason].
 */
fun assertQuarantined(
    reason: QuarantineReason,
    outcome: DecodeOutcome<*>,
) {
    assertEquals(DecodeOutcome.Quarantine(reason), outcome)
}

/**
 * A record that only exists to walk the migration path, since every real record is still in its
 * first shape.
 *
 * Schema 0 kept a single color under `color`. Schema 1 keeps a list under `colors`, and
 * [OneColorToMany] is the step between them.
 */
@Serializable
data class Swatch(
    @SerialName("name")
    val name: String,
    @SerialName("colors")
    val colors: List<String> = emptyList(),
)

/**
 * Schema 0 to 1 of [Swatch]. A schema 0 color that is not a string cannot be carried over.
 */
internal val OneColorToMany: MigrationStep =
    MigrationStep { data ->
        val color = data["color"] ?: return@MigrationStep data
        require(color is JsonPrimitive && color.isString) { "A schema 0 color is a string, got $color" }
        JsonObject(data - "color" + ("colors" to JsonArray(listOf(color))))
    }

/**
 * Reads and writes a [Swatch] at schema 1, declared after its step so the step is set up in time.
 */
internal val SwatchCodec: RecordCodec<Swatch> = RecordCodec(Swatch.serializer(), Migrations(listOf(OneColorToMany)))
