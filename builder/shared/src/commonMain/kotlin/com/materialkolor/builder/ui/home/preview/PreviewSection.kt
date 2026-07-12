package com.materialkolor.builder.ui.home.preview

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

enum class PreviewSection {
    Customize,
    Preview,
    Components,
    Themes;

    companion object {

        val All: PersistentList<PreviewSection> = entries.toPersistentList()
    }
}
