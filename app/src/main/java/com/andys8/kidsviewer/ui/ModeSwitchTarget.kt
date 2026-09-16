package com.andys8.kidsviewer.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull

/** Hold a corner this long to switch modes. */
private const val SWITCH_HOLD_MS = 2000L

/** Generous enough to hit deliberately, small enough to stay out of the way. */
private val SWITCH_TARGET_SIZE = 140.dp

/**
 * An invisible corner target that switches something when held. There is no button and no menu:
 * the target is unmarked and the hold is deliberately long, so a toddler does not find it by
 * accident.
 *
 * The whole gesture lives in one coroutine that begins on touch-down and ends when the finger
 * lifts, so exactly one switch per press falls out of the structure -- there is no state to get
 * out of step and no boundary for a drifting finger to cross, because the target itself is the
 * hit area. Nothing is consumed, so a swipe starting here still swipes.
 *
 * Both corners use this: top-left switches swipe-only and slideshow, top-right switches between
 * the phone's photos and the bundled pictures.
 */
@Composable
fun ModeSwitchTarget(modifier: Modifier = Modifier, onSwitch: () -> Unit) {
    // Keyed on nothing, so a recomposition never interrupts a hold in progress, and read
    // through the latest value, so the switch it performs is never a stale one.
    val switch by rememberUpdatedState(onSwitch)

    Box(
        modifier = modifier
            .size(SWITCH_TARGET_SIZE)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)

                    val liftedEarly = withTimeoutOrNull(SWITCH_HOLD_MS) {
                        waitForUpOrCancellation()
                        true
                    }
                    if (liftedEarly != null) return@awaitEachGesture

                    switch()
                }
            }
    )
}
