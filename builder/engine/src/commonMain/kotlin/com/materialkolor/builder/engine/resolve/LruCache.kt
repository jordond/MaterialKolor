package com.materialkolor.builder.engine.resolve

/**
 * A small least recently used cache for a single thread.
 *
 * The entries live in one insertion ordered map, oldest first. A hit moves its entry to the back
 * and a miss appends one, dropping from the front once the cache is over [maxSize]. Nothing here
 * is synchronized, because the values it holds are not safe to share between threads either.
 */
internal class LruCache<K : Any, V : Any>(
    private val maxSize: Int,
) {
    init {
        require(maxSize > 0) { "An LRU cache needs room for at least one entry, got $maxSize" }
    }

    private val entries = LinkedHashMap<K, V>()

    /**
     * How many entries the cache holds right now.
     */
    val size: Int
        get() = entries.size

    /**
     * Whether [key] is cached, without counting as a use.
     */
    operator fun contains(key: K): Boolean = key in entries

    /**
     * The value cached for [key], or the one [create] builds when there is none.
     */
    fun getOrPut(
        key: K,
        create: () -> V,
    ): V {
        entries.remove(key)?.let { cached ->
            entries[key] = cached
            return cached
        }
        val created = create()
        entries[key] = created
        while (entries.size > maxSize) {
            entries.remove(entries.keys.first())
        }
        return created
    }
}
