package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.core.platform.LinkCardSource
import com.materialkolor.builder.core.platform.PlatformServices

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
) : PlatformServices

/**
 * A [LinkCardSource] that answers every url with [bytes] and keeps the urls it was asked for.
 */
internal class FakeLinkCardSource : LinkCardSource {
    /**
     * What every fetch gives, null for no card.
     */
    var bytes: ByteArray? = null

    /**
     * Every url fetched, oldest first.
     */
    val urls: MutableList<String> = mutableListOf()

    override suspend fun fetch(url: String): ByteArray? {
        urls += url
        return bytes
    }
}
