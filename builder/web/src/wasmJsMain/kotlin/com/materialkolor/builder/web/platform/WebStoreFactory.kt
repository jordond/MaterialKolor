package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Store
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.domain.persist.DecodeOutcome
import com.materialkolor.builder.domain.persist.RecordCodec
import com.materialkolor.builder.domain.persist.StorageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// stub
// B-302 moves this onto localStorage through kstore-storage and reads other tabs' writes from the
// storage event. Until then records live in memory, as the text their codec writes.
internal class WebStoreFactory : StoreFactory {
    private val texts = MutableStateFlow<Map<String, String>>(emptyMap())

    override val externalChanges: Flow<StorageKey> = emptyFlow()

    override fun <T> create(
        key: String,
        codec: RecordCodec<T>,
        default: T,
    ): Store<T> = MemoryStore(key, codec, default, texts)
}

private class MemoryStore<T>(
    private val key: String,
    private val codec: RecordCodec<T>,
    private val default: T,
    private val texts: MutableStateFlow<Map<String, String>>,
) : Store<T> {
    override val data: Flow<T> = texts.map { stored -> stored[key] }.distinctUntilChanged().map(::read)

    override suspend fun get(): T = read(texts.value[key])

    override suspend fun update(block: (T) -> T): StoreError? {
        texts.update { stored -> stored + (key to codec.encode(block(read(stored[key])))) }
        return null
    }

    private fun read(text: String?): T {
        if (text == null) return default
        return when (val outcome = codec.decode(text)) {
            is DecodeOutcome.Ok -> outcome.value
            is DecodeOutcome.Quarantine -> default
        }
    }
}
