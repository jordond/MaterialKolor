package com.materialkolor.sample.unstyled

import androidx.compose.runtime.Composable
import com.materialkolor.sample.testing.SampleAppContract
import com.materialkolor.sample.unstyled.ui.UnstyledSampleApp

/**
 * Runs the shared behaviour contract against the Compose Unstyled app, rendered the way `Main.kt` shows it.
 */
class UnstyledSampleAppTest : SampleAppContract() {
    @Composable
    override fun Content() {
        UnstyledSampleApp()
    }
}
