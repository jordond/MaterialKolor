package com.materialkolor.builder.ui.home.preview.device.components.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.persistentListOf
import materialkolorbuilder.app.generated.resources.Res
import materialkolorbuilder.app.generated.resources.monstera_details
import org.jetbrains.compose.resources.painterResource

@Immutable
private data class Details(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val details = persistentListOf(
    Details(
        icon = Icons.Filled.Stars,
        title = "Most Popular",
        description = "A popular plant in the community",
    ),
    Details(
        icon = Icons.Filled.Park,
        title = "Faux Available",
        description = "Get the look of the real thing without maintenance",
    ),
    Details(
        icon = Icons.AutoMirrored.Filled.Assignment,
        title = "Easy Care",
        description = "This plant is easy to care for",
    ),
)

private val care = persistentListOf(
    Details(
        icon = Icons.Filled.Water,
        title = "Water",
        description = "Water every 1-2 weeks",
    ),
    Details(
        icon = Icons.Filled.Eco,
        title = "Feed",
        description = "Feed once monthly",
    ),
    Details(
        icon = Icons.Filled.BrightnessHigh,
        title = "Sun",
        description = "Moderate light",
    ),
)

@Composable
fun PhoneDetailsScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            TopBar()

            Text(
                text = "Monstera Siltepecana",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )

            Image(
                painter = painterResource(Res.drawable.monstera_details),
                contentDescription = "Monstera Siltepecana",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(270.dp)
                    .height(250.dp)
                    .clip(RoundedCornerShape(75.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(75.dp),
                    ),
            )
        }

        DetailsList()

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Care",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
            )

            care.forEach { details ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = details.icon,
                        contentDescription = details.title,
                    )

                    Text(
                        text = details.description,
                        fontWeight = FontWeight.Light,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")

            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
        }
    }
}

@Composable
private fun DetailsList(modifier: Modifier = Modifier) {
    LazyRow(
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(details) { detail ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier.size(125.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Icon(
                        imageVector = detail.icon,
                        contentDescription = detail.title,
                        modifier = Modifier.size(12.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = PhoneFont,
                    )

                    Text(
                        text = detail.description,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Light,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
