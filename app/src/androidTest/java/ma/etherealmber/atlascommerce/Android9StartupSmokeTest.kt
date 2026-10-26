package ma.etherealmber.atlascommerce

import android.content.Intent
import android.util.Log
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

        ActivityScenario.launch<MainActivity>(launchIntent).use { scenario ->
            scenario.onActivity { activity ->
                Log.i("Android9StartupSmoke", "MainActivity resumed; package=${activity.packageName}")
            }
            val reachedFirstRun = device.wait(Until.hasObject(firstRunMarker), 30_000L)
            if (!reachedFirstRun) {
                val hierarchy = java.io.File(context.cacheDir, "android9-window.xml")
                runCatching { device.dumpWindowHierarchy(hierarchy) }
                val hierarchyText = runCatching { hierarchy.readText().take(6_000) }
                    .getOrDefault("<window hierarchy unavailable>")
                Log.e(
                    "Android9StartupSmoke",
                    "Timed out; currentPackage=${device.currentPackageName}; hierarchy=$hierarchyText",
                )
            }
            assertTrue(
                "Cold start never reached Atlas's first-run screen on Android 9 (API 28).",
                reachedFirstRun,
            )
        }
    }
}
