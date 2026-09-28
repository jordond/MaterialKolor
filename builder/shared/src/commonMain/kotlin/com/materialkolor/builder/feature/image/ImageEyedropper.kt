package com.materialkolor.builder.feature.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.image_eyedropper_cancel
import com.materialkolor.builder.generated.resources.image_eyedropper_line
import com.materialkolor.builder.generated.resources.image_eyedropper_open
import com.materialkolor.builder.generated.resources.image_eyedropper_title
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCard
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.SchemeChipFootprint
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor

/**
 * Tags the picture in the eyedropper, for tests to click a spot on it.
 */
internal const val EYEDROPPER_PICTURE_TAG: String = "image-eyedropper-picture"

/**
 * How wide the loupe is, before it rounds down to whole cells.
 */
private val LoupeDiameter = 112.dp

/**
 * How many of the picture's pixels the loupe spans across, an odd count so one sits in the middle.
 */
private const val LOUPE_PIXELS = 11

/**
 * The opaque alpha byte, which a picked seed always carries.
 */
private const val OPAQUE: Int = 0xFF shl 24

/**
 * The picture in the candidate row, in the room a chip takes, on a card that opens the image
 * eyedropper over [detail]. The card presses, rings and reads out the way the kit's other controls
 * do. The eyedropper is open while [openPanel] is `Panel.ImageEyedropper`, and it hands the focus
 * back here once it closes.
 *
 * @param[thumbnail] The picture the row shows.
 * @param[detail] The same picture at the most pixels there are of it, which the eyedropper shows.
 * @param[source] The seed source a pick keeps.
 */
@Composable
internal fun EyedropperThumbnail(
    thumbnail: ImageBitmap,
    detail: ImageBitmap,
    source: SeedSource,
    openPanel: Panel?,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val radius = LocalBuilderTokens.current.radius
    val focus = remember { FocusRequester() }
    BuilderCard(
        modifier = Modifier.focusRequester(focus),
        onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.ImageEyedropper)) },
    ) {
        Image(
            bitmap = thumbnail,
            // The card has no text of its own, so the picture names it.
            contentDescription = stringResource(Res.string.image_eyedropper_open),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(SchemeChipFootprint).clip(RoundedCornerShape(radius.small)),
        )
    }
    ImageEyedropper(
        visible = openPanel == Panel.ImageEyedropper,
        picture = detail,
        source = source,
        dispatcher = dispatcher,
        returnFocusTo = focus,
    )
}

/**
 * The image eyedropper, a dialog over [picture] with a loupe that follows the pointer.
 *
 * A click takes the exact color of the pixel under it as the seed, keeping [source], and closes the
 * dialog. Esc, Cancel and a click on the veil close it and change nothing. It only answers a
 * pointer, so it says the chips under the image are the way in for a keyboard.
 *
 * Should whatever shows it go away while it is open, it closes itself, so Back is never left with a
 * dialog nobody can see.
 *
 * @param[visible] Whether the dialog is open, while the workspace's panel is `Panel.ImageEyedropper`.
 * @param[picture] What to pick from, the image at up to 1024 px or a preset's picture.
 * @param[source] The seed source a pick keeps.
 * @param[returnFocusTo] The picture in the row, which gets the focus back once the dialog is gone.
 */
@Composable
internal fun ImageEyedropper(
    visible: Boolean,
    picture: ImageBitmap,
    source: SeedSource,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
) {
    val close = { dispatcher.dispatch(WorkspaceAction.ClosePanel) }
    val open by rememberUpdatedState(visible)
    DisposableEffect(dispatcher) {
        onDispose { if (open) dispatcher.dispatch(WorkspaceAction.ClosePanel) }
    }
    BuilderDialog(
        visible = visible,
        onDismissRequest = close,
        title = stringResource(Res.string.image_eyedropper_title),
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        actions = {
            BuilderButton(
                onClick = close,
                label = stringResource(Res.string.image_eyedropper_cancel),
                emphasis = Emphasis.Subtle,
            )
        },
    ) {
        // The picture takes the height the line and the buttons leave, so the buttons stay on screen.
        Column(
            modifier = Modifier.leaveRoomBelow(dialogButtonRoom()),
            verticalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.medium),
        ) {
            BuilderText(text = stringResource(Res.string.image_eyedropper_line), emphasis = Emphasis.Secondary)
            PixelPicker(picture) { pixel ->
                val change = DocumentChange.SetSeed(pixel, source)
                dispatcher.dispatch(WorkspaceAction.EditWithReveal(change, origin = null))
                close()
            }
        }
    }
}

/**
 * [picture] as wide as the dialog lets it be, and no taller than the height it is offered, with a
 * loupe over the spot under the pointer. A click hands [onPick] the pixel under it.
 */
@Composable
private fun PixelPicker(
    picture: ImageBitmap,
    onPick: (pixel: Argb) -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    var aim by remember(picture) { mutableStateOf<Offset?>(null) }
    val pick by rememberUpdatedState(onPick)
    Box(
        modifier = Modifier
            .aspectRatio(picture.width.toFloat() / picture.height)
            .testTag(EYEDROPPER_PICTURE_TAG)
            // Pointer only. The chips are the way in for a keyboard or a screen reader.
            .clearAndSetSemantics { }
            .pointerInput(picture) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        aim = if (event.type == PointerEventType.Exit) null else event.changes.first().position
                    }
                }
            }.pointerInput(picture) {
                detectTapGestures { offset -> pick(picture.pixelAt(pixelUnder(offset, size.toSize(), picture))) }
            }.clipToBounds()
            .drawWithContent {
                drawImage(picture, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                val spot = aim ?: return@drawWithContent
                drawLoupe(
                    picture,
                    spot,
                    ring = tokens.textStrong,
                    halo = tokens.panel,
                    stroke = tokens.highlightWidth.toPx(),
                )
            },
    )
}

/**
 * The pixel of [picture] under [offset], in a box of [size] that shows all of it.
 */
internal fun pixelUnder(
    offset: Offset,
    size: Size,
    picture: ImageBitmap,
): IntOffset {
    val x = floor(offset.x / size.width * picture.width).toInt().coerceIn(0, picture.width - 1)
    val y = floor(offset.y / size.height * picture.height).toInt().coerceIn(0, picture.height - 1)
    return IntOffset(x, y)
}

/**
 * The exact color of the pixel at [at], made opaque.
 */
private fun ImageBitmap.pixelAt(at: IntOffset): Argb {
    val color = toPixelMap(startX = at.x, startY = at.y, width = 1, height = 1)[0, 0]
    return Argb(color.toArgb() or OPAQUE)
}

/**
 * The pixels around [spot] blown up in a circle beside it, each a whole number of pixels wide and
 * drawn without smoothing so it reads as a square. The pixel a click takes sits in the middle,
 * ringed.
 */
private fun DrawScope.drawLoupe(
    picture: ImageBitmap,
    spot: Offset,
    ring: Color,
    halo: Color,
    stroke: Float,
) {
    val cell = floor(LoupeDiameter.toPx() / LOUPE_PIXELS).coerceAtLeast(1f)
    val radius = cell * LOUPE_PIXELS / 2
    val gap = radius + stroke * 2
    val center = if (spot.y - gap * 2 >= 0f) Offset(spot.x, spot.y - gap) else Offset(spot.x, spot.y + gap)
    val middle = pixelUnder(spot, size, picture)
    val circle = Path().apply { addOval(Rect(center, radius)) }
    clipPath(circle) {
        drawCircle(halo, radius, center)
        // The whole picture at the loupe's scale, moved so the middle pixel lands in the middle.
        translate(center.x - (middle.x + 0.5f) * cell, center.y - (middle.y + 0.5f) * cell) {
            drawImage(
                image = picture,
                dstSize = IntSize((picture.width * cell).toInt(), (picture.height * cell).toInt()),
                filterQuality = FilterQuality.None,
            )
        }
    }
    drawCircle(halo, radius, center, style = Stroke(stroke * 2))
    drawCircle(ring, radius, center, style = Stroke(stroke))
    drawRect(ring, Offset(center.x - cell / 2, center.y - cell / 2), Size(cell, cell), style = Stroke(stroke))
}
