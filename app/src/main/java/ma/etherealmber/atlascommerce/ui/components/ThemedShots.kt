package ma.etherealmber.atlascommerce.ui.components

import ma.etherealmber.atlascommerce.R

/** Resolves a web demo capture to its matching Android resource, falling back to light when needed. */
fun themedShot(name: String, dark: Boolean): Int {
    val key = name.removeSuffix(".webp").replace('-', '_')
    val light = when (key) {
        "phone_home_demo" -> R.drawable.phone_home_demo
        "phone_products_demo" -> R.drawable.phone_products_demo
        "phone_balance_demo" -> R.drawable.phone_balance_demo
        "desktop_products_demo" -> R.drawable.desktop_products_demo
        "android_scan_demo" -> R.drawable.android_scan_demo
        else -> error("Unknown Atlas demo capture: $name")
    }
    if (!dark) return light
    return when (key) {
        "phone_home_demo" -> R.drawable.phone_home_demo_dark
        "phone_products_demo" -> R.drawable.phone_products_demo_dark
        "phone_balance_demo" -> R.drawable.phone_balance_demo_dark
        "desktop_products_demo" -> R.drawable.desktop_products_demo_dark
        else -> light
    }
}
