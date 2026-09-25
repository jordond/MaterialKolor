@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

package com.materialkolor.builder.web.interop

import org.khronos.webgl.Int32Array
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.WebAssembly
import kotlin.wasm.unsafe.wasmMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

// Through linear memory, since every element read across the boundary is a call on wasm.
internal actual fun Int32Array.copyToIntArray(): IntArray {
    val size = length
    if (size == 0) return IntArray(0)
    return withScopedMemoryAllocator { allocator ->
        val start = allocator.allocate(size * Int.SIZE_BYTES)
        copyIntoMemory(this, wasmMemory, start.address.toInt())
        IntArray(size) { index -> (start + index * Int.SIZE_BYTES).loadInt() }
    }
}

// The allocator hands out addresses that are multiples of 8, so the view lines up.
private fun copyIntoMemory(
    source: Int32Array,
    memory: WebAssembly.Memory,
    address: Int,
): Unit = js("new Int32Array(memory.buffer, address, source.length).set(source)")
