package com.materialkolor.sample.customtheme

import androidx.compose.runtime.Composable
import com.materialkolor.sample.customtheme.ui.SampleApp
import com.materialkolor.sample.testing.SampleAppContract

class CustomThemeSampleAppTest : SampleAppContract() {
    @Composable
    override fun Content() {
        SampleApp()
    }
}
