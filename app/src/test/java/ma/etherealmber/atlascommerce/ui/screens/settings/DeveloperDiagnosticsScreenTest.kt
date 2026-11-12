package ma.etherealmber.atlascommerce.ui.screens.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperDiagnosticsScreenTest {
    @Test
    fun apiHealthProbeIsOnlyTriggeredByItsDedicatedAction() {
        val source = File("src/main/java/ma/etherealmber/atlascommerce/ui/screens/settings/DeveloperDiagnosticsScreen.kt")
            .takeIf { it.isFile }
            ?: File("app/src/main/java/ma/etherealmber/atlascommerce/ui/screens/settings/DeveloperDiagnosticsScreen.kt")
        assertTrue("Developer diagnostics screen source must exist", source.isFile)

        val apiProbeCallCount = Regex("appViewModel\\.runDeveloperApiProbe\\(\\)")
            .findAll(source.readText())
            .count()
        assertEquals("Only the dedicated Test API action should run the health probe", 1, apiProbeCallCount)
    }
}
