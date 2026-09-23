package com.materialkolor.builder.kit.motion

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The motion of the surrounding skin.
 *
 * Each skin theme provides this next to its tokens, and it already accounts for
 * [LocalReducedMotion], so nothing downstream has to branch on the preference.
 */
public val LocalBuilderMotion: ProvidableCompositionLocal<BuilderMotion> = staticCompositionLocalOf {
    tweenBuilderMotion()
}

/**
 * True when animation has to be deterministic.
 *
 * Screenshot runs and unit tests set it, and so does `?motion=frozen` when the browser says it is
 * driving itself. Under it a skin switch applies with no capture and no reveal, and
 * [rememberLoopPhase] holds still.
 */
public val LocalMotionFrozen: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/**
 * True when the user asked for less motion, either through the browser or the in app override
 * (F-37).
 *
 * The skin reads it once and provides the reduced motion set through [LocalBuilderMotion]. Read
 * this directly only where a component has to drop a movement entirely rather than shorten it.
 */
public val LocalReducedMotion: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/**
 * False while the browser tab is in the background.
 *
 * Only [rememberLoopPhase] reads it, so it is a plain composition local rather than a static one.
 * The web shell provides it from the page visibility, everything else leaves it at true.
 */
public val LocalTabVisible: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }
