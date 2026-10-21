package ma.etherealmber.atlascommerce.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens an external link without crashing when no browser/mail app is installed. */
fun openExternal(context: Context, url: String): Boolean = runCatching {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
}.getOrDefault(false)
