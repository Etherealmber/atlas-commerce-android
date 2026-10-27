package ma.etherealmber.atlascommerce

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.data.model.AssistChatRequest
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
    fun agentRequestSendsResponseLanguageForBackendPromptPolicy() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val json = moshi.adapter(AssistChatRequest::class.java).toJson(
            AssistChatRequest(message = "hello", userMessage = "hello", responseLanguage = "ar"),
        )

        assertTrue(json.contains("\"response_language\":\"ar\""))
    }
}
