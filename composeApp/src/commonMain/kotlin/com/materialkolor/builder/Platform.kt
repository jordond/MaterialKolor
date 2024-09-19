package com.materialkolor.builder

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform