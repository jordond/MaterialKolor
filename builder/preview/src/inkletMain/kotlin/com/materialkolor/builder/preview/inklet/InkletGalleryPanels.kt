package com.materialkolor.builder.preview.inklet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plane
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import com.materialkolor.builder.preview.material.GalleryComponent
import com.materialkolor.builder.preview.material.Gap
import com.materialkolor.builder.preview.material.PaneGap
import com.materialkolor.builder.preview.material.previewRoles
import dev.ggoggam.inklet.InkletDecoration
import dev.ggoggam.inklet.material3.InkletCard
import dev.ggoggam.inklet.material3.InkletCircularProgressIndicator
import dev.ggoggam.inklet.material3.InkletDivider
import dev.ggoggam.inklet.material3.InkletLinearProgressIndicator
import dev.ggoggam.inklet.material3.InkletTabIndicator
import dev.ggoggam.inklet.material3.InkletTabRibbonIndicator
import dev.ggoggam.inklet.material3.inkletBorder
import dev.ggoggam.inklet.material3.inkletDecoration
import dev.ggoggam.inklet.material3.inkletSurface

// The samples of the Containment, Navigation and Feedback cards of InkletCards. None of them has a
// disabled look in Inklet, so each shows its default beside a copy on other roles of the scheme.

private val TabLabels = listOf("Flights", "Hotels", "Cars")

/**
 * How far along the determinate progress samples are.
 */
private const val DemoProgress = 0.6f

/**
 * A card on Inklet's defaults and one on the primary container.
 */
@Composable
internal fun Cards() {
    val colors = MaterialTheme.colorScheme
    PanelPair {
        InkletCard(Modifier.weight(1f).previewRoles(InkletComponent.Card), seed = CardSeed) { CardText("12 to 19 May") }
        InkletCard(
            modifier = Modifier
                .weight(1f)
                .previewRoles(Role.PrimaryContainer, Role.OnPrimaryContainer, Role.Primary),
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            ink = colors.primary,
            seed = CardSeed + 1,
        ) { CardText("Next up") }
    }
}

@Composable
internal fun Dividers() {
    Column(verticalArrangement = Arrangement.spacedBy(Gap / 2)) {
        Text("Lisbon")
        InkletDivider(Modifier.previewRoles(InkletComponent.Divider), seed = DividerSeed)
        Text("Kyoto")
        InkletDivider(
            modifier = Modifier.previewRoles(Role.Outline),
            color = MaterialTheme.colorScheme.outline,
            seed = DividerSeed + 1,
        )
        Text("Oslo")
    }
}

/**
 * `inkletSurface` and `inkletBorder` on plain boxes, the pen Inklet lends any container.
 */
@Composable
internal fun Surfaces() {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
        PanelPair {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .inkletSurface(seed = SurfaceSeed)
                    .previewRoles(InkletComponent.Surface),
            ) { PanelLabel("Surface") }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .inkletSurface(
                        containerColor = colors.tertiaryContainer,
                        ink = colors.tertiary,
                        scribble = true,
                        seed = SurfaceSeed + 1,
                    ).previewRoles(Role.TertiaryContainer, Role.OnTertiaryContainer, Role.Tertiary),
            ) { PanelLabel("Scribbled", colors.onTertiaryContainer) }
        }
        Box(Modifier.fillMaxWidth().inkletBorder(seed = SurfaceSeed + 2).previewRoles(InkletComponent.Border)) {
            PanelLabel("Border only")
        }
    }
}

/**
 * An underline, a highlight and a circle, each around one label.
 */
@Composable
internal fun Decorations() {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(PaneGap), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Lisbon",
            modifier = Modifier
                .inkletDecoration(InkletDecoration.Underline, seed = DecorationSeed)
                .previewRoles(Role.Primary, Role.OnSurface)
                .padding(Gap / 2),
        )
        Text(
            text = "Kyoto",
            modifier = Modifier
                .inkletDecoration(InkletDecoration.Highlight, colors.tertiary, seed = DecorationSeed + 1)
                .previewRoles(Role.Tertiary, Role.OnSurface)
                .padding(Gap / 2),
        )
        Text(
            text = "Oslo",
            modifier = Modifier
                .inkletDecoration(InkletDecoration.Circle, colors.secondary, seed = DecorationSeed + 2)
                .previewRoles(Role.Secondary, Role.OnSurface)
                .padding(horizontal = PaneGap, vertical = Gap),
        )
    }
}

/**
 * Material's primary tab row twice, once over Inklet's underline and divider and once over its
 * ribbon. Material gives tabs no disabled look, so every tab is enabled.
 */
@Composable
internal fun Tabs(state: DemoAppState) {
    val underlined = state.tabIndex.coerceIn(0, TabLabels.lastIndex)
    val ribboned = state.choice(RibbonKey, TabLabels.size)
    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
        PrimaryTabRow(
            selectedTabIndex = underlined,
            modifier = Modifier.previewRoles(GalleryComponent.TabRow),
            indicator = {
                InkletTabIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(underlined, matchContentSize = true)
                        .previewRoles(InkletComponent.TabIndicator),
                    seed = TabSeed,
                )
            },
            divider = { InkletDivider(Modifier.previewRoles(InkletComponent.Divider), seed = TabSeed + 1) },
        ) {
            TabLabels.forEachIndexed { index, label ->
                InkletTab(label, selected = index == underlined) { state.tabIndex = index }
            }
        }
        PrimaryTabRow(
            selectedTabIndex = ribboned,
            modifier = Modifier.previewRoles(Role.Surface, Role.Primary),
            indicator = {
                InkletTabRibbonIndicator(
                    selectedTabIndex = ribboned,
                    modifier = Modifier.previewRoles(InkletComponent.TabIndicator),
                    seed = TabSeed + 2,
                )
            },
            divider = {},
        ) {
            TabLabels.forEachIndexed { index, label ->
                InkletTab(label, selected = index == ribboned) { state.choose(RibbonKey, TabLabels.size, index) }
            }
        }
    }
}

@Composable
internal fun LinearProgress() {
    Column(verticalArrangement = Arrangement.spacedBy(PaneGap)) {
        InkletLinearProgressIndicator(
            progress = { DemoProgress },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Packing" }
                .previewRoles(InkletComponent.Progress),
            seed = ProgressSeed,
        )
        InkletLinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Loading" }
                .previewRoles(InkletComponent.Progress),
            seed = ProgressSeed + 1,
        )
    }
}

@Composable
internal fun CircularProgress() {
    Row(horizontalArrangement = Arrangement.spacedBy(PaneGap)) {
        InkletCircularProgressIndicator(
            progress = { DemoProgress },
            modifier = Modifier
                .semantics { contentDescription = "Packing" }
                .previewRoles(InkletComponent.Progress),
            seed = ProgressSeed + 2,
        )
        // The indeterminate circle leaves its track clear, so only Primary shows.
        InkletCircularProgressIndicator(
            modifier = Modifier
                .semantics { contentDescription = "Loading" }
                .previewRoles(Role.Primary),
            seed = ProgressSeed + 3,
        )
    }
}

private const val RibbonKey = "gallery.inklet.ribbon"

// The seed of each sample's first sketch, after those of InkletGallery.kt.
private const val CardSeed = 300
private const val DividerSeed = 310
private const val SurfaceSeed = 320
private const val DecorationSeed = 330
private const val TabSeed = 340
private const val ProgressSeed = 350

@Composable
private fun InkletTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.previewRoles(GalleryComponent.Tab),
        text = { Text(label) },
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Two panels sharing a row.
 */
@Composable
private fun PanelPair(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Gap), content = content)
}

@Composable
private fun CardText(dates: String) {
    Column(Modifier.padding(PaneGap), verticalArrangement = Arrangement.spacedBy(Gap / 2)) {
        Icon(Lucide.Plane, contentDescription = null)
        Text("Lisbon", style = MaterialTheme.typography.titleSmall)
        Text(dates, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PanelLabel(
    text: String,
    color: Color = Color.Unspecified,
) {
    Text(text, Modifier.padding(PaneGap), color = color, style = MaterialTheme.typography.labelLarge)
}
