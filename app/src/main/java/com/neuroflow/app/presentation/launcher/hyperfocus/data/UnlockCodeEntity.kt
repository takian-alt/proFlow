package com.neuroflow.app.presentation.launcher.hyperfocus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neuroflow.app.domain.model.RewardTier

@Entity(tableName = "unlock_codes")
data class UnlockCodeEntity(
    @PrimaryKey val id: String,           // UUID
    val encryptedCode: String,            // Base64(IV + AES-GCM ciphertext)
    val tier: RewardTier,
    val sessionId: String,
    val isUsed: Boolean = false,
    val usedAt: Long? = null,
    val unlockedUntil: Long? = null       // epoch millis, null = permanent (FULL tier)
)
