package ma.etherealmber.atlascommerce

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.fetchSemanticsNodes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cold-start smoke test run on an API 28 (Android 9) emulator in CI. */
@RunWith(AndroidJUnit4::class)
class Android9StartupSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun coldStartLeavesSplashAndDisplaysFirstRunScreen() {
        val firstRunMarker = "Aperçu interactif · mode démo"
        composeRule.waitUntil(timeoutMillis = 30_000L) {
            composeRule.onAllNodesWithText(firstRunMarker).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(firstRunMarker).assertExists()
    }
}
