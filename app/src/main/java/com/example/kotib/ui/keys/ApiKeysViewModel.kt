package com.example.kotib.ui.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kotib.data.local.entity.ApiKeyEntity
import com.example.kotib.data.repository.ApiKeyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ApiKeysUiState(
    val keys: List<ApiKeyEntity> = emptyList(),
    val testingKeyId: Long? = null,
    val testResultMessage: String? = null,
    val isTestSuccess: Boolean = false,
    val isAddingKey: Boolean = false
)

class ApiKeysViewModel(
    private val apiKeyRepository: ApiKeyRepository
) : ViewModel() {

    private val _testingState = MutableStateFlow<Triple<Long?, String?, Boolean>>(Triple(null, null, false))

    val uiState: StateFlow<List<ApiKeyEntity>> = apiKeyRepository.allKeys.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _testMessage = MutableStateFlow<String?>(null)
    val testMessage: StateFlow<String?> = _testMessage.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    fun addKey(key: String, label: String) {
        if (key.isBlank()) return
        viewModelScope.launch {
            apiKeyRepository.addKey(key, label)
        }
    }

    fun deleteKey(id: Long) {
        viewModelScope.launch {
            apiKeyRepository.deleteKey(id)
        }
    }

    fun setActiveKey(id: Long) {
        viewModelScope.launch {
            apiKeyRepository.setActiveKey(id)
        }
    }

    fun reactivateAll() {
        viewModelScope.launch {
            apiKeyRepository.reactivateAllKeys()
        }
    }

    fun testKey(key: ApiKeyEntity) {
        viewModelScope.launch {
            _isTesting.value = true
            _testMessage.value = "'${key.label}' tekshirilmoqda..."
            val (success, message) = apiKeyRepository.testKey(key.key)
            _isTesting.value = false
            _testMessage.value = if (success) "Muvaffaqiyatli: $message" else "Xato: $message"
        }
    }

    fun clearTestMessage() {
        _testMessage.value = null
    }

    class Factory(private val apiKeyRepository: ApiKeyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApiKeysViewModel(apiKeyRepository) as T
        }
    }
}
