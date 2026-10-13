package com.neuroflow.app.domain.engine

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import java.util.Calendar
import kotlin.math.ln

/**
 * DistractionEngine
 *
 * Correlates phone usage data (UsageStatsManager) with task focus sessions
 * to compute a per-task distraction score.
 *
 * Algorithm layers:
 *  1. App-switch frequency  — logarithmic (context-switching cost)
 *  2. Distraction app weight — social media > messaging > email
 *  3. Interruption depth     — how long each distraction lasted
 *  4. Recovery time penalty  — time to return to task after distraction
 *  5. Circadian penalty      — distractions during peak hours cost more
 */
object DistractionEngine {

    // Distraction weight per package: 0.0 = harmless, 1.0 = maximum distraction
    // Only apps in this map are tracked — system/utility apps are ignored entirely.
    private val DISTRACTION_WEIGHTS = mapOf(
        // ── Social media ──────────────────────────────────────────────────────
        "com.instagram.android"                 to 1.0f,
        "com.twitter.android"                   to 0.95f,
        "com.x.android"                         to 0.95f, // X (Twitter rebrand)
        "com.zhiliaoapp.musically"              to 0.95f, // TikTok
        "com.ss.android.ugc.trill"              to 0.95f, // TikTok (some regions)
        "com.snapchat.android"                  to 0.90f,
        "com.facebook.katana"                   to 0.90f,
        "com.facebook.lite"                     to 0.85f,
        "com.reddit.frontpage"                  to 0.85f,
        "com.linkedin.android"                  to 0.70f,
        "com.pinterest"                         to 0.80f,
        "com.tumblr"                            to 0.75f,
        "com.vkontakte.android"                 to 0.85f, // VK
        "com.bereal.app"                        to 0.75f,
        "com.threads.android"                   to 0.90f, // Threads
        "com.quora.android"                     to 0.65f,
        // ── Messaging ─────────────────────────────────────────────────────────
        "com.whatsapp"                          to 0.65f,
        "com.whatsapp.w4b"                      to 0.60f, // WhatsApp Business
        "org.telegram.messenger"                to 0.60f,
        "org.telegram.plus"                     to 0.60f,
        "com.discord"                           to 0.65f,
        "com.google.android.apps.messaging"     to 0.50f,
        "com.facebook.orca"                     to 0.70f, // Messenger
        "com.facebook.mlite"                    to 0.65f, // Messenger Lite
        "com.viber.voip"                        to 0.55f,
        "kik.android"                           to 0.60f,
        "com.skype.raider"                      to 0.50f,
        "com.microsoft.teams"                   to 0.45f,
        "com.slack"                             to 0.45f,
        "com.groupme.android"                   to 0.55f,
        "jp.naver.line.android"                 to 0.60f, // LINE
        "com.kakao.talk"                        to 0.60f, // KakaoTalk
        "com.tencent.mm"                        to 0.65f, // WeChat
        "com.signal.android"                    to 0.50f,
        // ── Email ─────────────────────────────────────────────────────────────
        "com.google.android.gm"                 to 0.35f,
        "com.microsoft.office.outlook"          to 0.35f,
        "com.yahoo.mobile.client.android.mail"  to 0.35f,
        "me.proton.android.mail"                to 0.30f,
        // ── Video / streaming ─────────────────────────────────────────────────
        "com.google.android.youtube"            to 0.80f,
        "com.netflix.mediaclient"               to 0.85f,
        "com.amazon.avod.thirdpartyclient"      to 0.80f, // Prime Video
        "com.disney.disneyplus"                 to 0.80f,
        "com.hbo.hbonow"                        to 0.80f,
        "com.hulu.plus"                         to 0.80f,
        "com.twitch.android.app"                to 0.85f,
        "tv.twitch.android.app"                 to 0.85f,
        "com.spotify.music"                     to 0.40f, // music = mild distraction
        "com.apple.android.music"               to 0.35f,
        "com.soundcloud.android"                to 0.40f,
        "com.vanced.android.youtube"            to 0.80f, // YouTube Vanced
        // ── Browsing ──────────────────────────────────────────────────────────
        "com.android.chrome"                    to 0.40f,
        "org.mozilla.firefox"                   to 0.40f,
        "com.opera.browser"                     to 0.40f,
        "com.brave.browser"                     to 0.40f,
        "com.microsoft.emmx"                    to 0.40f, // Edge
        "com.duckduckgo.mobile.android"         to 0.35f,
        // ── Games ─────────────────────────────────────────────────────────────
        "com.king.candycrushsaga"               to 1.0f,
        "com.king.candycrushsodasaga"           to 1.0f,
        "com.supercell.clashofclans"            to 0.95f,
        "com.supercell.clashroyale"             to 0.95f,
        "com.supercell.brawlstars"              to 0.95f,
        "com.mojang.minecraftpe"                to 0.90f,
        "com.roblox.client"                     to 0.90f,
        "com.pubg.imobile"                      to 0.95f,
        "com.tencent.ig"                        to 0.95f, // PUBG Mobile (some regions)
        "com.activision.callofduty.shooter"     to 0.95f,
        "com.garena.free.fire"                  to 0.95f,
        "com.epicgames.fortnite"                to 0.95f,
        "com.ea.game.pvzfh_row"                 to 0.90f,
        "com.halfbrick.fruitninjafree"          to 0.85f,
        "com.imangi.templerun2"                 to 0.85f,
        "com.outfit7.talkingtomgoldrun2"        to 0.85f,
        "com.miniclip.eightballpool"            to 0.90f,
        "com.chess"                             to 0.70f,
        "com.lichess.mobileapp"                 to 0.70f,
        // ── News / content ────────────────────────────────────────────────────
        "com.google.android.apps.magazines"     to 0.50f, // Google News
        "flipboard.app"                         to 0.60f,
        "com.buzzfeed.android"                  to 0.65f,
        "com.medium.reader"                     to 0.45f,
        // ── Shopping ──────────────────────────────────────────────────────────
        "com.amazon.mShop.android.shopping"     to 0.55f,
        "com.ebay.mobile"                       to 0.50f,
        "com.etsy.android"                      to 0.50f,
    )

    private const val MIN_DISTRACTION_MS = 3_000L  // ignore < 3s blips
    private const val LOOKBACK_MS = 30L * 24 * 60 * 60 * 1000 // 30 days

    /**
     * Ceiling for one app's uninterrupted foreground stretch. ROMs drop usage events, and an
     * uncapped stretch can credit a whole night's screen-off time to the last app that was open.
     */
    private const val MAX_SINGLE_APP_MS = 60L * 60_000L

    /**
     * System / utility packages that appear in UsageEvents but are never real distractions.
     * Covers: permission dialogs, system UI, GMS, setup wizards, input methods, etc.
     */
    private val SYSTEM_PACKAGE_PREFIXES = listOf(
        "com.android.",
        "android.",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.permissioncontroller",
        "com.google.android.packageinstaller",
        "com.google.android.setupwizard",
        "com.google.android.inputmethod",
        "com.google.android.accessibility",
        "com.google.android.tts",
        "com.google.android.syncadapters",
        "com.google.android.backuptransport",
        "com.google.android.feedback",
        "com.google.android.partnersetup",
        "com.google.android.onetimeinitializer",
        "com.google.android.overlay",
        "com.google.android.ext.",
        "com.samsung.android.",
        "com.sec.android.",
        "com.miui.",
        "com.xiaomi.",
        "com.oneplus.",
        "com.oppo.",
        "com.realme.",
        "com.huawei.android.",
        "com.lge.android.",
        "com.motorola.",
    )

    private fun isSystemPackage(pkg: String): Boolean =
        SYSTEM_PACKAGE_PREFIXES.any { pkg.startsWith(it) }

    // ── Public data types ─────────────────────────────────────────────────────

    data class TaskDistractionResult(
        val task: TaskEntity,
        /** Normalized 0–100 distraction score */
        val distractionScore: Float,
        val distractionEvents: Int,
        val totalDistractedMs: Long,
        val topDistractingApp: String?,
        val avgRecoveryMs: Long,
        val label: String
    )

    data class AppDistractionResult(
        val packageName: String,
        /** Human-readable app name, falls back to package name */
        val appLabel: String,
        /** Total time spent in this app during focus sessions (ms) */
        val totalDistractedMs: Long,
        /** Number of times this app was opened during focus sessions */
        val openCount: Int,
        /** Weighted distraction score 0–100 */
        val score: Float,
        val label: String
    )

    // ── Permission helpers ────────────────────────────────────────────────────

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsagePermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // Shared local event type used in both ranking functions
    private data class FgEvent(val pkg: String, val timeMs: Long)

    /**
     * Returns tasks ranked by distraction score (most distracted first).
     * Requires PACKAGE_USAGE_STATS permission — check [hasUsagePermission] first.
     */
    fun rankByDistraction(
        tasks: List<TaskEntity>,
        sessions: List<TimeSessionEntity>,
        context: Context,
        peakHourStart: Int = 9,
        peakHourEnd: Int = 12,
        nowMillis: Long = System.currentTimeMillis()
    ): List<TaskDistractionResult> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val lookbackStart = nowMillis - LOOKBACK_MS

        // Cache launcher packages once — queryIntentActivities is expensive
        val launcherPackages = getLauncherPackages(context)

        // Overall usage for dynamic weight of unknown apps (same logic as rankAppsByDistraction)
        val overallUsage: Map<String, Long> = usm
            .queryUsageStats(UsageStatsManager.INTERVAL_BEST, lookbackStart, nowMillis)
            ?.filter { it.packageName != context.packageName && !isSystemPackage(it.packageName) }
            ?.associate { it.packageName to it.totalTimeInForeground }
            ?: emptyMap()
        val maxOverallMs = overallUsage.values.maxOrNull()?.takeIf { it > 0 } ?: 1L

        return tasks.mapNotNull { task ->
            val taskSessions = sessions.filter {
                it.taskId == task.id && it.endedAt != null && it.startedAt >= lookbackStart
            }
            if (taskSessions.isEmpty()) return@mapNotNull null

            val distractionEvents = mutableListOf<Triple<String, Long, Long>>() // pkg, startMs, durationMs
            var totalRecoveryMs = 0L
            var recoveryCount = 0

            for (session in taskSessions) {
                val sessionEnd = session.endedAt ?: continue
                val rawEvents = usm.queryEvents(session.startedAt, sessionEnd)
                val buf = UsageEvents.Event()

                // Collect all foreground events first so we can compute accurate durations
                val fgEvents = mutableListOf<FgEvent>()
                while (rawEvents.hasNextEvent()) {
                    rawEvents.getNextEvent(buf)
                    @Suppress("DEPRECATION")
                    if (buf.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
                    fgEvents += FgEvent(buf.packageName, buf.timeStamp)
                }

                fgEvents.forEachIndexed { i, event ->
                    val pkg = event.pkg
                    val time = event.timeMs

                    if (pkg == context.packageName) {
                        // Measure recovery: time from last distraction end until we returned to our app
                        val lastEnd = distractionEvents.lastOrNull()?.let { it.second + it.third } ?: 0L
                        if (lastEnd > 0L && time > lastEnd) {
                            totalRecoveryMs += time - lastEnd
                            recoveryCount++
                        }
                        return@forEachIndexed
                    }

                    if (pkg in launcherPackages) return@forEachIndexed
                    // Known apps always pass; unknown apps skip only if system package
                    if (DISTRACTION_WEIGHTS[pkg] == null && isSystemPackage(pkg)) return@forEachIndexed

                    // Duration = time until next foreground event (or session end), capped at 5 min
                    val nextEventTime = fgEvents.getOrNull(i + 1)?.timeMs ?: sessionEnd
                    val duration = maxOf(0L, minOf(nextEventTime - time, 5 * 60_000L))
                    if (duration < MIN_DISTRACTION_MS) return@forEachIndexed

                    distractionEvents += Triple(pkg, time, duration)
                }
            }

            if (distractionEvents.isEmpty()) return@mapNotNull null

            // Build session-based weight fallback (same as rankAppsByDistraction)
            val sessionMsPerPkg = distractionEvents.groupBy { it.first }
                .mapValues { (_, evts) -> evts.sumOf { it.third } }
            val maxSessionMs = sessionMsPerPkg.values.maxOrNull()?.takeIf { it > 0L } ?: 1L

            fun effectiveWeight(pkg: String): Float {
                DISTRACTION_WEIGHTS[pkg]?.let { return it }
                val fromOverall = ((overallUsage[pkg] ?: 0L).toFloat() / maxOverallMs * 0.6f)
                if (fromOverall >= 0.1f) return fromOverall
                return ((sessionMsPerPkg[pkg] ?: 0L).toFloat() / maxSessionMs * 0.5f)
                    .coerceAtLeast(0.1f)
            }

            val frequencyScore = ln(1f + distractionEvents.size) * 15f

            val depthScore = minOf(
                distractionEvents.sumOf { (pkg, _, durationMs) ->
                    val w = effectiveWeight(pkg)
                    val mins = durationMs / 60_000f
                    minOf(mins * w * 10f, 30f).toDouble()
                }.toFloat(),
                40f
            )

            // Use a local Calendar per-task to avoid shared-state mutation bugs
            val cal = Calendar.getInstance()
            val peakCount = distractionEvents.count { (_, startMs, _) ->
                cal.timeInMillis = startMs
                cal.get(Calendar.HOUR_OF_DAY) in peakHourStart..peakHourEnd
            }
            val circadianPenalty = (peakCount.toFloat() / distractionEvents.size) * 20f

            val avgRecoveryMinutes = if (recoveryCount > 0)
                (totalRecoveryMs / recoveryCount) / 60_000f else 0f
            val recoveryPenalty = minOf(avgRecoveryMinutes * 2f, 15f)

            val score = minOf(frequencyScore + depthScore + circadianPenalty + recoveryPenalty, 100f)

            val topApp = distractionEvents
                .groupBy { it.first }
                .maxByOrNull { (pkg, evts) ->
                    evts.sumOf { it.third * effectiveWeight(pkg).toDouble() }
                }?.key

            TaskDistractionResult(
                task = task,
                distractionScore = score,
                distractionEvents = distractionEvents.size,
                totalDistractedMs = distractionEvents.sumOf { it.third },
                topDistractingApp = topApp,
                avgRecoveryMs = if (recoveryCount > 0) totalRecoveryMs / recoveryCount else 0L,
                label = label(score)
            )
        }.sortedByDescending { it.distractionScore }
    }

    /**
     * Returns the top [limit] most distracting apps during all focus sessions.
     *
     * Strategy: query ALL apps the user actually opened during focus sessions via
     * UsageStatsManager, then score them. Known apps (social/games/etc.) get their
     * hardcoded weight. Unknown apps get a dynamic weight derived from how long the
     * user spent in them — so Chrome, any browser, or any app you actually use will
     * appear naturally without needing to be hardcoded.
     *
     * Requires PACKAGE_USAGE_STATS permission.
     */
    fun rankAppsByDistraction(
        sessions: List<TimeSessionEntity>,
        context: Context,
        limit: Int = 3,
        nowMillis: Long = System.currentTimeMillis()
    ): List<AppDistractionResult> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val lookbackStart = nowMillis - LOOKBACK_MS

        // pkg -> (totalDistractedMs, openCount)
        val appStats = mutableMapOf<String, Pair<Long, Int>>()

        // Include both completed and still-open sessions (endedAt == null → use now as end)
        val relevantSessions = sessions.filter { it.startedAt >= lookbackStart }

        // Cache launcher packages once — queryIntentActivities is expensive
        val launcherPackages = getLauncherPackages(context)

        // Also get overall usage stats for the same window so we can compute
        // a dynamic weight for apps not in DISTRACTION_WEIGHTS (e.g. Chrome).
        // Exclude our own package and system packages from the max so they don't
        // suppress the dynamic weights of real user apps.
        val overallUsage: Map<String, Long> = usm
            .queryUsageStats(UsageStatsManager.INTERVAL_BEST, lookbackStart, nowMillis)
            ?.filter { it.packageName != context.packageName && !isSystemPackage(it.packageName) }
            ?.associate { it.packageName to it.totalTimeInForeground }
            ?: emptyMap()
        val maxOverallMs = overallUsage.values.maxOrNull()?.takeIf { it > 0 } ?: 1L

        for (session in relevantSessions) {
            val sessionEnd = session.endedAt ?: nowMillis
            val rawEvents = usm.queryEvents(session.startedAt, sessionEnd)
            val buf = UsageEvents.Event()

            // Collect all FOREGROUND events first, then compute durations between them
            val fgEvents = mutableListOf<FgEvent>()

            while (rawEvents.hasNextEvent()) {
                rawEvents.getNextEvent(buf)
                @Suppress("DEPRECATION")
                if (buf.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
                fgEvents += FgEvent(buf.packageName, buf.timeStamp)
            }

            // Compute per-app duration: time until next foreground event (or session end), capped at 5 min
            fgEvents.forEachIndexed { i, event ->
                val pkg = event.pkg
                if (pkg == context.packageName) return@forEachIndexed
                if (pkg in launcherPackages) return@forEachIndexed

                val knownWeight = DISTRACTION_WEIGHTS[pkg]
                if (knownWeight == null && isSystemPackage(pkg)) return@forEachIndexed
                // For unknown apps, accept them into appStats with a placeholder weight of 0.1f minimum.
                // The real weight is computed after appStats is fully built (dynamicWeight uses maxSessionMs).

                // Duration = time until next event (or session end), capped at 5 min
                val nextEventTime = fgEvents.getOrNull(i + 1)?.timeMs ?: sessionEnd
                val duration = maxOf(0L, minOf(nextEventTime - event.timeMs, 5 * 60_000L))
                if (duration < MIN_DISTRACTION_MS) return@forEachIndexed

                val current = appStats[pkg] ?: (0L to 0)
                appStats[pkg] = (current.first + duration) to (current.second + 1)
            }
        }

        if (appStats.isEmpty()) return emptyList()

        // If overallUsage is sparse/empty (throttled on some ROMs), fall back to
        // session-derived time as the weight signal for unknown apps.
        // maxSessionMs = longest total session time across all tracked apps.
        val maxSessionMs = appStats.values.maxOf { it.first }.takeIf { it > 0L } ?: 1L

        fun dynamicWeight(pkg: String): Float {
            val fromOverall = ((overallUsage[pkg] ?: 0L).toFloat() / maxOverallMs * 0.6f)
            return if (fromOverall >= 0.1f) fromOverall
            else {
                // Fallback: derive weight from how much session time this app consumed
                val fromSession = (appStats[pkg]?.first ?: 0L).toFloat() / maxSessionMs * 0.5f
                fromSession.coerceAtLeast(0.1f)
            }
        }

        val maxWeighted = appStats.maxOf { (pkg, stats) ->
            val w = DISTRACTION_WEIGHTS[pkg] ?: dynamicWeight(pkg)
            stats.first * w
        }.takeIf { it > 0f } ?: 1f

        return appStats
            .map { (pkg, stats) ->
                val w = DISTRACTION_WEIGHTS[pkg] ?: dynamicWeight(pkg)
                val weightedMs = stats.first * w
                val score = minOf((weightedMs / maxWeighted) * 100f, 100f)
                val name = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (_: Exception) { pkg }
                AppDistractionResult(
                    packageName = pkg,
                    appLabel = name,
                    totalDistractedMs = stats.first,
                    openCount = stats.second,
                    score = score,
                    label = label(score)
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * Priority boost to feed into TaskScoringEngine.
     * Highly distracted tasks need intentional scheduling in protected time.
     */
    fun priorityBoost(distractionScore: Float): Float = when {
        distractionScore >= 75f -> 80f
        distractionScore >= 50f -> 45f
        distractionScore >= 25f -> 20f
        else                    -> 0f
    }

    /**
     * Returns the set of all launcher package names on this device.
     * Cached per-call — call once at the top of each ranking function.
     */
    private fun getLauncherPackages(context: Context): Set<String> {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_HOME)
        return context.packageManager
            .queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    fun label(score: Float): String = when {
        score >= 75f -> "🔴 Highly distracted"
        score >= 50f -> "🟠 Moderately distracted"
        score >= 25f -> "🟡 Mildly distracted"
        else         -> "🟢 Focused"
    }

    // ── Daily screen time ────────────────────────────────────────────────────

    /**
     * Seed for the user-editable distracting-app list. Anything the weight table already
     * treats as at least moderately distracting (social, games, streaming) counts by default.
     */
    val DEFAULT_DISTRACTING_PACKAGES: Set<String> =
        DISTRACTION_WEIGHTS.filterValues { it >= 0.65f }.keys.toSet()

    /** Roughly how much tracked time is needed before a daily verdict means anything. */
    const val MIN_TRACKED_MS_FOR_VERDICT = 15L * 60_000L

    data class DailyScreenUsage(
        /** Foreground time in all real apps today (system UI and this app excluded) */
        val totalScreenMs: Long,
        /** Foreground time today in the user's distracting apps */
        val distractingMs: Long,
        /** Part of [distractingMs] that fell inside a focus block — not extra attention spent */
        val distractingInsideFocusMs: Long,
        /** pkg -> ms today, highest first, restricted to the flagged packages */
        val distractingByApp: List<Pair<String, Long>>,
        val permissionGranted: Boolean
    )

    /**
     * Today's screen time, split into "everything" and "apps the user flagged as distracting".
     * Event-based rather than [UsageStatsManager.queryUsageStats] because the interval APIs only
     * give coarse multi-hour buckets on most ROMs; pairing FOREGROUND/BACKGROUND events gives a
     * real per-app daily total. Must be called off the main thread.
     *
     * [focusWindows] lets flagged time be attributed to the block it interrupted rather than
     * counted twice, once as distraction and once as deep work.
     */
    fun dailyScreenUsage(
        context: Context,
        distractingPackages: Set<String>,
        startOfDayMillis: Long,
        endMillis: Long = System.currentTimeMillis(),
        focusWindows: List<Pair<Long, Long>> = emptyList()
    ): DailyScreenUsage {
        if (!hasUsagePermission(context)) {
            return DailyScreenUsage(0L, 0L, 0L, emptyList(), permissionGranted = false)
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return DailyScreenUsage(0L, 0L, 0L, emptyList(), permissionGranted = true)
        val perAppMs = mutableMapOf<String, Long>()
        var insideFocusMs = 0L

        fun record(pkg: String, ms: Long, fromMillis: Long, toMillis: Long) {
            if (ms <= 0L) return
            perAppMs[pkg] = (perAppMs[pkg] ?: 0L) + ms
            if (pkg in distractingPackages) {
                insideFocusMs += focusOverlapMs(fromMillis, toMillis, focusWindows)
            }
        }

        var openPkg: String? = null
        var openAtMillis = 0L

        fun closeOpen(closeAtMillis: Long) {
            val pkg = openPkg ?: return
            val duration = (closeAtMillis - openAtMillis).coerceAtMost(MAX_SINGLE_APP_MS)
            record(pkg, duration, closeAtMillis - duration, closeAtMillis)
            openPkg = null
        }

        val events = usm.queryEvents(startOfDayMillis, endMillis)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue

            @Suppress("DEPRECATION")
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (pkg == context.packageName) {
                        // Focus blocks run here: the previous app's turn still ends.
                        closeOpen(event.timeStamp)
                        continue
                    }
                    // Same precedence as rankAppsByDistraction: a package with a known weight is a
                    // real app even if its id looks system-owned, and the user may have flagged it.
                    if (isSystemPackage(pkg) && pkg !in distractingPackages &&
                        DISTRACTION_WEIGHTS[pkg] == null
                    ) {
                        closeOpen(event.timeStamp)
                        continue
                    }
                    // A background event can be dropped by the system; the next foreground
                    // event is the only reliable evidence of when the previous app stopped.
                    closeOpen(event.timeStamp)
                    openPkg = pkg
                    openAtMillis = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (openPkg == pkg) closeOpen(event.timeStamp)
                }
                // Screen-off and the keyguard end the day's clock for the last app. Without
                // this the app left open before sleep swallows the whole night.
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> {
                    closeOpen(event.timeStamp)
                }
                else -> Unit
            }
        }
        closeOpen(endMillis)

        val totalScreenMs = perAppMs.values.sum()
        val distracting = perAppMs
            .filter { (pkg, _) -> pkg in distractingPackages }
            .toList()
            .sortedByDescending { it.second }

        return DailyScreenUsage(
            totalScreenMs = totalScreenMs,
            distractingMs = distracting.sumOf { it.second },
            distractingInsideFocusMs = insideFocusMs.coerceAtMost(distracting.sumOf { it.second }),
            distractingByApp = distracting,
            permissionGranted = true
        )
    }

    /** How much of [start]..[end] is already spoken for by a focus block. */
    private fun focusOverlapMs(start: Long, end: Long, windows: List<Pair<Long, Long>>): Long {
        if (windows.isEmpty()) return 0L
        return windows.sumOf { (from, to) ->
            (minOf(end, to) - maxOf(start, from)).coerceAtLeast(0L)
        }.coerceAtMost(end - start).coerceAtLeast(0L)
    }

    /**
     * Share of the day's tracked attention that went to distracting apps, 0–100.
     * Deliberately ignores the rest of screen time — the comparison the user cares about
     * is deep work versus the apps they told us they lose time in.
     */
    fun dailyDistractionScore(distractedMs: Long, focusMinutes: Float): Float {
        val focusMs = (focusMinutes * 60_000f).toLong()
        val tracked = distractedMs + focusMs
        if (tracked <= 0L) return 0f
        return ((distractedMs.toFloat() / tracked) * 100f).coerceIn(0f, 100f)
    }

    fun appLabel(context: Context, packageName: String): String = try {
        context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(packageName, 0)
        ).toString()
    } catch (_: Exception) {
        packageName
    }
}
