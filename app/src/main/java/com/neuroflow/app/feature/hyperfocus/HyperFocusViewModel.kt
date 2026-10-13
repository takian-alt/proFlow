package com.neuroflow.app.feature.hyperfocus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.BuildConfig
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.GateGrantSource
import com.neuroflow.app.feature.hyperfocus.data.GateRequestLogger
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.domain.RewardEngine
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepository
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskEngine
import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HyperFocusProgress(
    val completedSinceActivation: Int,
    val totalTarget: Int,
    val currentTier: RewardTier,
    val fraction: Float,
    val tasksToNextTier: Int
)

@HiltViewModel
class HyperFocusViewModel @Inject constructor(
    private val hyperFocusManager: HyperFocusManager,
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val unlockCodeRepository: UnlockCodeRepository,
    private val taskRepository: TaskRepository,
    private val gateRequestLogger: GateRequestLogger,
    private val appCatalogRepository: AppCatalogRepository,
    private val systemExempt: SystemExempt,
) : ViewModel() {

    val hyperFocusPrefs: StateFlow<HyperFocusPreferences> = hyperFocusDataStore.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HyperFocusPreferences())

    private val tickerFlow = flow {
        while (true) {
            emit(Unit)
            delay(1000)
        }
    }

    val progress: StateFlow<HyperFocusProgress> = combine(
        hyperFocusDataStore.flow,
        taskRepository.observeCompletedTasks()
    ) { prefs, completedTasks ->
        if (prefs.sessionMode == HyperFocusSessionMode.TIME_BASED) {
            return@combine HyperFocusProgress(
                completedSinceActivation = 0,
                totalTarget = 0,
                currentTier = RewardTier.NONE,
                fraction = 0f,
                tasksToNextTier = 0
            )
        }

        val completedSinceActivation = if (prefs.lockedTaskIds.isNotEmpty()) {
            completedTasks.count { it.id in prefs.lockedTaskIds }
                .coerceAtLeast(0)
        } else {
            (completedTasks.size - prefs.tasksCompletedAtActivation).coerceAtLeast(0)
        }
        val fraction = completedSinceActivation.toFloat() / prefs.dailyTaskTarget.coerceAtLeast(1)
        val currentTier = RewardEngine.computeTier(completedSinceActivation, prefs.dailyTaskTarget, prefs.emergencyUsed)
        val tasksToNextTier = RewardEngine.tasksToNextTier(completedSinceActivation, prefs.dailyTaskTarget, prefs.emergencyUsed)
        HyperFocusProgress(
            completedSinceActivation = completedSinceActivation,
            totalTarget = prefs.dailyTaskTarget,
            currentTier = currentTier,
            fraction = fraction.coerceIn(0f, 1f),
            tasksToNextTier = tasksToNextTier
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HyperFocusProgress(0, 0, RewardTier.NONE, 0f, 1)
        )

    val isPassActive: StateFlow<Boolean> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        RewardEngine.isPassActive(prefs, System.currentTimeMillis())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val passSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        RewardEngine.passSecondsRemaining(prefs, System.currentTimeMillis())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sessionSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        if (!prefs.isActive || prefs.sessionMode != HyperFocusSessionMode.TIME_BASED) {
            return@combine null
        }
        val endsAt = prefs.sessionEndsAtMillis ?: return@combine null
        ((endsAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val claimedCodeToShow: MutableStateFlow<String?> = MutableStateFlow(null)
    // Which tier the currently shown code belongs to
    val claimedCodeTier: MutableStateFlow<RewardTier?> = MutableStateFlow(null)

    // Emits the result of the last submitCode call so the UI can react
    val submitCodeResult: MutableStateFlow<com.neuroflow.app.feature.hyperfocus.domain.UnlockResult?> =
        MutableStateFlow(null)

    // Count of unused codes per tier — refreshed whenever prefs change
    val rewardCounts: MutableStateFlow<Map<RewardTier, Int>> = MutableStateFlow(emptyMap())

    init {
        viewModelScope.launch {
            hyperFocusDataStore.flow.collect { prefs ->
                val sessionId = prefs.sessionId ?: return@collect
                val counts = RewardTier.entries
                    .filter { it != RewardTier.NONE }
                    .associateWith { tier ->
                        unlockCodeRepository.countUnusedByTier(sessionId, tier)
                    }
                rewardCounts.value = counts
            }
        }

        viewModelScope.launch {
            combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs -> prefs }
                .collect { prefs ->
                    if (!prefs.isActive || prefs.sessionMode != HyperFocusSessionMode.TIME_BASED) return@collect
                    val endsAt = prefs.sessionEndsAtMillis ?: return@collect
                    if (endsAt <= System.currentTimeMillis()) {
                        hyperFocusManager.deactivate()
                    }
                }
        }
    }

    val lockoutSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        val expiresAt = prefs.lockoutExpiresAt ?: return@combine null
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000
        if (remaining > 0) remaining else null
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeTasks: StateFlow<List<TaskEntity>> = taskRepository.observeActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTaskCount: StateFlow<Int> = taskRepository.observeActiveTasks()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ---- Gate, gateway, emergency (spec 5.2 and 5.3) ----

    /**
     * One engine for the whole ViewModel, so the task the screen renders is the task the answer is
     * checked against, and a rotation re-reads the same stimulus instead of rolling a new one.
     */
    val gateEngine = GateTaskEngine()
    val gateTask = MutableStateFlow(gateEngine.newTask())

    private val _gatePassed = MutableStateFlow(false)
    val gatePassed: StateFlow<Boolean> = _gatePassed

    private data class GatewayCatalog(
        val entries: List<AppCatalogRepository.AppEntry> = emptyList(),
        val exempt: Set<String> = emptySet()
    )

    /** Swept once per ViewModel, not once per tick: both of these are cross-process calls. */
    private val gatewayCatalog = MutableStateFlow(GatewayCatalog())

    /**
     * Hidden = launchable and not in the resolved allowlist. The exempt set is read with the catalog
     * because it is stable for the life of this screen, while the pass underneath the allowlist is
     * not — so the filtering re-runs on every prefs change and the sweeping never does.
     */
    val hiddenApps: StateFlow<List<AppCatalogRepository.AppEntry>> = combine(
        hyperFocusDataStore.flow,
        gatewayCatalog
    ) { prefs, catalog ->
        if (!prefs.isActive) return@combine emptyList()
        val keep = FocusSurfaceModel.resolvedAllowlist(
            prefs, catalog.exempt, BuildConfig.APPLICATION_ID, System.currentTimeMillis()
        )
        catalog.entries.filter { it.packageName !in keep }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * E1: an unspent budget keeps ticking whether or not an app holds it, which is exactly why this
     * is not `passSecondsRemaining` — that reads null the moment the granted app leaves the
     * foreground, and the gateway needs to say how much of the window the code bought is left.
     */
    val gateBudgetSecondsRemaining: StateFlow<Long?> = combine(
        tickerFlow,
        hyperFocusDataStore.flow
    ) { _, prefs ->
        val now = System.currentTimeMillis()
        prefs.activePassExpiresAt?.takeIf { it > now }?.let { (it - now) / 1000 }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val emergencyAvailable: StateFlow<Boolean> = hyperFocusDataStore.flow
        .map { it.isActive && !it.emergencyUsed && it.sessionMode != HyperFocusSessionMode.TIME_BASED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val entries = appCatalogRepository.launchableApps()
            val exempt = systemExempt.packages()
            gatewayCatalog.value = GatewayCatalog(entries, exempt)
        }
    }

    /** Whoever opens the gate names the app it was opened for; the log row outlives the navigation. */
    fun beginGate(packageName: String) {
        viewModelScope.launch {
            hyperFocusDataStore.update {
                it.copy(pendingGatePackage = packageName.takeIf { name -> name.isNotBlank() })
            }
        }
    }

    /**
     * Returns the decision synchronously, before the logging coroutine runs, so the screen can tally
     * a failed attempt on the click that failed it. Reading `gatePassed` instead would race the
     * launch and count a pass as a mismatch.
     */
    fun submitGateAnswer(answer: List<String>, reason: String, mismatchCount: Int, elapsedMillis: Long): Boolean {
        val task = gateTask.value
        val passed = gateEngine.checkAnswer(task, answer) || gateEngine.shouldAutoPass(mismatchCount)
        val willLog = passed && gateEngine.allowsSubmit(elapsedMillis, reason)
        viewModelScope.launch {
            if (willLog) {
                val prefs = hyperFocusDataStore.current()
                gateRequestLogger.log(
                    packageName = prefs.pendingGatePackage.orEmpty(),
                    statedReason = reason.trim(),
                    taskType = task.type.name,
                    mismatchCount = mismatchCount,
                    elapsedMillis = elapsedMillis,
                    grantSource = GateGrantSource.REWARD_CODE
                )
            }
            _gatePassed.value = willLog
        }
        return willLog
    }

    /** The gate is single-shot: consuming it shuts the route and rolls the next visit's stimulus. */
    fun consumeGatePass() {
        _gatePassed.value = false
        gateTask.value = gateEngine.newTask()
    }

    fun grantPassFor(packageName: String, onGranted: () -> Unit = {}) {
        viewModelScope.launch {
            // No fallback tier. Minutes are only ever what a code already bought (E1), so with no
            // budget there is nothing to spend and the gateway has to stay shut. The deadline is read
            // from the store, not from gateBudgetSecondsRemaining: that flow only carries a value
            // while a screen is subscribed to it, and an unsubscribed read would refuse a live budget.
            val now = System.currentTimeMillis()
            val budgetSeconds = hyperFocusDataStore.current().activePassExpiresAt
                ?.takeIf { it > now }
                ?.let { (it - now) / 1000 }
                ?: return@launch
            val minutes = (budgetSeconds / 60).coerceAtLeast(1L).toInt()
            hyperFocusManager.grantPass(packageName, minutes)
            // Launch on the written state, not on the request: the enforcer reads the store, so an
            // app opened before the grant lands gets bounced straight back to this screen.
            if (hyperFocusDataStore.current().activePassPackage == packageName) onGranted()
        }
    }

    fun grantEmergencyFor(packageName: String) {
        viewModelScope.launch {
            val before = hyperFocusDataStore.current()
            hyperFocusManager.triggerEmergencyBypass(packageName)
            // The manager refuses a second emergency in a session; logging a grant that never
            // happened would make the audit row claim an access the user did not get.
            if (before.emergencyUsed) return@launch
            gateRequestLogger.logEmergencyGrant(packageName)
        }
    }

    fun activate(blockedPackages: Set<String>, selectedTaskIds: Set<String>) {
        viewModelScope.launch {
            if (blockedPackages.isEmpty()) return@launch

            val activeIds = taskRepository.getActiveTasks().map { it.id }.toSet()
            val normalizedSelection = selectedTaskIds.intersect(activeIds)
            if (normalizedSelection.isEmpty()) return@launch

            hyperFocusManager.activate(
                blockedPackages = blockedPackages,
                dailyTaskTarget = normalizedSelection.size,
                lockedTaskIds = normalizedSelection
            )
        }
    }

    fun activateTimed(blockedPackages: Set<String>, durationMinutes: Int) {
        viewModelScope.launch {
            if (blockedPackages.isEmpty()) return@launch
            hyperFocusManager.activateTimed(blockedPackages, durationMinutes)
        }
    }

    fun addTasksToActiveSession(taskIds: Set<String>) {
        viewModelScope.launch {
            hyperFocusManager.addTasksToSession(taskIds)
        }
    }

    fun claimRewardForTier(tier: RewardTier) {
        viewModelScope.launch {
            val prefs = hyperFocusDataStore.current()
            if (prefs.sessionMode == HyperFocusSessionMode.TIME_BASED) return@launch
            val sessionId = prefs.sessionId ?: return@launch
            val currentProgress = progress.value

            val canClaimTier = RewardEngine.isTierEarned(
                tier = tier,
                completedSinceActivation = currentProgress.completedSinceActivation,
                totalTarget = currentProgress.totalTarget,
                emergencyUsed = prefs.emergencyUsed
            )
            if (!canClaimTier) return@launch

            // If there's already a pending code for this same tier, just show it again
            if (prefs.pendingCodeId != null && claimedCodeTier.value == tier) {
                val existing = unlockCodeRepository.getCodeById(prefs.pendingCodeId)
                claimedCodeToShow.value = existing
                return@launch
            }

            // Issue a new code for the requested tier
            val result = unlockCodeRepository.getClaimableCode(sessionId, tier) ?: return@launch
            val (codeId, plaintext) = result

            hyperFocusDataStore.update { it.copy(pendingCodeId = codeId) }
            claimedCodeTier.value = tier
            claimedCodeToShow.value = plaintext

            // Refresh counts
            val counts = RewardTier.entries
                .filter { it != RewardTier.NONE }
                .associateWith { t -> unlockCodeRepository.countUnusedByTier(sessionId, t) }
            rewardCounts.value = counts
        }
    }

    fun dismissClaimedCode() {
        claimedCodeToShow.value = null
        claimedCodeTier.value = null
    }

    fun submitCode(entered: String) {
        viewModelScope.launch {
            val result = hyperFocusManager.submitCode(entered)
            submitCodeResult.value = result
        }
    }

    fun clearSubmitCodeResult() {
        submitCodeResult.value = null
    }

    fun completePlanning(tomorrowTaskTitles: List<String>) {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val tomorrowMidnight = cal.timeInMillis
            val now = System.currentTimeMillis()
            
            tomorrowTaskTitles.filter { it.isNotBlank() }.forEach { title ->
                taskRepository.insert(
                    com.neuroflow.app.data.local.entity.TaskEntity(
                        title = title.trim(),
                        scheduledDate = tomorrowMidnight,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            hyperFocusManager.completePlanning()
        }
    }
}
