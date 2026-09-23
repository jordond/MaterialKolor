package com.materialkolor.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll

/**
 * Owns the `viewModelScope` of every model a test builds, because a model built bare is never
 * cleared. `ViewModelStore.clear()` is the public way to reach `ViewModel.clear()`.
 *
 * Clearing only asks a fold to stop. One that hops off Main is done only once its thread has resumed
 * it on Main, and a resume that lands during or after `resetMain()` throws into whichever test runs
 * next. So [clearAndJoin] waits for every scope, and a test calls it at the end of its `runTest`
 * body while the test's Main is still installed. There is no blocking way out, because on wasm a
 * nested `runTest` returns before its body has run and the wait would be skipped.
 */
internal class ViewModelHarness {
    private val store = ViewModelStore()
    private val owned = mutableListOf<ViewModel>()

    /** Hand [model] to the harness so it is cleared with the rest. */
    fun <T : ViewModel> own(model: T): T {
        store.put("model-${owned.size}", model)
        owned += model
        return model
    }

    /** Clear every model and wait for their scopes, from inside a `runTest` body. */
    suspend fun clearAndJoin() {
        val scopes = owned.map { model -> model.viewModelScope.coroutineContext.job }
        store.clear()
        scopes.joinAll()
    }
}
