package ma.etherealmber.atlascommerce.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportConsentTest {
    @Test
    fun debugVerificationEventRequiresDebugBuildConfigurationAndExplicitOptIn() {
        assertFalse(canRecordDebugCrashlyticsVerification(false, true, true, true))
        assertFalse(canRecordDebugCrashlyticsVerification(true, false, true, true))
        assertFalse(canRecordDebugCrashlyticsVerification(true, true, false, true))
        assertFalse(canRecordDebugCrashlyticsVerification(true, true, true, false))
        assertTrue(canRecordDebugCrashlyticsVerification(true, true, true, true))
    }

    @Test
    fun doesNotRequestCrashlyticsWhenDefaultFirebaseAppIsMissing() {
        var factoryCalled = false
        var failureReported = false

        val instance = resolveCrashlyticsInstance(
            defaultFirebaseAppPresent = false,
            getInstance = {
                factoryCalled = true
                Any()
            },
            onFailure = { failureReported = true },
        )

        assertNull(instance)
        assertFalse(factoryCalled)
        assertFalse(failureReported)
    }

    @Test
    fun treatsMissingCrashlyticsComponentAsUnavailableWithoutPropagating() {
        val expected = NullPointerException("FirebaseCrashlytics component is not present.")
        var reported: Throwable? = null

        val instance = resolveCrashlyticsInstance(
            defaultFirebaseAppPresent = true,
            getInstance = { throw expected },
            onFailure = { reported = it },
        )

        assertNull(instance)
        assertSame(expected, reported)
    }

    @Test
    fun returnsCrashlyticsWhenComponentIsAvailable() {
        val expected = Any()
        var failureReported = false

        val instance = resolveCrashlyticsInstance(
            defaultFirebaseAppPresent = true,
            getInstance = { expected },
            onFailure = { failureReported = true },
        )

        assertSame(expected, instance)
        assertFalse(failureReported)
    }

    @Test
    fun releaseRulesKeepFirebaseComponentRegistrarNoArgConstructorsForR8() {
        val rules = java.io.File("proguard-rules.pro").takeIf { it.isFile } ?: java.io.File("app/proguard-rules.pro")
        assertTrue("R8 rules file must exist", rules.isFile)
        val content = rules.readText()
        assertTrue(content.contains("-keep class * implements com.google.firebase.components.ComponentRegistrar {\n    *;\n}"))
    }

    @Test
    fun firebaseLookupDoesNotInitializeAnAppOutsideFirebaseInitProvider() {
        val source = java.io.File(
            "src/main/java/ma/etherealmber/atlascommerce/core/diagnostics/CrashReportConsent.kt",
        ).takeIf { it.isFile } ?: java.io.File(
            "app/src/main/java/ma/etherealmber/atlascommerce/core/diagnostics/CrashReportConsent.kt",
        )
        assertTrue("CrashReportConsent source must exist", source.isFile)
        assertFalse(
            "Do not create FirebaseApp on a background caller; FirebaseInitProvider owns default-app startup",
            source.readText().contains("FirebaseApp.initializeApp(context)"),
        )
    }
}
