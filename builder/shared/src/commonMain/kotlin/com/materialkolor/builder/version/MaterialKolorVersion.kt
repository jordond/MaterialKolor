package com.materialkolor.builder.version

import kotlinx.serialization.Serializable

data class MaterialKolorVersions(
    val stable: String,
    val prerelease: String?,
)

@Serializable
internal data class GitHubRelease(
    val tag_name: String,
    val prerelease: Boolean,
)
