package com.materialkolor.builder.domain.model

import com.materialkolor.builder.domain.color.Argb
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoleTest {
    /**
     * Every role the Material 3 module writes, in the order it writes them.
     *
     * Copied by hand from `animateColorScheme`, which is the one place that names all of them. If
     * this list and that function ever disagree, the module fills a role the builder cannot show.
     */
    private val material3Roles = listOf(
        "Primary",
        "OnPrimary",
        "PrimaryContainer",
        "OnPrimaryContainer",
        "InversePrimary",
        "Secondary",
        "OnSecondary",
        "SecondaryContainer",
        "OnSecondaryContainer",
        "Tertiary",
        "OnTertiary",
        "TertiaryContainer",
        "OnTertiaryContainer",
        "Background",
        "OnBackground",
        "Surface",
        "OnSurface",
        "SurfaceVariant",
        "OnSurfaceVariant",
        "SurfaceTint",
        "InverseSurface",
        "InverseOnSurface",
        "Error",
        "OnError",
        "ErrorContainer",
        "OnErrorContainer",
        "Outline",
        "OutlineVariant",
        "Scrim",
        "SurfaceBright",
        "SurfaceDim",
        "SurfaceContainer",
        "SurfaceContainerHigh",
        "SurfaceContainerHighest",
        "SurfaceContainerLow",
        "SurfaceContainerLowest",
        "PrimaryFixed",
        "PrimaryFixedDim",
        "OnPrimaryFixed",
        "OnPrimaryFixedVariant",
        "SecondaryFixed",
        "SecondaryFixedDim",
        "OnSecondaryFixed",
        "OnSecondaryFixedVariant",
        "TertiaryFixed",
        "TertiaryFixedDim",
        "OnTertiaryFixed",
        "OnTertiaryFixedVariant",
    )

    /**
     * The code the share codec writes for every role.
     *
     * Spelled out rather than derived, because a code is part of every link anyone has ever shared.
     * Reordering the entries or slotting a new one in the middle has to leave this table untouched,
     * and a test that reads the codes off the declaration order would quietly let them all move.
     */
    private val roleCodes = mapOf(
        "Primary" to 0,
        "OnPrimary" to 1,
        "PrimaryContainer" to 2,
        "OnPrimaryContainer" to 3,
        "InversePrimary" to 4,
        "Secondary" to 5,
        "OnSecondary" to 6,
        "SecondaryContainer" to 7,
        "OnSecondaryContainer" to 8,
        "Tertiary" to 9,
        "OnTertiary" to 10,
        "TertiaryContainer" to 11,
        "OnTertiaryContainer" to 12,
        "Background" to 13,
        "OnBackground" to 14,
        "Surface" to 15,
        "OnSurface" to 16,
        "SurfaceVariant" to 17,
        "OnSurfaceVariant" to 18,
        "SurfaceTint" to 19,
        "InverseSurface" to 20,
        "InverseOnSurface" to 21,
        "Error" to 22,
        "OnError" to 23,
        "ErrorContainer" to 24,
        "OnErrorContainer" to 25,
        "Outline" to 26,
        "OutlineVariant" to 27,
        "Scrim" to 28,
        "SurfaceBright" to 29,
        "SurfaceDim" to 30,
        "SurfaceContainer" to 31,
        "SurfaceContainerHigh" to 32,
        "SurfaceContainerHighest" to 33,
        "SurfaceContainerLow" to 34,
        "SurfaceContainerLowest" to 35,
        "PrimaryFixed" to 36,
        "PrimaryFixedDim" to 37,
        "OnPrimaryFixed" to 38,
        "OnPrimaryFixedVariant" to 39,
        "SecondaryFixed" to 40,
        "SecondaryFixedDim" to 41,
        "OnSecondaryFixed" to 42,
        "OnSecondaryFixedVariant" to 43,
        "TertiaryFixed" to 44,
        "TertiaryFixedDim" to 45,
        "OnTertiaryFixed" to 46,
        "OnTertiaryFixedVariant" to 47,
    )

    @Test
    fun role_entries_areEveryMaterial3RoleInOrder() {
        assertEquals(material3Roles, Role.entries.map { role -> role.name })
    }

    @Test
    fun role_entries_leaveOutThe2025DimAccents() {
        val dimAccents = listOf("PrimaryDim", "SecondaryDim", "TertiaryDim", "ErrorDim")
        val names = Role.entries.map { role -> role.name }

        dimAccents.forEach { dim ->
            assertTrue(dim !in names, "$dim is not part of a ColorScheme the builder hands back")
        }
    }

    @Test
    fun role_everyEntry_carriesTheCodeTheShareCodecWrites() {
        assertEquals(roleCodes.size, Role.entries.size, "A role was added or dropped without touching the table")

        Role.entries.forEach { role ->
            val expected = assertNotNull(roleCodes[role.name], "${role.name} is missing from the table")
            assertEquals(expected, role.code, "${role.name} carries a code no shared link knows about")
        }
    }

    @Test
    fun role_groups_holdEveryEntryExactlyOnce() {
        val grouped = RoleGroup.entries.flatMap { group -> Role.entries.filter { role -> role.group == group } }

        assertEquals(Role.entries.size, grouped.size)
        assertEquals(Role.entries.toSet(), grouped.toSet())
    }

    @Test
    fun role_onPair_pointsAtAContentRole() {
        Role.entries.mapNotNull { role -> role.onPair }.forEach { pair ->
            val isContent = pair.name.startsWith("On") || pair == Role.InverseOnSurface
            assertTrue(isContent, "${pair.name} is not a content role")
        }
    }

    @Test
    fun role_contentRoles_haveNoPairOfTheirOwn() {
        Role.entries.filter { role -> role.name.startsWith("On") }.forEach { role ->
            assertNull(role.onPair, "${role.name} is already a content role")
        }
    }

    @Test
    fun role_surfaceSteps_allPairWithOnSurface() {
        val steps = listOf(
            Role.SurfaceBright,
            Role.SurfaceDim,
            Role.SurfaceContainer,
            Role.SurfaceContainerHigh,
            Role.SurfaceContainerHighest,
            Role.SurfaceContainerLow,
            Role.SurfaceContainerLowest,
        )

        steps.forEach { step ->
            assertEquals(Role.OnSurface, step.onPair, "${step.name} reads under something else")
        }
    }

    @Test
    fun rolePin_withNeitherMode_isRejected() {
        assertFailsWith<IllegalArgumentException> { RolePin() }
    }

    @Test
    fun rolePin_decodedWithNeitherMode_isRejected() {
        val oneMode = """{"light": "#FF0000", "dark": null}"""
        val neitherMode = """{"light": null, "dark": null}"""

        // The one mode case decodes, so the empty one can only be the require block firing.
        assertEquals(RolePin(light = Argb(0x00FF0000)), Json.decodeFromString(RolePin.serializer(), oneMode))
        assertFailsWith<IllegalArgumentException> { Json.decodeFromString(RolePin.serializer(), neitherMode) }
    }
}
