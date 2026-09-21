package com.materialkolor

/**
 * Anything carrying this annotation is not part of the supported API. It can change shape or
 * disappear in any release, including a patch, and the migration guide will not mention it.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is internal MaterialKolor API. It can change or disappear in any release.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
@MustBeDocumented
public annotation class InternalMaterialKolorApi
