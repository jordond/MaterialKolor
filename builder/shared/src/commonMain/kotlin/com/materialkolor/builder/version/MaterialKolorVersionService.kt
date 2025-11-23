package com.materialkolor.builder.version

import co.touchlab.kermit.Logger
import com.materialkolor.builder.BuildKonfig
import com.materialkolor.builder.core.DI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

interface MaterialKolorVersionService {
    val versions: StateFlow<MaterialKolorVersions>

    suspend fun fetchVersions()

    fun getVersion(usePrerelease: Boolean): String
}

class DefaultMaterialKolorVersionService(
    private val cache: VersionCache = DI.versionCache,
) : MaterialKolorVersionService {

    private val _versions = MutableStateFlow(fallbackVersions())
    override val versions: StateFlow<MaterialKolorVersions> = _versions.asStateFlow()

    private var hasFetched = false

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }
    }

    override suspend fun fetchVersions() {
        if (hasFetched) return
        hasFetched = true

        val cached = cache.get()
        if (cached != null) {
            Logger.d { "Using cached MaterialKolor versions: stable=${cached.stable}, prerelease=${cached.prerelease}" }
            _versions.value = cached.toVersions()
            return
        }

        try {
            Logger.d { "Fetching MaterialKolor versions from GitHub..." }
            val releases = client.get(GITHUB_RELEASES_URL).body<List<GitHubRelease>>()

            val stableRelease = releases.firstOrNull { !it.prerelease }
            val prereleaseRelease = releases.firstOrNull { it.prerelease }

            val stable = stableRelease?.tag_name?.removePrefix("v")
                ?: BuildKonfig.MATERIAL_KOLOR_VERSION
            val prerelease = prereleaseRelease?.tag_name?.removePrefix("v")

            val versions = MaterialKolorVersions(stable = stable, prerelease = prerelease)
            _versions.value = versions
            cache.set(CachedVersions.from(versions))
            Logger.d { "Fetched MaterialKolor versions: stable=$stable, prerelease=$prerelease" }
        } catch (e: Exception) {
            Logger.w(e) { "Failed to fetch MaterialKolor versions, using fallback" }
        }
    }

    override fun getVersion(usePrerelease: Boolean): String {
        val current = _versions.value
        return if (usePrerelease && current.prerelease != null) {
            current.prerelease
        } else {
            current.stable
        }
    }

    private fun fallbackVersions(): MaterialKolorVersions {
        return MaterialKolorVersions(
            stable = BuildKonfig.MATERIAL_KOLOR_VERSION,
            prerelease = null,
        )
    }

    companion object {
        private const val GITHUB_RELEASES_URL =
            "https://api.github.com/repos/jordond/materialkolor/releases"
    }
}
