package ma.etherealmber.atlascommerce.ui.screens.agent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

/** Attachment payload for the worker's image-vision and bounded document-extraction paths. */
data class PreparedAgentAttachment(
    val displayName: String,
    val imageDataUrls: List<String> = emptyList(),
    val documentDataUrls: List<String> = emptyList(),
)

private const val MAX_AGENT_DOCUMENT_BYTES = 2_000_000

suspend fun readAgentAttachment(context: Context, uri: Uri): Result<PreparedAgentAttachment> = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty().lowercase()
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }?.takeIf { it.isNotBlank() } ?: "Fichier joint"
        val extension = displayName.substringAfterLast('.', "").lowercase()

        if (mime.startsWith("image/")) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = resolver.openInputStream(uri) ?: error("Impossible de lire l'image.")
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Format d'image non reconnu." }
            var sample = 1
            while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 2000) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Impossible de décoder l'image.")
            PreparedAgentAttachment(displayName, imageDataUrls = listOf(bitmap.toAgentDataUrl()))
        } else {
            val documentMime = when {
                mime == "application/pdf" || extension == "pdf" -> "application/pdf"
                mime == "text/csv" || extension == "csv" -> "text/csv"
                mime == "text/markdown" || extension in setOf("md", "markdown") -> "text/markdown"
                mime == "application/json" || extension == "json" -> "application/json"
                mime == "text/plain" || extension in setOf("txt", "log") -> "text/plain"
                else -> error("Format non pris en charge. Joignez une image, un PDF, un TXT, un CSV, un Markdown ou un JSON.")
            }
            val input = resolver.openInputStream(uri) ?: error("Impossible d'ouvrir le document.")
            val bytes = input.use { it.readBounded(MAX_AGENT_DOCUMENT_BYTES) }
            require(bytes.isNotEmpty()) { "Le document est vide." }
            val dataUrl = "data:$documentMime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
            PreparedAgentAttachment(displayName, documentDataUrls = listOf(dataUrl))
        }
    }
}

private fun InputStream.readBounded(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= maxBytes) { "Ce document dépasse la limite de 2 Mo." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
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
