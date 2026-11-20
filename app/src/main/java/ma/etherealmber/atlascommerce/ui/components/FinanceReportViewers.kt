package ma.etherealmber.atlascommerce.ui.components

import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasLocale
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import ma.etherealmber.atlascommerce.core.AppLanguage
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.text.DateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

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
        value = null
        value = openPdfRenderSession(file)
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
        val renderResult by produceState<Result<Bitmap>?>(null, session, pageIndex, widthPx) {
            value = null
            value = withContext(Dispatchers.IO) { runCatching { session.render(pageIndex, widthPx) } }
        }
        val size = session.dimensions[pageIndex]
        val pageModifier = Modifier.width(pageWidth).aspectRatio(size.first.toFloat() / size.second.coerceAtLeast(1))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
        ) {
            when {
                renderResult == null -> Box(pageModifier.background(Color.White), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                renderResult?.isFailure == true -> Box(pageModifier.background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        atlasText("Impossible d'afficher cette page. Fermez puis rouvrez le rapport.", "This page could not be rendered. Close and reopen the report.", "تعذر عرض هذه الصفحة. أغلق التقرير ثم افتحه مجدداً."),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                else -> renderResult?.getOrNull()?.let { renderedBitmap ->
                    Image(
                        bitmap = renderedBitmap.asImageBitmap(),
                        contentDescription = atlasText(
                            "Page ${pageIndex + 1} du rapport, zoom ${(zoom * 100).toInt()} pour cent",
                            "Report page ${pageIndex + 1}, zoom ${(zoom * 100).toInt()} percent",
                            "صفحة التقرير ${pageIndex + 1}، التكبير ${(zoom * 100).toInt()} بالمئة",
                        ),
                        modifier = pageModifier.background(Color.White),
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

/** Full-screen, account-scoped index of recent monthly PDFs stored in app-private cache. */
@Composable
fun FinanceReportHistoryDialog(
    reports: List<File>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (File) -> Unit,
) {
    var selectedPath by remember(reports) { mutableStateOf(reports.firstOrNull()?.absolutePath) }
    val selectedReport = reports.firstOrNull { it.absolutePath == selectedPath }
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = atlasText("Fermer", "Close", "إغلاق"))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            atlasText("Historique des rapports", "Report history", "سجل التقارير"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            atlasText("PDF récents enregistrés sur cet appareil", "Recent PDFs saved on this device", "ملفات PDF الأخيرة المحفوظة على هذا الجهاز"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onRefresh, enabled = !loading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser", "Refresh", "تحديث"))
                    }
                }
                Text(
                    atlasText(
                        "Privé à ce compte · conservation maximale de 7 jours et 8 rapports. Touchez un rapport pour afficher tout son contenu, le partager ou l'exporter.",
                        "Private to this account · kept for up to 7 days and 8 reports. Select a report to view all pages, share it, or export a copy.",
                        "خاص بهذا الحساب · يُحتفظ به حتى 7 أيام وبحد أقصى 8 تقارير. اختر تقريراً لعرض جميع صفحاته أو مشاركته أو تصدير نسخة.",
                    ),
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    reports.isEmpty() -> Box(
                        Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                atlasText("Aucun rapport récent", "No recent reports", "لا توجد تقارير حديثة"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                atlasText("Les relevés PDF que vous téléchargez apparaîtront ici.", "Downloaded PDF statements will appear here.", "ستظهر كشوف PDF التي تنزّلها هنا."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                        val tabletLayout = maxWidth >= 720.dp
                        if (tabletLayout) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                LazyColumn(
                                    modifier = Modifier.weight(0.38f).fillMaxHeight(),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(reports, key = { it.absolutePath }) { file ->
                                        FinanceReportHistoryRow(
                                            file = file,
                                            selected = file.absolutePath == selectedReport?.absolutePath,
                                            onClick = { selectedPath = file.absolutePath },
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(0.62f).fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (selectedReport == null) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                atlasText("Sélectionnez un rapport pour afficher son contenu.", "Select a report to view its contents.", "اختر تقريراً لعرض محتواه."),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                                                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    financeReportTitle(selectedReport),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    financeReportCreatedAt(selectedReport),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            Button(onClick = { onSelect(selectedReport) }) {
                                                Text(atlasText("Actions / exporter", "Actions / export", "إجراءات / تصدير"))
                                            }
                                        }
                                        FinanceReportInlinePreview(
                                            file = selectedReport,
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(reports, key = { it.absolutePath }) { file ->
                                    FinanceReportHistoryRow(
                                        file = file,
                                        selected = false,
                                        onClick = { onSelect(file) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceReportHistoryRow(file: File, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    financeReportTitle(file),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    financeReportCreatedAt(file),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = atlasText("Ouvrir le rapport", "Open report", "فتح التقرير"),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun financeReportTitle(file: File): String =
    financeReportTitle(file, LocalAtlasLanguage.current)

internal fun financeMonthLabel(period: String, locale: Locale): String = runCatching {
    YearMonth.parse(period)
        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        .replaceFirstChar { it.titlecase(locale) }
}.getOrDefault(period)

internal fun financeReportTitle(file: File, language: AppLanguage): String {
    val period = Regex("^rapport-mensuel-(\\d{4}-(?:0[1-9]|1[0-2]))(?:-\\d+)?$")
        .find(file.nameWithoutExtension)?.groupValues?.getOrNull(1)
    val locale = Locale.forLanguageTag(language.tag)
    val label = period?.let { financeMonthLabel(it, locale) } ?: file.nameWithoutExtension
    val prefix = when (language) {
        AppLanguage.FRENCH -> "Relevé mensuel"
        AppLanguage.ENGLISH -> "Monthly statement"
        AppLanguage.ARABIC -> "كشف شهري"
    }
    return "$prefix · $label"
}

@Composable
private fun financeReportCreatedAt(file: File): String {
    val createdAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, atlasLocale())
        .format(Date(file.lastModified()))
    return atlasText("Créé le $createdAt", "Created $createdAt", "أُنشئ في $createdAt")
}


private suspend fun openPdfRenderSession(file: File): Result<PdfRenderSession> {
    var openedSession: PdfRenderSession? = null
    return try {
        val session = withContext(Dispatchers.IO) {
            PdfRenderSession.open(file).also { openedSession = it }
        }
        Result.success(session)
    } catch (cancelled: CancellationException) {
        withContext(NonCancellable + Dispatchers.IO) { openedSession?.closeQuietly() }
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}

@Composable
private fun FinanceReportInlinePreview(file: File, modifier: Modifier = Modifier) {
    val result by produceState<Result<PdfRenderSession>?>(null, file.absolutePath) {
        value = null
        value = openPdfRenderSession(file)
    }
    val session = result?.getOrNull()
    DisposableEffect(session) {
        onDispose { session?.closeQuietly() }
    }
    when {
        result == null -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        session == null -> Box(modifier.padding(20.dp), contentAlignment = Alignment.Center) {
            Text(
                atlasText("Impossible de prévisualiser ce PDF.", "Could not preview this PDF.", "تعذرت معاينة ملف PDF هذا."),
                color = MaterialTheme.colorScheme.error,
            )
        }
        else -> LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(session.dimensions.indices.toList(), key = { it }) { index ->
                PdfPage(session = session, pageIndex = index, zoom = 1f)
            }
        }
    }
}
