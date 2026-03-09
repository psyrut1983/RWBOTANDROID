package com.rwbot.android.ui.nav

object NavRoutes {
    const val REVIEWS = "reviews"
    const val MODERATION = "moderation"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val REVIEW_DETAIL = "review/{reviewId}"
    fun reviewDetail(reviewId: String) = "review/$reviewId"
}
