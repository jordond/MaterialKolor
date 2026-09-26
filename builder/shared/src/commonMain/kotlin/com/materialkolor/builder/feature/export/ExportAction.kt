package com.materialkolor.builder.feature.export

import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.FrozenVariants

/**
 * Everything the export sheet can ask of its model.
 *
 * Each option lands in the export options of the target the document is on right now, so another
 * target keeps its own. The target and the theme name are not here, since both belong to the
 * document and go through the workspace as edits.
 */
internal sealed interface ExportAction {
    /**
     * Generate the theme at runtime or write every color out.
     */
    data class SetMode(
        val mode: ExportMode,
    ) : ExportAction

    /**
     * Declare the generated files in [packageName], valid or not. The sheet says what is wrong with it.
     */
    data class SetPackageName(
        val packageName: String,
    ) : ExportAction

    /**
     * Write the build snippets for a multiplatform project, or for an Android one.
     */
    data class SetMultiplatform(
        val multiplatform: Boolean,
    ) : ExportAction

    /**
     * Add the dependency through a version catalog, or straight into the build file.
     */
    data class SetVersionCatalog(
        val versionCatalog: Boolean,
    ) : ExportAction

    /**
     * Animate color changes in the generated theme, or not.
     */
    data class SetAnimate(
        val animate: Boolean,
    ) : ExportAction

    /**
     * Run those animations for [durationMs] milliseconds.
     */
    data class SetAnimationDuration(
        val durationMs: Int,
    ) : ExportAction

    /**
     * Write standard contrast alone into a frozen export, or all three levels.
     */
    data class SetFrozenVariants(
        val variants: FrozenVariants,
    ) : ExportAction

    /**
     * Let an Android only Material 3 theme use the wallpaper colors on Android 12 and up.
     */
    data class SetAndroidDynamicColor(
        val on: Boolean,
    ) : ExportAction

    /**
     * Show the file at [path] in the code view.
     */
    data class SelectFile(
        val path: String,
    ) : ExportAction

    /**
     * A copy, download or share went through, so the first export is done.
     */
    data object Exported : ExportAction
}
