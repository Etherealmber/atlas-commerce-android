package ma.etherealmber.atlascommerce.ui.components

import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import ma.etherealmber.atlascommerce.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
    BackHandler(onBack = onDismiss)
    var zoom by remember(file.absolutePath) { mutableStateOf(1f) }
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
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = atlasText("Retour au rapport précédent")) }
                    Column(Modifier.weight(1f)) {
                        Text(atlasText("Rapport mensuel · PDF"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = session?.let {
                                atlasText(
                                    "${it.pageCount} page${if (it.pageCount > 1) "s" else ""} · lecture privée",
                                    "${it.pageCount} page(s) · private viewing",
                                    "${it.pageCount} صفحات · عرض خاص",
                                )
                            } ?: atlasText("Ouverture du document…", "Opening document…", "جارٍ فتح المستند…"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (onShare != null) IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = atlasText("Partager le PDF")) }
                    if (onSave != null) IconButton(onClick = onSave) { Icon(Icons.Outlined.FileDownload, contentDescription = atlasText("Enregistrer une copie")) }
                    if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = atlasText("Supprimer le PDF temporaire")) }
                }
                if (session != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(atlasText("Zoom"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { zoom = (zoom - 0.25f).coerceAtLeast(1f) }, enabled = zoom > 1f) {
                            Icon(Icons.Outlined.ZoomOut, contentDescription = atlasText("Réduire le zoom"))
                        }
                        Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { zoom = (zoom + 0.25f).coerceAtMost(3f) }, enabled = zoom < 3f) {
                            Icon(Icons.Outlined.ZoomIn, contentDescription = atlasText("Agrandir le PDF"))
                        }
                    }
                }
                when {
                    loadResult == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    session == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(atlasText("Impossible de lire ce PDF. Téléchargez-le à nouveau."), color = MaterialTheme.colorScheme.error)
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(session.dimensions.indices.toList(), key = { "page-$it" }) { index ->
                            PdfPage(session = session, pageIndex = index, zoom = zoom)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPage(session: PdfRenderSession, pageIndex: Int, zoom: Float) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val pageWidth = maxWidth * zoom
        val widthPx = with(density) { pageWidth.roundToPx().coerceIn(1, 2600) }
        val bitmap by produceState<Bitmap?>(null, session, pageIndex, widthPx) {
            value = withContext(Dispatchers.IO) { runCatching { session.render(pageIndex, widthPx) }.getOrNull() }
        }
        val size = session.dimensions[pageIndex]
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (bitmap == null) {
                Box(Modifier.width(pageWidth).aspectRatio(size.first.toFloat() / size.second.coerceAtLeast(1)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                bitmap?.let { renderedBitmap ->
                    Image(
                        bitmap = renderedBitmap.asImageBitmap(),
                        contentDescription = atlasText(
                            "Page ${pageIndex + 1} du rapport, zoom ${(zoom * 100).toInt()} pour cent",
                            "Report page ${pageIndex + 1}, zoom ${(zoom * 100).toInt()} percent",
                            "صفحة التقرير ${pageIndex + 1}، التكبير ${(zoom * 100).toInt()} بالمئة",
                        ),
                        modifier = Modifier.width(pageWidth).aspectRatio(size.first.toFloat() / size.second.coerceAtLeast(1)),
                    )
                }
            }
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
fun CombinedReportWebViewDialog(
    url: String,
    onDismiss: () -> Unit,
    selectedSections: List<String> = FinanceReportSection.entries.map { it.key },
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val originUri = Uri.parse(url)
    val permittedScheme = originUri.scheme == "https" || (BuildConfig.ALLOW_CLEARTEXT && originUri.scheme == "http")
    val permittedHost = originUri.host?.lowercase().orEmpty()
    val sections = FinanceReportSection.entries.filter { it.key in selectedSections }
    val showSummary = sections.size == FinanceReportSection.entries.size && originUri.getQueryParameter("period") != "custom"
    val tabKeys = (if (showSummary) listOf("summary") else emptyList()) + sections.map { it.key }
    var activeKey by remember(url, selectedSections) {
        mutableStateOf(if (showSummary) "summary" else sections.firstOrNull()?.key ?: "summary")
    }
    val activeSection = sections.firstOrNull { it.key == activeKey }
    val activeTitle = if (activeKey == "summary") "Synthèse" else activeSection?.label ?: "Rapport"
    val periodSummary = FinanceReportSelection(
        period = originUri.getQueryParameter("period") ?: "month",
        from = originUri.getQueryParameter("from"),
        to = originUri.getQueryParameter("to"),
    ).shortLabel
    val activeUrl = remember(url, activeKey, activeSection) {
        if (activeKey == "summary" || activeSection == null) url else reportSectionUrl(originUri, activeSection)
    }
    var webView by remember(url) { mutableStateOf<WebView?>(null) }

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
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = atlasText("Retour à la page précédente")) }
                    Column(Modifier.weight(1f)) {
                        Text(atlasText("Rapports · $activeTitle", "Reports · $activeTitle", "التقارير · $activeTitle"), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(atlasText("$periodSummary · consultation sécurisée", "$periodSummary · secure view", "$periodSummary · عرض آمن"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = { printReport(context, webView, activeTitle) }, enabled = webView != null) {
                        Icon(Icons.Outlined.Print, contentDescription = atlasText("Imprimer ou enregistrer en PDF"))
                    }
                }
                if (tabKeys.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        tabKeys.forEach { key ->
                            val label = if (key == "summary") "Synthèse" else sections.firstOrNull { it.key == key }?.label.orEmpty()
                            FilterChip(selected = key == activeKey, onClick = { activeKey = key }, label = { Text(label, maxLines = 1) })
                        }
                    }
                }
                Text(
                    atlasText("Utilisez le bouton d'impression Atlas pour enregistrer en PDF. Retour ferme le rapport et vous ramène à l'écran précédent."),
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!permittedScheme || permittedHost.isBlank()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(atlasText("Lien de rapport non sécurisé refusé."), color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            WebView(viewContext).apply {
                                webView = this
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
                                        val destinationPort = destination.port.takeIf { it > 0 } ?: if (destination.scheme == "https") 443 else 80
                                        val expectedPort = originUri.port.takeIf { it > 0 } ?: if (originUri.scheme == "https") 443 else 80
                                        val sameOrigin = destination.scheme == originUri.scheme &&
                                            destination.host?.equals(permittedHost, ignoreCase = true) == true &&
                                            destinationPort == expectedPort
                                        return !sameOrigin
                                    }
                                }
                                loadUrl(activeUrl)
                            }
                        },
                        update = { view ->
                            webView = view
                            if (view.url != activeUrl) view.loadUrl(activeUrl)
                        },
                        onRelease = { view ->
                            webView = null
                            view.stopLoading()
                            view.loadUrl("about:blank")
                            view.clearHistory()
                            view.clearCache(true)
                            view.destroy()
                        },
                    )
                }
            }
        }
    }
}

private fun reportSectionUrl(origin: Uri, section: FinanceReportSection): String {
    val prefix = origin.path.orEmpty().substringBeforeLast('/', "/api/reports")
    val builder = origin.buildUpon().encodedPath("$prefix/${section.path}").clearQuery().fragment(null)
    origin.queryParameterNames
        .filterNot { it == "sections" }
        .forEach { name -> origin.getQueryParameters(name).forEach { value -> builder.appendQueryParameter(name, value) } }
    return builder.build().toString()
}

private fun printReport(context: Context, webView: WebView?, title: String) {
    val view = webView ?: return
    runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(
            "Atlas-${title.replace(Regex("[^A-Za-z0-9_-]"), "-")}",
            view.createPrintDocumentAdapter(title),
            PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build(),
        )
    }.onFailure { Toast.makeText(context, atlasText("Impossible d'ouvrir l'impression."), Toast.LENGTH_LONG).show() }
}
