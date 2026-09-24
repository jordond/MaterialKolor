package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.ProjectIndex
import com.materialkolor.builder.domain.persist.StorageKeys
import com.materialkolor.builder.web.interop.e2eHooksWanted
import com.materialkolor.builder.web.interop.exposeE2eHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Hooks the Playwright specs in `builder/e2e` drive the browser services through, while the app is
// still a placeholder with no UI that reaches them. They exist only when the spec asks for them.
//
// The scope is unconfined, so a hook that updates a store gets its answer before it returns, and a
// collector counts an event before the browser handler that sent it has finished.

private val hookScope = CoroutineScope(Dispatchers.Unconfined)

/** Hang the router's hooks on the page when a spec opened it. */
internal fun Router.exposeToE2e() {
    if (!e2eHooksWanted()) return
    var pops = 0
    hookScope.launch { overlayPops.collect { pops++ } }
    exposeE2eHook("route") { initial.toString() }
    exposeE2eAction("replaceHome") { replaceHome() }
    exposeE2eAction("pushOverlay") { id -> pushOverlay(id) }
    exposeE2eAction("popOverlay") { popOverlay() }
    exposeE2eHook("overlayPops") { pops.toString() }
}

/**
 * Hang hooks for the preferences and project index records on the page when a spec opened it.
 *
 * Quarantine reports are left to the app, their one collector. A spec reads what the user sees
 * instead, the newer data banner or the set aside toast.
 */
internal fun StoreFactory.exposeToE2e() {
    if (!e2eHooksWanted()) return
    val prefs = create(StorageKeys.PREFS, Preferences.Codec, Preferences())
    val index = create(StorageKeys.INDEX, ProjectIndex.Codec, ProjectIndex())
    val external = mutableListOf<String>()
    var seen = ""
    hookScope.launch { externalChanges.collect { key -> external += key.toString() } }
    hookScope.launch { prefs.data.collect { value -> seen = value.dismissedHints.sorted().joinToString(",") } }
    exposeE2eHook("addHint") { hint ->
        var outcome = "Pending"
        hookScope.launch {
            val error = prefs.update { value -> value.copy(dismissedHints = value.dismissedHints + hint) }
            outcome = error?.name ?: "Done"
        }
        outcome
    }
    exposeE2eHook("readHints") {
        var hints = "Pending"
        hookScope.launch {
            hints = prefs
                .get()
                .dismissedHints
                .sorted()
                .joinToString(",")
        }
        hints
    }
    exposeE2eHook("seenHints") { seen }
    exposeE2eHook("readIndex") {
        var projects = "Pending"
        hookScope.launch {
            projects = index
                .get()
                .projects.size
                .toString()
        }
        projects
    }
    exposeE2eHook("touchIndex") {
        var outcome = "Pending"
        hookScope.launch { outcome = index.update { value -> value }?.name ?: "Done" }
        outcome
    }
    exposeE2eHook("externalChanges") { external.joinToString(",") }
}

/** Hang the environment's hooks on the page when a spec opened it. */
internal fun Environment.exposeToE2e() {
    if (!e2eHooksWanted()) return
    var hides = 0
    var persisted = "Pending"
    hookScope.launch { pageHides.collect { hides++ } }
    exposeE2eHook("media") {
        "dark=${prefersDark.value} reducedMotion=${reducedMotion.value} coarse=${coarsePointer.value}"
    }
    exposeE2eHook("tabId") { tabId }
    exposeE2eHook("storageAvailable") { storageAvailable.toString() }
    exposeE2eHook("eyeDropperAvailable") { eyeDropperAvailable.toString() }
    exposeE2eHook("pageHides") { hides.toString() }
    exposeE2eAction("hideSplash") { hideSplash() }
    exposeE2eAction("setThemeColor") { hex -> setThemeColor(Argb.fromHex(hex)) }
    exposeE2eAction("writeSplashColors") { hexes ->
        val (light, dark) = hexes.split(",").map(Argb::fromHex)
        writeSplashColors(light, dark)
    }
    exposeE2eHook("readTabProject") { readTabProject().orEmpty() }
    exposeE2eAction("writeTabProject") { id -> writeTabProject(id.ifEmpty { null }) }
    exposeE2eHook("requestPersist") {
        hookScope.launch { persisted = requestPersist().toString() }
        persisted
    }
    exposeE2eHook("persisted") { persisted }
}

/** A hook that does something and has nothing to say back. */
private fun exposeE2eAction(
    name: String,
    action: (String) -> Unit,
) = exposeE2eHook(name) { argument ->
    action(argument)
    ""
}
