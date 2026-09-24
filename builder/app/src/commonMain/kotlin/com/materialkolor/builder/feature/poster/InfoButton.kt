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
import androidx.compose.ui.platform.LocalUriHandler
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.glossary_accents
import com.materialkolor.builder.generated.resources.glossary_accents_question
import com.materialkolor.builder.generated.resources.glossary_contrast
import com.materialkolor.builder.generated.resources.glossary_contrast_question
import com.materialkolor.builder.generated.resources.glossary_docs
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
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Where the builder's docs live. Each topic has a page of its own under it. */
private const val DOCS_BUILDER_URL = "https://docs.materialkolor.com/builder"

/**
 * The poster sections an info button explains (F-36).
 *
 * @property[slug] The topic's page under the builder docs.
 */
internal enum class InfoTopic(
    val slug: String,
) {
    Seed("seed"),
    Style("style"),
    Spec("spec"),
    Contrast("contrast"),
    KeyColors("key-colors"),
    Pins("pins"),
    Accents("accents"),
    Targets("targets"),
    ;

    /** The docs page that says more about this topic. */
    val docsUrl: String
        get() = "$DOCS_BUILDER_URL/$slug"

    /** What the info button asks, read out and shown as its tooltip. */
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

    /** The short answer the info button opens. */
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
 * open. The explanation sits in the column rather than a popup, so it scrolls with the poster and
 * its docs link is one more stop for Tab.
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
            InfoButton(topic = topic, onClick = { open = !open })
        }
        if (open) {
            InfoNote(topic)
        }
    }
}

/**
 * The glyph that opens [topic]'s explanation. It reads out as the question it answers.
 *
 * @param[topic] What it explains.
 * @param[onClick] Called when it is pressed, to show or hide the explanation.
 * @param[modifier] Applied to the button.
 */
@Composable
internal fun InfoButton(
    topic: InfoTopic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PosterIconButton(
        icon = IconId.Info,
        description = stringResource(topic.question),
        onClick = onClick,
        modifier = modifier,
    )
}

/** [topic]'s short explanation and a link to its docs page. */
@Composable
internal fun InfoNote(
    topic: InfoTopic,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val spacing = LocalBuilderTokens.current.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        BuilderText(text = stringResource(topic.explanation))
        BuilderButton(
            onClick = { uriHandler.openUri(topic.docsUrl) },
            label = stringResource(Res.string.glossary_docs),
            emphasis = Emphasis.Subtle,
            icon = IconId.ExternalLink,
        )
    }
}
