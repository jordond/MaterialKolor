package com.materialkolor.common

fun interface Function<T, R> {

    fun apply(t: T): R
}