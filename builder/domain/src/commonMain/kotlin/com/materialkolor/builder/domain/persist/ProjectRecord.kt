package com.materialkolor.builder.domain.persist

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.history.History
import com.materialkolor.builder.domain.history.HistoryEntry
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One saved project, what autosave writes under [StorageKeys.project].
 *
 * @property[id] The project's id, the part of its storage keys that tells it apart.
 * @property[name] What the project is called in the drawer and in the links shared from it.
 * @property[document] The theme.
 * @property[revision] How many times the project has been saved. Each save counts up by one, so a
 * tab can tell another tab wrote since it last read.
 * @property[writerTab] The tab that saved it last.
 */
@Serializable
public data class ProjectRecord(
    @SerialName("id")
    public val id: String,
    @SerialName("name")
    public val name: String,
    @SerialName("document")
    public val document: ThemeDocument,
    @SerialName("revision")
    public val revision: Long,
    @SerialName("writerTab")
    public val writerTab: String,
) {
    public companion object {
        /** Reads and writes a [ProjectRecord]. */
        public val Codec: RecordCodec<ProjectRecord> = RecordCodec(serializer(), Migrations.None)
    }
}

/**
 * Every project the drawer lists, stored under [StorageKeys.INDEX].
 *
 * The index repeats a little of each project so the drawer can draw itself without reading a
 * single project record.
 *
 * @property[projects] One entry per saved project.
 */
@Serializable
public data class ProjectIndex(
    @SerialName("projects")
    public val projects: List<ProjectMeta> = emptyList(),
) {
    public companion object {
        /** Reads and writes a [ProjectIndex]. */
        public val Codec: RecordCodec<ProjectIndex> = RecordCodec(serializer(), Migrations.None)
    }
}

/**
 * What the drawer shows about one project.
 *
 * @property[id] The project it describes.
 * @property[name] The project's name.
 * @property[createdAt] When the project was created, in milliseconds since the epoch.
 * @property[updatedAt] When the project was last saved, in milliseconds since the epoch.
 * @property[library] The library the project exports to, for the target chip.
 * @property[expressive] Whether the project exports the expressive theme, for the target chip.
 * @property[previewColors] The four colors of the project's thumbnail, in the order it draws them.
 */
@Serializable
public data class ProjectMeta(
    @SerialName("id")
    public val id: String,
    @SerialName("name")
    public val name: String,
    @SerialName("createdAt")
    public val createdAt: Long,
    @SerialName("updatedAt")
    public val updatedAt: Long,
    @SerialName("library")
    public val library: Library = Library.Material3,
    @SerialName("expressive")
    public val expressive: Boolean = false,
    @SerialName("previewColors")
    public val previewColors: List<Argb>,
) {
    init {
        require(previewColors.size == PREVIEW_COLORS) {
            "A thumbnail has $PREVIEW_COLORS colors, got ${previewColors.size}"
        }
    }

    public companion object {
        /** How many colors a project thumbnail shows. */
        public const val PREVIEW_COLORS: Int = 4
    }
}

/**
 * The undo history of one project, what [History.persisted] handed out, stored under
 * [StorageKeys.history].
 *
 * @property[entries] The steps that can be undone, oldest first, at most [History.PERSISTED].
 */
@Serializable
public data class HistoryRecord(
    @SerialName("entries")
    public val entries: List<HistoryEntry> = emptyList(),
) {
    init {
        require(entries.size <= History.PERSISTED) {
            "A saved history keeps at most ${History.PERSISTED} steps, got ${entries.size}"
        }
    }

    public companion object {
        /** Reads and writes a [HistoryRecord]. */
        public val Codec: RecordCodec<HistoryRecord> = RecordCodec(serializer(), Migrations.None)
    }
}

/**
 * How the preview of one project was left, stored under [StorageKeys.view].
 *
 * None of this is part of the theme. It never travels in a link and it is not undoable, it is only
 * here so a project opens the way it was closed.
 *
 * @property[tab] The preview tab that was open.
 * @property[mode] Whether the preview showed light, dark or both side by side.
 * @property[splitFraction] Where the divider sat in the split preview, from 0 at the left edge to 1
 * at the right.
 * @property[deviceWidth] The width the preview was framed at.
 * @property[openFineTuneRows] The fine tune rows that were expanded.
 */
@Serializable
public data class ProjectViewState(
    @SerialName("tab")
    public val tab: PreviewTab = PreviewTab.App,
    @SerialName("mode")
    public val mode: PreviewMode = PreviewMode.Split,
    @SerialName("splitFraction")
    public val splitFraction: Float = DEFAULT_SPLIT_FRACTION,
    @SerialName("deviceWidth")
    public val deviceWidth: DeviceWidth = DeviceWidth.Tablet,
    @SerialName("openFineTuneRows")
    public val openFineTuneRows: Set<FineTuneRow> = emptySet(),
) {
    init {
        require(splitFraction in 0f..1f) { "The split sits between 0 and 1, got $splitFraction" }
    }

    public companion object {
        /** Where the divider sits until someone moves it, halfway across. */
        public const val DEFAULT_SPLIT_FRACTION: Float = 0.5f

        /** Reads and writes a [ProjectViewState]. */
        public val Codec: RecordCodec<ProjectViewState> = RecordCodec(serializer(), Migrations.None)
    }
}

/**
 * The tabs of the preview.
 */
@Serializable
public enum class PreviewTab {
    /** A sample app wearing the theme. */
    @SerialName("App")
    App,

    /** The components gallery. */
    @SerialName("Components")
    Components,

    /** Every color role with its value. */
    @SerialName("Roles")
    Roles,

    /** The tonal palettes the roles are drawn from. */
    @SerialName("Palettes")
    Palettes,

    /** How each pair of roles scores for contrast. */
    @SerialName("Contrast")
    Contrast,
}

/**
 * Which scheme the preview shows.
 */
@Serializable
public enum class PreviewMode {
    /** The light scheme alone. */
    @SerialName("Light")
    Light,

    /** Light and dark side by side, what a project opens with. */
    @SerialName("Split")
    Split,

    /** The dark scheme alone. */
    @SerialName("Dark")
    Dark,
}

/**
 * The width the preview is framed at.
 */
@Serializable
public enum class DeviceWidth {
    /** A phone held upright. */
    @SerialName("Phone")
    Phone,

    /** A tablet, what a project opens with. */
    @SerialName("Tablet")
    Tablet,

    /** A desktop window. */
    @SerialName("Desktop")
    Desktop,
}

/**
 * The rows of the fine tune panel that open and close.
 */
@Serializable
public enum class FineTuneRow {
    /** The key colors set by hand. */
    @SerialName("CoreColors")
    CoreColors,

    /** The extra roles a newer spec adds. */
    @SerialName("SpecExtras")
    SpecExtras,
}
