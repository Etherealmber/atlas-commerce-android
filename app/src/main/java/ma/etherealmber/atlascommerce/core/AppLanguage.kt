package ma.etherealmber.atlascommerce.core

/** Supported app interface languages. French remains the default for existing installs. */
enum class AppLanguage(val tag: String, val nativeLabel: String) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية");

    val isRtl: Boolean get() = this == ARABIC

    companion object {
        fun fromTag(value: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(value?.substringBefore('-'), ignoreCase = true) }
                ?: FRENCH
    }
}
