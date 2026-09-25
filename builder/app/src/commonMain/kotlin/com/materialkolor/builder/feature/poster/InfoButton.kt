package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.glossary_accents
import com.materialkolor.builder.generated.resources.glossary_accents_question
import com.materialkolor.builder.generated.resources.glossary_contrast
import com.materialkolor.builder.generated.resources.glossary_contrast_question
import com.materialkolor.builder.generated.resources.glossary_key_colors
import com.materialkolor.builder.generated.resources.glossary_key_colors_question
import com.materialkolor.builder.generated.resources.glossary_pins
import com.materialkolor.builder.generated.resources.glossary_pins_question
import com.materialkolor.builder.generated.resources.glossary_seed
import com.materialkolor.builder.generated.resources.glossary_seed_question
import com.materialkolor.builder.generated.resources.glossary_spec
import com.materialkolor.builder.generated.resources.glossary_spec_question
import com.materialkolor.builder.generated.resources.glossary_style
import com.materialkolor.builder.generated.resources.glossary_style_question
import com.materialkolor.builder.generated.resources.glossary_targets
import com.materialkolor.builder.generated.resources.glossary_targets_question
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.foldedExpandedName
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The poster sections an info button explains.
 *
 * The docs have no builder pages yet, so a topic carries no link. Help lists every topic in one
 * dialog, and once the owner settles the help pages they live under `HELP_PAGES_URL`.
 */
internal enum class InfoTopic {
    Seed,
    Style,
    Spec,
    Contrast,
    KeyColors,
    Pins,
    Accents,
    Targets,
    ;

    /**
     * What the info button asks, read out and shown as its tooltip.
     */
    val question: StringResource
        get() = when (this) {
            Seed -> Res.string.glossary_seed_question
            Style -> Res.string.glossary_style_question
            Spec -> Res.string.glossary_spec_question
            Contrast -> Res.string.glossary_contrast_question
            KeyColors -> Res.string.glossary_key_colors_question
            Pins -> Res.string.glossary_pins_question
            Accents -> Res.string.glossary_accents_question
            Targets -> Res.string.glossary_targets_question
        }

    /**
     * The short answer the info button opens.
     */
    val explanation: StringResource
        get() = when (this) {
            Seed -> Res.string.glossary_seed
            Style -> Res.string.glossary_style
            Spec -> Res.string.glossary_spec
            Contrast -> Res.string.glossary_contrast
            KeyColors -> Res.string.glossary_key_colors
            Pins -> Res.string.glossary_pins
            Accents -> Res.string.glossary_accents
            Targets -> Res.string.glossary_targets
        }
}

/**
 * A poster section's small label with its info button, and the explanation under it while it is
 * open. The explanation sits in the column rather than a popup, so it scrolls with the poster.
 *
 * @param[label] The section's name, as in "Seed".
 * @param[topic] What the info button explains.
 * @param[modifier] Applied to the column.
 */
@Composable
internal fun InfoLabel(
    label: String,
    topic: InfoTopic,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    var open by rememberSaveable(topic) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderText(text = label, style = BuilderTextStyle.SectionLabel, maxLines = 1)
            InfoButton(topic = topic, expanded = open, onClick = { open = !open })
        }
        if (open) {
            InfoNote(topic)
        }
    }
}

/**
 * The glyph that opens [topic]'s explanation. It reads out as the question it answers, and as
 * expanded or collapsed with the explanation.
 *
 * It stays quiet beside its label, an outlined glyph in the ink with no fill of its own, so a
 * section's name leads and the button only shows up when looked for. It keeps the press and the
 * focus ring every skin's buttons show.
 *
 * @param[topic] What it explains.
 * @param[expanded] Whether the explanation is showing.
 * @param[onClick] Called when it is pressed, to show or hide the explanation.
 * @param[modifier] Applied to the button.
 */
@Composable
internal fun InfoButton(
    topic: InfoTopic,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // b-523
    val question = stringResource(topic.question)
    BuilderTooltip(text = question, modifier = modifier) {
        BuilderPressable(
            onClick = onClick,
            label = question,
            modifier = Modifier.foldedExpandedName(question, expanded),
            shape = CircleShape,
        ) {
            InfoGlyph(Modifier.padding(InfoGlyphRoom).size(InfoGlyphSize))
        }
    }
}

/**
 * An info glyph drawn as an outline in the ink, a ring with the letter i in it.
 */
@Composable
private fun InfoGlyph(modifier: Modifier = Modifier) {
    val ink = LocalBuilderTokens.current.textStrong
    Canvas(modifier) {
        val unit = size.minDimension / GLYPH_GRID
        val stroke = GLYPH_STROKE * unit
        val middle = center.x
        drawCircle(color = ink, radius = GLYPH_RING * unit, style = Stroke(width = stroke))
        drawLine(
            color = ink,
            start = Offset(middle, GLYPH_STEM_TOP * unit),
            end = Offset(middle, GLYPH_STEM_BOTTOM * unit),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(color = ink, radius = stroke * GLYPH_DOT, center = Offset(middle, GLYPH_DOT_Y * unit))
    }
}

/**
 * The glyph's size, small enough to sit beside a section's eyebrow.
 */
private val InfoGlyphSize = 16.dp

/**
 * The room around the glyph inside its press target, which the focus ring follows.
 */
private val InfoGlyphRoom = 4.dp

// The glyph is laid out on a 24 unit grid, the way icon sets draw theirs.
private const val GLYPH_GRID = 24f
private const val GLYPH_STROKE = 2f
private const val GLYPH_RING = 10f
private const val GLYPH_STEM_TOP = 11f
private const val GLYPH_STEM_BOTTOM = 16f
private const val GLYPH_DOT_Y = 7.75f
private const val GLYPH_DOT = 0.65f

/**
 * [topic]'s short explanation.
 */
@Composable
internal fun InfoNote(
    topic: InfoTopic,
    modifier: Modifier = Modifier,
) {
    BuilderText(text = stringResource(topic.explanation), modifier = modifier)
}
