package com.rwbot.android.ui.reviews

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.data.repository.Result
import com.rwbot.android.data.repository.ReviewRepository
import com.rwbot.android.domain.pipeline.PipelineResult
import com.rwbot.android.domain.pipeline.ReviewPipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewDetailUiState(
    val review: ReviewEntity? = null,
    val message: String? = null,
    val processing: Boolean = false
)

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reviewRepository: ReviewRepository,
    private val reviewPipeline: ReviewPipeline
) : ViewModel() {

    private val reviewId: String? = savedStateHandle.get<String>("reviewId")
    private val _state = MutableStateFlow(ReviewDetailUiState())
    val state: StateFlow<ReviewDetailUiState> = _state.asStateFlow()

    init {
        reviewId?.let { load(it) }
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val r = reviewRepository.getReviewById(id)
            _state.value = _state.value.copy(review = r)
        }
    }

    fun process() {
        val review = _state.value.review ?: return
        if (review.status != ReviewStatus.NEW) return
        viewModelScope.launch {
            _state.value = _state.value.copy(processing = true, message = null)
            when (val result = reviewPipeline.processReview(review)) {
                is PipelineResult.AutoSent -> _state.value = _state.value.copy(review = result.review, message = "Ответ отправлен в WB", processing = false)
                is PipelineResult.OnModeration -> _state.value = _state.value.copy(review = result.review, message = "Отзыв на модерации", processing = false)
                is PipelineResult.Error -> _state.value = _state.value.copy(message = result.message, processing = false)
            }
        }
    }

    fun approve() {
        val review = _state.value.review ?: return
        val text = review.generatedResponse ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(processing = true, message = null)
            when (val r = reviewRepository.sendAnswerToWildberries(review.id, text)) {
                is Result.Success -> {
                    reviewRepository.updateReview(review.copy(status = ReviewStatus.ANSWERED, updatedAt = System.currentTimeMillis()))
                    _state.value = _state.value.copy(review = review.copy(status = ReviewStatus.ANSWERED), message = "Отправлено", processing = false)
                }
                is Result.Error -> _state.value = _state.value.copy(message = r.message, processing = false)
            }
        }
    }

    fun reject() {
        val review = _state.value.review ?: return
        viewModelScope.launch {
            reviewRepository.updateReview(review.copy(status = ReviewStatus.REJECTED, updatedAt = System.currentTimeMillis()))
            _state.value = _state.value.copy(review = review.copy(status = ReviewStatus.REJECTED), message = "Отклонено")
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}
