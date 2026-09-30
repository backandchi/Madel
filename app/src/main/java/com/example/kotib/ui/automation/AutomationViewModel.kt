package com.example.kotib.ui.automation

import android.accessibilityservice.AccessibilityService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kotib.KotibContainer
import com.example.kotib.data.local.entity.AutomationTaskEntity
import com.example.kotib.domain.automation.ShellResult
import com.example.kotib.service.KotibAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AutomationUiState(
    val isRootAvailable: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val ringerMode: String = "Odatiy",
    val musicVolumePercent: Int = 50,
    val ringVolumePercent: Int = 50,
    val isPowerSaveMode: Boolean = false,
    val shellResult: ShellResult? = null,
    val isRunningShell: Boolean = false
)

class AutomationViewModel(
    private val container: KotibContainer
) : ViewModel() {

    private val rootManager = container.rootShellManager
    private val sysController = container.systemAutomationController
    private val scheduler = container.automationScheduler

    val tasks: StateFlow<List<AutomationTaskEntity>> = scheduler.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState: StateFlow<AutomationUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val hasRoot = rootManager.isRootAvailable()
            val isAccRunning = KotibAccessibilityService.isRunning()
            val summary = sysController.getVolumeSummary()

            _uiState.value = _uiState.value.copy(
                isRootAvailable = hasRoot,
                isAccessibilityEnabled = isAccRunning,
                ringerMode = summary["ringerMode"]?.toString() ?: "Odatiy",
                musicVolumePercent = summary["musicVolumePercent"] as? Int ?: 50,
                ringVolumePercent = summary["ringVolumePercent"] as? Int ?: 50,
                isPowerSaveMode = summary["isPowerSaveMode"] as? Boolean ?: false
            )
        }
    }

    fun setRingerMode(mode: String) {
        sysController.setRingerMode(mode)
        refreshStatus()
    }

    fun setVolume(stream: String, percent: Int) {
        sysController.setVolumePercent(stream, percent)
        refreshStatus()
    }

    fun openSetting(setting: String) {
        sysController.openSystemSetting(setting)
    }

    fun launchApp(appName: String) {
        sysController.launchApp(appName)
    }

    fun performGlobal(action: Int) {
        KotibAccessibilityService.performGlobalAction(action)
    }

    fun executeShell(command: String, asRoot: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunningShell = true)
            val result = rootManager.executeCommand(command, asRoot = asRoot)
            _uiState.value = _uiState.value.copy(
                isRunningShell = false,
                shellResult = result
            )
            refreshStatus()
        }
    }

    fun toggleTask(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            scheduler.toggleTask(id, enabled)
        }
    }

    fun addTask(title: String, time: String, command: String) {
        viewModelScope.launch {
            scheduler.addTask(title, time, command)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            scheduler.deleteTask(id)
        }
    }

    class Factory(private val container: KotibContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AutomationViewModel(container) as T
        }
    }
}
