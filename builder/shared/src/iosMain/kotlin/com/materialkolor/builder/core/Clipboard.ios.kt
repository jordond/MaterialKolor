package com.materialkolor.builder.core

import platform.UIKit.UIPasteboard

internal actual fun copyTextToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}