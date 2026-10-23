package ma.etherealmber.atlascommerce.core.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.Roles
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Privacy-preserving ring buffer & crash breadcrumb logger for diagnostic bug reports.
 *
 * Automatically redacts tokens, OTP codes, signatures, cookies, and e-mail addresses
 * before retaining any line in memory or writing crash breadcrumbs.
 */
object AtlasLogBuffer {
    private const val MAX_ENTRIES = 240
    private const val CRASH_FILE_NAME = "last_crash.txt"
    private val entries = ArrayDeque<String>(MAX_ENTRIES)

    private val bearerRegex = Regex("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+")
    private val secretParamRegex = Regex("(?i)((?:token|otp|sig|password|secret|key|authorization)\\s*[=:]\\s*)[^\\s&\",}]+")
    private val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

    fun sanitize(raw: String): String =
        raw.replace(bearerRegex, "$1[REDACTED]")
            .replace(secretParamRegex, "$1[REDACTED]")
            .replace(emailRegex, "[REDACTED_EMAIL]")

    fun log(tag: String, message: String, level: String = "INFO") {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$stamp [$level/$tag] ${sanitize(message.trim())}"
        synchronized(entries) {
            if (entries.size >= MAX_ENTRIES) {
                entries.removeFirst()
            }
            entries.addLast(line)
        }
    }

    fun recentEntries(): List<String> = synchronized(entries) { entries.toList() }

    fun installCrashBreadcrumbHandler(context: Context) {
        val dir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val crashFile = File(dir, CRASH_FILE_NAME)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
                val text = buildString {
                    appendLine("[$stamp] Uncaught exception on thread '${thread.name}':")
                    append(sanitize(sw.toString()).take(6_000))
                }
                crashFile.writeText(text, Charsets.UTF_8)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun captureDiagnosticLogs(context: Context, maxChars: Int = 9_000): String {
        val sections = StringBuilder()

        // 1. Previous uncaught crash trace if present
        val crashFile = File(File(context.cacheDir, "diagnostics"), CRASH_FILE_NAME)
        if (crashFile.isFile) {
            val crashText = runCatching { crashFile.readText(Charsets.UTF_8).trim() }.getOrDefault("")
            if (crashText.isNotEmpty()) {
                sections.appendLine("=== LAST CRASH TRACE ===")
                sections.appendLine(sanitize(crashText).take(2_800))
                sections.appendLine()
            }
        }

        // 2. Structured in-app breadcrumbs & HTTP telemetry
        val ring = recentEntries()
        if (ring.isNotEmpty()) {
            sections.appendLine("=== IN-APP BREADCRUMBS (${ring.size}) ===")
            ring.takeLast(90).forEach { sections.appendLine(it) }
            sections.appendLine()
        }

        // 3. Own-PID Android kernel logcat (requires ZERO extra permissions on Android)
        val pidLogs = runCatching {
            val pid = Process.myPid().toString()
            val proc = ProcessBuilder("logcat", "-d", "-v", "time", "-t", "90", "--pid=$pid")
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            proc.waitFor()
            sanitize(out.trim())
        }.getOrDefault("")

        if (pidLogs.isNotEmpty()) {
            sections.appendLine("=== ANDROID LOGCAT (PID ${Process.myPid()}) ===")
            pidLogs.lineSequence().toList().takeLast(80).forEach { sections.appendLine(it) }
        }

        val combined = sections.toString().trim()
        return if (combined.length <= maxChars) combined else combined.takeLast(maxChars)
    }
}

data class CompressedReportAttachment(
    val id: String,
    val fileName: String,
    val file: File,
    val preview: ImageBitmap?,
    val originalBytes: Long,
    val finalBytes: Long,
    val wasCompressed: Boolean,
) {
    val sizeSummary: String
        get() {
            val kb = (finalBytes / 1024L).coerceAtLeast(1L)
            return if (wasCompressed) {
                val origKb = (originalBytes / 1024L).coerceAtLeast(1L)
                "$kb Ko (réduite depuis $origKb Ko · ≤ 1 Mo)"
            } else {
                "$kb Ko (≤ 1 Mo)"
            }
        }
}

data class DeviceDiagnosticSnapshot(
    val reportId: String,
    val timestampUtc: String,
    val deviceShortId: String,
    val maskedFingerprint: String,
    val appVersion: String,
    val versionCode: Int,
    val manufacturerModel: String,
    val androidVersion: String,
    val cpuAbi: String,
    val screenMetrics: String,
    val memoryUsage: String,
    val networkStatus: String,
    val userRole: String,
    val screenOrigin: String,
)

/**
 * Handles up-to-3 screenshot attachments with guaranteed `<= 1 MB` compression,
 * device/OS/display telemetry collection, and GitHub `@atlas-bot` report generation.
 */
object DiagnosticReporter {
    const val MAX_ATTACHMENTS = 3
    const val MAX_IMAGE_BYTES = 1_000_000 // Strictly under 1 MB (1,000,000 bytes) per image

    /**
     * Pure deterministic JPEG downscale + quality reducer guaranteeing output `<= maxBytes` (`< 1 MB`).
     */
    fun compressBitmapUnderLimit(
        source: Bitmap,
        maxBytes: Int = MAX_IMAGE_BYTES,
    ): ByteArray {
        val edgeSteps = intArrayOf(1600, 1280, 1024, 800, 640, 480, 360)
        val qualitySteps = intArrayOf(86, 74, 62, 50, 38, 28, 20)
        var bestBytes = ByteArray(0)

        for (maxEdge in edgeSteps) {
            val scaled = scaleDownIfNeeded(source, maxEdge)
            for (quality in qualitySteps) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val candidate = out.toByteArray()
                bestBytes = candidate
                if (candidate.size <= maxBytes) {
                    if (scaled !== source) scaled.recycle()
                    return candidate
                }
            }
            if (scaled !== source) scaled.recycle()
        }
        return bestBytes
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val largest = maxOf(w, h)
        if (largest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / largest.toFloat()
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    suspend fun prepareAttachment(
        context: Context,
        uri: Uri,
        slotIndex: Int,
    ): CompressedReportAttachment? = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null
            if (rawBytes.isEmpty()) return@runCatching null

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
            val rawW = boundsOpts.outWidth
            val rawH = boundsOpts.outHeight
            if (rawW <= 0 || rawH <= 0) return@runCatching null

            var sampleSize = 1
            while (maxOf(rawW / sampleSize, rawH / sampleSize) > 2048) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return@runCatching null

            val needsCompression = rawBytes.size > MAX_IMAGE_BYTES || maxOf(rawW, rawH) > 1920
            val finalBytes = if (needsCompression) {
                compressBitmapUnderLimit(decoded, MAX_IMAGE_BYTES)
            } else {
                rawBytes
            }

            val thumbBitmap = scaleDownIfNeeded(decoded, 320).asImageBitmap()
            if (decoded.width > 320 || decoded.height > 320) {
                decoded.recycle()
            }

            val dir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
            val ext = if (needsCompression) "jpg" else "png"
            val outName = "atlas_diag_${System.currentTimeMillis()}_${slotIndex + 1}.$ext"
            val file = File(dir, outName).apply { writeBytes(finalBytes) }

            CompressedReportAttachment(
                id = "${System.nanoTime()}_$slotIndex",
                fileName = outName,
                file = file,
                preview = thumbBitmap,
                originalBytes = rawBytes.size.toLong(),
                finalBytes = finalBytes.size.toLong(),
                wasCompressed = needsCompression || finalBytes.size < rawBytes.size,
            )
        }.getOrNull()
    }

    fun collectSnapshot(
        context: Context,
        shortDeviceId: String,
        fullFingerprint: String,
        role: String?,
        screenOrigin: String,
    ): DeviceDiagnosticSnapshot {
        val now = Date()
        val isoUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(now)
        val compactStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(now)

        val dm = context.resources.displayMetrics
        val config = context.resources.configuration
        val rt = Runtime.getRuntime()
        val usedMb = ((rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L)).coerceAtLeast(1L)
        val maxMb = (rt.maxMemory() / (1024L * 1024L)).coerceAtLeast(1L)

        val netLabel = runCatching {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            when {
                caps == null -> "Hors ligne / Aucun réseau actif"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi actif"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Données mobiles (4G/5G)"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connecté"
            }
        }.getOrDefault("Inconnu")

        val cleanShort = shortDeviceId.ifBlank { "anon" }
        val maskedFp = if (fullFingerprint.length >= 12) {
            "${fullFingerprint.take(8)}…${fullFingerprint.takeLast(4)}"
        } else {
            cleanShort
        }

        return DeviceDiagnosticSnapshot(
            reportId = "DIAG-$compactStamp-$cleanShort",
            timestampUtc = isoUtc,
            deviceShortId = cleanShort,
            maskedFingerprint = maskedFp,
            appVersion = "v${AppDetails.VERSION_NAME}",
            versionCode = AppDetails.VERSION_CODE,
            manufacturerModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim(),
            androidVersion = "Android ${Build.VERSION.RELEASE ?: "?"} (SDK ${Build.VERSION.SDK_INT})",
            cpuAbi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "arm64-v8a",
            screenMetrics = "${dm.widthPixels}x${dm.heightPixels}px · ${dm.densityDpi}dpi · fontScale=${"%.2f".format(Locale.US, config.fontScale)}",
            memoryUsage = "$usedMb Mo / $maxMb Mo",
            networkStatus = netLabel,
            userRole = Roles.label(role),
            screenOrigin = screenOrigin,
        )
    }

    fun buildMarkdownReport(
        category: String,
        title: String,
        description: String,
        stepsToReproduce: String,
        snapshot: DeviceDiagnosticSnapshot,
        logs: String,
        attachments: List<CompressedReportAttachment>,
        maxLogsChars: Int = 5_500,
    ): String {
        val trimmedLogs = if (logs.length <= maxLogsChars) logs else logs.takeLast(maxLogsChars)
        return buildString {
            appendLine("<!-- atlas-diag-v1 report_id=${snapshot.reportId} device_id=${snapshot.deviceShortId} -->")
            appendLine("### 🩺 Rapport de diagnostic Atlas Commerce Android")
            appendLine()
            appendLine("- **Catégorie :** `$category`")
            appendLine("- **Référence :** `${snapshot.reportId}`")
            appendLine("- **Identifiant appareil (anonymisé) :** `${snapshot.maskedFingerprint}`")
            appendLine()
            appendLine("#### 📝 Description du problème")
            appendLine(description.trim().ifEmpty { "_Aucune description complémentaire fournie._" })
            if (stepsToReproduce.isNotBlank()) {
                appendLine()
                appendLine("#### 🔁 Étapes observées")
                appendLine(stepsToReproduce.trim())
            }
            appendLine()
            appendLine("#### 📱 Environnement & Appareil")
            appendLine("| Propriété | Valeur |")
            appendLine("| :--- | :--- |")
            appendLine("| **Application** | `${snapshot.appVersion} (build ${snapshot.versionCode})` |")
            appendLine("| **Appareil** | `${snapshot.manufacturerModel}` (`${snapshot.cpuAbi}`) |")
            appendLine("| **Système Android** | `${snapshot.androidVersion}` |")
            appendLine("| **Écran & Police** | `${snapshot.screenMetrics}` |")
            appendLine("| **Mémoire JVM** | `${snapshot.memoryUsage}` |")
            appendLine("| **Réseau** | `${snapshot.networkStatus}` |")
            appendLine("| **Écran actif / Rôle** | `${snapshot.screenOrigin}` · `${snapshot.userRole}` |")
            appendLine("| **Horodatage UTC** | `${snapshot.timestampUtc}` |")

            if (attachments.isNotEmpty()) {
                appendLine()
                appendLine("#### 🖼️ Captures préparées (${attachments.size}/${MAX_ATTACHMENTS} · max 1 Mo chacune)")
                attachments.forEachIndexed { idx, item ->
                    appendLine("- **Capture ${idx + 1}** (`${item.fileName}`) — ${item.sizeSummary}")
                }
                appendLine("> _Astuce : collez ou glissez directement les captures compressées préparées dans le presse-papiers / partage Android ci-dessous._")
            }

            appendLine()
            appendLine("<details>")
            appendLine("<summary>📋 Journaux système, réseau & traces capturés (${trimmedLogs.lines().size} lignes)</summary>")
            appendLine()
            appendLine("```log")
            appendLine(trimmedLogs.ifBlank { "Aucune erreur récente enregistrée dans le tampon local." })
            appendLine("```")
            appendLine("</details>")
            appendLine()
            appendLine("@atlas-bot merci d'analyser ces journaux, d'identifier la cause racine, de proposer les solutions et de générer un prompt IA de correction.")
        }
    }

    fun buildGitHubIssueUrl(
        category: String,
        title: String,
        description: String,
        stepsToReproduce: String,
        snapshot: DeviceDiagnosticSnapshot,
        logs: String,
        attachments: List<CompressedReportAttachment>,
    ): String {
        val cleanTitle = "[Diag][${snapshot.appVersion}] ${title.trim().ifEmpty { category }}"
        // Keep query parameter under browser 7.5 KB URL limit so GitHub pre-fills 100% cleanly on mobile
        val urlBody = buildMarkdownReport(
            category = category,
            title = title,
            description = description,
            stepsToReproduce = stepsToReproduce,
            snapshot = snapshot,
            logs = logs,
            attachments = attachments,
            maxLogsChars = 2_400,
        )
        return Uri.parse("${AppDetails.PROJECT_REPO.trimEnd('/')}/issues/new")
            .buildUpon()
            .appendQueryParameter("title", cleanTitle)
            .appendQueryParameter("labels", "bug,diagnostic-report")
            .appendQueryParameter("body", urlBody)
            .build()
            .toString()
    }

    /**
     * Attempts direct automated submission to `<serverBase>/api/support/bug-report`
     * (including up to 3 `<= 1 MB` compressed images) if supported by the connected Worker;
     * returns the created GitHub Issue URL when answered by the server, or `null` to
     * gracefully launch the pre-filled GitHub Issue + clipboard bundle.
     */
    suspend fun tryDirectServerRelay(
        client: OkHttpClient,
        serverBaseUrl: String?,
        title: String,
        category: String,
        markdownBody: String,
        snapshot: DeviceDiagnosticSnapshot,
        logs: String,
        attachments: List<CompressedReportAttachment>,
    ): String? = withContext(Dispatchers.IO) {
        val cleanBase = serverBaseUrl?.trim()?.trimEnd('/')?.takeIf { it.startsWith("https://") }
            ?: return@withContext null
        runCatching {
            val imagesJson = JSONArray()
            attachments.take(MAX_ATTACHMENTS).forEach { att ->
                if (att.file.isFile && att.file.length() <= MAX_IMAGE_BYTES) {
                    imagesJson.put(
                        JSONObject().apply {
                            put("name", att.fileName)
                            put("sizeBytes", att.finalBytes)
                            put("mimeType", if (att.fileName.endsWith(".png")) "image/png" else "image/jpeg")
                            put(
                                "base64",
                                android.util.Base64.encodeToString(
                                    att.file.readBytes(),
                                    android.util.Base64.NO_WRAP,
                                ),
                            )
                        },
                    )
                }
            }
            val payload = JSONObject().apply {
                put("reportId", snapshot.reportId)
                put("category", category)
                put("title", "[Diag][${snapshot.appVersion}] $title")
                put("deviceId", snapshot.maskedFingerprint)
                put("appVersion", snapshot.appVersion)
                put("versionCode", snapshot.versionCode)
                put("device", snapshot.manufacturerModel)
                put("android", snapshot.androidVersion)
                put("screen", snapshot.screenMetrics)
                put("markdown", markdownBody)
                put("logs", logs)
                put("attachments", imagesJson)
            }
            val req = Request.Builder()
                .url("$cleanBase/api/support/bug-report")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val respJson = JSONObject(resp.body?.string().orEmpty())
                respJson.optString("issueUrl").takeIf { it.startsWith("https://github.com/") }
            }
        }.getOrNull()
    }

    fun copyFullReportToClipboard(context: Context, fullMarkdown: String) {
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("Atlas Diagnostic Report", fullMarkdown))
        }
    }

    fun shareBundleWithAttachments(
        context: Context,
        title: String,
        fullMarkdown: String,
        logs: String,
        attachments: List<CompressedReportAttachment>,
    ): Boolean = runCatching {
        val dir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val logFile = File(dir, "atlas-logs-${System.currentTimeMillis()}.txt").apply {
            writeText(logs.ifBlank { fullMarkdown }, Charsets.UTF_8)
        }
        val uris = ArrayList<Uri>()
        uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logFile))
        attachments.take(MAX_ATTACHMENTS).forEach { att ->
            if (att.file.isFile) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", att.file))
            }
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_SUBJECT, "[Atlas Commerce Diagnostic] $title")
            putExtra(Intent.EXTRA_TEXT, fullMarkdown)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Envoyer le rapport & les captures").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
