package com.neuroflow.app.feature.hyperfocus.home

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.domain.RewardEngine

data class HomeTile(val packageName: String, val label: String)

sealed interface HomeSurfaceState {
    /** Session is off: the shell must give the device back to the user's real launcher. */
    data object HandOff : HomeSurfaceState

    data class Active(
        val essentials: List<HomeTile>,
        val passSecondsRemaining: Long?,
        val passPackage: String?
    ) : HomeSurfaceState
}

/**
 * Exemption means "never gated", not "shown as an icon" (spec 6.4): Settings keeps working while
 * Hyper Focus is on, but it does not live on the home surface. Task 16's activation guard calls this
 * too, so "there is nothing to show" and "the session would start with an empty home" are one fact.
 */
fun renderableEssentials(
    prefs: HyperFocusPreferences,
    systemExempt: Set<String>,
    selfPackage: String
): Set<String> = prefs.essentialPackages - systemExempt - selfPackage

/**
 * The dead-end placeholder claims no other launcher exists. That is only a fact once a resolution
 * has completed: on a cold start `handoffTarget` is still null while the DataStore read is in
 * flight, and showing the dead end there announces a conclusion we have not reached yet.
 */
fun showsOffPlaceholder(handoffResolved: Boolean, canHandOff: Boolean): Boolean =
    handoffResolved && !canHandOff

fun homeSurfaceStateOf(
    prefs: HyperFocusPreferences,
    labels: Map<String, String>,
    systemExempt: Set<String>,
    selfPackage: String,
    nowMillis: Long
): HomeSurfaceState {
    if (!prefs.isActive) return HomeSurfaceState.HandOff
    val seconds = RewardEngine.passSecondsRemaining(prefs, nowMillis)
    return HomeSurfaceState.Active(
        essentials = renderableEssentials(prefs, systemExempt, selfPackage)
            .map { HomeTile(packageName = it, label = labels[it] ?: it) }
            .sortedBy { it.label.lowercase() },
        passSecondsRemaining = seconds,
        passPackage = if (seconds != null) prefs.activePassPackage else null
    )
}
