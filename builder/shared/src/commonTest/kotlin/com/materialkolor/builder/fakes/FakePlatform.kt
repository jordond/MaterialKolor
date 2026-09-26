package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.InMemoryStoreFactory
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
) : PlatformServices
