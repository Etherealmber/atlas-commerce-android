package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.data.repo.composeAgentPrompt
import ma.etherealmber.atlascommerce.data.repo.AGENT_MAX_QUESTION_LENGTH
import ma.etherealmber.atlascommerce.ui.root.passkeyRegistrationFailureMessage
import ma.etherealmber.atlascommerce.ui.screens.agent.localizedAgentText
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentLocaleAndPasskeyTest {
    @Test fun builtInAgentCopyFollowsLanguageWithoutTranslatingUserContent() {
        val greeting = "Bonjour — je suis Atlas Agent. Je reste ouvert quand vous changez de page. Posez une question ou utilisez une suggestion."
        assertTrue(localizedAgentText(greeting, AppLanguage.ARABIC).startsWith("مرحباً"))
        assertTrue(localizedAgentText(greeting, AppLanguage.ENGLISH).startsWith("Hello"))
        assertEquals("بلا عنوان", localizedAgentText("Sans titre", AppLanguage.ARABIC))
        assertEquals("Mon titre personnel", localizedAgentText("Mon titre personnel", AppLanguage.ARABIC))
        assertEquals("Message du serveur", localizedAgentText("Message du serveur", AppLanguage.ENGLISH))
    }

    @Test fun passkeyErrorsDoNotAssumeCauseAndAreTranslated() {
        assertTrue(atlasNotice(passkeyRegistrationFailureMessage(401), AppLanguage.ENGLISH).contains("session"))
        assertTrue(atlasNotice(passkeyRegistrationFailureMessage(422), AppLanguage.ARABIC).contains("التحقق"))
        assertTrue(atlasNotice(passkeyRegistrationFailureMessage(409), AppLanguage.ENGLISH).contains("conflict"))
        assertEquals(passkeyRegistrationFailureMessage(null), passkeyRegistrationFailureMessage(500))
    }
    @Test fun longContextCannotRemoveTheActualQuestion() {
        for (language in AppLanguage.entries) {
            val question = "س".repeat(AGENT_MAX_QUESTION_LENGTH)
            val result = composeAgentPrompt(question, "x".repeat(10_000), language)
            assertTrue(result.contains(question))
            assertTrue(result.contains(language.agentContextInstruction))
            assertTrue(result.length <= 2450)
        }
    }

}
