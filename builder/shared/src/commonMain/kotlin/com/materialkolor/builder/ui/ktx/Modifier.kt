package com.materialkolor.builder.ui.ktx

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun Modifier.debugBorder(
    color: Color = Color.Red,
    width: Dp = 1.dp,
) = border(width, color = color)

@Composable
fun Modifier.conditional(
    condition: Boolean,
    block: @Composable () -> Modifier,
): Modifier = if (condition) composed { then(block()) } else this

@Composable
fun <T> Modifier.whenNotNull(
    value: T?,
    block: @Composable (T) -> Modifier,
): Modifier = if (value != null) composed { then(block(value)) } else this

@Composable
fun Modifier.then(block: Modifier.() -> Modifier): Modifier = then(block(this)).clickable { }

@Composable
fun Modifier.clickable(
    coroutineScope: CoroutineScope,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: suspend () -> Unit,
): Modifier =
    clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = { coroutineScope.launch { onClick() } },
    )

fun Modifier.nullableClip(shape: Shape?) = if (shape != null) clip(shape) else this

fun Modifier.clickableWithoutRipple(onClick: () -> Unit) =
    composed(
        factory = {
            this.then(
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onClick() },
                ),
            )
        },
    )
