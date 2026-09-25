package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.materialkolor.builder.feature.image.ImageCandidateRow
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.kit.control.BottomSheetDetent
import com.materialkolor.builder.kit.control.BottomSheetState
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.ShortHeightBreakpoint
import dev.stateholder.dispatcher.Dispatcher

/**
 * The poster's sections in the phone sheet, in the order the sheet shows them, each with the
 * detent that first brings it into view.
 */
internal enum class SheetSection(
    val detent: BottomSheetDetent,
) {
    SeedPeek(BottomSheetDetent.Peek),
    SeedActions(BottomSheetDetent.Peek),
    FirstRunHint(BottomSheetDetent.Peek),
    ImageCandidates(BottomSheetDetent.Peek),
    StyleChips(BottomSheetDetent.Peek),
    Contrast(BottomSheetDetent.Peek),

    ContrastDetails(BottomSheetDetent.Half),
    StyleDetails(BottomSheetDetent.Half),
    Explainer(BottomSheetDetent.Half),
    Locks(BottomSheetDetent.Half),
    CoreColors(BottomSheetDetent.Half),
    SpecExtras(BottomSheetDetent.Half),
    Hero(BottomSheetDetent.Full),
    Header(BottomSheetDetent.Full),
}

/**
 * The sections a sheet resting at [detent] has in view. The peek holds the seed row, Pick and Image,
 * one scrolling row of style chips and the contrast levels, half adds the contrast readout, what
 * the chosen style does, the explainer, the locks and the fine tune rows, and full the hero and the
 * header. A phone on its side, [short], peeks at the seed row and Shuffle alone.
 */
internal fun sheetSectionsInView(
    detent: BottomSheetDetent,
    short: Boolean,
): List<SheetSection> =
    if (short && detent == BottomSheetDetent.Peek) {
        listOf(SheetSection.SeedPeek)
    } else {
        SheetSection.entries.filter { section -> section.detent <= detent }
    }

/**
 * The sheet the poster rests in on a phone, or null where the poster is not in one.
 */
internal val LocalPosterSheetState: ProvidableCompositionLocal<BottomSheetState?> =
    staticCompositionLocalOf { null }

/**
 * Told the sheet's view each time the sheet composes, or null, which it always is outside tests.
 * Tests provide it to read which sections are in view at each detent.
 */
internal val LocalPosterSheetProbe: ProvidableCompositionLocal<((PosterSheetView) -> Unit)?> =
    staticCompositionLocalOf { null }

/**
 * What the phone sheet has in view, read from the detent its [sheet] is heading for and the window
 * it sits in. A poster outside a sheet the workspace owns counts as resting at the peek.
 */
@Stable
internal class PosterSheetView(
    private val sheet: BottomSheetState?,
    private val layout: LayoutInfo,
) {
    /**
     * The detent the sheet rests at, or is on its way to.
     */
    val detent: BottomSheetDetent
        get() = sheet?.targetDetent ?: BottomSheetDetent.Peek

    /**
     * Whether this is a phone on its side, where the peek only has room for the seed row.
     */
    val short: Boolean
        get() = layout.heightDp < ShortHeightBreakpoint && layout.coarsePointer

    /**
     * The sections in view at [detent], in the order the sheet shows them.
     */
    val inView: List<SheetSection>
        get() = sheetSectionsInView(detent, short)
}

/**
 * The poster in the phone sheet, every section in the order [SheetSection] lists them, so each
 * detent shows the ones it holds from the top. The sheet scrolls to the rest.
 */
@Composable
internal fun ColumnScope.PosterSheet(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    focus: PosterFocus?,
) {
    val sheet = LocalPosterSheetState.current
    val layout = LocalLayout.current
    val view = remember(sheet, layout) { PosterSheetView(sheet, layout) }
    LocalPosterSheetProbe.current?.invoke(view)
    SheetSection.entries.forEach { section ->
        when (section) {
            SheetSection.SeedPeek -> SeedPeekRow(context, dispatcher)
            SheetSection.SeedActions -> SeedActions(context, dispatcher, shuffle = false, locks = false)
            SheetSection.FirstRunHint -> FirstRunHint(context, dispatcher)
            SheetSection.ImageCandidates -> ImageCandidateRow(context, dispatcher)
            SheetSection.StyleChips -> StyleChipsSection(context, dispatcher, scrolling = true, details = false)
            SheetSection.Contrast -> ContrastSection(context, dispatcher, details = false)
            SheetSection.ContrastDetails -> ContrastDetails(context)
            SheetSection.StyleDetails -> StyleDetails(context, dispatcher, info = true)
            SheetSection.Explainer -> PrimaryExplainerLine(context, dispatcher, why = focus?.why)
            SheetSection.Locks -> ShuffleLocks(context, dispatcher)
            SheetSection.CoreColors -> CoreColorsRow(context, dispatcher)
            SheetSection.SpecExtras -> SpecExtrasRow(context, dispatcher)
            SheetSection.Hero -> SeedHero(context, dispatcher, focus = focus)
            SheetSection.Header -> PosterHeader(context, dispatcher, focus = focus)
        }
    }
}
