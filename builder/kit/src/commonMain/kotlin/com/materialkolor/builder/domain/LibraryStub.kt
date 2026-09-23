package com.materialkolor.builder.domain

/**
 * Stands in for the real `Library` until B-102 lands the domain model.
 *
 * It sits in the domain package on purpose. When B-102 merges, deleting this file is the whole fix
 * and no import in the kit changes. If someone forgets, the redeclaration stops the build and says
 * exactly where.
 */
public enum class Library {
    Material3,
    Unstyled,
    Fluent,
    Custom,
}
