package com.materialkolor.builder.domain.audit

import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SlotResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ContrastPairsTest {
    /** The libraries that read Material roles directly. */
    private val roleLibraries = listOf(Library.Material3, Library.Unstyled)

    /** Every library a document can pin roles and add accents on. */
    private val pinnableLibraries = listOf(Library.Material3, Library.Unstyled, Library.Custom)

    /** Roles nothing is drawn on and that need no contrast of their own. */
    private val unratedRoles = setOf(Role.Scrim, Role.SurfaceTint, Role.OutlineVariant)

    private val surfaceLevels = listOf(
        Role.Surface,
        Role.SurfaceDim,
        Role.SurfaceBright,
        Role.SurfaceContainerLowest,
        Role.SurfaceContainerLow,
        Role.SurfaceContainer,
        Role.SurfaceContainerHigh,
        Role.SurfaceContainerHighest,
    )

    private fun pairs(
        library: Library,
        accentCount: Int = 0,
        pinned: Set<Role> = emptySet(),
    ): List<ContrastPair> = ContrastPairs.forTarget(library = library, accentCount = accentCount, pinned = pinned)

    private fun text(
        foreground: Role,
        background: Role,
    ) = ContrastPair(ColorRef.OfRole(foreground), ColorRef.OfRole(background), PairKind.Text)

    private fun shape(
        foreground: Role,
        background: Role,
    ) = ContrastPair(ColorRef.OfRole(foreground), ColorRef.OfRole(background), PairKind.NonText)

    private fun slot(
        foreground: CustomSlot,
        background: CustomSlot,
        kind: PairKind,
    ) = ContrastPair(ColorRef.OfSlot(foreground), ColorRef.OfSlot(background), kind)

    private fun List<ContrastPair>.refs(): Set<ColorRef> =
        flatMapTo(mutableSetOf()) { pair -> listOf(pair.foreground, pair.background) }

    @Test
    fun forTarget_everyLibrary_isNotEmpty() {
        for (library in Library.entries) {
            assertTrue(pairs(library).isNotEmpty(), library.name)
        }
    }

    @Test
    fun forTarget_everyLibraryAndInput_neverRepeatsAPair() {
        for (library in Library.entries) {
            val list = pairs(library, accentCount = 3, pinned = Role.entries.toSet())
            assertEquals(list.size, list.toSet().size, library.name)
        }
    }

    @Test
    fun forTarget_negativeAccentCount_isRejected() {
        assertFailsWith<IllegalArgumentException> { pairs(Library.Material3, accentCount = -1) }
    }

    @Test
    fun forTarget_roleLibraries_rateOnXOverEveryAccentFamily() {
        val families = listOf(
            Role.OnPrimary to Role.Primary,
            Role.OnPrimaryContainer to Role.PrimaryContainer,
            Role.OnSecondary to Role.Secondary,
            Role.OnSecondaryContainer to Role.SecondaryContainer,
            Role.OnTertiary to Role.Tertiary,
            Role.OnTertiaryContainer to Role.TertiaryContainer,
            Role.OnError to Role.Error,
            Role.OnErrorContainer to Role.ErrorContainer,
        )
        for (library in roleLibraries) {
            val list = pairs(library)
            families.forEach { (on, background) ->
                assertTrue(text(on, background) in list, "$library $on over $background")
            }
        }
    }

    @Test
    fun forTarget_roleLibraries_rateBothOnColorsOverEveryFixedFamily() {
        val families = listOf(
            Triple(Role.OnPrimaryFixed, Role.OnPrimaryFixedVariant, listOf(Role.PrimaryFixed, Role.PrimaryFixedDim)),
            Triple(
                Role.OnSecondaryFixed,
                Role.OnSecondaryFixedVariant,
                listOf(Role.SecondaryFixed, Role.SecondaryFixedDim),
            ),
            Triple(
                Role.OnTertiaryFixed,
                Role.OnTertiaryFixedVariant,
                listOf(Role.TertiaryFixed, Role.TertiaryFixedDim),
            ),
        )
        for (library in roleLibraries) {
            val list = pairs(library)
            for ((on, variant, backgrounds) in families) {
                backgrounds.forEach { background ->
                    assertTrue(text(on, background) in list, "$library $on over $background")
                    assertTrue(text(variant, background) in list, "$library $variant over $background")
                }
            }
        }
    }

    @Test
    fun forTarget_roleLibraries_rateOnSurfaceAndOnSurfaceVariantOverEverySurfaceLevel() {
        for (library in roleLibraries) {
            val list = pairs(library)
            surfaceLevels.forEach { level ->
                assertTrue(text(Role.OnSurface, level) in list, "$library onSurface over $level")
                assertTrue(text(Role.OnSurfaceVariant, level) in list, "$library onSurfaceVariant over $level")
            }
            assertTrue(text(Role.OnBackground, Role.Background) in list, "$library onBackground")
            assertTrue(text(Role.OnSurfaceVariant, Role.SurfaceVariant) in list, "$library onSurfaceVariant")
        }
    }

    @Test
    fun forTarget_roleLibraries_rateTheInversePairs() {
        for (library in roleLibraries) {
            val list = pairs(library)
            assertTrue(text(Role.InverseOnSurface, Role.InverseSurface) in list, "$library inverseOnSurface")
            assertTrue(text(Role.InversePrimary, Role.InverseSurface) in list, "$library inversePrimary")
        }
    }

    @Test
    fun forTarget_roleLibraries_rateOutlineAsAShapeOverEverySurfaceLevel() {
        for (library in roleLibraries) {
            val list = pairs(library)
            surfaceLevels.forEach { level ->
                assertTrue(shape(Role.Outline, level) in list, "$library outline over $level")
            }
            assertTrue(
                list.none { pair ->
                    pair.foreground == ColorRef.OfRole(Role.Outline) &&
                        pair.kind == PairKind.Text
                },
            )
        }
    }

    @Test
    fun forTarget_custom_ratesABorderAndTheFocusRingAsShapes() {
        val list = pairs(Library.Custom)
        for (surface in listOf(CustomSlot.Surface, CustomSlot.SurfaceRaised, CustomSlot.SurfaceSunken)) {
            assertTrue(slot(CustomSlot.BorderStrong, surface, PairKind.NonText) in list, "border over $surface")
            assertTrue(slot(CustomSlot.FocusRing, surface, PairKind.NonText) in list, "focus ring over $surface")
            assertTrue(slot(CustomSlot.TextStrong, surface, PairKind.Text) in list, "strong text over $surface")
            assertTrue(slot(CustomSlot.TextMuted, surface, PairKind.Text) in list, "muted text over $surface")
            assertTrue(slot(CustomSlot.OnSurface, surface, PairKind.Text) in list, "onSurface over $surface")
        }
    }

    @Test
    fun forTarget_custom_ratesOnXOverEverySlotFamily() {
        val list = pairs(Library.Custom)
        val families = listOf(
            CustomSlot.OnPrimary to CustomSlot.Primary,
            CustomSlot.OnPrimary to CustomSlot.PrimaryPressed,
            CustomSlot.OnPrimary to CustomSlot.PrimaryRaised,
            CustomSlot.OnPrimaryContainer to CustomSlot.PrimaryContainer,
            CustomSlot.OnSecondary to CustomSlot.Secondary,
            CustomSlot.OnSecondaryContainer to CustomSlot.SecondaryContainer,
            CustomSlot.OnTertiary to CustomSlot.Tertiary,
            CustomSlot.OnTertiaryContainer to CustomSlot.TertiaryContainer,
            CustomSlot.OnError to CustomSlot.Error,
            CustomSlot.OnErrorContainer to CustomSlot.ErrorContainer,
            CustomSlot.OnSurfaceInverse to CustomSlot.SurfaceInverse,
        )
        families.forEach { (on, background) ->
            assertTrue(slot(on, background, PairKind.Text) in list, "$on over $background")
        }
    }

    @Test
    fun forTarget_customWithoutPinsOrAccents_namesOnlySlots() {
        val refs = pairs(Library.Custom).refs()

        assertTrue(refs.all { ref -> ref is ColorRef.OfSlot }, "Custom named something other than a slot, $refs")
    }

    @Test
    fun forTarget_everyLibraryButFluent_ratesEveryAccent() {
        for (library in pinnableLibraries) {
            val list = pairs(library, accentCount = 3)
            for (index in 0 until 3) {
                val onColor = ContrastPair(
                    foreground = ColorRef.OfAccent(AccentSlot(index, AccentPart.OnColor)),
                    background = ColorRef.OfAccent(AccentSlot(index, AccentPart.Color)),
                    kind = PairKind.Text,
                )
                val onContainer = ContrastPair(
                    foreground = ColorRef.OfAccent(AccentSlot(index, AccentPart.OnContainer)),
                    background = ColorRef.OfAccent(AccentSlot(index, AccentPart.Container)),
                    kind = PairKind.Text,
                )
                assertTrue(onColor in list, "$library accent $index color")
                assertTrue(onContainer in list, "$library accent $index container")
            }
            val accentIndices = list.refs().filterIsInstance<ColorRef.OfAccent>().mapTo(
                mutableSetOf(),
            ) { ref -> ref.slot.index }
            assertEquals(setOf(0, 1, 2), accentIndices, "$library rated an accent the document does not have")
        }
    }

    @Test
    fun forTarget_everyPinnableRole_isRatedWhenPinned() {
        for (library in pinnableLibraries) {
            for (role in Role.entries - unratedRoles) {
                val refs = pairs(library, pinned = setOf(role)).refs()
                val rated = ColorRef.OfRole(role) in refs ||
                    refs.any { ref -> ref is ColorRef.OfSlot && ref.slot.resolution == SlotResolution.FromRole(role) }
                assertTrue(rated, "$library pinned $role and the audit does not rate it")
            }
        }
    }

    @Test
    fun forTarget_customPinOnARoleNoSlotTakes_addsItsRolePairs() {
        val list = pairs(Library.Custom, pinned = setOf(Role.SurfaceContainerHigh))

        assertTrue(text(Role.OnSurface, Role.SurfaceContainerHigh) in list)
        assertTrue(text(Role.OnSurfaceVariant, Role.SurfaceContainerHigh) in list)
        assertTrue(shape(Role.Outline, Role.SurfaceContainerHigh) in list)
    }

    @Test
    fun forTarget_customPinOnARoleASlotTakes_addsNothing() {
        assertEquals(pairs(Library.Custom), pairs(Library.Custom, pinned = setOf(Role.Primary, Role.OnSurface)))
    }

    @Test
    fun forTarget_pinOnAnUnratedRole_addsNothing() {
        for (library in pinnableLibraries) {
            assertEquals(pairs(library), pairs(library, pinned = unratedRoles), library.name)
        }
    }

    @Test
    fun forTarget_fluent_ratesTheAccentFillAgainstEveryFixedTextColor() {
        val list = pairs(Library.Fluent)
        val expected = FluentText.entries.map { text ->
            ContrastPair(ColorRef.OfFluentText(text), ColorRef.OfRole(Role.Primary), PairKind.Text)
        }

        assertEquals(expected, list)
    }

    @Test
    fun forTarget_fluent_ignoresAccentsAndPins() {
        assertEquals(pairs(Library.Fluent), pairs(Library.Fluent, accentCount = 4, pinned = Role.entries.toSet()))
    }
}
