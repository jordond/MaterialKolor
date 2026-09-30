package com.materialkolor.builder.preview.fluent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.preview.GalleryHarness
import com.materialkolor.builder.preview.ShellExpressive
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.gallerySnapshot
import com.materialkolor.builder.preview.split.PaneSpec

/**
 * The switches the gallery keeps in [DemoAppState].
 */
private val GallerySwitches: List<String> =
    listOf(FluentGalleryKeys.Bold, FluentGalleryKeys.Wifi, FluentGalleryKeys.Details)

/**
 * The single choices the gallery keeps in [DemoAppState], each a switch per option.
 */
private val GalleryChoices: List<String> = listOf(
    FluentGalleryKeys.Volume,
    FluentGalleryKeys.Delivery,
    FluentGalleryKeys.View,
    FluentGalleryKeys.Folder,
    FluentGalleryKeys.Tab,
    FluentGalleryKeys.Page,
)

/**
 * The boxes the gallery keeps in [DemoAppState].
 */
private val GalleryChecks: List<String> = listOf(FluentGalleryKeys.Updates, FluentGalleryKeys.Understood)

/**
 * Everything the Fluent gallery keeps in [DemoAppState], to tell whether anything changed.
 */
internal fun DemoAppState.fluentGallerySnapshot(): List<Any> =
    gallerySnapshot(GalleryChoices, GallerySwitches, GalleryChecks)

/**
 * The Fluent gallery in a Fluent pane of [spec], under the shell chrome, with motion frozen. With
 * [webFolds] the kit's fold modifiers fold state into names as they do on the web.
 */
@Composable
internal fun FluentGalleryHarness(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
    composed: MutableSet<String>? = null,
    webFolds: Boolean = false,
) = GalleryHarness(spec, state, modifier, composed, expressive = ShellExpressive, webFolds = webFolds)
