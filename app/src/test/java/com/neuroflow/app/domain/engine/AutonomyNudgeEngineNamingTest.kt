package com.neuroflow.app.domain.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AutonomyNudgeEngineNamingTest : StringSpec({

    "unique work name and tag are stable and task scoped" {
        AutonomyNudgeEngine.uniqueWorkName("task-42") shouldBe "autonomy_nudge_task-42"
        AutonomyNudgeEngine.workTag("task-42") shouldBe "autonomy_nudge_task-42"
    }

    "different task ids produce different work names" {
        AutonomyNudgeEngine.uniqueWorkName("one") shouldNotBe AutonomyNudgeEngine.uniqueWorkName("two")
    }
})
