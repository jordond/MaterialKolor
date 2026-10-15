package com.materialkolor.convention.mcu

/**
 * Format constants and shape rules for the reviewed MCU source lock.
 */
internal object McuLockSchema {
    /**
     * Layout of the lock file itself. Bump together with SourceLock.kt.
     */
    const val SCHEMA_VERSION = 1

    /**
     * Transformation policy the lock was reviewed against. Bump together with SourceLock.kt.
     */
    const val POLICY_VERSION = 1

    /**
     * Upstream revisions are recorded as full lowercase Git object names.
     */
    const val REVISION_PATTERN = "[0-9a-f]{40}"

    /**
     * Content digests are recorded as lowercase SHA-256 hex.
     */
    const val HASH_PATTERN = "[0-9a-f]{64}"

    /**
     * Locked sources are Kotlin files one directory below the upstream Kotlin tree.
     */
    const val SOURCE_PATH_PATTERN = """[a-z]+/[A-Za-z][A-Za-z0-9]*\.kt"""
}
