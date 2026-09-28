package com.materialkolor.builder.di

import dev.zacsweers.metro.Qualifier

/**
 * The dispatcher a model moves a heavy fold onto.
 *
 * It is injected rather than named at the `flowOn` so a test can hand in its own scheduler. On wasm
 * it is the main thread anyway.
 */
@Qualifier
internal annotation class Computation
