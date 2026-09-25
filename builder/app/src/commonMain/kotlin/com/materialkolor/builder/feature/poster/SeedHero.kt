package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorNames
import com.materialkolor.builder.domain.color.InvalidReason
import com.materialkolor.builder.domain.color.ParseNote
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.keycolors_note_alpha
import com.materialkolor.builder.generated.resources.poster_copied_hex
import com.materialkolor.builder.generated.resources.poster_copied_kotlin
import com.materialkolor.builder.generated.resources.poster_copy_hex
import com.materialkolor.builder.generated.resources.poster_copy_kotlin
import com.materialkolor.builder.generated.resources.poster_hct
import com.materialkolor.builder.generated.resources.poster_hex_bad_arguments
import com.materialkolor.builder.generated.resources.poster_hex_bad_hex
import com.materialkolor.builder.generated.resources.poster_hex_empty
import com.materialkolor.builder.generated.resources.poster_hex_unknown_function
import com.materialkolor.builder.generated.resources.poster_hex_unknown_name
import com.materialkolor.builder.generated.resources.poster_hex_unrecognized
import com.materialkolor.builder.generated.resources.poster_note_alpha
import com.materialkolor.builder.generated.resources.poster_note_both
import com.materialkolor.builder.generated.resources.poster_note_clamped
import com.materialkolor.builder.generated.resources.poster_seed
import com.materialkolor.builder.generated.resources.poster_seed_field
import com.materialkolor.builder.generated.resources.poster_source_eyedropper
import com.materialkolor.builder.generated.resources.poster_source_image
import com.materialkolor.builder.generated.resources.poster_source_image_named
import com.materialkolor.builder.generated.resources.poster_source_picked
import com.materialkolor.builder.generated.resources.poster_source_preset
import com.materialkolor.builder.generated.resources.poster_source_shuffled
import com.materialkolor.builder.generated.resources.poster_source_typed
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderHexField
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The seed as the poster's headline. The hex is a real field that edits in place, and under it one
 * line holds the seed's name, its HCT readout, where it came from and whether the project is saved,
 * with the two copy buttons as icons at its end.
 *
 * The field shows the seed as stored, not as the target sees it. A commit lands as a typed seed,
 * one keystroke folding into the next in the history.
 *
 * @param[focus] Holds the copy buttons, which a refused copy's manual copy dialog hands focus back
 * to while the hero still shows them.
 */
@Composable
internal fun SeedHero(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    focus: PosterFocus? = null,
) {
    val seed = context.document.seed
    val spacing = LocalBuilderTokens.current.spacing
    val messages = rememberHexMessages()
    val hct = remember(seed) { HctReadout.of(seed).rounded() }
    val hexLabel = stringResource(Res.string.poster_copied_hex)
    val kotlinLabel = stringResource(Res.string.poster_copied_kotlin)
    // A refused copy opens the manual copy dialog, which hands focus back to the button pressed.
    // The workspace's triggers count the buttons on screen, so the dialog asks nothing of a hero
    // that left while it was open. Without them the hero keeps its own.
    val own = remember { PosterFocus() }
    val triggers = focus ?: own
    val copyHex = triggers.copyHex
    val copyKotlin = triggers.copyKotlin
    Column(modifier) {
        InfoLabel(label = stringResource(Res.string.poster_seed), topic = InfoTopic.Seed)
        BuilderHexField(
            value = seed,
            onCommit = { argb, _ ->
                val change = DocumentChange.SetSeed(argb, SeedSource.Typed)
                dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete))
            },
            label = stringResource(Res.string.poster_seed_field),
            errorMessage = messages::errorOf,
            noteMessage = messages::noteOf,
            modifier = Modifier.fillMaxWidth(),
            large = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The name, its readout and where it came from share a line while it has room.
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderText(
                    text = remember(seed) { ColorNames.nameOf(seed) },
                    style = BuilderTextStyle.Title,
                    maxLines = 1,
                )
                BuilderText(
                    text = stringResource(Res.string.poster_hct, hct.hue, hct.chroma, hct.tone),
                    style = BuilderTextStyle.Value,
                    maxLines = 1,
                )
                BuilderBadge(label = sourceLabel(context.document.seedSource).text())
                SaveState(context.saveStatus)
            }
            PosterIconButton(
                icon = IconId.Copy,
                description = stringResource(Res.string.poster_copy_hex),
                onClick = { dispatcher.dispatch(WorkspaceAction.CopyText(seed.toHex(), hexLabel, copyHex.requester)) },
                buttonModifier = triggerFocus(copyHex),
            )
            PosterIconButton(
                icon = IconId.Export,
                description = stringResource(Res.string.poster_copy_kotlin),
                onClick = {
                    val copy = WorkspaceAction.CopyText(kotlinLiteralOf(seed), kotlinLabel, copyKotlin.requester)
                    dispatcher.dispatch(copy)
                },
                buttonModifier = triggerFocus(copyKotlin),
            )
        }
    }
}

/**
 * [argb] as a Compose color literal, `Color(0xFF6750A4)`.
 */
internal fun kotlinLiteralOf(argb: Argb): String = "Color(0xFF${argb.toHex().removePrefix("#")})"

/**
 * A seed's hue, chroma and tone as the readout shows them, each rounded to a whole number.
 *
 * @property[hue] The hue in whole degrees, 0 to 359.
 * @property[chroma] The chroma, rounded.
 * @property[tone] The tone, rounded.
 */
@Immutable
internal data class RoundedHct(
    val hue: Int,
    val chroma: Int,
    val tone: Int,
)

/**
 * This readout rounded for the screen. A hue that rounds up to a full turn reads as 0.
 */
internal fun HctReadout.rounded(): RoundedHct =
    RoundedHct(hue.roundToInt() % FULL_TURN, chroma.roundToInt(), tone.roundToInt())

private const val FULL_TURN = 360

/**
 * The words for where a seed came from, one string and the argument it takes, if any.
 *
 * @property[resource] The string to show.
 * @property[argument] What fills its placeholder, or null when it has none.
 */
@Immutable
internal data class SourceLabel(
    val resource: StringResource,
    val argument: String? = null,
) {
    @Composable
    fun text(): String = if (argument == null) stringResource(resource) else stringResource(resource, argument)
}

/**
 * Where [source] says the seed came from. An image names its file when it has one.
 */
internal fun sourceLabel(source: SeedSource): SourceLabel =
    when (source) {
        SeedSource.Typed -> SourceLabel(Res.string.poster_source_typed)
        SeedSource.Picked -> SourceLabel(Res.string.poster_source_picked)
        SeedSource.Eyedropper -> SourceLabel(Res.string.poster_source_eyedropper)
        SeedSource.Shuffled -> SourceLabel(Res.string.poster_source_shuffled)
        is SeedSource.Preset -> SourceLabel(Res.string.poster_source_preset)
        is SeedSource.Image -> if (source.name.isBlank()) {
            SourceLabel(Res.string.poster_source_image)
        } else {
            SourceLabel(Res.string.poster_source_image_named, source.name)
        }
    }

/**
 * What a color field says about text it cannot read, and about what it had to change to read a
 * color, resolved once so the field can ask outside composition.
 */
@Immutable
internal class HexMessages(
    private val errors: Map<InvalidReason, String>,
    private val alpha: String,
    private val clamped: String,
    private val both: String,
) {
    fun errorOf(reason: InvalidReason): String = errors.getValue(reason)

    fun noteOf(notes: Set<ParseNote>): String =
        when {
            ParseNote.AlphaDropped in notes && ParseNote.Clamped in notes -> both
            ParseNote.AlphaDropped in notes -> alpha
            else -> clamped
        }
}

/**
 * What a color field is for, which the note about a dropped alpha names.
 */
internal enum class HexSubject {
    /**
     * The seed, or the second seed Cmf takes.
     */
    Seed,

    /**
     * A key color set by hand.
     */
    KeyColor,
}

/**
 * What a color field for [subject] says about text it cannot read and colors it had to change.
 */
@Composable
internal fun rememberHexMessages(subject: HexSubject = HexSubject.Seed): HexMessages {
    val errors = InvalidReason.entries.associateWith { reason -> stringResource(errorResource(reason)) }
    val alpha = stringResource(
        when (subject) {
            HexSubject.Seed -> Res.string.poster_note_alpha
            HexSubject.KeyColor -> Res.string.keycolors_note_alpha
        },
    )
    val clamped = stringResource(Res.string.poster_note_clamped)
    val both = stringResource(Res.string.poster_note_both)
    return remember(errors, alpha, clamped, both) { HexMessages(errors, alpha, clamped, both) }
}

private fun errorResource(reason: InvalidReason): StringResource =
    when (reason) {
        InvalidReason.Empty -> Res.string.poster_hex_empty
        InvalidReason.BadHex -> Res.string.poster_hex_bad_hex
        InvalidReason.BadArguments -> Res.string.poster_hex_bad_arguments
        InvalidReason.UnknownFunction -> Res.string.poster_hex_unknown_function
        InvalidReason.UnknownName -> Res.string.poster_hex_unknown_name
        InvalidReason.Unrecognized -> Res.string.poster_hex_unrecognized
    }
