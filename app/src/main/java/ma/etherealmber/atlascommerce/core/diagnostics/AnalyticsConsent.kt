package ma.etherealmber.atlascommerce.core.diagnostics

import android.content.Context
import android.os.Bundle
import ma.etherealmber.atlascommerce.BuildConfig

internal fun canRecordDebugAnalyticsTest(
    debugBuild: Boolean,
    analyticsConfigured: Boolean,
    consentGranted: Boolean,
    collectionEnabled: Boolean,
): Boolean = debugBuild && analyticsConfigured && consentGranted && collectionEnabled

/**
 * Firebase Analytics is optional, disabled by default, and may be enabled only in debug builds
 * after an explicit opt-in. Reflection keeps Firebase-free self-hosted builds compilable.
 */
object AnalyticsConsent {
    private const val PREFS = "atlas_analytics"
    private const val KEY_ENABLED = "analytics_collection_enabled"
    private const val SDK_CLASS = "com.google.firebase.analytics.FirebaseAnalytics"
    private val primitiveBooleanType: Class<*> = java.lang.Boolean.TYPE
    @Volatile private var appliedCollectionState: Boolean? = null

    fun isConfigured(context: Context): Boolean = analyticsOrNull(context) != null

    fun isEnabled(context: Context): Boolean =
        BuildConfig.DEBUG && hasStoredOptIn(context) && appliedCollectionState == true && analyticsOrNull(context) != null

    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        if (enabled && (!BuildConfig.DEBUG || analyticsOrNull(context) == null)) return false
        val analytics = analyticsOrNull(context)
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (enabled && analytics == null) return false
        val applied = if (analytics == null) !enabled else runCatching {
            analytics.javaClass.getMethod("setAnalyticsCollectionEnabled", primitiveBooleanType)
                .invoke(analytics, enabled)
            true
        }.getOrDefault(false)
        val saved = stored.edit().putBoolean(KEY_ENABLED, enabled && BuildConfig.DEBUG).commit()
        if (!saved && enabled && analytics != null) {
            runCatching {
                analytics.javaClass.getMethod("setAnalyticsCollectionEnabled", primitiveBooleanType)
                    .invoke(analytics, false)
            }
            appliedCollectionState = false
        } else if (saved && applied) {
            appliedCollectionState = enabled
        }
        return saved && applied
    }

    /** Restore consent before UI startup; release builds always force collection off. */
    fun applyStoredConsent(context: Context) {
        val enabled = BuildConfig.DEBUG && hasStoredOptIn(context)
        if (!BuildConfig.DEBUG) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, false).apply()
        }
        val analytics = analyticsOrNull(context) ?: run {
            appliedCollectionState = false
            return
        }
        runCatching {
            analytics.javaClass.getMethod("setAnalyticsCollectionEnabled", primitiveBooleanType)
                .invoke(analytics, enabled)
            appliedCollectionState = enabled
        }.onFailure { error ->
            appliedCollectionState = false
            AtlasLogBuffer.log("Analytics", "Could not restore analytics consent (${error.javaClass.simpleName})", "WARN")
        }
    }

    /** Sends a constant developer-only event with no account, tenant, product, or business values. */
    fun recordDebugTestEvent(context: Context): Boolean {
        val analytics = analyticsOrNull(context) ?: return false
        val consentGranted = hasStoredOptIn(context)
        val collectionEnabled = isEnabled(context)
        if (!canRecordDebugAnalyticsTest(BuildConfig.DEBUG, true, consentGranted, collectionEnabled)) return false
        return runCatching {
            val params = Bundle().apply {
                putString("test_source", "developer_diagnostics")
                putString("synthetic", "true")
            }
            analytics.javaClass.getMethod("logEvent", String::class.java, Bundle::class.java)
                .invoke(analytics, "atlas_developer_test", params)
        }.onFailure { error ->
            AtlasLogBuffer.log("Analytics", "Could not queue the synthetic verification event (${error.javaClass.simpleName})", "WARN")
        }.isSuccess
    }

    private fun hasStoredOptIn(context: Context): Boolean = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENABLED, false)

    private fun analyticsOrNull(context: Context): Any? = runCatching {
        val sdk = Class.forName(SDK_CLASS)
        sdk.getMethod("getInstance", Context::class.java).invoke(null, context)
    }.onFailure { error ->
        if (error !is ClassNotFoundException) {
            AtlasLogBuffer.log("Analytics", "Firebase Analytics is not usable (${error.javaClass.simpleName})", "WARN")
        }
    }.getOrNull()
}
