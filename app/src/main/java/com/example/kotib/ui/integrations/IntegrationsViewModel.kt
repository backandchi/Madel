package com.example.kotib.ui.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kotib.KotibContainer
import com.example.kotib.data.local.entity.IntegrationConfigEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IntegrationTestState(
    val serviceId: String? = null,
    val isTesting: Boolean = false,
    val isSuccess: Boolean = false,
    val message: String = ""
)

class IntegrationsViewModel(
    private val container: KotibContainer
) : ViewModel() {

    private val repo = container.integrationRepository

    val configs: StateFlow<List<IntegrationConfigEntity>> = repo.allConfigs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _testState = MutableStateFlow(IntegrationTestState())
    val testState: StateFlow<IntegrationTestState> = _testState.asStateFlow()

    fun toggleService(serviceId: String, enabled: Boolean) {
        viewModelScope.launch {
            repo.toggleEnabled(serviceId, enabled)
        }
    }

    fun saveConfig(config: IntegrationConfigEntity) {
        viewModelScope.launch {
            repo.saveConfig(config)
        }
    }

    fun testService(serviceId: String) {
        viewModelScope.launch {
            _testState.value = IntegrationTestState(serviceId = serviceId, isTesting = true)
            val (success, msg) = repo.testConnection(serviceId)
            _testState.value = IntegrationTestState(
                serviceId = serviceId,
                isTesting = false,
                isSuccess = success,
                message = msg
            )
        }
    }

    fun clearTestState() {
        _testState.value = IntegrationTestState()
    }

    class Factory(private val container: KotibContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IntegrationsViewModel(container) as T
        }
    }
}
