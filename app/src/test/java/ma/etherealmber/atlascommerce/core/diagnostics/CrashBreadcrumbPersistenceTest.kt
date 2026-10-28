package ma.etherealmber.atlascommerce.core.diagnostics

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashBreadcrumbPersistenceTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun breadcrumbIsReplacedAtomicallyAndStaysLocal() {
        val privateDir = File(temporaryFolder.root, "internal/diagnostics")
        val breadcrumb = File(privateDir, "last_crash.txt")
        writeCrashBreadcrumbAtomically(breadcrumb, "first crash")
        writeCrashBreadcrumbAtomically(breadcrumb, "second crash")

        assertEquals("second crash", breadcrumb.readText())
        assertFalse(File(privateDir, "last_crash.txt.tmp").exists())
    }
}
