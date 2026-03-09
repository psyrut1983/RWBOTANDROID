package com.rwbot.android.domain.classification

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewClassifierTest {

    private val classifier = ReviewClassifier()

    @Test
    fun blacklist_triggered_sets_complexity_5() {
        val r = classifier.classify("Хочу возврат товара", rating = 3, blacklistWords = setOf("возврат"))
        assertEquals(true, r.blacklistTriggered)
        assertEquals(5, r.complexityScore)
    }

    @Test
    fun gratitude_short_high_rating_low_complexity() {
        val r = classifier.classify("Спасибо, всё отлично!", rating = 5, blacklistWords = emptySet())
        assertEquals(ReviewCategory.GRATITUDE, r.category)
        assertEquals(1, r.complexityScore)
    }

    @Test
    fun low_rating_increases_complexity() {
        val r = classifier.classify("Плохо", rating = 1, blacklistWords = emptySet())
        assertEquals(ReviewCategory.COMPLAINT, r.category)
        assertEquals(true, r.complexityScore >= 3)
    }
}
