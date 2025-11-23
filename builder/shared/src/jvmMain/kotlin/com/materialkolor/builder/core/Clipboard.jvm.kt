package com.materialkolor.builder.core

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

internal actual fun copyTextToClipboard(text: String) {
    val selection = StringSelection(text)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
}