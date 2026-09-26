package com.materialkolor.builder.domain.model

import com.materialkolor.builder.domain.color.ColorNames
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StarterSeedTest {
    private val names = STARTER_SEEDS.map(ColorNames::nameOf)

    @Test
    fun starterSeeds_eachHaveTheirOwnName_andLeaveTheDefaultOut() {
        assertEquals(names.size, names.toSet().size)
        assertFalse(ColorNames.nameOf(DEFAULT_SEED) in names)
    }

    @Test
    fun starterSeed_skipsTakenNames_andIsTheSameForTheSameRandom() {
        val taken = names.drop(1).toSet()

        repeat(5) { attempt -> assertEquals(STARTER_SEEDS.first(), starterSeed(taken, Random(attempt))) }
        assertEquals(starterSeed(emptySet(), Random(42)), starterSeed(emptySet(), Random(42)))
    }

    @Test
    fun starterSeed_withEveryNameTaken_picksFromAllOfThem() {
        assertTrue(starterSeed(names.toSet(), Random(7)) in STARTER_SEEDS)
    }
}
