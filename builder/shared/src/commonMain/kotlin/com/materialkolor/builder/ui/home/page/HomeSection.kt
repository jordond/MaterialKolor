package com.materialkolor.builder.ui.home.page

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

enum class HomeSection {
    Customize,
    Preview,
    Components,
    Themes,
    Palettes;

    companion object {

        val All: PersistentList<HomeSection> = entries.toPersistentList()
    }
}
