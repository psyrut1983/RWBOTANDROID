package com.rwbot.android.domain.classification

/**
 * Результат классификации отзыва (правила на Kotlin).
 */
data class ClassificationResult(
    val category: ReviewCategory,
    val complexityScore: Int,
    val blacklistTriggered: Boolean
)

enum class ReviewCategory {
    GRATITUDE,
    COMPLAINT,
    QUESTION,
    NEUTRAL
}

/**
 * Классификатор отзывов по правилам: ключевые слова, blacklist, сложность по рейтингу и длине текста.
 * Blacklist и пороги не хардкодятся — передаются снаружи (из настроек).
 */
class ReviewClassifier {

    private val gratitudeKeywords = listOf(
        "спасибо", "благодар", "отлично", "супер", "рекомендую", "доволен", "довольна",
        "качество", "быстрая доставка", "всё понравилось", "замечательн"
    )
    private val complaintKeywords = listOf(
        "плохо", "ужас", "разочарован", "брак", "возврат", "не рекомендую",
        "обман", "претензи", "жалоб", "испорчен"
    )
    private val questionKeywords = listOf(
        "как ", "когда ", "почему ", "можно ли", "подскажите", "вопрос",
        "уточнить", "гарантия", "вернуть"
    )

    fun classify(reviewText: String, rating: Int, blacklistWords: Set<String>): ClassificationResult {
        val text = reviewText.trim().lowercase()
        val blacklistTriggered = blacklistWords.any { word ->
            word.trim().isNotEmpty() && text.contains(word.trim().lowercase())
        }
        if (blacklistTriggered) {
            return ClassificationResult(
                category = ReviewCategory.COMPLAINT,
                complexityScore = 5,
                blacklistTriggered = true
            )
        }
        val category = classifyCategory(text)
        val complexityScore = computeComplexity(text, rating, category)
        return ClassificationResult(
            category = category,
            complexityScore = complexityScore.coerceIn(1, 5),
            blacklistTriggered = false
        )
    }

    private fun classifyCategory(text: String): ReviewCategory {
        if (gratitudeKeywords.any { text.contains(it) }) return ReviewCategory.GRATITUDE
        if (complaintKeywords.any { text.contains(it) }) return ReviewCategory.COMPLAINT
        if (questionKeywords.any { text.contains(it) }) return ReviewCategory.QUESTION
        return ReviewCategory.NEUTRAL
    }

    private fun computeComplexity(text: String, rating: Int, category: ReviewCategory): Int {
        var score = when (rating) {
            1, 2 -> 4
            3 -> 3
            4 -> 2
            5 -> 1
            else -> 3
        }
        when {
            text.length < 20 && rating >= 4 && category == ReviewCategory.GRATITUDE -> score = 1
            text.length > 200 -> score = (score + 1).coerceAtMost(5)
            category == ReviewCategory.COMPLAINT -> score = (score + 1).coerceAtMost(5)
            category == ReviewCategory.QUESTION -> score = (score + 1).coerceAtMost(5)
        }
        if (rating <= 2) score = score.coerceAtLeast(3)
        return score.coerceIn(1, 5)
    }
}
