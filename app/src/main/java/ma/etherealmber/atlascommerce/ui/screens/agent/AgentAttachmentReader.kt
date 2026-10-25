package ma.etherealmber.atlascommerce.ui.screens.agent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Image/PDF adapter for the existing Atlas Agent vision input (up to four image pages). */
data class PreparedAgentAttachment(
    val displayName: String,
    val imageDataUrls: List<String>,
)

suspend fun readAgentAttachment(context: Context, uri: Uri): Result<PreparedAgentAttachment> = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty().lowercase()
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }?.takeIf { it.isNotBlank() } ?: "Fichier joint"

        val pages = when {
            mime.startsWith("image/") -> {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val boundsStream = resolver.openInputStream(uri) ?: error("Impossible de lire l'image.")
                boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Format d'image non reconnu." }
                var sample = 1
                while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 2000) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: error("Impossible de décoder l'image.")
                listOf(bitmap.toAgentDataUrl())
            }
            mime == "application/pdf" || displayName.endsWith(".pdf", ignoreCase = true) -> {
                val descriptor: ParcelFileDescriptor = resolver.openFileDescriptor(uri, "r")
                    ?: error("Impossible d'ouvrir le PDF.")
                descriptor.use { fd ->
                    PdfRenderer(fd).use { pdf ->
                        require(pdf.pageCount > 0) { "Le PDF ne contient aucune page." }
                        (0 until minOf(pdf.pageCount, 4)).map { index ->
                            pdf.openPage(index).use { page ->
                                val scale = 1280f / max(page.width, page.height).coerceAtLeast(1)
                                val width = (page.width * scale).toInt().coerceAtLeast(1)
                                val height = (page.height * scale).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                page.render(bitmap, Rect(0, 0, width, height), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap.toAgentDataUrl()
                            }
                        }
                    }
                }
            }
            else -> error("Format non pris en charge. Joignez une image ou un PDF.")
        }
        require(pages.isNotEmpty()) { "Aucune page exploitable dans ce fichier." }
        PreparedAgentAttachment(
            displayName = if (mime == "application/pdf" || displayName.endsWith(".pdf", true)) {
                if (pages.size == 4) "$displayName (4 premières pages)" else displayName
            } else displayName,
            imageDataUrls = pages,
        )
    }
}

private fun Bitmap.toAgentDataUrl(): String {
    val target = if (max(width, height) > 1280) {
        val scale = 1280f / max(width, height)
        Bitmap.createScaledBitmap(this, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true)
    } else this
    return try {
        val output = ByteArrayOutputStream()
        target.compress(Bitmap.CompressFormat.JPEG, 78, output)
        "data:image/jpeg;base64,${Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)}"
    } finally {
        if (target !== this) target.recycle()
        recycle()
    }
}
