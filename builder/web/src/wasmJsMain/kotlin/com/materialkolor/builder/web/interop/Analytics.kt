@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop

// b-504

/**
 * Cloudflare Web Analytics, which sets no cookies and loads once the first frame is up (D13, PB-10).
 *
 * The token comes from the page. A site built with the Gradle property `builder.analyticsToken`, or
 * with `CF_WEB_ANALYTICS_TOKEN` in the environment when that is absent, carries it in `index.html` as
 * `<script type="application/json" id="mk-config">{"analyticsToken":"<token>"}</script>`. The token
 * is public, since the beacon sends it from every page. A page with no such tag or an empty token
 * never loads the beacon, which is how the dev page, the e2e run and the desktop build stay quiet.
 *
 * The beacon counts page loads only. Its single page mode is off, because the builder pushes a
 * history entry for every panel it opens and each of those would count as a visit.
 */
internal object Analytics {
    private var loaded = false

    /**
     * Add the beacon to the page once, when the page has a token. Later calls do nothing.
     */
    fun load() {
        if (loaded) return
        loaded = true
        val token = readAnalyticsToken() ?: return
        addBeacon(BEACON_URL, token)
    }
}

/**
 * The token in `#mk-config`, or null when the page has none, an empty one or unreadable JSON.
 */
private fun readAnalyticsToken(): String? =
    js(
        """{
        const config = document.getElementById('mk-config');
        if (!config) return null;
        try {
            const token = JSON.parse(config.textContent || '{}').analyticsToken;
            return typeof token === 'string' && token.length > 0 ? token : null;
        } catch (error) {
            return null;
        }
    }""",
    )

/**
 * Load the beacon at [url] with [token], deferred, the way Cloudflare's own snippet does.
 */
private fun addBeacon(
    url: String,
    token: String,
): Unit =
    js(
        """{
        const script = document.createElement('script');
        script.defer = true;
        script.src = url;
        script.setAttribute('data-cf-beacon', JSON.stringify({ token: token, spa: false }));
        document.head.appendChild(script);
    }""",
    )

/**
 * The script the site's content security policy lets in, next to `cloudflareinsights.com` for its reports.
 */
private const val BEACON_URL = "https://static.cloudflareinsights.com/beacon.min.js"
