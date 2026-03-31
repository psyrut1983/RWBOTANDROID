package com.rwbot.android.ui.reviews

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewsUiState(
    val reviews: List<ReviewEntity> = emptyList(),
    val selectedReview: ReviewEntity? = null,
    val filter: ReviewStatus? = null,
    val syncMessage: String? = null,
    val reviewMessage: String? = null,
    val isLoading: Boolean = false,
    val processing: Boolean = false,
    val draftResponseText: String = "",
    val isApprovalDialogVisible: Boolean = false
)

private data class ReviewsLocalUiState(
    val filter: ReviewStatus? = null,
    val syncMessage: String? = null,
    val isLoading: Boolean = false,
    val selectedReviewId: String? = null,
    val reviewMessage: String? = null,
    val processing: Boolean = false,
    val draftResponseText: String = "",
    val isApprovalDialogVisible: Boolean = false
)

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val reviewPipeline: ReviewPipeline,
    private val ragRetriever: RagRetriever
) : ViewModel() {

    private val _filter = MutableStateFlow<ReviewStatus?>(null)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _selectedReviewId = MutableStateFlow<String?>(null)
    private val _reviewMessage = MutableStateFlow<String?>(null)
    private val _processing = MutableStateFlow(false)
    private val _draftResponseText = MutableStateFlow("")
    private val _isApprovalDialogVisible = MutableStateFlow(false)

    private val localUiState = combine(
        combine(_filter, _syncMessage, _isLoading) { filter, syncMessage, isLoading ->
            Triple(filter, syncMessage, isLoading)
        },
        combine(_selectedReviewId, _reviewMessage, _processing) { selectedReviewId, reviewMessage, processing ->
            Triple(selectedReviewId, reviewMessage, processing)
        },
        combine(_draftResponseText, _isApprovalDialogVisible) { draftResponseText, isApprovalDialogVisible ->
            draftResponseText to isApprovalDialogVisible
        }
    ) { topState, middleState, bottomState ->
        ReviewsLocalUiState(
            filter = topState.first,
            syncMessage = topState.second,
            isLoading = topState.third,
            selectedReviewId = middleState.first,
            reviewMessage = middleState.second,
            processing = middleState.third,
            draftResponseText = bottomState.first,
            isApprovalDialogVisible = bottomState.second
        )
    }

    val state: StateFlow<ReviewsUiState> = combine(
        reviewRepository.getAllReviewsFlow(),
        localUiState
    ) { list, localState ->
        // На главном экране показываем только "живые" отзывы, с которыми еще нужно что-то сделать.
        val activeReviews = list.filter { it.status == ReviewStatus.NEW || it.status == ReviewStatus.ON_MODERATION }
        val filtered = when (localState.filter) {
            null -> activeReviews
            ReviewStatus.NEW -> activeReviews.filter { it.status == ReviewStatus.NEW }
            ReviewStatus.ON_MODERATION -> activeReviews.filter { it.status == ReviewStatus.ON_MODERATION }
            else -> emptyList()
        }
        val selectedReview = list.firstOrNull { it.id == localState.selectedReviewId }
        ReviewsUiState(
            reviews = filtered,
            selectedReview = selectedReview,
            filter = localState.filter,
            syncMessage = localState.syncMessage,
            reviewMessage = localState.reviewMessage,
            isLoading = localState.isLoading,
            processing = localState.processing,
            draftResponseText = localState.draftResponseText,
            isApprovalDialogVisible = localState.isApprovalDialogVisible
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReviewsUiState())

    fun setFilter(filter: ReviewStatus?) {
        _filter.value = filter
    }

    fun selectReview(reviewId: String) {
        _selectedReviewId.value = reviewId
        _reviewMessage.value = null
        _draftResponseText.value = ""
        _isApprovalDialogVisible.value = false
    }

    fun closeReviewDetails() {
        _selectedReviewId.value = null
        _reviewMessage.value = null
        _draftResponseText.value = ""
        _isApprovalDialogVisible.value = false
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

    fun clearReviewMessage() {
        _reviewMessage.value = null
    }

    fun processSelectedReview() {
        val review = state.value.selectedReview ?: return
        if (review.status != ReviewStatus.NEW) return
        viewModelScope.launch {
            _processing.value = true
            _reviewMessage.value = null
            when (val result = reviewPipeline.processReview(review)) {
                is PipelineResult.OnModeration -> {
                    _draftResponseText.value = result.review.generatedResponse.orEmpty()
                    _reviewMessage.value = "Проверьте и при необходимости отредактируйте ответ"
                    _isApprovalDialogVisible.value = true
                }
                is PipelineResult.Error -> {
                    _reviewMessage.value = result.message
                }
            }
            _processing.value = false
        }
    }

    fun openApprovalDialog() {
        val review = state.value.selectedReview ?: return
        val text = review.generatedResponse ?: return
        _draftResponseText.value = text
        _reviewMessage.value = null
        _isApprovalDialogVisible.value = true
    }

    fun updateDraftResponse(newText: String) {
        _draftResponseText.value = newText
    }

    fun dismissApprovalDialog() {
        _isApprovalDialogVisible.value = false
    }

    fun sendApprovedAnswer() {
        val review = state.value.selectedReview ?: return
        val text = _draftResponseText.value.trim()
        if (text.isBlank()) {
            _reviewMessage.value = "Текст ответа пустой"
            return
        }
        viewModelScope.launch {
            _processing.value = true
            _reviewMessage.value = null
            when (val result = reviewRepository.sendAnswerToWildberries(review.id, text)) {
                is Result.Success -> {
                    reviewRepository.updateReview(
                        review.copy(
                            status = ReviewStatus.ANSWERED,
                            generatedResponse = text,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    // Сохраняем уже финальную версию ответа, если пользователь ее отредактировал.
                    ragRetriever.addToArchive(review.id, review.text, review.rating, text)
                    _isApprovalDialogVisible.value = false
                    _processing.value = false
                    closeReviewDetails()
                }
                is Result.Error -> {
                    _reviewMessage.value = result.message
                    _processing.value = false
                }
            }
        }
    }

    fun rejectSelectedReview() {
        val review = state.value.selectedReview ?: return
        viewModelScope.launch {
            reviewRepository.updateReview(
                review.copy(
                    status = ReviewStatus.REJECTED,
                    updatedAt = System.currentTimeMillis()
                )
            )
            closeReviewDetails()
        }
    }

    /** Поток количества неотвеченных отзывов (NEW + ON_MODERATION) для бейджа на иконке. */
    val unansweredCountFlow: Flow<Int> = reviewRepository.getUnansweredCountFlow()
}
