package com.neuroflow.app.presentation.launcher.hyperfocus.domain

import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.presentation.launcher.hyperfocus.data.UnlockCodeDao
import com.neuroflow.app.presentation.launcher.hyperfocus.data.UnlockCodeEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface UnlockCodeRepository {
    suspend fun generateCodePool(sessionId: String, poolSize: Int = 50)
    suspend fun getClaimableCode(sessionId: String, tier: RewardTier): String?
    suspend fun validateAndClaim(sessionId: String, enteredCode: String): UnlockCodeEntity?
    suspend fun markUsed(codeId: String, unlockedUntil: Long?)
    suspend fun deleteSessionCodes(sessionId: String)
}

@Singleton
class UnlockCodeRepositoryImpl @Inject constructor(
    private val dao: UnlockCodeDao
) : UnlockCodeRepository {

    // Visually unambiguous characters only (excludes 0, O, 1, I)
    private val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private fun generateCode(): String =
        (1..6).map { CODE_CHARS.random() }.joinToString("")

    override suspend fun generateCodePool(sessionId: String, poolSize: Int) {
        // Distribute tiers: first 20 MICRO, next 15 PARTIAL, next 10 EARNED, last 5 FULL
        // For poolSize != 50, scale proportionally using the same ratios
        val tierDistribution = buildTierDistribution(poolSize)

        val entities = tierDistribution.map { tier ->
            val plaintext = generateCode()
            val encrypted = AESUtil.encrypt(plaintext)
            UnlockCodeEntity(
                id = UUID.randomUUID().toString(),
                encryptedCode = encrypted,
                tier = tier,
                sessionId = sessionId
            )
        }

        dao.insertAll(entities)
    }

    private fun buildTierDistribution(poolSize: Int): List<RewardTier> {
        if (poolSize == 50) {
            return List(20) { RewardTier.MICRO } +
                    List(15) { RewardTier.PARTIAL } +
                    List(10) { RewardTier.EARNED } +
                    List(5) { RewardTier.FULL }
        }
        // Scale proportionally: 40% MICRO, 30% PARTIAL, 20% EARNED, 10% FULL
        val micro = (poolSize * 0.4).toInt()
        val partial = (poolSize * 0.3).toInt()
        val earned = (poolSize * 0.2).toInt()
        val full = poolSize - micro - partial - earned
        return List(micro) { RewardTier.MICRO } +
                List(partial) { RewardTier.PARTIAL } +
                List(earned) { RewardTier.EARNED } +
                List(full) { RewardTier.FULL }
    }

    override suspend fun getClaimableCode(sessionId: String, tier: RewardTier): String? {
        val unused = dao.getUnusedBySession(sessionId)
        // Use first (not random) so repeated calls return the same code until it's claimed
        val picked = unused.firstOrNull { it.tier == tier } ?: return null
        return AESUtil.decrypt(picked.encryptedCode)
    }

    override suspend fun validateAndClaim(sessionId: String, enteredCode: String): UnlockCodeEntity? {
        val normalized = enteredCode.trim().uppercase()
        val unused = dao.getUnusedBySession(sessionId)
        return unused.firstOrNull { entity ->
            runCatching { AESUtil.decrypt(entity.encryptedCode) }
                .getOrNull() == normalized
        }
    }

    override suspend fun markUsed(codeId: String, unlockedUntil: Long?) {
        dao.markUsed(codeId, System.currentTimeMillis(), unlockedUntil)
    }

    override suspend fun deleteSessionCodes(sessionId: String) {
        dao.deleteBySession(sessionId)
    }
}
