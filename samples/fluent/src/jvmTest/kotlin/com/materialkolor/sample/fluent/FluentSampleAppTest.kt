package com.materialkolor.sample.fluent

import androidx.compose.runtime.Composable
import com.materialkolor.sample.fluent.ui.FluentSampleApp
import com.materialkolor.sample.testing.SampleAppContract

class FluentSampleAppTest : SampleAppContract() {
    @Composable
    override fun Content() {
        FluentSampleApp()
    }
}
