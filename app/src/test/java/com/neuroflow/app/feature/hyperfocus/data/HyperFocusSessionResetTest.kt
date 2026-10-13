package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.HyperFocusState
import com.neuroflow.app.domain.model.RewardTier
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HyperFocusSessionResetTest : StringSpec({

    val session = HyperFocusPreferences(
        isActive = true,
        sessionId = "session-1",
        state = HyperFocusState.FULL_REWARD_PENDING,
        essentialPackages = setOf("com.android.dialer"),
        allowedPackages = setOf("com.todoist"),
        activePassPackage = "com.todoist",
        activePassExpiresAt = 9_000L,
        realHomeComponent = "com.sg-ai.power_launcher/com.sg_ai.MainActivity",
        currentTier = RewardTier.PARTIAL,
        tasksCompletedAtActivation = 3,
        dailyTaskTarget = 5,
        lastServiceHeartbeat = 123L,
        wrongCodeAttempts = 2,
        lockoutExpiresAt = 45L,
        isTamperDetected = true,
        tamperReason = "accessibility turned off",
        tamperDetectedAt = 66L,
        pendingCodeId = "code-1",
        lockedTaskIds = setOf("t1", "t2"),
        emergencyUsed = true,
        sessionMode = HyperFocusSessionMode.TIME_BASED,
        sessionDurationMinutes = 30,
        sessionEndsAtMillis = 99L,
        allowlistMigratedAt = 1_000L,
        distractionScores = mapOf("com.instagram.android" to 82)
    )

    "SR-1: deactivation clears the session but keeps the user's global settings" {
        val cleared = session.afterSession()

        cleared.isActive shouldBe false
        cleared.sessionId shouldBe null
        cleared.state shouldBe HyperFocusState.INACTIVE
        cleared.currentTier shouldBe RewardTier.NONE
        cleared.tasksCompletedAtActivation shouldBe 0
        cleared.dailyTaskTarget shouldBe 0
        cleared.wrongCodeAttempts shouldBe 0
        cleared.lockoutExpiresAt shouldBe null
        cleared.isTamperDetected shouldBe false
        cleared.tamperReason shouldBe null
        cleared.tamperDetectedAt shouldBe null
        cleared.pendingCodeId shouldBe null
        cleared.lockedTaskIds shouldBe emptySet()
        cleared.emergencyUsed shouldBe false
        cleared.sessionMode shouldBe HyperFocusSessionMode.TASK_BASED
        cleared.sessionDurationMinutes shouldBe null
        cleared.sessionEndsAtMillis shouldBe null
        cleared.activePassPackage shouldBe null
        cleared.activePassExpiresAt shouldBe null

        cleared.essentialPackages shouldBe setOf("com.android.dialer")
        cleared.allowedPackages shouldBe setOf("com.todoist")
        cleared.distractionScores shouldBe mapOf("com.instagram.android" to 82)
        cleared.allowlistMigratedAt shouldBe 1_000L
        cleared.realHomeComponent shouldBe "com.sg-ai.power_launcher/com.sg_ai.MainActivity"
    }

    "SR-2: the migration flag outlives the session it was written in" {
        // If deactivation erased allowlistMigratedAt, the runner would fire again at the next
        // startup and overwrite an allowlist the user had since edited with a fresh derivation of
        // the installed app catalog. One-shot has to mean one-shot.
        val afterDeactivation = session.afterSession()
        afterDeactivation.allowlistMigratedAt shouldBe session.allowlistMigratedAt
        afterDeactivation.allowedPackages shouldBe session.allowedPackages
    }
})
