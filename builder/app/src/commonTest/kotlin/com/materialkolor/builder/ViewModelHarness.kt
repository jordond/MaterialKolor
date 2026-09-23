package com.materialkolor.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.runTest

/**
 * Owns the `viewModelScope` of every model a test builds, because a model built bare is never
 * cleared. `ViewModelStore.clear()` is the public way to reach `ViewModel.clear()`.
 *
 * Clearing only asks a fold to stop. One that hops off Main is done only once its thread has resumed
 * it on Main, and a resume that lands during or after `resetMain()` throws into whichever test runs
 * next. So both ways out wait for every scope while the test's Main is still installed.
 * [clearAndJoin] is for inside a `runTest` body and [clear] is for `@AfterTest`, ahead of
 * `resetMain()`. [clear] runs its own `runTest`, because those resumes queue on Main's test
 * scheduler and nothing else runs it once the body has ended, so it must not be called from inside
 * one.
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

    /** Clear every model and wait for their scopes, from outside any `runTest`. */
    fun clear() {
        runTest { clearAndJoin() }
    }

    /** Clear every model and wait for their scopes. */
    suspend fun clearAndJoin() {
        val scopes = owned.map { model -> model.viewModelScope.coroutineContext.job }
        store.clear()
        scopes.joinAll()
    }
}
