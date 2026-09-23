package com.materialkolor.builder.domain.link

import kotlin.test.Test
import kotlin.test.assertEquals

class RoutePathTest {
    @Test
    fun parse_root_isHome() {
        assertEquals(Route.Home, RoutePath.parse(path = "/", query = ""))
        assertEquals(Route.Home, RoutePath.parse(path = "", query = ""))
        assertEquals(Route.Home, RoutePath.parse(path = "/", query = "?"))
    }

    @Test
    fun parse_rootWithAQueryTheOldBuilderNeverWrote_isHome() {
        assertEquals(Route.Home, RoutePath.parse(path = "/", query = "?ref=producthunt"))
        assertEquals(Route.Home, RoutePath.parse(path = "/", query = "?utm_source=mastodon&ref"))
    }

    @Test
    fun parse_rootWithAnOldBuilderKey_isLegacyWithoutTheQuestionMark() {
        assertEquals(
            Route.Legacy("color_seed=FF6750A4&dark_mode=false"),
            RoutePath.parse(path = "/", query = "?color_seed=FF6750A4&dark_mode=false"),
        )
        assertEquals(Route.Legacy("ref=x&is_amoled=true"), RoutePath.parse(path = "", query = "ref=x&is_amoled=true"))
    }

    @Test
    fun parse_themePath_isThemeWithItsCode() {
        assertEquals(Route.Theme("AdllO0AAAAD1"), RoutePath.parse(path = "/t/AdllO0AAAAD1", query = ""))
        assertEquals(Route.Theme("AdllO0AAAAD1"), RoutePath.parse(path = "/t/AdllO0AAAAD1/", query = ""))
    }

    @Test
    fun parse_themePathWithAnOldBuilderQuery_isStillTheme() {
        assertEquals(Route.Theme("abc"), RoutePath.parse(path = "/t/abc", query = "?color_seed=FF6750A4"))
    }

    @Test
    fun parse_themePathWithoutASingleCode_isUnknown() {
        listOf("/t/", "/t", "/t//", "/t/abc/def").forEach { path ->
            assertEquals(Route.Unknown(path), RoutePath.parse(path = path, query = ""), path)
        }
    }

    @Test
    fun parse_anyOtherPath_isUnknown() {
        listOf("/settings", "/theme/abc", "/T/abc", "//").forEach { path ->
            assertEquals(Route.Unknown(path), RoutePath.parse(path = path, query = "?color_seed=FF6750A4"), path)
        }
    }
}
