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
import com.rwbot.android.domain.rag.RagRetriever
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewDetailUiState(
    val review: ReviewEntity? = null,
    val message: String? = null,
    val processing: Boolean = false,
    // Черновик ответа, который пользователь может отредактировать перед отправкой
    val draftResponseText: String = "",
    // Показывать ли окно предпросмотра/редактирования
    val isApprovalDialogVisible: Boolean = false
)

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reviewRepository: ReviewRepository,
    private val reviewPipeline: ReviewPipeline,
    private val ragRetriever: RagRetriever
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
                is PipelineResult.OnModeration -> _state.value = _state.value.copy(
                    review = result.review,
                    message = "Проверьте и отредактируйте ответ перед отправкой",
                    processing = false,
                    draftResponseText = result.review.generatedResponse.orEmpty(),
                    isApprovalDialogVisible = true
                )
                is PipelineResult.Error -> _state.value = _state.value.copy(message = result.message, processing = false)
            }
        }
    }

    /**
     * Раньше эта кнопка сразу отправляла ответ.
     * Теперь она открывает окно одобрения, чтобы пользователь мог отредактировать текст.
     */
    fun approve() {
        val review = _state.value.review ?: return
        val text = review.generatedResponse ?: return
        _state.value = _state.value.copy(
            draftResponseText = text,
            isApprovalDialogVisible = true,
            message = null
        )
    }

    fun onDraftResponseChanged(newText: String) {
        _state.value = _state.value.copy(draftResponseText = newText)
    }

    fun dismissApprovalDialog() {
        _state.value = _state.value.copy(isApprovalDialogVisible = false)
    }

    fun sendApprovedAnswer() {
        val review = _state.value.review ?: return
        val text = _state.value.draftResponseText.trim()
        if (text.isBlank()) {
            _state.value = _state.value.copy(message = "Текст ответа пустой")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(processing = true, message = null)
            when (val r = reviewRepository.sendAnswerToWildberries(review.id, text)) {
                is Result.Success -> {
                    reviewRepository.updateReview(review.copy(status = ReviewStatus.ANSWERED, updatedAt = System.currentTimeMillis()))
                    // Обновить архив RAG финальным отправленным ответом (если пользователь редактировал)
                    ragRetriever.addToArchive(review.id, review.text, text)
                    _state.value = _state.value.copy(
                        review = review.copy(status = ReviewStatus.ANSWERED, generatedResponse = text),
                        message = "Отправлено",
                        processing = false,
                        isApprovalDialogVisible = false
                    )
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
