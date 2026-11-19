package ma.etherealmber.atlascommerce

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.shouldRefreshSystemAppLanguage
import ma.etherealmber.atlascommerce.core.diagnostics.resolveAnalyticsConsentUi
import ma.etherealmber.atlascommerce.data.model.AssistChatRequest
import ma.etherealmber.atlascommerce.ui.components.financeReportTitle
import ma.etherealmber.atlascommerce.ui.screens.home.isCurrentFinanceReportHistoryRequest
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test
    fun arabicSelectsRtlAndAllLanguagesHaveAnAgentResponseInstruction() {
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertFalse(AppLanguage.FRENCH.isRtl)
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertTrue(AppLanguage.ARABIC.agentContextInstruction.contains("باللغة العربية"))
        assertTrue(AppLanguage.ENGLISH.agentContextInstruction.contains("in English"))
        assertTrue(AppLanguage.FRENCH.agentContextInstruction.contains("en français"))
    }

    @Test
    fun languageTagsResolveRegionVariants() {
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag("ar-MA"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-GB"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromTag("fr-FR"))
    }

    @Test
    fun systemLocaleSelectsSupportedLanguageAndFallsBackToFrench() {
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromSystemLocale(java.util.Locale.forLanguageTag("ar-MA")))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromSystemLocale(java.util.Locale.forLanguageTag("en-US")))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromSystemLocale(java.util.Locale.forLanguageTag("ja-JP")))
    }

    @Test
    fun initialPlaceholderCannotOverrideSavedLanguageWithSystemLocale() {
        assertFalse(shouldRefreshSystemAppLanguage(appearanceLoaded = false, followsSystemPreferenceLoaded = true, followsSystem = true))
        assertFalse(shouldRefreshSystemAppLanguage(appearanceLoaded = true, followsSystemPreferenceLoaded = false, followsSystem = true))
        assertFalse(shouldRefreshSystemAppLanguage(appearanceLoaded = true, followsSystemPreferenceLoaded = true, followsSystem = false))
        assertTrue(shouldRefreshSystemAppLanguage(appearanceLoaded = true, followsSystemPreferenceLoaded = true, followsSystem = true))
    }

    @Test
    fun staleFinanceHistoryRequestsCannotUpdateANewerAccountOrRefresh() {
        assertTrue(isCurrentFinanceReportHistoryRequest(7L, 7L, "account-a", "account-a"))
        assertFalse(isCurrentFinanceReportHistoryRequest(6L, 7L, "account-a", "account-a"))
        assertFalse(isCurrentFinanceReportHistoryRequest(7L, 7L, "account-a", "account-b"))
    }

    @Test
    fun failedAnalyticsOptOutKeepsPriorConsentVisibleAndRetryable() {
        val result = resolveAnalyticsConsentUi(
            previousEnabled = true,
            requestedEnabled = false,
            saved = false,
            actualEnabled = false,
        )
        assertTrue(result.enabled)
        assertTrue(result.failed)

        val successfulOptOut = resolveAnalyticsConsentUi(
            previousEnabled = true,
            requestedEnabled = false,
            saved = true,
            actualEnabled = false,
        )
        assertFalse(successfulOptOut.enabled)
        assertFalse(successfulOptOut.failed)
    }

    @Test
    fun financeReportTitleUsesTheSameLocaleAwareFilenameFormatting() {
        val report = File("rapport-mensuel-2025-01-1740000000000.pdf")
        assertEquals("Relevé mensuel · Janvier 2025", financeReportTitle(report, AppLanguage.FRENCH))
        assertEquals("Monthly statement · January 2025", financeReportTitle(report, AppLanguage.ENGLISH))
        assertEquals("كشف شهري · يناير 2025", financeReportTitle(report, AppLanguage.ARABIC))
        assertEquals(
            "Monthly statement · custom-report",
            financeReportTitle(File("custom-report.pdf"), AppLanguage.ENGLISH),
        )
    }

    @Test
    fun agentRequestSendsResponseLanguageForBackendPromptPolicy() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val json = moshi.adapter(AssistChatRequest::class.java).toJson(
            AssistChatRequest(message = "hello", userMessage = "hello", responseLanguage = "ar"),
        )

        assertTrue(json.contains("\"response_language\":\"ar\""))
    }
}
