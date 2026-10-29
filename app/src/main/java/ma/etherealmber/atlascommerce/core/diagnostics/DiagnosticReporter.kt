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
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.Roles
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
    private const val MAX_ENTRIES = 1_600
    private const val CRASH_FILE_NAME = "last_crash.txt"
    private val entries = ArrayDeque<String>(MAX_ENTRIES)

    private val bearerRegex = Regex("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+")
    private val secretParamRegex = Regex(
        "(?i)((?:access[_-]?token|refresh[_-]?token|token|otp|sig(?:nature)?|password|passphrase|secret|api[_-]?key|authorization|cookie|set-cookie|(?<![A-Za-z0-9_])rt(?![A-Za-z0-9_])|ticket|hmac|challenge)\\s*[=:]\\s*)[^\\s&\\\",}]+",
    )
    private val jwtRegex = Regex("(?<![A-Za-z0-9_-])eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}")
    private val githubTokenRegex = Regex("\\b(?:gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,})\\b")
    private val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val ipv4Regex = Regex("""(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])""")
    private val macAddressRegex = Regex("""(?i)(?<![0-9a-f])(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}(?![0-9a-f])""")
    private val ipv6CandidateRegex = Regex("""(?i)(?<![0-9a-f:])(?:[0-9a-f]{0,4}:){2,7}[0-9a-f]{0,4}(?![0-9a-f:])""")
    private val lowSignalPlatformNoise = listOf(
        Regex("EGL_BAD_MATCH", RegexOption.IGNORE_CASE),
        Regex("Unable to match the desired swap behavior", RegexOption.IGNORE_CASE),
        Regex("queryAndSetGLESMaxVersion", RegexOption.IGNORE_CASE),
        Regex("avc: denied.*fastpipe", RegexOption.IGNORE_CASE),
    )

    fun sanitize(raw: String): String =
        raw.replace(bearerRegex, "$1[REDACTED]")
            .replace(secretParamRegex, "$1[REDACTED]")
            .replace(jwtRegex, "[REDACTED_JWT]")
            .replace(githubTokenRegex, "[REDACTED_GITHUB_TOKEN]")
            .replace(emailRegex, "[REDACTED_EMAIL]")
            .replace(ipv4Regex, "[REDACTED_IP]")
            .replace(macAddressRegex, "[REDACTED_MAC]")
            .replace(ipv6CandidateRegex) { match ->
                val candidate = match.value
                // Avoid treating ordinary HH:mm:ss log timestamps as IPv6 addresses.
                if (candidate.count { it == ':' } >= 3 || "::" in candidate || candidate.any { it.lowercaseChar() in 'a'..'f' }) {
                    "[REDACTED_IP]"
                } else {
                    candidate
                }
            }

    fun log(tag: String, message: String, level: String = "INFO") {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$stamp [$level/$tag] ${sanitize(message.trim())}"
        synchronized(entries) {
            if (entries.size >= MAX_ENTRIES) entries.removeFirst()
            entries.addLast(line)
        }
    }

    fun recentEntries(): List<String> = synchronized(entries) { entries.toList() }

    fun installCrashBreadcrumbHandler(context: Context) {
        val crashFile = crashBreadcrumbFile(context)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
                val recentEvents = recentEntries().takeLast(40).joinToString("\n") { sanitize(it) }.takeLast(1_800)
                val text = buildString {
                    appendLine("[$stamp] Uncaught exception on thread '${thread.name}':")
                    appendLine("=== RECENT LOCAL APP EVENTS ===")
                    appendLine(recentEvents)
                    appendLine("=== STACK TRACE ===")
                    append(sanitize(sw.toString()).takeLast(5_800))
                }.take(8_000)
                writeCrashBreadcrumbAtomically(crashFile, text)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun crashBreadcrumbFile(context: Context): File {
        val durableFile = File(File(context.filesDir, "diagnostics"), CRASH_FILE_NAME)
        val legacyCacheFile = File(File(context.cacheDir, "diagnostics"), CRASH_FILE_NAME)
        if (!durableFile.exists() && legacyCacheFile.isFile) {
            runCatching { writeCrashBreadcrumbAtomically(durableFile, legacyCacheFile.readText(Charsets.UTF_8).take(8_000)) }
        }
        return durableFile
    }

    suspend fun captureDiagnosticLogsAsync(context: Context, maxChars: Int = 32_000): String =
        withContext(Dispatchers.IO) { captureDiagnosticLogs(context, maxChars) }

    fun captureDiagnosticLogs(context: Context, maxChars: Int = 32_000): String {
        val deviceSection = buildString {
            appendLine("=== APP / DEVICE ===")
            appendLine("App: ${AppDetails.NAME} ${AppDetails.VERSION_NAME} (versionCode ${AppDetails.VERSION_CODE})")
            appendLine("Package/build: ${context.packageName} · ${BuildConfig.BUILD_TYPE}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} · brand=${Build.BRAND} · device=${Build.DEVICE}")
            appendLine("Android: ${Build.VERSION.RELEASE ?: "?"} · API ${Build.VERSION.SDK_INT} · securityPatch=${Build.VERSION.SECURITY_PATCH}")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}")
            appendLine("IP/MAC/serial/unique device identifiers: not collected in this section")
            appendLine()
        }

        val crashFile = crashBreadcrumbFile(context)
        val crashText = if (crashFile.isFile) {
            runCatching { crashFile.readText(Charsets.UTF_8).trim() }.getOrDefault("")
        } else ""
        val crashSection = crashText.takeIf(String::isNotEmpty)?.let {
            "=== LAST CRASH TRACE / LOCAL EVENTS ===\n${sanitize(it).takeLast(8_000)}\n\n"
        }.orEmpty()

        val ring = recentEntries()
        val ringHeader = "=== APP EVENTS / NETWORK / AUTH (${ring.size}) ===\n"
        val ringBody = ring.takeLast(700).joinToString("\n") { sanitize(it) }
        // Keep structured application events ahead of verbose logcat so auth and HTTP
        // details survive the final size limit even on very chatty devices.
        val ringBudget = (maxChars - deviceSection.length - crashSection.length - 2_000)
            .coerceAtLeast(1_000)
            .coerceAtMost(18_000)
        val ringSection = if (ring.isEmpty()) "" else ringHeader + ringBody.takeLast(ringBudget) + "\n\n"
        val essentials = deviceSection + crashSection + ringSection

        // Android does not grant apps access to other processes' logcat without privileged
        // permissions. Capture our process only; structured app events above carry auth details.
        val pid = Process.myPid()
        val pidLogResult = runCatching {
            val proc = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t", "1200", "--pid=$pid")
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            proc.waitFor()
            out
        }.fold(
            onSuccess = { it },
            onFailure = { "Logcat capture unavailable (${it.javaClass.simpleName}: ${sanitize(it.message.orEmpty()).take(160)})" },
        )
        val logLines = pidLogResult.lineSequence().filter(String::isNotBlank).toList()
        val (noiseLines, usefulLines) = logLines.partition { line ->
            lowSignalPlatformNoise.any { it.containsMatchIn(line) }
        }
        val noiseSection = if (noiseLines.isEmpty()) "" else
            "\n=== PLATFORM NOISE ===\n${noiseLines.size} avertissements graphiques/fastpipe à faible signal omis; ils ne sont pas des erreurs de l’application.\n"
        val logHeader = if (usefulLines.isEmpty()) "" else
            "\n=== ANDROID LOGCAT (APP PID $pid · ${usefulLines.size} lignes) ===\n"
        val logBody = usefulLines.takeLast(700).joinToString("\n") { sanitize(it) }
        val logBudget = (maxChars - essentials.length - noiseSection.length - logHeader.length).coerceAtLeast(0)
        val logcatSection = logHeader + logBody.takeLast(logBudget)

        val combined = sanitize(essentials + logcatSection + noiseSection).trim()
        return if (combined.length <= maxChars) combined else combined.takeLast(maxChars)
    }
}

/** Writes a small private crash breadcrumb durably and atomically; never performs network work. */
internal fun writeCrashBreadcrumbAtomically(target: File, text: String) {
    val parent = target.parentFile ?: throw IllegalArgumentException("Crash breadcrumb needs a parent directory")
    if (!parent.exists() && !parent.mkdirs()) throw java.io.IOException("Cannot create local diagnostics directory")
    val temporary = File(parent, "${target.name}.tmp")
    FileOutputStream(temporary).use { stream ->
        stream.write(text.toByteArray(Charsets.UTF_8))
        stream.fd.sync()
    }
    try {
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } catch (_: Exception) {
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
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
 * Handles local screenshot compression, privacy-safe device diagnostics, report-text creation,
 * and temporary sharing. User reports are submitted through the authenticated Atlas Worker API.
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
            appendLine("<summary>📋 Journaux Atlas, réseau & traces du processus (${trimmedLogs.lines().size} lignes)</summary>")
            appendLine()
            appendLine("```log")
            appendLine(trimmedLogs.ifBlank { "Aucune erreur récente enregistrée dans le tampon local." })
            appendLine("```")
            appendLine("</details>")
            appendLine()
            appendLine("@atlas-bot merci d'analyser ces journaux, d'identifier la cause racine, de proposer les solutions et de générer un prompt IA de correction.")
        }
    }

    /**
     * Builds the compact message accepted by the authenticated Worker report endpoint.
     * The Worker caps messages at 2,000 UTF-16 characters; include recent sanitized logs
     * after the human summary so the most useful failure context survives truncation.
     */
    fun buildUserReportMessage(
        kind: String,
        category: String,
        title: String,
        description: String,
        stepsToReproduce: String,
        snapshot: DeviceDiagnosticSnapshot,
        logs: String,
        maxChars: Int = 2_000,
    ): String {
        val summary = buildString {
            appendLine("Type: ${kind.uppercase(Locale.ROOT)} · ${category.take(80)}")
            appendLine("Titre: ${title.trim().ifBlank { category }.take(120)}")
            if (description.isNotBlank()) appendLine("Description: ${description.trim().take(420)}")
            if (stepsToReproduce.isNotBlank()) appendLine("Étapes: ${stepsToReproduce.trim().take(260)}")
            appendLine("Appareil: ${snapshot.manufacturerModel.take(90)} · ${snapshot.androidVersion}")
            appendLine("App: ${snapshot.appVersion} (${snapshot.versionCode}) · écran ${snapshot.screenOrigin.take(80)} · réseau ${snapshot.networkStatus.take(50)}")
            appendLine("Référence locale: ${snapshot.reportId} · ID appareil masqué: ${snapshot.maskedFingerprint}")
            appendLine("Journaux récents assainis:")
            appendLine("```log")
        }
        val suffix = "\n```\n@atlas-bot merci d'analyser ces journaux et de proposer la cause racine et les étapes de correction."
        if (summary.length + suffix.length >= maxChars) return (summary + suffix).take(maxChars)
        val safeLogs = AtlasLogBuffer.sanitize(logs).ifBlank { "Aucun journal récent." }
        val logBudget = maxChars - summary.length - suffix.length
        // Logcat often ends with IME/graphics noise; prioritize the recent app events
        // and retain crash context when it is present.
        val eventsMatch = Regex("""(?s)=== APP EVENTS / NETWORK / AUTH[^\n]*\n(.*?)(?=\n=== |$)""").find(safeLogs)
        val remainingBudget = logBudget
        val crashMatch = Regex("""(?s)=== LAST CRASH TRACE ===\s*(.*?)(?=\n=== |$)""").find(safeLogs)
        val remainingLogs = if (crashMatch == null) {
            (eventsMatch?.value ?: safeLogs).takeLast(remainingBudget)
        } else {
            // The captured crash block appears near the start of the full local diagnostics.
            // A plain takeLast(logBudget) silently discarded it from the short Worker payload.
            // Reserve space for the tail of the stack trace, then use the remainder for recent events.
            val crashLabel = "\n=== LAST CRASH TRACE ===\n"
            val eventsLabel = "\n=== RECENT DIAGNOSTICS ===\n"
            if (remainingBudget <= crashLabel.length + eventsLabel.length) {
                safeLogs.takeLast(remainingBudget)
            } else {
                val remainingForBodies = remainingBudget - crashLabel.length - eventsLabel.length
                val crashBudget = minOf(1_000, remainingForBodies * 2 / 3)
                val rawCrash = crashMatch.groupValues[1].trim()
                val truncationMarker = "\n… [stack trace excerpt] …\n"
                val crashExcerpt = when {
                    rawCrash.length <= crashBudget -> rawCrash
                    crashBudget <= truncationMarker.length -> rawCrash.take(crashBudget)
                    else -> {
                        val headBudget = minOf(320, (crashBudget - truncationMarker.length) / 3)
                        val tailBudget = crashBudget - headBudget - truncationMarker.length
                        rawCrash.take(headBudget) + truncationMarker + rawCrash.takeLast(tailBudget)
                    }
                }
                val withoutCrash = eventsMatch?.value ?: safeLogs.removeRange(crashMatch.range).trim()
                val eventsBudget = (remainingBudget - crashLabel.length - eventsLabel.length - crashExcerpt.length).coerceAtLeast(0)
                crashLabel + crashExcerpt + eventsLabel + withoutCrash.takeLast(eventsBudget)
            }
        }
        return summary + remainingLogs + suffix
    }

    fun copyFullReportToClipboard(context: Context, fullMarkdown: String): Boolean = runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (cm == null) {
            false
        } else {
            cm.setPrimaryClip(ClipData.newPlainText("Atlas Diagnostic Report", fullMarkdown))
            true
        }
    }.getOrDefault(false)

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
