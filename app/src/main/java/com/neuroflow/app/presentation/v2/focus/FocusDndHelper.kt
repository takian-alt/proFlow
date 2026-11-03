package com.neuroflow.app.presentation.v2.focus

import android.app.NotificationManager
import android.content.Context

object FocusDndHelper {

    fun enableDndIfPermitted(context: Context?): Boolean {
        return try {
            val ctx = context ?: return false
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return false
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun disableDnd(context: Context?): Boolean {
        return try {
            val ctx = context ?: return false
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return false
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }
}
