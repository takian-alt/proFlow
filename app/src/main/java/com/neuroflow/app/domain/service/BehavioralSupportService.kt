package com.neuroflow.app.domain.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

data class MicroStepSuggestion(
    val title: String,
    val estimatedMinutes: Int
)

data class DistractionFeedback(
    val shouldPromptBreakdown: Boolean,
    val shouldPromptDecompression: Boolean,
    val supportiveMessage: String
)

interface BehavioralSupportService {
    fun resetScheduleAnchors(tasks: List<SchedulableTask>): List<SchedulableTask>
    fun generateMicroSteps(taskTitle: String, estimatedMinutes: Int): List<MicroStepSuggestion>
    fun evaluateDistraction(interruptionCount: Int, durationMinutes: Int): DistractionFeedback

    companion object {
        val DEFAULT_DISTRACTING_PACKAGES: Set<String> = setOf(
            "com.instagram.android",
            "com.twitter.android",
            "com.x.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.facebook.lite",
            "com.reddit.frontpage",
            "com.linkedin.android",
            "com.pinterest",
            "com.tumblr",
            "com.vkontakte.android",
            "com.bereal.app",
            "com.threads.android",
            "com.quora.android",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.plus",
            "com.discord",
            "com.facebook.orca",
            "com.facebook.mlite",
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient",
            "com.disney.disneyplus",
            "com.hbo.hbonow",
            "com.hulu.plus",
            "com.twitch.android.app",
            "tv.twitch.android.app",
            "com.vanced.android.youtube",
            "com.king.candycrushsaga",
            "com.king.candycrushsodasaga",
            "com.supercell.clashofclans",
            "com.supercell.clashroyale",
            "com.supercell.brawlstars",
            "com.mojang.minecraftpe",
            "com.roblox.client",
            "com.pubg.imobile",
            "com.tencent.ig",
            "com.activision.callofduty.shooter",
            "com.garena.free.fire",
            "com.epicgames.fortnite",
            "com.ea.game.pvzfh_row",
            "com.halfbrick.fruitninjafree",
            "com.imangi.templerun2",
            "com.outfit7.talkingtomgoldrun2",
            "com.miniclip.eightballpool",
            "com.chess",
            "com.buzzfeed.android"
        )

        fun hasUsagePermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
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
    }
}

@Singleton
class DefaultBehavioralSupportService @Inject constructor() : BehavioralSupportService {

    override fun resetScheduleAnchors(tasks: List<SchedulableTask>): List<SchedulableTask> {
        // Unlock unpinned tasks to allow a completely clean replan
        return tasks.map { task ->
            if (task.isLocked) task else task.copy(scheduledStartMillis = null)
        }
    }

    override fun generateMicroSteps(taskTitle: String, estimatedMinutes: Int): List<MicroStepSuggestion> {
        val clean = taskTitle.trim().take(30)
        return listOf(
            MicroStepSuggestion("Open workspace & gather materials for $clean", 5),
            MicroStepSuggestion("Spend 5 uninterrupted minutes on the very first sub-step", 5),
            MicroStepSuggestion("Review progress & polish final result", 5)
        )
    }

    override fun evaluateDistraction(interruptionCount: Int, durationMinutes: Int): DistractionFeedback {
        // Non-punitive: High distraction on tasks signals cognitive friction or excessive task size,
        // never boosting task priority.
        val isHighInterruption = interruptionCount >= 3 || (interruptionCount >= 2 && durationMinutes <= 25)
        val isLongSlog = durationMinutes >= 60 && interruptionCount >= 2

        return when {
            isLongSlog -> DistractionFeedback(
                shouldPromptBreakdown = true,
                shouldPromptDecompression = true,
                supportiveMessage = "This task seems heavy right now. Would you like to break it into 5-minute micro-steps or take a 3-minute decompression breather?"
            )
            isHighInterruption -> DistractionFeedback(
                shouldPromptBreakdown = true,
                shouldPromptDecompression = false,
                supportiveMessage = "Frequent interruptions detected. Breaking this task down into bite-sized steps can help rebuild momentum."
            )
            else -> DistractionFeedback(
                shouldPromptBreakdown = false,
                shouldPromptDecompression = false,
                supportiveMessage = "Great focus! You are maintaining healthy cognitive flow."
            )
        }
    }
}
