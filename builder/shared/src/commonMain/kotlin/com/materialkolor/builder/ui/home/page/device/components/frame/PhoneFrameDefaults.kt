package com.materialkolor.builder.ui.home.page.device.components.frame

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PhoneFrameSize(
    val height: Dp,
    val aspectRatio: Float,
)

data class PhoneFramePadding(
    val outer: Dp,
    val inner: Dp,
    val thickness: Dp,
)

object PhoneFrameDefaults {

    val height = 800.dp
    val aspectRatio = 9f / 19.5f
    val androidAspectRatio = 9f / 18.5f

    private val androidOuterPadding = 20.dp
    private val androidInnerPadding = 10.dp
    private val androidThickness = 10.dp
    private val iosOuterPadding = 40.dp
    private val iosInnerPadding = 30.dp
    private val iosThickness = 10.dp

    fun size(
        height: Dp = this.height,
        aspectRatio: Float = this.aspectRatio,
    ) = PhoneFrameSize(height, aspectRatio)

    fun framePaddingIos(
        outer: Dp = iosOuterPadding,
        inner: Dp = iosInnerPadding,
        thickness: Dp = iosThickness,
    ) = PhoneFramePadding(outer, inner, thickness)

    fun androidFramePadding(
        outer: Dp = androidOuterPadding,
        inner: Dp = androidInnerPadding,
        thickness: Dp = androidThickness,
    ) = PhoneFramePadding(outer, inner, thickness)
}