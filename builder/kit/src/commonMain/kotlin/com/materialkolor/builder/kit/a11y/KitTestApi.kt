package com.materialkolor.builder.kit.a11y

/**
 * Marks kit API that only tests call, such as [ProvideWebFoldsForTest].
 *
 * A call needs an opt in, and without one it does not compile. A test opts in to this marker where
 * it calls such API. Product code has no reason to, since what such API turns on the kit already
 * turns on by itself where it applies.
 */
@RequiresOptIn(
    message = "Only tests call this. Product code gets the same from the kit where it applies.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class KitTestApi
