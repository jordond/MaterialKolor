package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
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
    PosterIconButton(
        icon = IconId.Info,
        description = stringResource(topic.question),
        onClick = onClick,
        modifier = modifier,
        expanded = expanded,
    )
}

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
