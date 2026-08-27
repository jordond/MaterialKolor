package com.materialkolor.builder.ui.home.preview.device.components.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.ktx.hasEnoughContrast
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import materialkolorbuilder.app.generated.resources.Res
import materialkolorbuilder.app.generated.resources.cactus
import materialkolorbuilder.app.generated.resources.monstera
import materialkolorbuilder.app.generated.resources.snakegrass
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Stable
data class CardDetails(
    val room: String,
    val image: DrawableResource,
    val options: PersistentList<CardOption>,
)

@Stable
data class CardOption(
    val title: String,
    val description: String,
    val selected: Boolean = true,
)

private val Options = persistentListOf(
    CardDetails(
        room = "Living Room",
        image = Res.drawable.monstera,
        options = persistentListOf(
            CardOption(title = "Water", description = "hoya australis"),
            CardOption(title = "Feed", description = "monstera siltepecana"),
        ),
    ),
    CardDetails(
        room = "Kitchen",
        image = Res.drawable.snakegrass,
        options = persistentListOf(
            CardOption(title = "Water", description = "clinacanthus nutans"),
            CardOption(title = "Water", description = "hoya australis"),
        ),
    ),
    CardDetails(
        room = "Bedroom",
        image = Res.drawable.cactus,
        options = persistentListOf(
            CardOption(title = "Feed", description = "monstera siltepecana"),
            CardOption(title = "Water", description = "opuntia basilaris "),
        ),
    ),
)

@Composable
fun PhoneHomeScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxSize(),
    ) {
        Title(modifier = Modifier.padding(top = 18.dp))
        Spacer(modifier = Modifier.height(24.dp))
        DailyTip()
        Spacer(modifier = Modifier.height(16.dp))
        List()
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.weight(1f),
        )

        FilledIconButton(
            onClick = {},
            modifier = Modifier
                .width(48.dp)
                .height(24.dp),
        ) {
            Icon(Icons.Filled.Person, contentDescription = "Person")
        }
    }
}

@Composable
private fun DailyTip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(colorScheme.tertiaryContainer, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp)),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .padding(start = 8.dp, end = 12.dp)
                .fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = "Tip",
                tint = colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "During the winter your plants slow down and need less water",
                color = colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Light,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun List(
    modifier: Modifier = Modifier,
) {
    var options by remember { mutableStateOf(Options) }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        options.forEach { details ->
            ListItem(
                details = details,
                update = { option ->
                    val index = options.indexOfFirst { it.room == details.room }
                    val updated = details.options
                        .map { if (it.description == option.description) option else it }
                        .toPersistentList()

                    options = options.set(index, details.copy(options = updated))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ListItem(
    details: CardDetails,
    update: (CardOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.secondaryContainer,
        ),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 16.dp)
                    .weight(1f),
            ) {
                val hasEnoughContrast = colorScheme.primary
                    .hasEnoughContrast(colorScheme.secondaryContainer)

                val titleColor =
                    if (hasEnoughContrast) colorScheme.primary
                    else colorScheme.onSecondaryContainer

                Text(
                    text = details.room,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp,
                    color = titleColor,
                    modifier = Modifier.padding(start = 16.dp),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    details.options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .toggleable(
                                    value = option.selected,
                                    onValueChange = {
                                        update(option.copy(selected = it))
                                    },
                                    role = Role.Checkbox,
                                )
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .weight(1f),
                        ) {
                            Checkbox(
                                checked = option.selected,
                                onCheckedChange = null,
                            )

                            Column {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Light,
                                )
                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Light,
                                    fontStyle = FontStyle.Italic,
                                )
                            }
                        }
                    }
                }
            }

            Image(
                painter = painterResource(details.image),
                contentDescription = details.options.first().title,
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .fillMaxHeight(0.6f)
                    .padding(end = 8.dp),
            )
        }
    }
}
