package com.materialkolor.builder.ui.ktx

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

fun SnackbarHostState.launch(
    scope: CoroutineScope,
    message: StringResource,
    duration: SnackbarDuration = SnackbarDuration.Long,
) {
    scope.launch { show(message, duration) }
}

fun SnackbarHostState.launch(
    scope: CoroutineScope,
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Long,
) {
    scope.launch { show(message, duration) }
}

suspend fun SnackbarHostState.show(
    message: StringResource,
    duration: SnackbarDuration = SnackbarDuration.Long,
) {
    show(getString(message), duration = duration)
}

suspend fun SnackbarHostState.show(
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Long,
) {
    currentSnackbarData?.dismiss()
    showSnackbar(message, duration = duration, withDismissAction = true)
}
