package ma.etherealmber.atlascommerce.ui.screens.settings

/** Returns a real profile name only; session state may use the username as its fallback value. */
internal fun profileNameOrNull(displayName: String?, username: String): String? =
    displayName?.trim()?.takeIf { it.isNotEmpty() && !it.equals(username.trim(), ignoreCase = true) }
