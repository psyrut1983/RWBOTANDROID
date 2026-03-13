package com.rwbot.android.ui.settings

import com.rwbot.android.data.local.SecureSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.rwbot.android.util.MainCoroutineRule

/**
 * Тесты SettingsViewModel: загрузка настроек, обновление полей, сохранение.
 */
class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var secureSettings: SecureSettings
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        secureSettings = mockk(relaxed = true)
        every { secureSettings.wbApiToken } returns "token-wb"
        every { secureSettings.yandexApiKey } returns "key-y"
        every { secureSettings.yandexFolderId } returns "folder-1"
        every { secureSettings.complexityThreshold } returns 4
        every { secureSettings.confidenceThreshold } returns 0.8
        every { secureSettings.minRatingForAutoResponse } returns 3
        every { secureSettings.blacklistWords } returns setOf("возврат", "брак")
        viewModel = SettingsViewModel(secureSettings)
    }

    @Test
    fun load_fillsStateFromSecureSettings() = runTest {
        advanceUntilIdle()
        assertEquals("token-wb", viewModel.state.value.wbToken)
        assertEquals("key-y", viewModel.state.value.yandexApiKey)
        assertEquals("folder-1", viewModel.state.value.yandexFolderId)
        assertEquals(4, viewModel.state.value.complexityThreshold)
        assertEquals(0.8, viewModel.state.value.confidenceThreshold, 1e-6)
        assertEquals(3, viewModel.state.value.minRating)
        assertEquals("возврат\nбрак", viewModel.state.value.blacklistText)
    }

    @Test
    fun updateFields_changesState() = runTest {
        viewModel.updateWbToken("new-token")
        viewModel.updateComplexityThreshold(5)
        viewModel.updateBlacklistText("слово1\nслово2")
        assertEquals("new-token", viewModel.state.value.wbToken)
        assertEquals(5, viewModel.state.value.complexityThreshold)
        assertEquals("слово1\nслово2", viewModel.state.value.blacklistText)
    }

    @Test
    fun save_writesToSecureSettings() = runTest {
        viewModel.updateWbToken("t")
        viewModel.updateYandexApiKey("k")
        viewModel.updateYandexFolderId("f")
        viewModel.updateComplexityThreshold(5)
        viewModel.updateMinRating(4)
        viewModel.updateBlacklistText("a\nb")
        viewModel.save()
        advanceUntilIdle()
        verify { secureSettings.wbApiToken = "t" }
        verify { secureSettings.yandexApiKey = "k" }
        verify { secureSettings.yandexFolderId = "f" }
        verify { secureSettings.complexityThreshold = 5 }
        verify { secureSettings.minRatingForAutoResponse = 4 }
        verify { secureSettings.blacklistWords = setOf("a", "b") }
        assertEquals(true, viewModel.state.value.saved)
    }
}
