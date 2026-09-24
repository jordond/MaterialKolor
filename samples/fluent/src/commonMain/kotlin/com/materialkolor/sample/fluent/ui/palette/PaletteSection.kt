package com.materialkolor.sample.fluent.ui.palette

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.toHex
import com.materialkolor.sample.fluent.theme.SampleTheme
import com.materialkolor.sample.fluent.ui.component.SectionCard
import com.materialkolor.sample.fluent.ui.component.SectionLabel
import com.materialkolor.sample.fluent.ui.component.TagChip
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text

/**
 * The Palette section. What material-kolor-fluent made of [seed], where Fluent puts it, and what Fluent would
 * have done on its own.
 *
 * @param[seed] The seed the theme is generated from.
 * @param[modifier] The modifier for the section.
 */
@Composable
internal fun PaletteSection(
    seed: SampleSeed,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SampleTags.Palette),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PaletteCard(
            title = "Accent ramp",
            caption = "The seven shades Fluent themes from, generated from ${SampleCopy.label(seed)} " +
                "${seed.color.toHex()} by rememberFluentColors.",
        ) {
            ShadeRamp(shades = FluentTheme.colors.shades, labelsFrom = SampleTheme.colors.accentTarget)
        }

        PaletteCard(
            title = "Where Fluent puts it",
            caption = "Fluent derives its accent tokens from those shades, and picks different ones in light " +
                "and dark.",
        ) {
            AccentTokens()
        }

        PaletteCard(
            title = "Tag accents",
            caption = "Personal wears the accent. Work and Errand turn its hue a third and two thirds of the way " +
                "round, then go through toFluentShades like the accent does.",
        ) {
            TagRamps()
        }

        PaletteCard(
            title = "Without the adapter",
            caption = "Fluent's own generateShades looks the seed up in a table with one entry, so every seed " +
                "comes back as Windows blue.",
        ) {
            WithoutAdapter(seed = seed.color)
        }
    }
}

@Composable
private fun PaletteCard(
    title: String,
    caption: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel(text = title)
            Text(
                text = caption,
                color = FluentTheme.colors.text.text.secondary,
            )
        }
        content()
    }
}

@Composable
private fun TagRamps() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (tag in TaskTag.entries) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TagChip(
                    tag = tag,
                    modifier = Modifier.width(88.dp),
                )
                ShadeRamp(
                    shades = SampleTheme.colors.shades(tag),
                    height = 24.dp,
                )
            }
        }
    }
}
