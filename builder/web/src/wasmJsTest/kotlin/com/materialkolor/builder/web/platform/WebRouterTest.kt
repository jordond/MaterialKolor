package com.materialkolor.builder.web.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

// b-315d

/**
 * The router against a history whose moves the test lands, holds back or drops.
 */
class WebRouterTest {
    @Test
    fun aPushWhileABackIsOnItsWay_landsOnceTheBackDoes() {
        val history = FakeHistory()
        val router = WebRouter(history)
        val pops = router.countPops()

        router.pushOverlay("Palette")
        router.popOverlay()
        router.pushOverlay("Projects")
        assertEquals("Palette", history.current)

        history.land()

        assertEquals("Projects", history.current)
        assertEquals(listOf("/", "Projects"), history.ids)
        assertEquals(0, pops())
    }

    @Test
    fun aPushWhoseBackNeverMoves_landsAfterTheWait_andBackClosesIt() {
        val history = FakeHistory()
        val router = WebRouter(history)
        val pops = router.countPops()
        router.pushOverlay("Palette")
        router.popOverlay()
        router.pushOverlay("Projects")
        history.drop()

        history.runTimers()

        assertEquals("Projects", history.current)
        // Once a late move could no longer come, the user's back closes Projects, and the router then
        // goes back over the entry the lost back was to pop.
        history.runTimers()
        history.userBack()
        assertEquals(1, pops())
        history.land()
        assertEquals("/", history.current)
    }

    @Test
    fun aBackThatMovesAfterTheWait_closesNothing_andTheNextBackClosesTheOverlay() {
        val history = FakeHistory()
        val router = WebRouter(history)
        val pops = router.countPops()
        router.pushOverlay("Palette")
        router.popOverlay()
        router.pushOverlay("Projects")
        history.runTimers()

        history.land()

        assertEquals(0, pops())
        history.userBack()
        assertEquals(1, pops())
        assertEquals("/", history.current)
    }

    @Test
    fun aBackWhoseReportNeverCame_settlesFromTheEntrysDepth() {
        val history = FakeHistory()
        val router = WebRouter(history)
        val pops = router.countPops()
        router.pushOverlay("Palette")
        router.popOverlay()
        router.pushOverlay("Projects")

        history.land(report = false)
        history.runTimers()

        assertEquals(listOf("/", "Projects"), history.ids)
        assertEquals(0, pops())
    }

    private fun WebRouter.countPops(): () -> Int {
        var count = 0
        CoroutineScope(Dispatchers.Unconfined).launch { overlayPops.collect { count++ } }
        return { count }
    }
}

/**
 * A history of entries by overlay id, "/" for the page's own. A back waits until [land] moves it,
 * the way the browser moves later, and [later] waits until [runTimers].
 */
private class FakeHistory : HistoryPort {
    private val entries = mutableListOf("/" to 0)
    private var index = 0
    private val backs = ArrayDeque<Int>()
    private val timers = mutableListOf<() -> Unit>()
    private var listener: (Int) -> Unit = {}

    /**
     * The ids from the first entry to the current one.
     */
    val ids: List<String>
        get() = entries.take(index + 1).map { (id, _) -> id }

    val current: String
        get() = entries[index].first

    override fun path(): String = "/"

    override fun query(): String = ""

    override fun depth(): Int = entries[index].second

    override fun push(
        id: String,
        depth: Int,
    ): Boolean {
        while (entries.size > index + 1) entries.removeAt(entries.lastIndex)
        entries += id to depth
        index++
        return true
    }

    override fun back(steps: Int): Boolean {
        backs.addLast(steps)
        return true
    }

    override fun replaceUrl(url: String): Boolean = true

    override fun onPop(listener: (depth: Int) -> Unit) {
        this.listener = listener
    }

    override fun later(
        delayMs: Int,
        block: () -> Unit,
    ) {
        timers += block
    }

    /**
     * Moves the oldest back from wherever the history is now, and reports it unless not [report].
     */
    fun land(report: Boolean = true) {
        val steps = backs.removeFirst()
        index = (index - steps).coerceAtLeast(0)
        if (report) listener(depth())
    }

    /**
     * Loses the oldest back, the way a browser may.
     */
    fun drop() {
        backs.removeFirst()
    }

    /**
     * The back button, which moves at once.
     */
    fun userBack() {
        index--
        listener(depth())
    }

    fun runTimers() {
        val due = timers.toList()
        timers.clear()
        due.forEach { timer -> timer() }
    }
}
