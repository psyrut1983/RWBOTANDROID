package com.rwbot.android.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rwbot.android.data.local.dao.ReviewDao
import com.rwbot.android.data.local.entity.ReviewStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val total: Int = 0,
    val answered: Int = 0,
    val onModeration: Int = 0,
    val rejected: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val reviewDao: ReviewDao
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = StatsUiState(
                total = reviewDao.countByStatus(ReviewStatus.NEW) +
                    reviewDao.countByStatus(ReviewStatus.ON_MODERATION) +
                    reviewDao.countByStatus(ReviewStatus.ANSWERED) +
                    reviewDao.countByStatus(ReviewStatus.REJECTED),
                answered = reviewDao.countByStatus(ReviewStatus.ANSWERED),
                onModeration = reviewDao.countByStatus(ReviewStatus.ON_MODERATION),
                rejected = reviewDao.countByStatus(ReviewStatus.REJECTED)
            )
        }
    }
}
