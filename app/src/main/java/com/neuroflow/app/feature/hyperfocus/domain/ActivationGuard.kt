package com.neuroflow.app.feature.hyperfocus.domain

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.home.renderableEssentials

enum class ActivationRejection { AlreadyActive, EmptySurface }

sealed interface ActivationDecision {
    data object Allowed : ActivationDecision
    data class Rejected(val reason: ActivationRejection) : ActivationDecision
}

/**
 * Asks the same question the home surface asks when it draws its grid, so a session can never start
 * on a home with no icons on it.
 */
fun decideActivation(
    prefs: HyperFocusPreferences,
    systemExempt: Set<String>,
    selfPackage: String
): ActivationDecision = when {
    prefs.isActive -> ActivationDecision.Rejected(ActivationRejection.AlreadyActive)
    renderableEssentials(prefs, systemExempt, selfPackage).isEmpty() ->
        ActivationDecision.Rejected(ActivationRejection.EmptySurface)
    else -> ActivationDecision.Allowed
}
