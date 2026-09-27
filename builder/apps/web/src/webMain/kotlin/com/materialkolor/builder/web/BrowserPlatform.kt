package com.materialkolor.builder.web

import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.ImageInput
import com.materialkolor.builder.core.platform.LinkCardSource
import com.materialkolor.builder.core.platform.PasteInput
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.web.platform.WebClipboard
import com.materialkolor.builder.web.platform.WebEnvironment
import com.materialkolor.builder.web.platform.WebFileSaver
import com.materialkolor.builder.web.platform.WebImageInput
import com.materialkolor.builder.web.platform.WebLinkCards
import com.materialkolor.builder.web.platform.WebPasteInput
import com.materialkolor.builder.web.platform.WebRouter
import com.materialkolor.builder.web.platform.WebStoreFactory
import com.materialkolor.builder.web.platform.exposeBrowserApisToE2e

internal object BrowserPlatform : PlatformServices {
    override val router: Router = WebRouter()
    override val stores: StoreFactory = WebStoreFactory()
    override val clipboard: Clipboard = WebClipboard
    override val files: FileSaver = WebFileSaver
    override val images: ImageInput = WebImageInput
    override val pastes: PasteInput = WebPasteInput
    override val environment: Environment = WebEnvironment()
    override val linkCards: LinkCardSource = WebLinkCards

    init {
        exposeBrowserApisToE2e()
    }
}
