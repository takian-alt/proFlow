package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences

enum class AllowlistTier { Essential, Allowed }

data class AllowlistDraft(val essential: Set<String>, val allowed: Set<String>)

fun draftFrom(prefs: HyperFocusPreferences): AllowlistDraft =
    AllowlistDraft(essential = prefs.essentialPackages, allowed = prefs.allowedPackages)

fun applyDraft(prefs: HyperFocusPreferences, draft: AllowlistDraft): HyperFocusPreferences =
    prefs.copy(essentialPackages = draft.essential, allowedPackages = draft.allowed)

/**
 * The two tiers are exclusive: a package is either always on the home surface or only reachable
 * through the gateway. Both at once would make the gateway's list disagree with the home's grid
 * about the same app.
 */
fun toggleTier(draft: AllowlistDraft, packageName: String, tier: AllowlistTier): AllowlistDraft =
    when (tier) {
        AllowlistTier.Essential ->
            if (packageName in draft.essential) draft.copy(essential = draft.essential - packageName)
            else draft.copy(essential = draft.essential + packageName, allowed = draft.allowed - packageName)
        AllowlistTier.Allowed ->
            if (packageName in draft.allowed) draft.copy(allowed = draft.allowed - packageName)
            else draft.copy(allowed = draft.allowed + packageName, essential = draft.essential - packageName)
    }

fun editableApps(all: Set<String>, systemExempt: Set<String>, selfPackage: String): Set<String> =
    all - systemExempt - selfPackage
