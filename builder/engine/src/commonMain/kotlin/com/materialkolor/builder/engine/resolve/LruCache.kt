package com.materialkolor.builder.engine.resolve

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A small least recently used cache that is safe to share between threads.
 *
 * The entries live in one ordered persistent map behind an atomic reference, oldest first. A hit
 * moves its entry to the back and a miss appends one, dropping from the front once the cache is
 * over [maxSize]. Every change swaps the whole map, so readers never see one half written.
 *
 * Two threads that miss on the same key at once may both run the factory, but only the first
 * answer is stored and both callers get that one back. That keeps the promise the resolver makes,
 * the same key gives the same instance.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class LruCache<K : Any, V : Any>(
    private val maxSize: Int,
) {
    init {
        require(maxSize > 0) { "An LRU cache needs room for at least one entry, got $maxSize" }
    }

    private val entries = AtomicReference<PersistentMap<K, V>>(persistentMapOf())

    /** How many entries the cache holds right now. */
    val size: Int
        get() = entries.load().size

    /** Whether [key] is cached, without counting as a use. */
    operator fun contains(key: K): Boolean = key in entries.load()

    /**
     * The value cached for [key], or the one [create] builds when there is none.
     */
    fun getOrPut(
        key: K,
        create: () -> V,
    ): V {
        touch(key)?.let { cached -> return cached }
        val created = create()
        while (true) {
            val current = entries.load()
            val existing = current[key]
            val next = if (existing != null) current.moveToBack(key, existing) else current.putting(key, created).trim()
            if (entries.compareAndSet(current, next)) return existing ?: created
        }
    }

    private fun touch(key: K): V? {
        while (true) {
            val current = entries.load()
            val value = current[key] ?: return null
            if (entries.compareAndSet(current, current.moveToBack(key, value))) return value
        }
    }

    private fun PersistentMap<K, V>.moveToBack(
        key: K,
        value: V,
    ): PersistentMap<K, V> = removing(key).putting(key, value)

    private fun PersistentMap<K, V>.trim(): PersistentMap<K, V> {
        var map = this
        while (map.size > maxSize) {
            map = map.removing(map.keys.first())
        }
        return map
    }
}
