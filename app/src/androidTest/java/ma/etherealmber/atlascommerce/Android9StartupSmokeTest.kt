package ma.etherealmber.atlascommerce

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Cold-start smoke test run on an API 28 (Android 9) emulator in CI. */
@RunWith(AndroidJUnit4::class)
class Android9StartupSmokeTest {
    @Test
    fun coldStartLeavesSplashAndDisplaysFirstRunScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val launchIntent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val device = UiDevice.getInstance(instrumentation)
        val firstRunMarker = By.text("Aperçu interactif · mode démo")

        ActivityScenario.launch<MainActivity>(launchIntent).use {
            assertTrue(
                "Cold start never reached Atlas's first-run screen on Android 9 (API 28).",
                device.wait(Until.hasObject(firstRunMarker), 30_000L),
            )
        }
    }
}
