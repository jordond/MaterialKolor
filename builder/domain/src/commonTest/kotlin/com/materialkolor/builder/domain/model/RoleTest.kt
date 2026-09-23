package com.materialkolor.builder.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun role_codes_matchTheDeclarationOrderTheyWereGivenIn() {
        assertEquals(Role.entries.indices.toList(), Role.entries.map { role -> role.code })
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
}
