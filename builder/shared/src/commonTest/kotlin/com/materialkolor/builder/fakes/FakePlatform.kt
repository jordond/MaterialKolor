package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.core.platform.LibraryVersionSource
import com.materialkolor.builder.core.platform.LinkCardSource
import com.materialkolor.builder.core.platform.PlatformServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.Volatile

/**
 * Every fake in one [PlatformServices], typed as the fakes so a test can reach their controls.
 */
internal class FakePlatform(
    override val router: FakeRouter = FakeRouter(),
    override val stores: InMemoryStoreFactory = InMemoryStoreFactory(),
    override val clipboard: FakeClipboard = FakeClipboard(),
    override val files: FakeFileSaver = FakeFileSaver(),
    override val images: FakeImageInput = FakeImageInput(),
    override val pastes: FakePasteInput = FakePasteInput(),
    override val environment: FakeEnvironment = FakeEnvironment(),
    override val linkCards: FakeLinkCardSource = FakeLinkCardSource(),
    override val libraryVersions: FakeLibraryVersionSource = FakeLibraryVersionSource(),
) : PlatformServices

/**
 * A [LinkCardSource] that answers every url with [bytes] and keeps the urls it was asked for.
 */
internal class FakeLinkCardSource : LinkCardSource {
    /**
     * What every fetch gives, null for no card.
     */
    @Volatile
    var bytes: ByteArray? = null

    /**
     * Every url fetched, oldest first.
     */
    val urls: List<String>
        get() = fetched.value

    private val fetched = MutableStateFlow<List<String>>(emptyList())

    override suspend fun fetch(url: String): ByteArray? {
        fetched.update { urls -> urls + url }
        return bytes
    }
}

/**
 * A [LibraryVersionSource] that answers with [json] and counts how often it was asked.
 */
internal class FakeLibraryVersionSource(
    json: String? = null,
) : LibraryVersionSource {
    /**
     * What every fetch gives, null for no answer.
     */
    @Volatile
    var json: String? = json

    /**
     * How many times [fetch] ran.
     */
    val fetches: Int
        get() = count.value

    private val count = MutableStateFlow(0)

    override suspend fun fetch(): String? {
        count.update { fetches -> fetches + 1 }
        return json
    }
}
