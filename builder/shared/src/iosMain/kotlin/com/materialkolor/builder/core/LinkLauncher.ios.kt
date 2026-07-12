package com.materialkolor.builder.core

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal actual fun launchUrl(url: String) {
    UIApplication.sharedApplication.openURL(NSURL.URLWithString(url)!!)
}