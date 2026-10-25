package ma.etherealmber.atlascommerce.data

import java.io.File
import java.util.concurrent.TimeUnit

/** Private, short-lived cache for finance PDFs (never a public Downloads path). */
object FinanceReportCache {
    private val retentionMs = TimeUnit.DAYS.toMillis(7)
    private const val MAX_REPORTS_PER_ACCOUNT = 8

    fun pruneExpired(root: File, nowMs: Long = System.currentTimeMillis()) {
        if (!root.exists()) return
        root.walkTopDown()
            .filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
            .forEach { file ->
                if (nowMs - file.lastModified() > retentionMs) runCatching { file.delete() }
            }
        root.walkBottomUp()
            .filter { it.isDirectory && it != root }
            .forEach { directory ->
                if (directory.listFiles().isNullOrEmpty()) runCatching { directory.delete() }
            }
    }

    /** Keeps the newest few reports only within the active account's private folder. */
    fun pruneAccount(directory: File, nowMs: Long = System.currentTimeMillis()) {
        if (!directory.exists()) return
        val reports = directory.listFiles()
            ?.filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        reports.forEachIndexed { index, file ->
            val expired = nowMs - file.lastModified() > retentionMs
            if (expired || index >= MAX_REPORTS_PER_ACCOUNT) runCatching { file.delete() }
        }
    }
}
