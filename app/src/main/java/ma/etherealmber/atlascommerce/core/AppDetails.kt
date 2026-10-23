package ma.etherealmber.atlascommerce.core

import ma.etherealmber.atlascommerce.BuildConfig
import java.util.Calendar

/**
 * Product identity and build information.
 *
 * Kept in one place so the About / Legal screens, the User-Agent header and the
 * README never disagree about the version.
 */
object AppDetails {
    const val NAME = "Atlas Commerce"
    const val TAGLINE = "Stock · Caisse · Facturation"
    const val DESCRIPTION =
        "ERP de terrain pour commerces et grossistes : stock multi-dépôts, caisse, tickets VR, facturation TVA."

    const val VERSION_NAME: String = BuildConfig.VERSION_NAME
    const val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    const val DEFAULT_API_BASE: String = BuildConfig.DEFAULT_API_BASE
    const val PROJECT_REPO: String = BuildConfig.PROJECT_REPO
    const val WEB_REPO: String = BuildConfig.WEB_REPO
    const val ISSUES_URL: String = BuildConfig.ISSUES_URL
    const val SECURITY_EMAIL: String = BuildConfig.SECURITY_EMAIL

    const val LICENSE_NAME = "MIT"
    const val COPYRIGHT_HOLDER = "Etherealmber"
    val COPYRIGHT_YEAR: String
        get() = Calendar.getInstance().get(Calendar.YEAR).coerceAtLeast(2026).toString()
    const val GITHUB_HANDLE = "Etherealmber"
    const val GITHUB_PROFILE_URL = "https://github.com/Etherealmber"
    const val GITHUB_AVATAR_URL = "https://github.com/Etherealmber.png"
    val GITHUB_LATEST_JSON_URL: String =
        "${PROJECT_REPO.trimEnd('/')}/releases/latest/download/latest.json"
    val RELEASES_API_URL: String =
        "${PROJECT_REPO.trimEnd('/').replace("https://github.com/", "https://api.github.com/repos/")}/releases?per_page=8"

    /** Identifier sent to the worker so it can skip Turnstile for the native app. */
    const val CLIENT_ID = "android"
}
