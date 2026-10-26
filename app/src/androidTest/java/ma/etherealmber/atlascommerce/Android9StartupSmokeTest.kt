package ma.etherealmber.atlascommerce

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.fetchSemanticsNodes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.printToString
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
        try {
            composeRule.waitUntil(timeoutMillis = 30_000L) {
                composeRule.onAllNodesWithText(firstRunMarker).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (error: Throwable) {
            val visibleUi = runCatching { composeRule.onRoot(useUnmergedTree = true).printToString() }
                .getOrDefault("<Compose semantics tree unavailable>")
            throw AssertionError(
                "Cold start did not reach the first-run screen within 30 seconds: ${error.message}. Visible UI: $visibleUi",
            )
        }
        composeRule.onNodeWithText(firstRunMarker).assertExists()
    }
}
