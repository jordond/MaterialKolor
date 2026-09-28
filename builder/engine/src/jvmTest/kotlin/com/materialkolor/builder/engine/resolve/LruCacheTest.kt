package com.materialkolor.builder.engine.resolve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LruCacheTest {
    @Test
    fun getOrPut_hit_returnsTheCachedValueWithoutCreating() {
        val cache = LruCache<String, Any>(maxSize = 2)
        val first = cache.getOrPut("a") { Any() }

        val second = cache.getOrPut("a") { error("a hit must not build a value") }

        assertSame(first, second)
    }

    @Test
    fun getOrPut_overCapacity_evictsTheLeastRecentlyUsed() {
        val cache = LruCache<String, Int>(maxSize = 2)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        cache.getOrPut("a") { 0 }

        cache.getOrPut("c") { 3 }

        assertEquals(2, cache.size)
        assertTrue("a" in cache)
        assertFalse("b" in cache)
        assertTrue("c" in cache)
    }

    @Test
    fun contains_cachedKey_doesNotCountAsAUse() {
        val cache = LruCache<String, Int>(maxSize = 2)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        assertTrue("a" in cache)

        cache.getOrPut("c") { 3 }

        assertFalse("a" in cache)
        assertTrue("b" in cache)
        assertTrue("c" in cache)
    }

    @Test
    fun getOrPut_evictedKey_buildsAFreshValue() {
        val cache = LruCache<String, Any>(maxSize = 1)
        val first = cache.getOrPut("a") { Any() }
        cache.getOrPut("b") { Any() }

        val second = cache.getOrPut("a") { Any() }

        assertNotSame(first, second)
        assertEquals(1, cache.size)
    }

    @Test
    fun init_noRoom_isRejected() {
        assertFailsWith<IllegalArgumentException> { LruCache<String, Int>(maxSize = 0) }
    }
}
