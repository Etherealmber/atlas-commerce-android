package ma.etherealmber.atlascommerce.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Compose exposes a ContextWrapper; unwrap it to reach the hosting Activity. */
fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
