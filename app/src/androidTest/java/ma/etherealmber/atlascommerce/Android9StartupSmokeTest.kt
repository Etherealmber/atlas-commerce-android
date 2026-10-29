package ma.etherealmber.atlascommerce

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
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
        // The app follows the emulator's system language, so don't pin this smoke test to French.
        val firstRunMarkers = listOf(
            By.textContains("Aperçu interactif"),
            By.textContains("Interactive preview"),
            By.textContains("معاينة تفاعلية"),
        )

        ActivityScenario.launch<MainActivity>(launchIntent).use { scenario ->
            scenario.onActivity { activity ->
                Log.i("Android9StartupSmoke", "MainActivity resumed; package=${activity.packageName}")
            }
            val deadline = SystemClock.uptimeMillis() + 30_000L
            var reachedFirstRun = false
            while (!reachedFirstRun && SystemClock.uptimeMillis() < deadline) {
                reachedFirstRun = firstRunMarkers.any { marker -> device.findObject(marker) != null }
                if (!reachedFirstRun) SystemClock.sleep(250L)
            }
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
