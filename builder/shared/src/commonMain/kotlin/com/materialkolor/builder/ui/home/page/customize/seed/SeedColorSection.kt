package com.materialkolor.builder.ui.home.page.customize.seed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.core.UrlLink
import com.materialkolor.builder.settings.model.ImagePresets
import com.materialkolor.builder.settings.model.SeedImage
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.home.components.ColorCard
import com.materialkolor.builder.ui.theme.LocalUrlLauncher
import kotlinx.collections.immutable.PersistentList

@Composable
fun SeedColorSection(
    settings: Settings,
    onSelectImage: (SeedImage.Resource?) -> Unit,
    openColorPicker: () -> Unit,
    onRandomColor: () -> Unit,
    imagePresets: PersistentList<SeedImage.Resource> = ImagePresets.all,
    modifier: Modifier = Modifier,
    processingImage: Boolean = false,
) {
    val urlLauncher = LocalUrlLauncher.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = "Seed color",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "The seed color will be used to generate the Material 3 color scheme. " +
                "You can also select one of these images or upload your own. The dominant color" +
                " will be extracted from the image and used as the seed color.",
        )

        Text(
            text = "Learn more about dynamic colors.",
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.clickable { urlLauncher.launch(UrlLink.DynamicColorDocs) },
        )

        SeedImageRow(
            imagePresets = imagePresets,
            selectedImage = settings.selectedImage,
            onSelectImage = onSelectImage,
            processingImage = processingImage,
        )

        ColorCard(
            color = settings.colors.seed,
            title = "Seed color",
            subtitle = "The seed color will be used to generate the Material 3 color scheme.",
            onClick = openColorPicker,
            trailingIcon = {
                IconButton(onClick = onRandomColor) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Random color")
                }
            },
        )
    }
}
