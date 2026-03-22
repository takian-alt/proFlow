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

    // ── Core algorithm ────────────────────────────────────────────────────────

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
        // Reuse a single Calendar instance to avoid per-event allocation in the hot loop
        val cal = Calendar.getInstance()

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

                // Reset per-session so a distraction at the end of one session
                // doesn't bleed into the next session's recovery measurement
                var lastDistractionEndMs = 0L

                while (rawEvents.hasNextEvent()) {
                    rawEvents.getNextEvent(buf)
                    if (buf.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue

                    val pkg = buf.packageName
                    val time = buf.timeStamp

                    if (pkg == context.packageName) {
                        // User returned to our app — measure recovery
                        if (lastDistractionEndMs > 0L) {
                            totalRecoveryMs += time - lastDistractionEndMs
                            recoveryCount++
                            lastDistractionEndMs = 0L
                        }
                        continue
                    }

                    // Skip the system launcher — it appears on every home press, not a real distraction
                    if (isLauncherPackage(context, pkg)) continue
                    if (isSystemPackage(pkg)) continue

                    val weight = DISTRACTION_WEIGHTS[pkg] ?: continue // skip unknown apps
                    if (weight <= 0.1f) continue

                    // Estimate duration: cap at 5 min; guard against negative if event is at session boundary
                    val estimatedDuration = maxOf(0L, minOf(sessionEnd - time, 5 * 60_000L))
                    if (estimatedDuration < MIN_DISTRACTION_MS) continue

                    distractionEvents += Triple(pkg, time, estimatedDuration)
                    lastDistractionEndMs = time + estimatedDuration
                }
            }

            if (distractionEvents.isEmpty()) return@mapNotNull null

            // Layer 1: frequency — logarithmic diminishing returns
            val frequencyScore = ln(1f + distractionEvents.size) * 15f  // ~0–60

            // Layer 2: weighted depth (duration × app weight)
            val depthScore = minOf(
                distractionEvents.sumOf { (pkg, _, durationMs) ->
                    val w = DISTRACTION_WEIGHTS[pkg] ?: 0f
                    val mins = durationMs / 60_000f
                    minOf(mins * w * 10f, 30f).toDouble()
                }.toFloat(),
                40f
            )

            // Layer 3: circadian penalty — peak-hour distractions cost more
            val peakCount = distractionEvents.count { (_, startMs, _) ->
                cal.timeInMillis = startMs
                cal.get(Calendar.HOUR_OF_DAY) in peakHourStart..peakHourEnd
            }
            val circadianPenalty = (peakCount.toFloat() / distractionEvents.size) * 20f

            // Layer 4: recovery time penalty
            val avgRecoveryMinutes = if (recoveryCount > 0)
                (totalRecoveryMs / recoveryCount) / 60_000f else 0f
            val recoveryPenalty = minOf(avgRecoveryMinutes * 2f, 15f)

            val score = minOf(frequencyScore + depthScore + circadianPenalty + recoveryPenalty, 100f)

            // Top distracting app: ranked by total weighted distraction time (not just event count)
            val topApp = distractionEvents
                .groupBy { it.first }
                .maxByOrNull { (pkg, evts) ->
                    val w = DISTRACTION_WEIGHTS[pkg] ?: 0f
                    evts.sumOf { it.third * w.toDouble() }
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

        // Also get overall usage stats for the same window so we can compute
        // a dynamic weight for apps not in DISTRACTION_WEIGHTS (e.g. Chrome).
        // totalUsageMs is used to normalise: apps used a lot get a higher base weight.
        val overallUsage: Map<String, Long> = usm
            .queryUsageStats(UsageStatsManager.INTERVAL_BEST, lookbackStart, nowMillis)
            ?.associate { it.packageName to it.totalTimeInForeground }
            ?: emptyMap()
        val maxOverallMs = overallUsage.values.maxOrNull()?.takeIf { it > 0 } ?: 1L

        for (session in relevantSessions) {
            val sessionEnd = session.endedAt ?: nowMillis
            val rawEvents = usm.queryEvents(session.startedAt, sessionEnd)
            val buf = UsageEvents.Event()

            while (rawEvents.hasNextEvent()) {
                rawEvents.getNextEvent(buf)
                if (buf.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue

                val pkg = buf.packageName
                if (pkg == context.packageName) continue
                if (isLauncherPackage(context, pkg)) continue
                if (isSystemPackage(pkg)) continue

                // Known apps use their hardcoded weight.
                // Unknown apps get a dynamic weight: (their total usage / max usage) * 0.6
                // so a heavily-used unknown app can score up to 0.6 — below social media
                // but above noise. Minimum threshold 0.1 to filter truly idle background apps.
                val weight = DISTRACTION_WEIGHTS[pkg]
                    ?: ((overallUsage[pkg] ?: 0L).toFloat() / maxOverallMs * 0.6f)
                        .takeIf { it >= 0.1f } ?: continue

                val estimatedDuration = maxOf(0L, minOf(sessionEnd - buf.timeStamp, 5 * 60_000L))
                if (estimatedDuration < MIN_DISTRACTION_MS) continue

                val current = appStats[pkg] ?: (0L to 0)
                appStats[pkg] = (current.first + estimatedDuration) to (current.second + 1)
            }
        }

        if (appStats.isEmpty()) return emptyList()

        val maxWeighted = appStats.maxOf { (pkg, stats) ->
            val w = DISTRACTION_WEIGHTS[pkg]
                ?: ((overallUsage[pkg] ?: 0L).toFloat() / maxOverallMs * 0.6f)
            stats.first * w
        }.takeIf { it > 0f } ?: 1f

        return appStats
            .map { (pkg, stats) ->
                val w = DISTRACTION_WEIGHTS[pkg]
                    ?: ((overallUsage[pkg] ?: 0L).toFloat() / maxOverallMs * 0.6f)
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
     * Returns true if [pkg] is a home screen launcher.
     * Checks via CATEGORY_HOME intent resolution — covers any launcher, not just ours.
     */
    private fun isLauncherPackage(context: Context, pkg: String): Boolean {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_HOME)
        return context.packageManager
            .queryIntentActivities(intent, 0)
            .any { it.activityInfo.packageName == pkg }
    }

    fun label(score: Float): String = when {
        score >= 75f -> "🔴 Highly distracted"
        score >= 50f -> "🟠 Moderately distracted"
        score >= 25f -> "🟡 Mildly distracted"
        else         -> "🟢 Focused"
    }
}
