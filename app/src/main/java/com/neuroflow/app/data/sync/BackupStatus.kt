package com.neuroflow.app.data.sync

sealed class BackupStatus {
    object Idle : BackupStatus()
    data class InProgress(val message: String = "Backing up...") : BackupStatus()
    /** [backupId] is the Firestore document ID of the newly created backup snapshot. */
    data class Success(val backupId: String, val timestamp: Long = System.currentTimeMillis()) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

sealed class RestoreStatus {
    object Idle : RestoreStatus()
    data class InProgress(val message: String = "Restoring...") : RestoreStatus()
    /** [itemCount] is the total number of entities written to the local database. */
    data class Success(val itemCount: Int = 0, val timestamp: Long = System.currentTimeMillis()) : RestoreStatus()
    data class Error(val message: String) : RestoreStatus()
}

data class BackupMeta(
    val backupId: String,
    val createdAt: Long,
    val deviceId: String,
    val appVersion: String,
    val taskCount: Int,
    val goalCount: Int
)
