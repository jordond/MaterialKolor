package com.materialkolor.builder.feature.export

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.codegen.copyAll
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.generate
import com.materialkolor.builder.codegen.validate.ReservedNames
import com.materialkolor.builder.codegen.zipArchive
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.OutgoingFile
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.link.shareLink
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.validate.ValidationError
import com.materialkolor.builder.domain.validate.validateDocument
import com.materialkolor.builder.domain.validate.validatePackageName
import com.materialkolor.builder.engine.export.ExportResolver
import com.materialkolor.builder.engine.resolve.ThemeResolver
import dev.stateholder.extensions.viewmodel.StateViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

/**
 * The media type of the downloaded zip.
 */
internal const val ZIP_MIME = "application/zip"

/**
 * The export sheet's view of the open document and of the options its target was last exported
 * with.
 *
 * Options live in the browser per target, so switching the target brings back what that target
 * used last time, and a theme edit or a reload leaves them where they were. The theme name is the
 * exception. It belongs to the document, so the sheet edits it through the workspace.
 *
 * The files come from [outcome], worked out on the thread that asks, which has to be the UI thread
 * since the resolver belongs to it. They are kept until the document, the project name, the options
 * or the versions change, so a sheet that recomposes, or opens again on the same theme, generates
 * nothing.
 *
 * Every header and the README link back with the link Share gives, [shareLink], which carries the
 * project name. The package name stays out of it.
 *
 * The model never copies or saves anything itself. Browsers only allow that inside the click, so
 * the sheet calls [clipboard] and [files] straight from its click handler.
 *
 * @property[clipboard] Where Copy and Copy all write.
 * @property[files] Where Download zip and Share files go.
 */
@Stable
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class ExportModel(
    session: ProjectSession,
    private val preferences: PreferencesRepository,
    private val resolver: ThemeResolver,
    private val versions: ExportVersions,
    private val generator: ExportGenerator,
    val clipboard: Clipboard,
    val files: FileSaver,
    private val environment: Environment,
) : StateViewModel<ExportModel.State>(
        State(document = session.document.value, preferences = preferences.preferences.value),
    ) {
    private val exports = ExportResolver(themes = resolver)

    /**
     * The MaterialKolor version every export is built and checked against.
     */
    val materialKolorVersion: String
        get() = versions.materialKolor
    private var memo: Memo? = null
    private var glyphMemo: Pair<ThemeDocument, SchemeGlyph>? = null

    init {
        session.document.mergeState { state, document -> state.copy(document = document) }
        session.projectName.mergeState { state, name -> state.copy(projectName = name) }
        preferences.preferences.mergeState { state, prefs -> state.copy(preferences = prefs) }
    }

    fun handle(action: ExportAction) {
        when (action) {
            is ExportAction.SetMode -> {
                updatePrefs { prefs -> prefs.copy(mode = action.mode) }
            }
            is ExportAction.SetPackageName -> {
                updatePrefs { prefs -> prefs.copy(packageName = action.packageName) }
            }
            is ExportAction.SetMultiplatform -> {
                updatePrefs { prefs -> prefs.copy(multiplatform = action.multiplatform) }
            }
            is ExportAction.SetVersionCatalog -> {
                updatePrefs { prefs -> prefs.copy(versionCatalog = action.versionCatalog) }
            }
            is ExportAction.SetAnimate -> {
                updatePrefs { prefs -> prefs.copy(animate = action.animate) }
            }
            is ExportAction.SetAnimationDuration -> {
                updatePrefs { prefs -> prefs.copy(animationDurationMs = action.durationMs) }
            }
            is ExportAction.SetFrozenVariants -> {
                updatePrefs { prefs -> prefs.copy(frozenVariants = action.variants) }
            }
            is ExportAction.SetAndroidDynamicColor -> {
                updatePrefs { prefs -> prefs.copy(androidDynamicColor = action.on) }
            }
            is ExportAction.SelectFile -> {
                updateState { state -> state.copy(selectedPath = action.path) }
            }
            ExportAction.Exported -> {
                markExported()
            }
        }
    }

    /**
     * The export of [state], or what has to be fixed before there can be one.
     *
     * The same document, project name, options and versions give back the very same value without
     * generating again. Call it on the UI thread.
     */
    fun outcome(state: State = this.state.value): ExportOutcome {
        val key = Memo.Key(state.document, state.projectName, state.prefs, versions)
        memo?.takeIf { held -> held.key == key }?.let { held -> return held.outcome }

        val outcome = exportOf(state.document, state.projectName, state.prefs)
        memo = Memo(key, outcome)
        return outcome
    }

    /**
     * The scheme glyph of [document] as its export target builds it, from the light scheme like a
     * project's thumbnail. The same document gives back the same glyph without resolving again. Call
     * it on the UI thread.
     */
    fun glyph(document: ThemeDocument): SchemeGlyph {
        glyphMemo?.takeIf { (held, _) -> held == document }?.let { (_, glyph) -> return glyph }
        val target = ExportTarget.of(document.library, document.expressive)
        val scheme = resolver.resolve(document.forTarget(target)).light
        val glyph = SchemeGlyph(
            primary = Color(scheme.primary),
            secondaryContainer = Color(scheme.secondaryContainer),
            tertiaryContainer = Color(scheme.tertiaryContainer),
        )
        glyphMemo = document to glyph
        return glyph
    }

    private fun exportOf(
        document: ThemeDocument,
        projectName: String,
        prefs: ExportPrefs,
    ): ExportOutcome {
        val target = ExportTarget.of(document.library, document.expressive)
        val targeted = document.forTarget(target)
        val problems = problemsOf(targeted, prefs)
        if (problems.isNotEmpty()) return ExportOutcome.Blocked(problems)
        // Only accents that do not fit stop a link, and the checks above turn those down first.
        val link = shareLink(document, projectName, environment.siteOrigin)
            ?: return ExportOutcome.Blocked(listOf(ExportProblem.ExtraColors))

        val input = ExportInput(
            document = document,
            prefs = prefs,
            resolved = exports.resolve(targeted, prefs),
            versions = versions,
            shareUrl = link,
        )
        val files = generator.files(input)
        val themeName = targeted.themeName
        return ExportOutcome.Ready(
            files = files,
            allText = copyAll(files),
            zip = OutgoingFile(name = "$themeName.zip", bytes = zipArchive(themeName, files), mime = ZIP_MIME),
        )
    }

    private fun updatePrefs(block: (ExportPrefs) -> ExportPrefs) {
        val target = state.value.target
        viewModelScope.launch { preferences.updateExportPrefs(target, block) }
    }

    private fun markExported() {
        if (state.value.preferences.firstExportDone) return
        viewModelScope.launch { preferences.update { prefs -> prefs.copy(firstExportDone = true) } }
    }

    /**
     * The last export worked out, and what it was worked out from.
     */
    private class Memo(
        val key: Key,
        val outcome: ExportOutcome,
    ) {
        data class Key(
            val document: ThemeDocument,
            val projectName: String,
            val prefs: ExportPrefs,
            val versions: ExportVersions,
        )
    }

    /**
     * @property[document] The document as stored. The export reads it through `forTarget`.
     * @property[projectName] The open project's name, which the link back carries. Empty until the
     * session has opened one.
     * @property[preferences] What this browser remembers, the export options of every target among it.
     * @property[selectedPath] The file the code view shows, or null for the first one.
     */
    @Immutable
    data class State(
        val document: ThemeDocument,
        val preferences: Preferences,
        val projectName: String = "",
        val selectedPath: String? = null,
    ) {
        /**
         * What [document] exports to.
         */
        val target: ExportTarget
            get() = ExportTarget.of(document.library, document.expressive)

        /**
         * The options [target] was last exported with.
         */
        val prefs: ExportPrefs
            get() = preferences.exportPrefsFor(target)

        /**
         * Whether an Expressive theme runs on the 2021 spec without saying so, because its style has no
         * 2025 form. Only Tonal spot, Neutral, Vibrant and Expressive have one.
         */
        val expressiveOn2021: Boolean
            get() = target == ExportTarget.Material3Expressive &&
                SpecVersion.Spec2025 !in EffectiveSpec.offered(document.forTarget(target).style)
    }
}

/**
 * Turns an export input into its files. The model takes it from the graph so a test can count how
 * often it generates.
 */
internal fun interface ExportGenerator {
    fun files(input: ExportInput): List<GeneratedFile>
}

/**
 * Puts codegen's [generate] into the graph.
 */
@BindingContainer
@ContributesTo(AppScope::class)
internal object ExportBindings {
    @Provides
    fun provideExportGenerator(): ExportGenerator = ExportGenerator { input -> generate(input) }
}

/**
 * What the sheet can offer for the document and options it was worked out from.
 */
internal sealed interface ExportOutcome {
    /**
     * The files, ready to copy or download.
     *
     * @property[files] Every file, theme files first and the README last.
     * @property[allText] Every file joined, each under a comment with its path, for Copy all.
     * @property[zip] The zip, built before anyone clicks so the click only has to hand it over.
     */
    class Ready(
        val files: List<GeneratedFile>,
        val allText: String,
        val zip: OutgoingFile,
    ) : ExportOutcome

    /**
     * Nothing can be exported until [problems] are fixed.
     */
    data class Blocked(
        val problems: List<ExportProblem>,
    ) : ExportOutcome
}

/**
 * Something that stops an export, each named once however many fields it covers.
 */
internal sealed interface ExportProblem {
    /**
     * [packageName] is not a package Kotlin accepts.
     */
    data class PackageName(
        val packageName: String,
    ) : ExportProblem

    /**
     * [themeName] is not a name Kotlin accepts.
     */
    data class ThemeName(
        val themeName: String,
    ) : ExportProblem

    /**
     * One or more extra colors have a name the export cannot use, or there are too many of them.
     */
    data object ExtraColors : ExportProblem

    /**
     * The export already uses [name] for something else.
     */
    data class NameTaken(
        val name: String,
    ) : ExportProblem
}

/**
 * What stops [targeted], the document as its target sees it, from being exported with [prefs].
 */
internal fun problemsOf(
    targeted: ThemeDocument,
    prefs: ExportPrefs,
): List<ExportProblem> {
    val invalid = (validatePackageName(prefs.packageName) + validateDocument(targeted)).mapNotNull { error ->
        problemOf(error, prefs.packageName, targeted.themeName)
    }
    val taken = ReservedNames.clashes(targeted).map { clash -> ExportProblem.NameTaken(clash.name) }
    return (invalid + taken).distinct()
}

private fun problemOf(
    error: ValidationError,
    packageName: String,
    themeName: String,
): ExportProblem? =
    when (error) {
        is ValidationError.PackageSegmentInvalid,
        is ValidationError.PackageSegmentKeyword,
        -> ExportProblem.PackageName(packageName)
        is ValidationError.ThemeNameInvalid,
        is ValidationError.ThemeNameKeyword,
        -> ExportProblem.ThemeName(themeName)
        is ValidationError.TooManyAccents,
        is ValidationError.AccentNameInvalid,
        is ValidationError.AccentNameKeyword,
        is ValidationError.AccentNameTooLong,
        is ValidationError.AccentNameDuplicate,
        is ValidationError.AccentNameCaseClash,
        is ValidationError.AccentNameRole,
        -> ExportProblem.ExtraColors
        // An export has no project name to check.
        is ValidationError.ProjectNameTooLong -> null
    }
