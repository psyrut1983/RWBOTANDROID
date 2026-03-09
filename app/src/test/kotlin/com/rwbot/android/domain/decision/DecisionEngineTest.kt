package com.rwbot.android.domain.decision

import com.rwbot.android.domain.classification.ClassificationResult
import com.rwbot.android.domain.classification.ReviewCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionEngineTest {

    private val engine = DecisionEngine()
    private val settings = DecisionSettings(
        complexityThreshold = 4,
        confidenceThreshold = 0.8,
        minRatingForAutoResponse = 3
    )

    @Test
    fun blacklist_always_moderate() {
        val classification = ClassificationResult(ReviewCategory.COMPLAINT, 5, blacklistTriggered = true)
        assertEquals(Decision.MODERATE, engine.decide(classification, rating = 5, confidenceScore = 1.0, settings))
    }

    @Test
    fun low_rating_moderate() {
        val classification = ClassificationResult(ReviewCategory.GRATITUDE, 1, blacklistTriggered = false)
        assertEquals(Decision.MODERATE, engine.decide(classification, rating = 2, confidenceScore = 1.0, settings))
    }

    @Test
    fun high_complexity_moderate() {
        val classification = ClassificationResult(ReviewCategory.COMPLAINT, 5, blacklistTriggered = false)
        assertEquals(Decision.MODERATE, engine.decide(classification, rating = 4, confidenceScore = 1.0, settings))
    }

    @Test
    fun simple_case_auto_send() {
        val classification = ClassificationResult(ReviewCategory.GRATITUDE, 1, blacklistTriggered = false)
        assertEquals(Decision.AUTO_SEND, engine.decide(classification, rating = 5, confidenceScore = 1.0, settings))
    }
}
