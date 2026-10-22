package ma.etherealmber.atlascommerce.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.core.AppDetails
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Shared profile picture & image loader supporting every Atlas Commerce avatar representation:
 *  1. Raster data URIs (`data:image/webp;base64,...`, `png`, `jpeg`, `gif`)
 *  2. Worker SVG data URIs (`data:image/svg+xml;charset=utf-8,...` or `;base64,...`)
 *  3. Server-relative API/R2 paths (`/api/...`, `/r2/...`, `/uploads/...`) resolved against the active server
 *  4. Full HTTPS CDN URLs (`https://...`)
 *
 * Decoded bitmaps are cached in memory via [AvatarBitmapCache] for instant re-composition across tabs.
 */
object AvatarBitmapCache {
    private val memory = LruCache<String, ImageBitmap>(48)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun get(key: String): ImageBitmap? = memory.get(key)

    fun clear() {
        memory.evictAll()
    }

    suspend fun load(
        rawUrl: String?,
        serverBaseUrl: String?,
    ): ImageBitmap? {
        val clean = rawUrl?.trim().orEmpty()
        if (clean.isEmpty()) return null
        val cacheKey = "${serverBaseUrl.orEmpty()}|$clean"
        memory.get(cacheKey)?.let { return it }

        val decoded = withContext(Dispatchers.IO) {
            runCatching { decodeAvatarBitmap(clean, serverBaseUrl) }.getOrNull()
        }
        val imageBitmap = decoded?.asImageBitmap()
        if (imageBitmap != null) {
            memory.put(cacheKey, imageBitmap)
        }
        return imageBitmap
    }

    internal fun decodeAvatarBitmap(clean: String, serverBaseUrl: String?): Bitmap? {
        if (clean.startsWith("data:image/svg+xml", ignoreCase = true)) {
            return renderSvgDataUri(clean)
        }
        if (clean.startsWith("data:image/", ignoreCase = true)) {
            val comma = clean.indexOf(',')
            if (comma <= 0) return null
            val meta = clean.substring(0, comma)
            val payload = clean.substring(comma + 1)
            val bytes = if (meta.contains(";base64", ignoreCase = true)) {
                Base64.decode(payload, Base64.DEFAULT)
            } else {
                URLDecoder.decode(payload, StandardCharsets.UTF_8.name()).toByteArray(StandardCharsets.UTF_8)
            }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        val resolvedUrl = when {
            clean.startsWith("https://", ignoreCase = true) ||
                clean.startsWith("http://", ignoreCase = true) -> clean

            clean.startsWith("/") -> {
                val base = serverBaseUrl?.takeIf { it.isNotBlank() }?.trimEnd('/')
                    ?: return null
                "$base$clean"
            }

            else -> return null
        }

        val request = Request.Builder()
            .url(resolvedUrl)
            .header("Accept", "image/webp,image/png,image/jpeg,image/svg+xml,*/*")
            .build()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val contentType = resp.header("Content-Type").orEmpty()
            val bodyBytes = resp.body?.bytes() ?: return null
            if (contentType.contains("svg", ignoreCase = true)) {
                return renderSvgXml(String(bodyBytes, StandardCharsets.UTF_8))
            }
            return BitmapFactory.decodeByteArray(bodyBytes, 0, bodyBytes.size)
        }
    }

    /**
     * Parses Atlas worker's generated SVG avatars (`<svg><rect fill="..."/><text>AB</text></svg>`)
     * into a crisp 256x256 ARGB bitmap without requiring a WebView.
     */
    internal fun renderSvgDataUri(dataUri: String): Bitmap? {
        val comma = dataUri.indexOf(',')
        if (comma <= 0) return null
        val meta = dataUri.substring(0, comma)
        val payload = dataUri.substring(comma + 1)
        val xml = if (meta.contains(";base64", ignoreCase = true)) {
            String(Base64.decode(payload, Base64.DEFAULT), StandardCharsets.UTF_8)
        } else {
            URLDecoder.decode(payload, StandardCharsets.UTF_8.name())
        }
        return renderSvgXml(xml)
    }

    internal fun renderSvgXml(xml: String): Bitmap? {
        var bgColor = android.graphics.Color.parseColor("#1D4ED8")
        var textColor = android.graphics.Color.WHITE
        var label = ""
        runCatching {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            var insideText = false
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "rect", "circle" -> {
                                parser.getAttributeValue(null, "fill")?.let { fill ->
                                    parseCssColor(fill)?.let { bgColor = it }
                                }
                            }
                            "stop" -> {
                                parser.getAttributeValue(null, "stop-color")?.let { stop ->
                                    parseCssColor(stop)?.let { bgColor = it }
                                }
                            }
                            "text" -> {
                                insideText = true
                                parser.getAttributeValue(null, "fill")?.let { fill ->
                                    parseCssColor(fill)?.let { textColor = it }
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideText) label += parser.text.orEmpty().trim()
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("text", ignoreCase = true)) insideText = false
                    }
                }
                event = parser.next()
            }
        }
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), bgPaint)
        if (label.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = size * 0.42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val y = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label.take(3).uppercase(), size / 2f, y, textPaint)
        }
        return bmp
    }

    private fun parseCssColor(raw: String): Int? = runCatching {
        val clean = raw.trim()
        if (clean.startsWith("#") && clean.length == 4) {
            val r = clean[1]
            val g = clean[2]
            val b = clean[3]
            android.graphics.Color.parseColor("#$r$r$g$g$b$b")
        } else {
            android.graphics.Color.parseColor(clean)
        }
    }.getOrNull()

    /**
     * Reads a picked gallery image, center-crops & scales to max 256x256, and encodes a compact
     * WebP/JPEG data URI suitable for `PATCH /api/auth/me` (`avatar_url`).
     */
    suspend fun encodeGalleryAvatarDataUri(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@runCatching null
                val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@runCatching null
                val minEdge = minOf(original.width, original.height)
                val x = (original.width - minEdge) / 2
                val y = (original.height - minEdge) / 2
                val square = Bitmap.createBitmap(original, x, y, minEdge, minEdge)
                val scaled = Bitmap.createScaledBitmap(square, 256, 256, true)
                val out = ByteArrayOutputStream()
                @Suppress("DEPRECATION")
                val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                scaled.compress(format, 82, out)
                val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                "data:image/webp;base64,$encoded"
            }.getOrNull()
        }
}

@Composable
fun AtlasAvatar(
    avatarUrl: String?,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    serverBaseUrl: String? = null,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    onClick: (() -> Unit)? = null,
) {
    val cleanUrl = avatarUrl?.trim().orEmpty()
    val cacheKey = remember(cleanUrl, serverBaseUrl) { "${serverBaseUrl.orEmpty()}|$cleanUrl" }
    var bitmap by remember(cacheKey) { mutableStateOf(AvatarBitmapCache.get(cacheKey)) }

    LaunchedEffect(cleanUrl, serverBaseUrl) {
        if (cleanUrl.isNotEmpty() && bitmap == null) {
            bitmap = AvatarBitmapCache.load(cleanUrl, serverBaseUrl)
        }
    }

    val initials = remember(label) {
        label.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "AT" }
    }

    val boxModifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        .border(1.dp, borderColor, CircleShape)
        .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center,
    ) {
        val img = bitmap
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = "Photo de profil de $label",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = initials,
                style = if (size >= 52.dp) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.labelMedium
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
