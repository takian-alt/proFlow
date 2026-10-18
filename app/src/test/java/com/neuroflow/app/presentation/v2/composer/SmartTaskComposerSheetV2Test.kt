package com.neuroflow.app.presentation.v2.composer

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartTaskComposerSheetV2Test {

    @Test
    fun testSheetModeSealedInterface() {
        val createMode: SheetMode = SheetMode.Create
        assertTrue(createMode is SheetMode.Create)

        val sampleTask = TaskEntity(
            id = "task-edit-1",
            title = "Polymorphic Architecture Review",
            estimatedDurationMinutes = 45,
            energyLevel = EnergyLevel.HIGH,
            quadrant = Quadrant.DO_FIRST,
            taskType = TaskType.ANALYTICAL,
            isFrog = true
        )

        val editMode: SheetMode = SheetMode.Edit(sampleTask)
        assertTrue(editMode is SheetMode.Edit)
        assertEquals("task-edit-1", (editMode as SheetMode.Edit).task.id)
        assertEquals("Polymorphic Architecture Review", editMode.task.title)
        assertEquals(45, editMode.task.estimatedDurationMinutes)
        assertEquals(EnergyLevel.HIGH, editMode.task.energyLevel)
        assertTrue(editMode.task.isFrog)
    }
}
