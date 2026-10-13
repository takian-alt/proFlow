package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName

fun componentKey(component: ComponentName): String =
    "${component.packageName}/${component.className}"

/** A malformed or partial key is not a crash waiting on the next resume; it is simply no key. */
fun parseComponentKey(key: String?): ComponentName? {
    val parts = key?.split('/') ?: return null
    val pkg = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
    val cls = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
    return ComponentName(pkg, cls)
}

/**
 * Chooses which launcher to hand the device to when Hyper Focus is off, in string form so the choice
 * is testable without an Android runtime: this is the whole loop-breaker — returning this package's
 * own component means `onResume -> startActivity(HOME) -> onResume` on a `singleTask` activity, which
 * re-delivers to the same live instance instead of failing, and reads as a frozen black screen.
 */
fun pickHandoffKey(
    candidateKeys: List<String>,
    storedKey: String?,
    selfPackage: String
): String? {
    val others = candidateKeys.filterNot { it.substringBefore('/') == selfPackage }
    return others.firstOrNull { it == storedKey } ?: others.firstOrNull()
}

fun pickHandoff(
    candidates: List<ComponentName>,
    storedKey: String?,
    selfPackage: String
): ComponentName? {
    val picked = pickHandoffKey(candidates.map(::componentKey), storedKey, selfPackage) ?: return null
    return candidates.firstOrNull { componentKey(it) == picked }
}
