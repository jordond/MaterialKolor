package com.materialkolor.builder.ui.home.preview.device.components.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.materialkolor.builder.ui.home.preview.device.components.frame.PhoneFrameDefaults.androidAspectRatio
import com.materialkolor.builder.ui.home.preview.device.components.frame.status.AndroidStatusBar
import com.materialkolor.builder.ui.home.preview.device.components.frame.status.IPhoneStatusBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

object PhotoFrameScope

@Composable
fun IosPhoneFrame(
    modifier: Modifier = Modifier,
    size: PhoneFrameSize = PhoneFrameDefaults.size(),
    framePadding: PhoneFramePadding = PhoneFrameDefaults.framePaddingIos(),
    autoUpdateTime: Boolean = true,
    content: @Composable () -> Unit,
) {
    PhoneFrame(
        framePadding = framePadding,
        modifier = modifier,
        size = size,
        autoUpdateTime = autoUpdateTime,
        statusBar = { IPhoneStatusBar() },
        content = content,
    )
}

@Composable
fun AndroidPhoneFrame(
    modifier: Modifier = Modifier,
    size: PhoneFrameSize = PhoneFrameDefaults.size(aspectRatio = androidAspectRatio),
    framePadding: PhoneFramePadding = PhoneFrameDefaults.androidFramePadding(),
    autoUpdateTime: Boolean = true,
    content: @Composable () -> Unit,
) {
    PhoneFrame(
        framePadding = framePadding,
        modifier = modifier,
        size = size,
        autoUpdateTime = autoUpdateTime,
        statusBar = { AndroidStatusBar() },
        content = content,
    )
}

@Composable
fun PhoneFrame(
    framePadding: PhoneFramePadding,
    modifier: Modifier = Modifier,
    size: PhoneFrameSize = PhoneFrameDefaults.size(),
    autoUpdateTime: Boolean = true,
    statusBar: @Composable PhotoFrameScope.(Long) -> Unit,
    content: @Composable () -> Unit,
) {
    var time by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        if (!autoUpdateTime) return@LaunchedEffect

        while (isActive) {
            if (!autoUpdateTime) break
            time = Clock.System.now().toEpochMilliseconds()
            delay(60.seconds)
        }
    }

    Box(
        modifier = modifier
            .height(size.height)
            .aspectRatio(size.aspectRatio)
            .clip(RoundedCornerShape(framePadding.outer))
            .background(MaterialTheme.colorScheme.outlineVariant)
            .padding(framePadding.thickness),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(framePadding.inner),
        ) {
            Column {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    statusBar(PhotoFrameScope, time)
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    content()
                }
            }
        }
    }
}
