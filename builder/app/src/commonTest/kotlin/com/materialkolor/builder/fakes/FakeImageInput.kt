package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.DecodedImage
import com.materialkolor.builder.core.platform.ImageHandle
import com.materialkolor.builder.core.platform.ImageInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * An [ImageInput] a test fills in, with what the picker returns and what each handle decodes to.
 */
internal class FakeImageInput : ImageInput {
    private val dropped = Channel<ImageHandle>(Channel.UNLIMITED)

    /** What the next [pick] returns, null for a closed picker. */
    var picked: ImageHandle? = null

    /** How many times the picker was opened. */
    var picks: Int = 0
        private set

    /** What each handle decodes to. A handle missing here is not an image. */
    val decoded: MutableMap<ImageHandle, DecodedImage> = mutableMapOf()

    override val drops: Flow<ImageHandle> = dropped.receiveAsFlow()

    override suspend fun pick(): ImageHandle? {
        picks++
        return picked
    }

    override suspend fun decode(handle: ImageHandle): DecodedImage? = decoded[handle]

    /** Drop [handle] on the builder. It waits for a collector if there is none yet. */
    fun drop(handle: ImageHandle) {
        dropped.trySend(handle)
    }
}

/**
 * An [ImageHandle] that is only its [name].
 */
internal data class FakeImageHandle(
    override val name: String? = null,
) : ImageHandle
