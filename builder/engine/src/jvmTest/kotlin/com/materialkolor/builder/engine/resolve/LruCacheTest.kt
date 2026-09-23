package com.materialkolor.builder.engine.resolve

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    fun getOrPut_racingMisses_allGetTheSameInstance() {
        val cache = LruCache<String, Any>(maxSize = 4)
        val threads = 8
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        val answers = List(threads) {
            pool.submit<Any> {
                start.await()
                cache.getOrPut("shared") { Any() }
            }
        }

        start.countDown()
        val values = answers.map { answer -> answer.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(1, values.distinct().size)
        assertSame(values.first(), cache.getOrPut("shared") { Any() })
    }

    @Test
    fun init_noRoom_isRejected() {
        assertFailsWith<IllegalArgumentException> { LruCache<String, Int>(maxSize = 0) }
    }
}
