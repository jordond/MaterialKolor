package com.materialkolor.builder.feature.command

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-315a
class PaletteSearchTest {
    private val entries = listOf(
        entry("motionScheme.Expressive", "Motion scheme, Expressive", "Target options"),
        entry("style.TonalSpot", "Use style TonalSpot", "Style"),
        entry("style.Vibrant", "Use style Vibrant", "Style"),
        entry("shuffle", "Shuffle", "Seed", words = listOf("random", "surprise me")),
        entry("tab.Contrast", "Show the Contrast tab", "Preview"),
        entry("section.Contrast", "Go to Contrast", "Poster", onlyWhenAsked = true),
    )

    @Test
    fun tsp_findsTonalSpotFirst() {
        searchPalette(entries, "tsp", recents = emptyList()).first().id shouldBe "style.TonalSpot"
    }

    @Test
    fun emptySearch_listsRecentsFirstAndLeavesOutWhatIsOnlyListedWhenAsked() {
        val rows = searchPalette(entries, "", recents = listOf("shuffle", "style.Vibrant"))

        rows.map { row -> row.id } shouldBe listOf(
            "shuffle",
            "style.Vibrant",
            "motionScheme.Expressive",
            "style.TonalSpot",
            "tab.Contrast",
        )
    }

    // b-511
    @Test
    fun groups_leadWithRecents_thenEachCategoryWhereItsBestRowStood() {
        val rows = searchPalette(entries, "", recents = listOf("shuffle", "style.Vibrant"))

        val groups = paletteGroups(rows, recents = listOf("shuffle", "style.Vibrant"), recentTitle = "Recent")
        val ranked = paletteGroups(searchPalette(entries, "contrast", emptyList()), emptyList(), "Recent")

        groups.map { group -> group.title to group.entries.map { entry -> entry.id } } shouldBe listOf(
            "Recent" to listOf("shuffle", "style.Vibrant"),
            "Target options" to listOf("motionScheme.Expressive"),
            "Style" to listOf("style.TonalSpot"),
            "Preview" to listOf("tab.Contrast"),
        )
        ranked.flatMap { group -> group.entries }.first().id shouldBe searchPalette(entries, "contrast", emptyList()).first().id
    }

    // b-511
    @Test
    fun keycapChords_splitChordsAtCommasAndKeysAtPlusSigns() {
        keycapChords("Ctrl+Shift+Z, Ctrl+Y") shouldBe listOf(listOf("Ctrl", "Shift", "Z"), listOf("Ctrl", "Y"))
        keycapChords("?") shouldBe listOf(listOf("?"))
    }

    @Test
    fun search_findsByOtherWordsAndCategory_andDropsWhatDoesNotMatch() {
        val surprise = searchPalette(entries, "surprise", recents = emptyList())
        val poster = searchPalette(entries, "poster", recents = emptyList())

        surprise.map { row -> row.id } shouldBe listOf("shuffle")
        poster.map { row -> row.id } shouldBe listOf("section.Contrast")
        searchPalette(entries, "zzz", recents = emptyList()) shouldBe emptyList()
    }

    @Test
    fun equalMatches_putARecentFirst_thenTheShorterLabel() {
        searchPalette(entries, "contrast", recents = emptyList()).map { row -> row.id } shouldBe
            listOf("section.Contrast", "tab.Contrast")
        searchPalette(entries, "contrast", recents = listOf("tab.Contrast")).map { row -> row.id } shouldBe
            listOf("tab.Contrast", "section.Contrast")
    }

    @Test
    fun fuzzyScore_prefersWordStartsAndRunsOfLetters() {
        fuzzyScore("tsp", "Show surfaceTint").shouldBeNull()
        val humps = fuzzyScore("tsp", "TonalSpot").shouldNotBeNull()
        val scattered = fuzzyScore("tsp", "the useful paper").shouldNotBeNull()
        (humps > scattered) shouldBe true
        fuzzyScore("PRIMARY container", "primaryContainer").shouldNotBeNull()
        fuzzyScore("", "anything") shouldBe 0
    }

    private fun entry(
        id: String,
        label: String,
        category: String,
        words: List<String> = emptyList(),
        onlyWhenAsked: Boolean = false,
    ) = PaletteEntry(
        id = id,
        label = label,
        category = category,
        keys = null,
        state = CommandState.Enabled,
        selected = null,
        words = words,
        onlyWhenAsked = onlyWhenAsked,
        run = {},
    )
}
