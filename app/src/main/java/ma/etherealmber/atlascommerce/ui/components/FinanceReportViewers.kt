package ma.etherealmber.atlascommerce.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import ma.etherealmber.atlascommerce.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

@Composable
fun FinancePdfViewerDialog(
    file: File,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val loadResult by produceState<Result<PdfRenderSession>?>(null, file.absolutePath) {
        value = withContext(Dispatchers.IO) { runCatching { PdfRenderSession.open(file) } }
    }
    val session = loadResult?.getOrNull()
    DisposableEffect(session) {
        onDispose { session?.closeQuietly() }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Fermer le rapport") }
                    Column(Modifier.weight(1f)) {
                        Text("Rapport PDF", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = session?.let { "${it.pageCount} page${if (it.pageCount > 1) "s" else ""}" } ?: "Lecture sécurisée sur cet appareil",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (onShare != null) IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = "Partager le PDF") }
                    if (onSave != null) IconButton(onClick = onSave) { Icon(Icons.Outlined.FileDownload, contentDescription = "Enregistrer une copie") }
                    if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Supprimer le PDF temporaire") }
                }
                when {
                    loadResult == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    session == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Impossible de lire ce PDF. Téléchargez-le à nouveau.", color = MaterialTheme.colorScheme.error)
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(session.dimensions.indices.toList(), key = { "page-$it" }) { index ->
                            PdfPage(session = session, pageIndex = index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPage(session: PdfRenderSession, pageIndex: Int) {
    BoxWithPdfWidth(session = session, pageIndex = pageIndex)
}

@Composable
private fun BoxWithPdfWidth(session: PdfRenderSession, pageIndex: Int) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx().coerceIn(1, 1800) }
        val bitmap by produceState<Bitmap?>(null, session, pageIndex, widthPx) {
            value = withContext(Dispatchers.IO) { runCatching { session.render(pageIndex, widthPx) }.getOrNull() }
        }
        val size = session.dimensions[pageIndex]
        if (bitmap == null) {
            Box(Modifier.fillMaxWidth().aspectRatio(size.first.toFloat() / size.second.coerceAtLeast(1)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1} du rapport",
                modifier = Modifier.fillMaxWidth().aspectRatio(size.first.toFloat() / size.second.coerceAtLeast(1)),
            )
        }
    }
}

private class PdfRenderSession private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    val dimensions: List<Pair<Int, Int>>,
) : Closeable {
    val pageCount: Int get() = dimensions.size

    @Synchronized
    fun render(index: Int, widthPx: Int): Bitmap {
        val page = renderer.openPage(index)
        try {
            val pageWidth = widthPx.coerceAtLeast(1)
            val pageHeight = (page.height.toDouble() * pageWidth / page.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } finally {
            page.close()
        }
    }

    @Synchronized
    override fun close() {
        renderer.close()
        descriptor.close()
    }

    fun closeQuietly() { runCatching { close() } }

    companion object {
        fun open(file: File): PdfRenderSession {
            require(file.isFile && file.length() > 0) { "PDF is missing or empty" }
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                val renderer = PdfRenderer(descriptor)
                try {
                    val dimensions = buildList {
                        for (index in 0 until renderer.pageCount) {
                            val page = renderer.openPage(index)
                            try { add(page.width to page.height) } finally { page.close() }
                        }
                    }
                    require(dimensions.isNotEmpty()) { "PDF has no pages" }
                    return PdfRenderSession(descriptor, renderer, dimensions)
                } catch (t: Throwable) {
                    renderer.close()
                    throw t
                }
            } catch (t: Throwable) {
                descriptor.close()
                throw t
            }
        }
    }
}

@Composable
fun CombinedReportWebViewDialog(url: String, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val uri = Uri.parse(url)
    val permittedScheme = uri.scheme == "https" || (BuildConfig.ALLOW_CLEARTEXT && uri.scheme == "http")
    val permittedHost = uri.host?.lowercase().orEmpty()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Fermer le rapport") }
                    Column(Modifier.weight(1f)) {
                        Text("Rapport combiné", style = MaterialTheme.typography.titleMedium)
                        Text("Consultation sécurisée dans Atlas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!permittedScheme || permittedHost.isBlank()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Lien de rapport non sécurisé refusé.", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = false
                                settings.domStorageEnabled = false
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                                settings.saveFormData = false
                                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val destination = request.url
                                        val destinationPort = destination.port.takeIf { it > 0 } ?: 443
                                        val expectedPort = uri.port.takeIf { it > 0 } ?: 443
                                        val sameOrigin = destination.scheme == uri.scheme &&
                                            destination.host?.equals(permittedHost, ignoreCase = true) == true &&
                                            destinationPort == expectedPort
                                        return !sameOrigin
                                    }
                                }
                                loadUrl(url)
                            }
                        },
                        onRelease = { webView ->
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.clearHistory()
                            webView.clearCache(true)
                            webView.destroy()
                        },
                    )
                }
            }
        }
    }
}
