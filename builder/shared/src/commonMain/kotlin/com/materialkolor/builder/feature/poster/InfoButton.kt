package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
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
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.foldedExpandedName
import com.materialkolor.builder.kit.icon.IconId
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
 * @param[end] What sits at the far end of the label's line, such as a readout. The explanation
 * opens under the whole line, so it never squeezes what sits here.
 */
@Composable
internal fun InfoLabel(
    label: String,
    topic: InfoTopic,
    modifier: Modifier = Modifier,
    end: @Composable RowScope.() -> Unit = {},
) {
    val spacing = LocalBuilderTokens.current.spacing
    var open by rememberSaveable(topic) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(label)
            InfoButton(topic = topic, expanded = open, onClick = { open = !open })
            end()
        }
        if (open) {
            InfoNote(topic)
        }
    }
}

/**
 * A section's eyebrow, its name set in capitals the way the poster draws them. It reads out as
 * [label] in its own case, so a screen reader says the word rather than spelling it.
 */
@Composable
internal fun Eyebrow(
    label: String,
    modifier: Modifier = Modifier,
) {
    BuilderText(
        text = label.uppercase(),
        modifier = modifier.clearAndSetSemantics { text = AnnotatedString(label) },
        style = BuilderTextStyle.SectionLabel,
        maxLines = 1,
    )
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
    val question = stringResource(topic.question)
    BuilderTooltip(text = question, modifier = modifier) {
        BuilderPressable(
            onClick = onClick,
            label = question,
            modifier = Modifier.foldedExpandedName(question, expanded),
            shape = CircleShape,
        ) {
            // The skin's own outlined info glyph, in the ink.
            BuilderIcon(
                id = IconId.InfoOutline,
                contentDescription = null,
                modifier = Modifier.padding(InfoGlyphRoom),
                tint = LocalBuilderTokens.current.textStrong,
                size = InfoGlyphSize,
            )
        }
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
