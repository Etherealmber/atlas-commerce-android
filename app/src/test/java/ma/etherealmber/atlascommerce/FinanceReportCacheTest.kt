package ma.etherealmber.atlascommerce

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import ma.etherealmber.atlascommerce.data.FinanceReportCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceReportCacheTest {
    @Test
    fun pruneExpiredRemovesOldPdfButLeavesRecentAndNonPdfFiles() {
        val root = Files.createTempDirectory("atlas-report-cache").toFile()
        try {
            val now = System.currentTimeMillis()
            val stale = File(root, "account/old.pdf").apply { parentFile?.mkdirs(); writeText("old") }
            val recent = File(root, "account/recent.pdf").apply { writeText("recent") }
            val unrelated = File(root, "account/notes.txt").apply { writeText("keep") }
            stale.setLastModified(now - TimeUnit.DAYS.toMillis(8))
            recent.setLastModified(now - TimeUnit.DAYS.toMillis(1))

            FinanceReportCache.pruneExpired(root, now)

            assertFalse(stale.exists())
            assertTrue(recent.exists())
            assertTrue(unrelated.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun pruneAccountKeepsOnlyNewestEightAndRemovesExpiredFiles() {
        val root = Files.createTempDirectory("atlas-report-account").toFile()
        try {
            val now = System.currentTimeMillis()
            val files = (0 until 9).map { index ->
                File(root, "report-$index.pdf").apply {
                    writeText("pdf")
                    setLastModified(now - TimeUnit.HOURS.toMillis(index.toLong()))
                }
            }

            FinanceReportCache.pruneAccount(root, now)

            assertFalse(files.last().exists())
            assertTrue(files.take(8).all { it.exists() })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun listAccountReturnsNewestPdfFilesAndIgnoresOtherFiles() {
        val root = Files.createTempDirectory("atlas-report-history").toFile()
        try {
            val older = File(root, "rapport-mensuel-2025-01-10.pdf").apply {
                writeText("pdf")
                setLastModified(10_000L)
            }
            val newer = File(root, "rapport-mensuel-2025-02-20.pdf").apply {
                writeText("pdf")
                setLastModified(20_000L)
            }
            File(root, "notes.txt").writeText("not a report")

            val history = FinanceReportCache.listAccount(root, 30_000L)

            assertEquals(listOf(newer, older), history)
        } finally {
            root.deleteRecursively()
        }
    }
}
