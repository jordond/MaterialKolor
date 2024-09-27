package com.materialkolor.builder.ui.home.page.customize.seed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.settings.model.SeedImage
import com.materialkolor.builder.ui.ktx.conditional
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.compose.resources.painterResource

@Composable
fun SeedImageRow(
    imagePresets: PersistentList<SeedImage.Resource>,
    selectedImage: SeedImage?,
    onSelectImage: (SeedImage.Resource?) -> Unit,
    processingImage: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        imagePresets.forEachIndexed { index, image ->
            ImageBox(
                isSelected = image.id == selectedImage?.id,
                onSelectImage = { onSelectImage(image) },
                modifier = Modifier.align(Alignment.CenterVertically),
            ) {
                Image(
                    painter = painterResource(image.drawableResource),
                    contentDescription = "Preset image #${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        ImageBox(
            isSelected = selectedImage is SeedImage.Custom,
            onSelectImage = { onSelectImage(null) },
            showBorder = true,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                selectedImage is SeedImage.Custom -> {
                    Image(
                        bitmap = selectedImage.image,
                        contentDescription = "Uploaded image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                processingImage -> {
                    CircularProgressIndicator()
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Upload custom image",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize(0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ImageBox(
    isSelected: Boolean,
    onSelectImage: () -> Unit,
    modifier: Modifier = Modifier,
    showBorder: Boolean = isSelected,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else if (isHovered) 0.9f else 0.8f,
    )

    val shape =
        if (isSelected) MaterialTheme.shapes.small
        else MaterialTheme.shapes.medium

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .hoverable(interactionSource)
            .weight(1f)
            .aspectRatio(1f)
            .scale(scale)
            .clip(shape)
            .conditional(showBorder) {
                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
            }
            .clickable(onClick = onSelectImage),
    ) {
        content()

        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize(0.3f)
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxSize(0.6f)
                        .aspectRatio(1f),
                )
            }
        }
    }
}
