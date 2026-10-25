package ma.etherealmber.atlascommerce.core.diagnostics

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Opt-in automatic crash uploads. Collection is also disabled by default in the manifest. */
object CrashReportConsent {
    private const val PREFS = "atlas_crash_reporting"
    private const val KEY_ENABLED = "automatic_crash_reports_enabled"

    fun isConfigured(context: Context): Boolean =
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    /** Restore the user's explicit choice early in app startup, before the first screen is shown. */
    fun applyStoredConsent(context: Context) {
        if (!isConfigured(context)) return
        runCatching {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isEnabled(context))
        }
    }

    /** Returns false when enabling was requested but no Firebase app configuration is installed. */
    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        if (enabled && !isConfigured(context)) return false
        return runCatching {
            if (isConfigured(context)) {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .commit()
        }.getOrDefault(false)
    }
}
