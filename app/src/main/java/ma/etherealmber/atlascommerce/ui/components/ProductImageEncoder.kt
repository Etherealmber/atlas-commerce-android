package ma.etherealmber.atlascommerce.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Encodes picked product photos as centered square WebP data URIs for both create and edit flows. */
internal suspend fun encodeProductImageDataUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: return@withContext null
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    var sampleSize = 1
    while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > MAX_DECODE_EDGE) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return@withContext null

    val cropEdge = minOf(decoded.width, decoded.height)
    val square = Bitmap.createBitmap(
        decoded,
        (decoded.width - cropEdge) / 2,
        (decoded.height - cropEdge) / 2,
        cropEdge,
        cropEdge,
    )
    if (square !== decoded) decoded.recycle()
    val outputBitmap = if (cropEdge > OUTPUT_EDGE) {
        Bitmap.createScaledBitmap(square, OUTPUT_EDGE, OUTPUT_EDGE, true).also { scaled ->
            if (scaled !== square) square.recycle()
        }
    } else {
        square
    }

    try {
        var encoded: ByteArray? = null
        for (quality in intArrayOf(86, 80, 74, 68)) {
            val output = ByteArrayOutputStream()
            outputBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, output)
            encoded = output.toByteArray()
            if (encoded.size <= MAX_ENCODED_BYTES) break
        }
        encoded?.takeIf { it.size <= MAX_ENCODED_BYTES }?.let {
            "data:image/webp;base64,${Base64.encodeToString(it, Base64.NO_WRAP)}"
        }
    } finally {
        outputBitmap.recycle()
    }
}

private const val MAX_DECODE_EDGE = 2_048
private const val OUTPUT_EDGE = 1_024
private const val MAX_ENCODED_BYTES = 3_500_000
