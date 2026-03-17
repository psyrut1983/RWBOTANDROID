package com.rwbot.android.ui.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.data.repository.ReviewRepository
import com.rwbot.android.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewsUiState(
    val reviews: List<ReviewEntity> = emptyList(),
    val filter: ReviewStatus? = null,
    val syncMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _filter = MutableStateFlow<ReviewStatus?>(null)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val state: StateFlow<ReviewsUiState> = combine(
        reviewRepository.getAllReviewsFlow(),
        _filter,
        _syncMessage,
        _isLoading
    ) { list, filter, msg, loading ->
        val filtered = if (filter == null) list else list.filter { it.status == filter }
        ReviewsUiState(
            reviews = filtered,
            filter = filter,
            syncMessage = msg,
            isLoading = loading
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), ReviewsUiState())

    fun setFilter(filter: ReviewStatus?) {
        _filter.value = filter
    }

    fun sync() {
        viewModelScope.launch {
            _isLoading.value = true
            _syncMessage.value = null
            when (val r = reviewRepository.syncFromWildberries()) {
                is Result.Success -> _syncMessage.value = "Загружено: ${r.data}"
                is Result.Error -> _syncMessage.value = r.message
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() { _syncMessage.value = null }

    /** Поток количества неотвеченных отзывов (NEW + ON_MODERATION) для бейджа на иконке. */
    val unansweredCountFlow: Flow<Int> = reviewRepository.getUnansweredCountFlow()
}
