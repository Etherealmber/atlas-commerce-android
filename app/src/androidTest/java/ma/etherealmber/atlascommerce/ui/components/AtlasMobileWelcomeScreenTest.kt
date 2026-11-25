package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.ui.theme.AtlasTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AtlasMobileWelcomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tourShieldConsumesTouchesAndSkipMarksTourFinished() {
        val underlyingTapped = mutableStateOf(false)
        var tourFinished = false
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AtlasTheme(themeMode = ThemeMode.LIGHT) {
                androidx.compose.foundation.layout.Box {
                    Button(onClick = { underlyingTapped.value = true }) { androidx.compose.material3.Text("Underlying action") }
                    AtlasMobileTourOverlay(
                        currentStep = 0,
                        onStep = {},
                        onFinish = { tourFinished = true },
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(400L)
        compose.waitForIdle()
        compose.onNodeWithText("Underlying action").performClick()
        compose.runOnIdle { assertTrue(!underlyingTapped.value) }
        compose.onNodeWithText("Passer").performClick()
        compose.runOnIdle { assertTrue(tourFinished) }
    }

    @Test
    fun welcomeShowsFiveOrderedStagesAndFinishesByStartingTour() {
        var welcomeCompletedWithTour = false
        compose.setContent {
            AtlasTheme(themeMode = ThemeMode.LIGHT) {
                AtlasMobileWelcomeScreen(
                    firstName = "Amina",
                    role = "stock",
                    onComplete = {},
                    onCompleteWithTour = { welcomeCompletedWithTour = true },
                )
            }
        }

        compose.onNodeWithText("Bonjour, Amina").assertIsDisplayed()
        compose.onNodeWithText("Commencer").performClick()
        compose.onNodeWithText("Choisissez votre thème").assertIsDisplayed()
        compose.onNodeWithText("Continuer").performClick()
        compose.onNodeWithText("L’essentiel, au bout du pouce").assertIsDisplayed()
        compose.onNodeWithText("Scanner").assertIsDisplayed()
        compose.onNodeWithText("Produits & stock").assertIsDisplayed()
        compose.onNodeWithText("Vente rapide").assertDoesNotExist()
        compose.onNodeWithText("Continuer").performClick()
        compose.onNodeWithText("Voici à quoi ça ressemble").assertIsDisplayed()
        compose.onNodeWithText("Continuer").performClick()
        compose.onNodeWithText("Tout est prêt").assertIsDisplayed()
        compose.onNodeWithText("Ouvrir Atlas").performClick()
        compose.runOnIdle { assertTrue(welcomeCompletedWithTour) }
    }
}
