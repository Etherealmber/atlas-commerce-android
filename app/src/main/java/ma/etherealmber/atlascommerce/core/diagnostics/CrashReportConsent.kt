package ma.etherealmber.atlascommerce.core.diagnostics

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Opt-in automatic crash uploads. Collection is also disabled by default in the manifest. */
object CrashReportConsent {
    private const val PREFS = "atlas_crash_reporting"
    private const val KEY_ENABLED = "automatic_crash_reports_enabled"

    /** Only expose the opt-in when the default Firebase app and Crashlytics SDK are usable. */
    fun isConfigured(context: Context): Boolean = crashlyticsOrNull(context) != null

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    /** Restore the user's explicit choice early in app startup, before the first screen is shown. */
    fun applyStoredConsent(context: Context) {
        val crashlytics = crashlyticsOrNull(context) ?: return
        runCatching {
            crashlytics.setCrashlyticsCollectionEnabled(isEnabled(context))
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
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .commit()
        }.onFailure { error ->
            logCrashlyticsFailure("Could not update crash-report consent", error)
        }.getOrDefault(false)
    }

    private fun crashlyticsOrNull(context: Context): FirebaseCrashlytics? {
        val hasDefaultApp = runCatching {
            FirebaseApp.getApps(context).any { it.name == FirebaseApp.DEFAULT_APP_NAME }
        }.onFailure { error ->
            logCrashlyticsFailure("Could not inspect Firebase configuration", error)
        }.getOrDefault(false)
        if (!hasDefaultApp) return null

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
