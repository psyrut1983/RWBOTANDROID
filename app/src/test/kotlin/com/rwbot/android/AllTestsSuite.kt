package com.rwbot.android

import com.rwbot.android.data.remote.wb.WildberriesApiTest
import com.rwbot.android.data.remote.yandex.YandexApiTest
import com.rwbot.android.data.repository.ReviewRepositoryImplTest
import com.rwbot.android.data.repository.YandexRepositoryTest
import com.rwbot.android.domain.classification.ReviewClassifierTest
import com.rwbot.android.domain.decision.DecisionEngineTest
import com.rwbot.android.domain.pipeline.ReviewPipelineTest
import com.rwbot.android.ui.reviews.ReviewDetailViewModelTest
import com.rwbot.android.ui.reviews.ReviewsViewModelTest
import com.rwbot.android.ui.settings.SettingsViewModelTest
import com.rwbot.android.ui.stats.StatsViewModelTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Общая точка входа для всех unit-тестов приложения.
 * Запуск: Run 'AllTestsSuite' в IDE или
 *   ./gradlew :app:testDebugUnitTest
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Domain
    ReviewClassifierTest::class,
    DecisionEngineTest::class,
    ReviewPipelineTest::class,
    // Data — API
    WildberriesApiTest::class,
    YandexApiTest::class,
    // Data — репозитории
    ReviewRepositoryImplTest::class,
    YandexRepositoryTest::class,
    // UI — ViewModels
    ReviewsViewModelTest::class,
    ReviewDetailViewModelTest::class,
    StatsViewModelTest::class,
    SettingsViewModelTest::class
)
class AllTestsSuite
