@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

package com.materialkolor.builder.web.interop

import org.khronos.webgl.Int32Array
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.WebAssembly
import kotlin.wasm.unsafe.wasmMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

internal actual fun Int32Array.copyToIntArray(): IntArray {
    val size = length
    if (size == 0) return IntArray(0)
    return withScopedMemoryAllocator { allocator ->
        val start = allocator.allocate(size * Int.SIZE_BYTES)
        copyIntoMemory(this, wasmMemory, start.address.toInt())
        IntArray(size) { index -> (start + index * Int.SIZE_BYTES).loadInt() }
    }
}

private fun copyIntoMemory(
    source: Int32Array,
    memory: WebAssembly.Memory,
    address: Int,
): Unit = js("new Int32Array(memory.buffer, address, source.length).set(source)")
