package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.FrozenVariants
import com.materialkolor.builder.feature.topbar.ExpressiveSwitch
import com.materialkolor.builder.feature.topbar.LibraryChoice
import com.materialkolor.builder.feature.topbar.RevealOrigin
import com.materialkolor.builder.feature.topbar.expressiveChange
import com.materialkolor.builder.feature.topbar.libraryName
import com.materialkolor.builder.feature.topbar.trackRevealOrigin
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.export_animate
import com.materialkolor.builder.generated.resources.export_duration
import com.materialkolor.builder.generated.resources.export_duration_ms
import com.materialkolor.builder.generated.resources.export_dynamic_color
import com.materialkolor.builder.generated.resources.export_dynamic_color_note
import com.materialkolor.builder.generated.resources.export_expressive_2021
import com.materialkolor.builder.generated.resources.export_expressive_caption
import com.materialkolor.builder.generated.resources.export_library_custom_caption
import com.materialkolor.builder.generated.resources.export_library_custom_frozen_note
import com.materialkolor.builder.generated.resources.export_library_custom_note
import com.materialkolor.builder.generated.resources.export_library_fluent_caption
import com.materialkolor.builder.generated.resources.export_library_fluent_note
import com.materialkolor.builder.generated.resources.export_library_m3_caption
import com.materialkolor.builder.generated.resources.export_library_unstyled_caption
import com.materialkolor.builder.generated.resources.export_library_unstyled_note
import com.materialkolor.builder.generated.resources.export_mode_dynamic
import com.materialkolor.builder.generated.resources.export_mode_dynamic_note
import com.materialkolor.builder.generated.resources.export_mode_dynamic_short
import com.materialkolor.builder.generated.resources.export_mode_frozen
import com.materialkolor.builder.generated.resources.export_mode_frozen_note
import com.materialkolor.builder.generated.resources.export_mode_frozen_short
import com.materialkolor.builder.generated.resources.export_options_summary
import com.materialkolor.builder.generated.resources.export_project
import com.materialkolor.builder.generated.resources.export_project_android
import com.materialkolor.builder.generated.resources.export_project_multiplatform
import com.materialkolor.builder.generated.resources.export_section_colors
import com.materialkolor.builder.generated.resources.export_section_library
import com.materialkolor.builder.generated.resources.export_section_project
import com.materialkolor.builder.generated.resources.export_variants
import com.materialkolor.builder.generated.resources.export_variants_all
import com.materialkolor.builder.generated.resources.export_variants_standard
import com.materialkolor.builder.generated.resources.export_version_catalog
import com.materialkolor.builder.generated.resources.export_version_catalog_caption
import com.materialkolor.builder.kit.control.BuilderChoiceChips
import com.materialkolor.builder.kit.control.BuilderChoiceGroup
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The animation lengths offered, in milliseconds. A length stored from elsewhere joins them.
 */
private val DURATIONS_MS = listOf(150, 300, 500, 1000)

/**
 * Whether an export writes a multiplatform project or an Android one.
 */
private enum class ProjectKind {
    Multiplatform,
    AndroidOnly,
}

/**
 * The export options in three labelled sections, each option shown only where the target and the
 * colors picked use it.
 *
 * Library picks the target, the same edit the top bar makes, so the app re-skins behind the sheet.
 * Colors picks live or fixed colors, with color animation under a live export and the contrast
 * levels under a fixed one, since only a fixed export writes every color out. Project holds the
 * project's shape, and the wallpaper colors branch for an Android only Material 3 theme. Every
 * option but the library stays in this browser under the target.
 *
 * @param[capabilities] How each control shows up for the document's target.
 */
@Composable
internal fun ExportOptionsForm(
    state: ExportModel.State,
    capabilities: Capabilities,
    dispatcher: Dispatcher<ExportAction>,
    workspace: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
        OptionsSection(Res.string.export_section_library) { LibraryOptions(state, workspace) }
        OptionsSection(Res.string.export_section_colors) { ColorOptions(state, capabilities, dispatcher) }
        val project = capabilities[Control.KmpOrAndroid].shown ||
            capabilities[Control.VersionCatalog].shown ||
            state.wallpaperShown
        if (project) {
            OptionsSection(Res.string.export_section_project) { ProjectOptions(state, capabilities, dispatcher) }
        }
    }
}

/**
 * One section of the options under its label.
 */
@Composable
private fun OptionsSection(
    label: StringResource,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.medium)) {
        BuilderText(
            text = stringResource(label),
            style = BuilderTextStyle.SectionLabel,
            emphasis = Emphasis.Secondary,
        )
        content()
    }
}

/**
 * The four libraries as cards two a row, then the Expressive switch on Material 3 or a note on
 * where the others run.
 *
 * The arrows only move the focus and Enter or Space picks, as on the top bar's switcher, since each
 * pick re-skins the app. The reveal grows from the press that picked, or from the middle of the
 * cards after a key.
 */
@Composable
private fun LibraryOptions(
    state: ExportModel.State,
    workspace: Dispatcher<WorkspaceAction>,
) {
    val selected = LibraryChoice.of(state.document)
    val origin = remember { RevealOrigin() }
    val pick = { choice: LibraryChoice ->
        if (choice != selected) workspace.dispatch(WorkspaceAction.EditWithReveal(choice.change, origin.take()))
    }
    BuilderChoiceGroup(
        options = LibraryChoice.entries,
        selected = selected,
        onSelect = pick,
        label = stringResource(Res.string.export_section_library),
        modifier = Modifier.trackRevealOrigin(origin),
        selectOnFocus = false,
        columns = 2,
    ) { choice, isSelected, optionModifier ->
        OptionCard(
            title = libraryName(choice),
            caption = stringResource(libraryCaption(choice)),
            selected = isSelected,
            onClick = { pick(choice) },
            modifier = optionModifier,
            cellGap = true,
        )
    }
    when (selected) {
        LibraryChoice.M3 -> {
            Column {
                ExpressiveSwitch(
                    checked = state.document.expressive,
                    onCheckedChange = { on, from ->
                        workspace.dispatch(WorkspaceAction.EditWithReveal(expressiveChange(on), from))
                    },
                )
                BuilderText(text = stringResource(Res.string.export_expressive_caption), emphasis = Emphasis.Secondary)
            }
            if (state.expressiveOn2021) {
                Notice(text = stringResource(Res.string.export_expressive_2021), icon = IconId.Warning)
            }
        }
        else -> {
            BuilderText(text = stringResource(libraryNote(selected, state.prefs.mode)), emphasis = Emphasis.Secondary)
        }
    }
}

/**
 * What a library card says under the library's name.
 */
private fun libraryCaption(choice: LibraryChoice): StringResource =
    when (choice) {
        LibraryChoice.M3 -> Res.string.export_library_m3_caption
        LibraryChoice.Unstyled -> Res.string.export_library_unstyled_caption
        LibraryChoice.Fluent -> Res.string.export_library_fluent_caption
        LibraryChoice.Custom -> Res.string.export_library_custom_caption
    }

/**
 * The note under the cards for a library other than Material 3, on where it runs and what it needs.
 */
private fun libraryNote(
    choice: LibraryChoice,
    mode: ExportMode,
): StringResource =
    when {
        choice == LibraryChoice.Unstyled -> Res.string.export_library_unstyled_note
        choice == LibraryChoice.Fluent -> Res.string.export_library_fluent_note
        mode == ExportMode.Frozen -> Res.string.export_library_custom_frozen_note
        else -> Res.string.export_library_custom_note
    }

/**
 * What a color card calls [mode].
 */
private fun modeTitle(mode: ExportMode): StringResource =
    when (mode) {
        ExportMode.Dynamic -> Res.string.export_mode_dynamic
        ExportMode.Frozen -> Res.string.export_mode_frozen
    }

/**
 * The line under a color card's title.
 */
private fun modeNote(mode: ExportMode): StringResource =
    when (mode) {
        ExportMode.Dynamic -> Res.string.export_mode_dynamic_note
        ExportMode.Frozen -> Res.string.export_mode_frozen_note
    }

/**
 * What the folded options' summary calls [mode].
 */
private fun modeShort(mode: ExportMode): StringResource =
    when (mode) {
        ExportMode.Dynamic -> Res.string.export_mode_dynamic_short
        ExportMode.Frozen -> Res.string.export_mode_frozen_short
    }

/**
 * Live or fixed colors as two cards, then what goes with the one picked.
 */
@Composable
private fun ColorOptions(
    state: ExportModel.State,
    capabilities: Capabilities,
    dispatcher: Dispatcher<ExportAction>,
) {
    val prefs = state.prefs
    BuilderChoiceGroup(
        options = ExportMode.entries,
        selected = prefs.mode,
        onSelect = { mode -> dispatcher.dispatch(ExportAction.SetMode(mode)) },
        label = stringResource(Res.string.export_section_colors),
        columns = 1,
    ) { mode, isSelected, optionModifier ->
        OptionCard(
            title = stringResource(modeTitle(mode)),
            caption = stringResource(modeNote(mode)),
            selected = isSelected,
            onClick = { dispatcher.dispatch(ExportAction.SetMode(mode)) },
            modifier = optionModifier,
        )
    }
    val animation = capabilities[Control.ColorAnimation]
    val variants = capabilities[Control.FrozenExport]
    if (prefs.mode == ExportMode.Dynamic && animation.shown) AnimationOptions(state, animation.usable, dispatcher)
    if (prefs.mode == ExportMode.Frozen && variants.shown) {
        val names = mapOf(
            FrozenVariants.StandardOnly to stringResource(Res.string.export_variants_standard),
            FrozenVariants.AllContrasts to stringResource(Res.string.export_variants_all),
        )
        BuilderSegmented(
            options = FrozenVariants.entries,
            selected = prefs.frozenVariants,
            onSelect = { chosen -> dispatcher.dispatch(ExportAction.SetFrozenVariants(chosen)) },
            label = stringResource(Res.string.export_variants),
            enabled = variants.usable,
            optionLabel = { chosen -> names.getValue(chosen) },
        )
    }
}

/**
 * Color animation, and how long it runs once it is on.
 */
@Composable
private fun AnimationOptions(
    state: ExportModel.State,
    enabled: Boolean,
    dispatcher: Dispatcher<ExportAction>,
) {
    val prefs = state.prefs
    BuilderSwitch(
        checked = prefs.animate,
        onCheckedChange = { on -> dispatcher.dispatch(ExportAction.SetAnimate(on)) },
        label = stringResource(Res.string.export_animate),
        enabled = enabled,
    )
    if (!prefs.animate) return

    val durations = (DURATIONS_MS + prefs.animationDurationMs).distinct().sorted()
    val names = durations.associateWith { ms -> stringResource(Res.string.export_duration_ms, ms) }
    BuilderChoiceChips(
        options = durations,
        selected = prefs.animationDurationMs,
        onSelect = { ms -> dispatcher.dispatch(ExportAction.SetAnimationDuration(ms)) },
        label = stringResource(Res.string.export_duration),
        enabled = enabled,
        optionLabel = { ms -> names.getValue(ms) },
    )
}

/**
 * The project's shape, the version catalog and, for an Android only Material 3 theme, the
 * wallpaper colors.
 */
@Composable
private fun ProjectOptions(
    state: ExportModel.State,
    capabilities: Capabilities,
    dispatcher: Dispatcher<ExportAction>,
) {
    val prefs = state.prefs
    val kind = capabilities[Control.KmpOrAndroid]
    if (kind.shown) {
        val names = mapOf(
            ProjectKind.Multiplatform to stringResource(Res.string.export_project_multiplatform),
            ProjectKind.AndroidOnly to stringResource(Res.string.export_project_android),
        )
        BuilderSegmented(
            options = ProjectKind.entries,
            selected = if (prefs.multiplatform) ProjectKind.Multiplatform else ProjectKind.AndroidOnly,
            onSelect = { chosen ->
                dispatcher.dispatch(ExportAction.SetMultiplatform(chosen == ProjectKind.Multiplatform))
            },
            label = stringResource(Res.string.export_project),
            enabled = kind.usable,
            optionLabel = { chosen -> names.getValue(chosen) },
        )
    }
    val catalog = capabilities[Control.VersionCatalog]
    if (catalog.shown) {
        Column {
            BuilderSwitch(
                checked = prefs.versionCatalog,
                onCheckedChange = { on -> dispatcher.dispatch(ExportAction.SetVersionCatalog(on)) },
                label = stringResource(Res.string.export_version_catalog),
                enabled = catalog.usable,
            )
            BuilderText(text = stringResource(Res.string.export_version_catalog_caption), emphasis = Emphasis.Secondary)
        }
    }
    if (state.wallpaperShown) {
        Column {
            BuilderSwitch(
                checked = prefs.androidDynamicColor,
                onCheckedChange = { on -> dispatcher.dispatch(ExportAction.SetAndroidDynamicColor(on)) },
                label = stringResource(Res.string.export_dynamic_color),
            )
            BuilderText(text = stringResource(Res.string.export_dynamic_color_note), emphasis = Emphasis.Secondary)
        }
    }
}

/**
 * What the folded options say is picked, the library, the colors and the project, "M3, Live,
 * Multiplatform".
 */
@Composable
internal fun optionsSummary(state: ExportModel.State): String {
    val library = libraryName(state.document.library, state.document.expressive)
    val mode = stringResource(modeShort(state.prefs.mode))
    val project = stringResource(
        if (state.prefs.multiplatform) Res.string.export_project_multiplatform else Res.string.export_project_android,
    )
    return stringResource(Res.string.export_options_summary, library, mode, project)
}

/**
 * Whether the wallpaper colors branch applies, which it does to an Android only Material 3 theme.
 */
private val ExportModel.State.wallpaperShown: Boolean
    get() = target in MATERIAL3_TARGETS && !prefs.multiplatform

/**
 * The targets a Material 3 theme is written for, the only ones with the wallpaper colors branch.
 */
private val MATERIAL3_TARGETS = setOf(ExportTarget.Material3, ExportTarget.Material3Expressive)

/**
 * Whether the control shows at all.
 */
private val ControlState.shown: Boolean
    get() = this !is ControlState.Hidden

/**
 * Whether the control takes input.
 */
private val ControlState.usable: Boolean
    get() = this is ControlState.Enabled
