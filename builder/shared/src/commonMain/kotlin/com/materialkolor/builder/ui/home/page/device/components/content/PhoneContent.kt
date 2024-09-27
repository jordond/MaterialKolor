package com.materialkolor.builder.ui.home.page.device.components.content

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class PhoneContentScreen {
    Home,
    Details,
}

@Composable
fun PhoneContent(
    screen: PhoneContentScreen,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(
        typography = PhoneTypography,
    ) {
        when (screen) {
            PhoneContentScreen.Home -> PhoneHomeScreen(modifier = modifier)
            PhoneContentScreen.Details -> PhoneDetailsScreen(modifier = modifier)
        }
    }
}
