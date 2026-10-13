package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.GateRequestLogger
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class HyperFocusHomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val hyperFocusManager: HyperFocusManager,
    private val appCatalogRepository: AppCatalogRepository,
    private val systemExempt: SystemExempt,
    private val gateRequestLogger: GateRequestLogger,
    private val realHomeResolver: RealHomeResolver
) : ViewModel() {

    private val tick = MutableStateFlow(System.currentTimeMillis())
    private val labels = MutableStateFlow<Map<String, String>>(emptyMap())
    private val entries = MutableStateFlow<List<AppCatalogRepository.AppEntry>>(emptyList())
    private var storedHome: String? = null
    private val _handoffTarget = MutableStateFlow<ComponentName?>(null)
    val handoffTarget: StateFlow<ComponentName?> = _handoffTarget.asStateFlow()

    val state: StateFlow<HomeSurfaceState> =
        combine(hyperFocusDataStore.flow, tick, labels) { prefs, now, known ->
            homeSurfaceStateOf(
                prefs = prefs,
                labels = known,
                // STATIC, not packages(): the grid must hide never-gated machinery, but the dialers
                // are in packages() and must stay visible tiles (spec 6.2 + 6.4).
                systemExempt = SystemExempt.STATIC,
                selfPackage = context.packageName,
                nowMillis = now
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSurfaceState.HandOff)

    init {
        viewModelScope.launch {
            while (isActive) {
                tick.value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val all = appCatalogRepository.launchableApps()
            entries.value = all
            labels.value = all.associate { it.packageName to it.label }
        }
    }

    /** Hidden = launchable and not in the resolved allowlist: what the gate and the emergency spend. */
    val hiddenApps: StateFlow<List<AppCatalogRepository.AppEntry>> =
        combine(hyperFocusDataStore.flow, entries) { prefs, all ->
            if (!prefs.isActive) emptyList()
            else {
                val keep = FocusSurfaceModel.resolvedAllowlist(
                    prefs, systemExempt.packages(), context.packageName, System.currentTimeMillis()
                )
                all.filter { it.packageName !in keep }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val emergencyAvailable: StateFlow<Boolean> = hyperFocusDataStore.flow
        .map { it.isActive && !it.emergencyUsed && it.sessionMode != HyperFocusSessionMode.TIME_BASED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Called from the activity's `onCreate` and `onResume`; cheap and idempotent. */
    fun resolveHandoff() {
        viewModelScope.launch {
            val prefs = hyperFocusDataStore.current()
            val known = prefs.realHomeComponent
            storedHome = known
            val target = realHomeResolver.handoff(known)
            _handoffTarget.value = target
            if (known == null && target != null) {
                hyperFocusDataStore.update { it.copy(realHomeComponent = componentKey(target)) }
            }
        }
    }

    fun open(packageName: String) {
        viewModelScope.launch { appCatalogRepository.launch(packageName) }
    }

    /**
     * Ungated by F4, so it is reachable straight from the home — but it is still a grant, and the
     * gateway logs its emergency. A home that grants without writing the row would leave the audit
     * looking like the user never left the black screen.
     */
    fun emergencyOpen(packageName: String) {
        viewModelScope.launch {
            val before = hyperFocusDataStore.current()
            hyperFocusManager.triggerEmergencyBypass(packageName)
            if (!before.emergencyUsed) gateRequestLogger.logEmergencyGrant(packageName)
            appCatalogRepository.launch(packageName)
        }
    }

    fun deactivate() {
        viewModelScope.launch { hyperFocusManager.deactivate() }
    }

    fun openHomeAppSettings() {
        runCatching {
            context.startActivity(
                Intent("android.settings.HOME_SETTINGS")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
