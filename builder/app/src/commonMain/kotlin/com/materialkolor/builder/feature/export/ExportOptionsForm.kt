package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.codegen.validate.ReservedNameClash
import com.materialkolor.builder.codegen.validate.ReservedNames
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.FrozenVariants
import com.materialkolor.builder.domain.validate.validatePackageName
import com.materialkolor.builder.domain.validate.validateThemeName
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.export_animate
import com.materialkolor.builder.generated.resources.export_duration
import com.materialkolor.builder.generated.resources.export_duration_ms
import com.materialkolor.builder.generated.resources.export_dynamic_color
import com.materialkolor.builder.generated.resources.export_dynamic_color_note
import com.materialkolor.builder.generated.resources.export_package
import com.materialkolor.builder.generated.resources.export_package_hint
import com.materialkolor.builder.generated.resources.export_package_invalid
import com.materialkolor.builder.generated.resources.export_project
import com.materialkolor.builder.generated.resources.export_project_android
import com.materialkolor.builder.generated.resources.export_project_multiplatform
import com.materialkolor.builder.generated.resources.export_theme_name
import com.materialkolor.builder.generated.resources.export_theme_name_invalid
import com.materialkolor.builder.generated.resources.export_theme_name_taken
import com.materialkolor.builder.generated.resources.export_variants
import com.materialkolor.builder.generated.resources.export_variants_all
import com.materialkolor.builder.generated.resources.export_variants_standard
import com.materialkolor.builder.generated.resources.export_version_catalog
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSelect
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/** The animation lengths offered, in milliseconds. A length stored from elsewhere joins them. */
private val DURATIONS_MS = listOf(150, 300, 500, 1000)

/** Whether an export writes a multiplatform project or an Android one. */
private enum class ProjectKind {
    Multiplatform,
    AndroidOnly,
}

/**
 * The export options (F-27), each shown only where the target and the mode use it.
 *
 * Contrast levels only matter to a frozen export, which writes every color out, and color animation
 * only to a dynamic one. The wallpaper colors branch is for an Android only Material 3 theme. The
 * package and the theme name keep a draft that is not valid to themselves and say what is wrong
 * under the field. The theme name goes to the document, so it travels with the project and its
 * share link, while every other option stays in this browser under the target.
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
    val prefs = state.prefs
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        PackageField(
            packageName = prefs.packageName,
            onCommit = { name -> dispatcher.dispatch(ExportAction.SetPackageName(name)) },
        )
        ThemeNameField(state, workspace)
        if (capabilities[Control.KmpOrAndroid].shown) {
            val names = mapOf(
                ProjectKind.Multiplatform to stringResource(Res.string.export_project_multiplatform),
                ProjectKind.AndroidOnly to stringResource(Res.string.export_project_android),
            )
            BuilderSegmented(
                options = ProjectKind.entries,
                selected = if (prefs.multiplatform) ProjectKind.Multiplatform else ProjectKind.AndroidOnly,
                onSelect = { kind ->
                    dispatcher.dispatch(ExportAction.SetMultiplatform(kind == ProjectKind.Multiplatform))
                },
                label = stringResource(Res.string.export_project),
                enabled = capabilities[Control.KmpOrAndroid].usable,
                optionLabel = { kind -> names.getValue(kind) },
            )
        }
        if (capabilities[Control.VersionCatalog].shown) {
            BuilderSwitch(
                checked = prefs.versionCatalog,
                onCheckedChange = { on -> dispatcher.dispatch(ExportAction.SetVersionCatalog(on)) },
                label = stringResource(Res.string.export_version_catalog),
                enabled = capabilities[Control.VersionCatalog].usable,
            )
        }
        if (prefs.mode == ExportMode.Dynamic && capabilities[Control.ColorAnimation].shown) {
            AnimationOptions(state, capabilities[Control.ColorAnimation].usable, dispatcher)
        }
        if (prefs.mode == ExportMode.Frozen && capabilities[Control.FrozenExport].shown) {
            val names = mapOf(
                FrozenVariants.StandardOnly to stringResource(Res.string.export_variants_standard),
                FrozenVariants.AllContrasts to stringResource(Res.string.export_variants_all),
            )
            BuilderSegmented(
                options = FrozenVariants.entries,
                selected = prefs.frozenVariants,
                onSelect = { variants -> dispatcher.dispatch(ExportAction.SetFrozenVariants(variants)) },
                label = stringResource(Res.string.export_variants),
                enabled = capabilities[Control.FrozenExport].usable,
                optionLabel = { variants -> names.getValue(variants) },
            )
        }
        if (state.target in MATERIAL3_TARGETS && !prefs.multiplatform) {
            BuilderSwitch(
                checked = prefs.androidDynamicColor,
                onCheckedChange = { on -> dispatcher.dispatch(ExportAction.SetAndroidDynamicColor(on)) },
                label = stringResource(Res.string.export_dynamic_color),
            )
            BuilderText(text = stringResource(Res.string.export_dynamic_color_note), emphasis = Emphasis.Secondary)
        }
    }
}

@Composable
private fun PackageField(
    packageName: String,
    onCommit: (String) -> Unit,
) {
    val invalid = stringResource(Res.string.export_package_invalid)
    BuilderTextField(
        value = packageName,
        onCommit = onCommit,
        label = stringResource(Res.string.export_package),
        error = { draft -> if (validatePackageName(draft).isEmpty()) null else invalid },
        supportingText = stringResource(Res.string.export_package_hint),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The theme name, checked against Kotlin and against the names the target's export already uses. */
@Composable
private fun ThemeNameField(
    state: ExportModel.State,
    workspace: Dispatcher<WorkspaceAction>,
) {
    val invalid = stringResource(Res.string.export_theme_name_invalid)
    val taken = stringResource(Res.string.export_theme_name_taken)
    val targeted = state.document.forTarget(state.target)
    BuilderTextField(
        value = state.document.themeName,
        onCommit = { name ->
            workspace.dispatch(WorkspaceAction.Edit(DocumentChange.SetThemeName(name), EditPhase.Discrete))
        },
        label = stringResource(Res.string.export_theme_name),
        error = { draft ->
            when {
                validateThemeName(draft).isNotEmpty() -> invalid
                targeted.takesReservedName(draft) -> taken
                else -> null
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Whether the export of this document would clash with a name it already uses, were the theme called [name]. */
private fun ThemeDocument.takesReservedName(name: String): Boolean =
    ReservedNames.clashes(copy(themeName = name)).any { clash -> clash is ReservedNameClash.ThemeName }

/** Color animation, and how long it runs once it is on. */
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
    BuilderSelect(
        label = stringResource(Res.string.export_duration),
        options = durations,
        selected = prefs.animationDurationMs,
        onSelect = { ms -> dispatcher.dispatch(ExportAction.SetAnimationDuration(ms)) },
        enabled = enabled,
        optionLabel = { ms -> names.getValue(ms) },
    )
}

/** The targets a Material 3 theme is written for, the only ones with the wallpaper colors branch. */
private val MATERIAL3_TARGETS = setOf(ExportTarget.Material3, ExportTarget.Material3Expressive)

/** Whether the control shows at all. */
private val ControlState.shown: Boolean
    get() = this !is ControlState.Hidden

/** Whether the control takes input. */
private val ControlState.usable: Boolean
    get() = this is ControlState.Enabled
