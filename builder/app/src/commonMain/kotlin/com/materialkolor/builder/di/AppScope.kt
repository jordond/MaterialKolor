package com.materialkolor.builder.di

/**
 * The scope of the one app graph. Anything bound `@SingleIn(AppScope::class)` lives as long as the
 * builder does.
 */
internal abstract class AppScope private constructor()
