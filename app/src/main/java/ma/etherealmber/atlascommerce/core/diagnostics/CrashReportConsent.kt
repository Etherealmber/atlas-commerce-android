package ma.etherealmber.atlascommerce.core.diagnostics

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Opt-in automatic crash uploads. Collection is also disabled by default in the manifest. */
object CrashReportConsent {
    private const val PREFS = "atlas_crash_reporting"
    private const val KEY_ENABLED = "automatic_crash_reports_enabled"

    /** True when Firebase Core has initialized the app used by FCM and other Firebase products. */
    fun hasDefaultFirebaseApp(context: Context): Boolean = runCatching {
        FirebaseApp.getApps(context).any { it.name == FirebaseApp.DEFAULT_APP_NAME }
    }.getOrDefault(false)

    /** Only expose the opt-in when the default Firebase app and Crashlytics SDK are usable. */
    fun isConfigured(context: Context): Boolean = crashlyticsOrNull(context) != null

    /** Read the effective Firebase SDK state, not only the saved preference. */
    fun isEnabled(context: Context): Boolean =
        crashlyticsOrNull(context)?.isCrashlyticsCollectionEnabled() == true

    private fun hasStoredOptIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    /** Restore the user's explicit choice early in app startup, before the first screen is shown. */
    fun applyStoredConsent(context: Context) {
        val crashlytics = crashlyticsOrNull(context) ?: return
        runCatching {
            crashlytics.setCrashlyticsCollectionEnabled(hasStoredOptIn(context))
        }.onFailure { error ->
            logCrashlyticsFailure("Could not restore crash-report consent", error)
        }
    }

    /** Returns false when enabling was requested but no usable Firebase/Crashlytics app exists. */
    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        val crashlytics = crashlyticsOrNull(context)
        if (enabled && crashlytics == null) return false
        return runCatching {
            crashlytics?.setCrashlyticsCollectionEnabled(enabled)
            val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .commit()
            saved && (crashlytics == null || crashlytics.isCrashlyticsCollectionEnabled() == enabled)
        }.onFailure { error ->
            logCrashlyticsFailure("Could not update crash-report consent", error)
        }.getOrDefault(false)
    }

    private fun crashlyticsOrNull(context: Context): FirebaseCrashlytics? {
        val defaultApp = runCatching {
            FirebaseApp.getApps(context)
                .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                ?: FirebaseApp.initializeApp(context)
        }.onFailure { error ->
            logCrashlyticsFailure("Could not initialize Firebase configuration", error)
        }.getOrNull()
        if (defaultApp?.name != FirebaseApp.DEFAULT_APP_NAME) return null

        return runCatching { FirebaseCrashlytics.getInstance() }
            .onFailure { error ->
                logCrashlyticsFailure("Firebase Crashlytics is not usable in this build", error)
            }
            .getOrNull()
    }

    private fun logCrashlyticsFailure(message: String, error: Throwable) {
        // Keep details useful for local diagnostics without recording raw config or exception text.
        AtlasLogBuffer.log(
            tag = "Crashlytics",
            message = "$message (${error.javaClass.simpleName})",
            level = "WARN",
        )
    }
}
