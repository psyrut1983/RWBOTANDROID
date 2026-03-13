package com.rwbot.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rwbot.android.data.local.SecureSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val wbToken: String = "",
    val yandexApiKey: String = "",
    val yandexFolderId: String = "",
    val complexityThreshold: Int = 4,
    val confidenceThreshold: Double = 0.8,
    val minRating: Int = 3,
    val blacklistText: String = "",
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secureSettings: SecureSettings
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        try {
            _state.value = SettingsUiState(
                wbToken = secureSettings.wbApiToken ?: "",
                yandexApiKey = secureSettings.yandexApiKey ?: "",
                yandexFolderId = secureSettings.yandexFolderId ?: "",
                complexityThreshold = secureSettings.complexityThreshold,
                confidenceThreshold = secureSettings.confidenceThreshold,
                minRating = secureSettings.minRatingForAutoResponse,
                blacklistText = secureSettings.blacklistWords.joinToString("\n")
            )
        } catch (_: Exception) {
            _state.value = SettingsUiState()
        }
    }

    fun updateWbToken(s: String) { _state.value = _state.value.copy(wbToken = s) }
    fun updateYandexApiKey(s: String) { _state.value = _state.value.copy(yandexApiKey = s) }
    fun updateYandexFolderId(s: String) { _state.value = _state.value.copy(yandexFolderId = s) }
    fun updateComplexityThreshold(i: Int) { _state.value = _state.value.copy(complexityThreshold = i) }
    fun updateConfidenceThreshold(d: Double) { _state.value = _state.value.copy(confidenceThreshold = d) }
    fun updateMinRating(i: Int) { _state.value = _state.value.copy(minRating = i) }
    fun updateBlacklistText(s: String) { _state.value = _state.value.copy(blacklistText = s) }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            secureSettings.wbApiToken = s.wbToken.ifBlank { null }
            secureSettings.yandexApiKey = s.yandexApiKey.ifBlank { null }
            secureSettings.yandexFolderId = s.yandexFolderId.ifBlank { null }
            secureSettings.complexityThreshold = s.complexityThreshold
            secureSettings.confidenceThreshold = s.confidenceThreshold
            secureSettings.minRatingForAutoResponse = s.minRating
            secureSettings.blacklistWords = s.blacklistText.lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            _state.value = s.copy(saved = true)
        }
    }
}
