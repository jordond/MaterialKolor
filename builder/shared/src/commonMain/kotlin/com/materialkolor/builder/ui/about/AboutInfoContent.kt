package com.materialkolor.builder.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.core.UrlLink
import com.materialkolor.builder.ui.theme.LocalUrlLauncher

@Composable
fun AboutInfoContent() {
    SectionHeader("What is MaterialKolor Builder?")
    Paragraph(
        "MaterialKolor Builder is an interactive web application designed to help developers" +
            " and designers create Material 3 color schemes with ease. It leverages the power of" +
            " MaterialKolor, a Compose multiplatform library, to generate comprehensive color " +
            "palettes from a single seed color.",
    )

    SectionHeader("Key Features")
    BulletList(
        listOf(
            "Custom Seed Color: Choose your own seed color to generate a unique theme.",
            "Random Color Generation: Explore new color possibilities with a random color generator.",
            "Image Color Extraction: Pick a color directly from an uploaded image.",
            "Color Overrides: Fine-tune your theme by overriding key colors like secondary and tertiary.",
            "Live Preview: See your changes in real-time with our interactive preview.",
        ),
    )

    SectionHeader("How It Works")
    Paragraph(
        "MaterialKolor Builder is powered by MaterialKolor, a Kotlin port of Google's " +
            "material-color-utilities library. This ensures that all generated color schemes " +
            "adhere to Material 3 design principles and accessibility standards.",
    )
    NumberedList(
        listOf(
            "Select a seed color using one of our input methods.",
            "MaterialKolor processes this seed color to generate a full color scheme.",
            "Optionally override specific colors in the scheme.",
            "View and interact with your new theme in the live preview.",
        ),
    )

    SectionHeader("Learn More")
    BulletList(
        items = listOf(
            buildAnnotatedString {
                appendLink("MaterialKolor", UrlLink.MaterialKolor)
            },
            buildAnnotatedString {
                appendLink("material-color-utilities", UrlLink.MaterialColorUtilities)
            },
            buildAnnotatedString {
                appendLink("Material3 Documentation", UrlLink.Material3)
            },
        ),
    )

    SectionHeader(text = "Why Use MaterialKolor Builder?")
    Paragraph(
        text = "Whether you're a developer implementing Material 3 in your Compose application or" +
            " a designer exploring color options, MaterialKolor Builder streamlines the process" +
            " of creating beautiful, cohesive color schemes. It combines the precision of" +
            " programmatic color generation with the flexibility of manual adjustments, all" +
            " in an intuitive, visual interface.",
    )
    Paragraph(
        text = "Start exploring the world of Material 3 colors today with MaterialKolor Builder!",
    )
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun Paragraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun BulletList(items: List<Any>) {
    Column(
        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(text = "•", modifier = Modifier.padding(end = 8.dp))

                when (item) {
                    is String -> Text(item, style = MaterialTheme.typography.bodyMedium)
                    is AnnotatedString -> ClickableTextWithLinks(item)
                    else -> error("Unsupported item type: $item")
                }
            }
        }
    }
}

@Composable
fun NumberedList(items: List<String>) {
    Column(
        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(text = "${index + 1}.", modifier = Modifier.padding(end = 8.dp))
                Text(text = item, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun AnnotatedString.Builder.appendLink(text: String, url: UrlLink) {
    val startIndex = this.length
    append(text)
    addStyle(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Bold,
        ),
        start = startIndex,
        end = this.length,
    )
    addStringAnnotation(
        tag = "URL",
        annotation = url.value,
        start = startIndex,
        end = this.length,
    )
}

@Composable
fun ClickableTextWithLinks(
    text: AnnotatedString,
) {
    val urlLauncher = LocalUrlLauncher.current
    @Suppress("DEPRECATION")
    ClickableText(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        onClick = { offset ->
            text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    urlLauncher.launch(annotation.item)
                }
        },
    )
}
