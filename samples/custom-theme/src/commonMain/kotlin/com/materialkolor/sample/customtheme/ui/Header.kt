package com.materialkolor.sample.customtheme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.AppType
import com.materialkolor.sample.customtheme.ui.component.GroupLabel
import com.materialkolor.sample.customtheme.ui.component.OverprintText
import com.materialkolor.sample.customtheme.ui.component.SeedSwatch
import com.materialkolor.sample.customtheme.ui.component.SegmentedControl
import com.materialkolor.sample.customtheme.ui.component.TabStrip
import com.materialkolor.sample.customtheme.ui.component.Text
import com.materialkolor.sample.customtheme.ui.component.halftone
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.state.SampleState
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.ui.SampleCopy
import kotlin.math.hypot

@Composable
internal fun Header(
    state: SampleState,
    dispatch: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40).dp, y = 46.dp)
                .size(380.dp)
                .halftone(color = colors.yellow, colors = colors, shape = CircleShape, cell = 11.dp) { x, y ->
                    val distance = hypot(x - 0.5f, y - 0.5f) / 0.5f
                    SUN_CORE - distance * (SUN_CORE - SUN_EDGE)
                },
        )

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${SampleCopy.label(state.seed)} edition".uppercase(),
                    style = AppType.Caption,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )

                LabeledGroup(label = SampleCopy.modeGroup) {
                    SegmentedControl(
                        options = ThemeMode.entries,
                        selected = state.mode,
                        onSelect = { mode -> dispatch(SampleAction.SelectMode(mode)) },
                        label = SampleCopy::label,
                    )
                }
            }

            OverprintText(
                text = SampleCopy.appTitle.uppercase(),
                style = AppType.Masthead,
                first = colors.primary,
                second = colors.pink,
                shift = MastheadShift,
                modifier = Modifier.padding(top = 4.dp),
            )

            LabeledGroup(label = SampleCopy.seedGroup) {
                Row {
                    for (seed in SampleSeed.entries) {
                        SeedSwatch(
                            color = seed.color,
                            isSelected = seed == state.seed,
                            onClick = { dispatch(SampleAction.SelectSeed(seed)) },
                        )
                    }
                }
            }

            TabStrip(
                tabs = AppSection.entries,
                selected = state.section,
                onSelect = { section -> dispatch(SampleAction.SelectSection(section)) },
                label = SampleCopy::label,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun LabeledGroup(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupLabel(text = label)

        content()
    }
}

private val MastheadShift = DpOffset(9.dp, 6.dp)

private const val SUN_CORE = 1f
private const val SUN_EDGE = 0.06f
