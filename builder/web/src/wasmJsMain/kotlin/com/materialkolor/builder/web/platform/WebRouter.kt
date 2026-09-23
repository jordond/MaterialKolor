package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.link.RoutePath
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// stub
// B-301 wires the history API. For now the route is read once and overlays keep no history.
internal class WebRouter : Router {
    override val initial: Route = RoutePath.parse(window.location.pathname, window.location.search)
    override val overlayPops: Flow<Unit> = emptyFlow()

    override fun replaceHome() = Unit

    override fun pushOverlay(id: String) = Unit

    override fun popOverlay() = Unit
}
