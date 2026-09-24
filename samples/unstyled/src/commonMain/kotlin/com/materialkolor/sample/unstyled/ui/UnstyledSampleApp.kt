package com.materialkolor.sample.unstyled.ui

import androidx.compose.runtime.Composable
import com.composeunstyled.Text
import com.materialkolor.sample.shared.ui.SampleCopy

/**
 * The Tasks app built on Compose Unstyled and themed by `material-kolor-unstyled`.
 *
 * Run it with `./gradlew :samples:unstyled:run`.
 */
@Composable
public fun UnstyledSampleApp() {
    Text(text = SampleCopy.appTitle)
}
