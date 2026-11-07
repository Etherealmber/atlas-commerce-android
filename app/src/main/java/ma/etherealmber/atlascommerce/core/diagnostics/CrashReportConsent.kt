package ma.etherealmber.atlascommerce.core.diagnostics

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

internal fun <T : Any> resolveCrashlyticsInstance(
    defaultFirebaseAppPresent: Boolean,
    getInstance: () -> T,
    onFailure: (Throwable) -> Unit,
): T? {
    if (!defaultFirebaseAppPresent) return null
    return runCatching(getInstance)
        .onFailure(onFailure)
        .getOrNull()
}

/** Opt-in automatic crash uploads. Collection is also disabled by default in the manifest. */
object CrashReportConsent {
    private const val PREFS = "atlas_crash_reporting"
    private const val KEY_ENABLED = "automatic_crash_reports_enabled"
    private val lock = Any()

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

    // Only use FirebaseInitProvider's app. Creating a default FirebaseApp here can run from an
    // IO caller and expose the Crashlytics accessor before Firebase's UI-executor registrars finish.
    private fun crashlyticsOrNull(context: Context): FirebaseCrashlytics? = synchronized(lock) {
        val hasDefaultApp = runCatching {
            FirebaseApp.getApps(context).any { it.name == FirebaseApp.DEFAULT_APP_NAME }
        }.onFailure { error ->
            logCrashlyticsFailure("Could not inspect Firebase configuration", error)
        }.getOrDefault(false)

        resolveCrashlyticsInstance(
            defaultFirebaseAppPresent = hasDefaultApp,
            getInstance = { FirebaseCrashlytics.getInstance() },
            onFailure = { error ->
                logCrashlyticsFailure("Firebase Crashlytics is not usable in this build", error)
            },
        )
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
