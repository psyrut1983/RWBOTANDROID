package com.rwbot.android.domain.decision

import com.rwbot.android.domain.classification.ClassificationResult

/** Решение: отправить ответ автоматически или отдать на модерацию. */
enum class Decision {
    AUTO_SEND,
    MODERATE
}

/** Параметры порогов (из настроек). */
data class DecisionSettings(
    val complexityThreshold: Int,
    val confidenceThreshold: Double,
    val minRatingForAutoResponse: Int
)

/**
 * Движок решений: по правилам из docs/DECISION_RULES.md.
 * Порядок: blacklist → рейтинг → сложность → уверенность → иначе AUTO_SEND.
 */
class DecisionEngine {

    fun decide(
        classification: ClassificationResult,
        rating: Int,
        confidenceScore: Double,
        settings: DecisionSettings
    ): Decision {
        if (classification.blacklistTriggered) return Decision.MODERATE
        if (rating < settings.minRatingForAutoResponse) return Decision.MODERATE
        if (classification.complexityScore > settings.complexityThreshold) return Decision.MODERATE
        if (confidenceScore < settings.confidenceThreshold) return Decision.MODERATE
        return Decision.AUTO_SEND
    }
}
