package com.materialkolor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform