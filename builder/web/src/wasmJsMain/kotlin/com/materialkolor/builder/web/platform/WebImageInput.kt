package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.DecodedImage
import com.materialkolor.builder.core.platform.ImageHandle
import com.materialkolor.builder.core.platform.ImageInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// stub
// B-301 picks through FileKit, listens for drops on the page and decodes on a canvas.
internal object WebImageInput : ImageInput {
    override val drops: Flow<ImageHandle> = emptyFlow()

    override suspend fun pick(): ImageHandle? = null

    override suspend fun decode(handle: ImageHandle): DecodedImage? = null
}
