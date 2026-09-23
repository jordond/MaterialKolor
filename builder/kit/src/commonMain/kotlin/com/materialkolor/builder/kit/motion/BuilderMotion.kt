package com.materialkolor.builder.kit.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * How long each kind of change takes, in milliseconds.
 *
 * The three base steps are the Unstyled and Custom scale from MO-09. Material3 and Fluent keep
 * their own curves but still report their timings here, so a test or a screenshot run can reason
 * about any skin the same way.
 *
 * @property[quick] A colour, an opacity or anything else that only fades.
 * @property[standard] The default spatial move, a chip sliding or a row reordering.
 * @property[slow] A move that crosses the canvas, such as the split wipe handle (MO-03).
 * @property[reveal] The circle that grows out of the library switcher (MO-04).
 * @property[panelEnter] Panels, sheets and the poster collapse arriving (MO-05).
 * @property[panelExit] The same things leaving, which is always quicker than arriving.
 * @property[popover] A menu or tooltip scaling in (MO-05).
 * @property[press] The press scale (MO-06).
 * @property[reducedCrossfade] What a discrete change costs under reduced motion (F-37).
 */
@Immutable
public data class BuilderDurations(
    public val quick: Int = 120,
    public val standard: Int = 220,
    public val slow: Int = 380,
    public val reveal: Int = 420,
    public val panelEnter: Int = 300,
    public val panelExit: Int = 200,
    public val popover: Int = 160,
    public val press: Int = 100,
    public val reducedCrossfade: Int = 150,
) {
    init {
        require(reveal in 400..450) { "The skin reveal runs 400 to 450 ms, got $reveal" }
        require(panelEnter in 250..350) { "Panels arrive in 250 to 350 ms, got $panelEnter" }
        require(popover in 125..200) { "Popovers arrive in 125 to 200 ms, got $popover" }
        require(reducedCrossfade <= 150) { "Reduced motion crossfades in 150 ms or less, got $reducedCrossfade" }
    }
}

/** The curves the builder draws its own motion on. Skins with their own curves ignore these. */
public object BuilderEasing {
    /** Quick out, settles gently. The default for anything that moves a short distance. */
    public val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Leaves fast and lands soft. The reveal and every panel arrival use this. */
    public val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Builds speed on the way out. Panels and sheets leave on this. */
    public val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/** How far a press shrinks whatever was pressed (MO-06). */
public const val PressScale: Float = 0.97f

/**
 * The motion of the surrounding skin.
 *
 * Every spec is a function with its own type parameter, the same shape Material3 uses, so one
 * motion set serves a `Float` reveal, a `Dp` panel width and a `Color` accent without anybody
 * casting.
 */
@Immutable
public interface BuilderMotion {
    /** The timings behind the specs, for tests, screenshots and the reduced motion fallback. */
    public val durations: BuilderDurations

    /** How far a press shrinks its target. One under reduced motion, where nothing moves. */
    public val pressScale: Float

    /** Something moving a short distance. */
    public fun <T> spatial(): FiniteAnimationSpec<T>

    /** Something only changing colour or opacity. */
    public fun <T> effects(): FiniteAnimationSpec<T>

    /** Something crossing the canvas, such as the split wipe handle. */
    public fun <T> slide(): FiniteAnimationSpec<T>

    /** The circle growing out of the library switcher. */
    public fun <T> reveal(): FiniteAnimationSpec<T>

    /** A panel, a sheet or the poster arriving. */
    public fun <T> panelEnter(): FiniteAnimationSpec<T>

    /** The same, leaving. */
    public fun <T> panelExit(): FiniteAnimationSpec<T>

    /** A menu or a tooltip. */
    public fun <T> popover(): FiniteAnimationSpec<T>

    /** The press scale. */
    public fun <T> press(): FiniteAnimationSpec<T>

    /** The snapshot crossfade behind a discrete change. */
    public fun <T> crossfade(): FiniteAnimationSpec<T>
}

/**
 * The tween set the Unstyled and Custom skins wear (MO-09).
 *
 * Material3 builds its own from `MotionScheme` and Fluent from `FluentDuration`, both of them
 * reporting the same [durations] so the rest of the builder does not have to care.
 */
public fun tweenBuilderMotion(durations: BuilderDurations = BuilderDurations()): BuilderMotion =
    TweenBuilderMotion(durations)

/**
 * The set every skin falls back to under reduced motion (F-37, MO-10).
 *
 * Nothing moves. Discrete changes crossfade inside [BuilderDurations.reducedCrossfade] and
 * everything spatial snaps.
 */
public fun reducedBuilderMotion(durations: BuilderDurations = BuilderDurations()): BuilderMotion =
    ReducedBuilderMotion(durations)

private class TweenBuilderMotion(
    override val durations: BuilderDurations,
) : BuilderMotion {
    override val pressScale: Float = PressScale

    override fun <T> spatial(): FiniteAnimationSpec<T> =
        tween(durations.standard, easing = BuilderEasing.Standard)

    override fun <T> effects(): FiniteAnimationSpec<T> =
        tween(durations.quick, easing = BuilderEasing.Standard)

    override fun <T> slide(): FiniteAnimationSpec<T> =
        tween(durations.slow, easing = BuilderEasing.Standard)

    override fun <T> reveal(): FiniteAnimationSpec<T> =
        tween(durations.reveal, easing = BuilderEasing.EmphasizedDecelerate)

    override fun <T> panelEnter(): FiniteAnimationSpec<T> =
        tween(durations.panelEnter, easing = BuilderEasing.EmphasizedDecelerate)

    override fun <T> panelExit(): FiniteAnimationSpec<T> =
        tween(durations.panelExit, easing = BuilderEasing.EmphasizedAccelerate)

    override fun <T> popover(): FiniteAnimationSpec<T> =
        tween(durations.popover, easing = BuilderEasing.EmphasizedDecelerate)

    override fun <T> press(): FiniteAnimationSpec<T> =
        tween(durations.press, easing = BuilderEasing.Standard)

    override fun <T> crossfade(): FiniteAnimationSpec<T> =
        tween(durations.standard, easing = BuilderEasing.Standard)
}

private class ReducedBuilderMotion(
    override val durations: BuilderDurations,
) : BuilderMotion {
    override val pressScale: Float = 1f

    private fun <T> fade(): FiniteAnimationSpec<T> =
        tween(durations.reducedCrossfade, easing = BuilderEasing.Standard)

    override fun <T> spatial(): FiniteAnimationSpec<T> = snap()

    override fun <T> effects(): FiniteAnimationSpec<T> = fade()

    override fun <T> slide(): FiniteAnimationSpec<T> = snap()

    override fun <T> reveal(): FiniteAnimationSpec<T> = fade()

    override fun <T> panelEnter(): FiniteAnimationSpec<T> = fade()

    override fun <T> panelExit(): FiniteAnimationSpec<T> = fade()

    override fun <T> popover(): FiniteAnimationSpec<T> = fade()

    override fun <T> press(): FiniteAnimationSpec<T> = snap()

    override fun <T> crossfade(): FiniteAnimationSpec<T> = fade()
}
