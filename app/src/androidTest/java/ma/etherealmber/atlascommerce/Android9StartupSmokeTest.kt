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
        // New installs choose a server before onboarding or sign-in. Accept the localized
        // server-setup heading because the app follows the emulator's system language.
        val firstRunMarkers = listOf(
            // Address entry is the usual clean-install route. A restored saved-server
            // list is still a valid first-run setup screen, so accept its step heading too.
            By.textContains("Connectez votre serveur"),
            By.textContains("Connect your server"),
            By.textContains("اربط الخادم الخاص بك"),
            By.textContains("Choisissez un serveur"),
            By.textContains("Choose a server"),
            By.textContains("اختر خادماً"),
            By.textContains("STEP 1 · SAVED SERVERS"),
            By.textContains("ÉTAPE 1 · SERVEURS ENREGISTRÉS"),
            By.textContains("الخطوة ١ · الخوادم المحفوظة"),
            By.textContains("Saisissez l'adresse de votre organisation."),
            By.textContains("Enter your organization's server address."),
            By.textContains("أدخل عنوان خادم مؤسستك."),
            By.textContains("Saisissez l'adresse du site Atlas Commerce de votre organisation."),
            By.textContains("Enter your organization's Atlas Commerce site address."),
            By.textContains("أدخل عنوان موقع Atlas Commerce الخاص بمؤسستك."),
        )

        ActivityScenario.launch<MainActivity>(launchIntent).use { scenario ->
            scenario.onActivity { activity ->
                Log.i("Android9StartupSmoke", "MainActivity resumed; package=${activity.packageName}")
            }
            // API 28 emulator first launches can spend several seconds in dexopt and
            // Compose startup after MainActivity is visible. Give the UI a full minute.
            val deadline = SystemClock.uptimeMillis() + 60_000L
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
