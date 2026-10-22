package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.ui.components.AtlasMarkdownParser
import ma.etherealmber.atlascommerce.ui.components.MarkdownBlock
import ma.etherealmber.atlascommerce.ui.screens.devices.QrDeviceViewModel
import ma.etherealmber.atlascommerce.ui.screens.scan.ScanViewModel
import ma.etherealmber.atlascommerce.ui.shell.MainTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasFormatTest {

    @Test
    fun `money always carries the currency code`() {
        val formatted = AtlasFormat.money(1250.5, "MAD")
        assertTrue(formatted.endsWith(" MAD"))
        assertTrue(formatted.contains("1"))
        assertTrue(formatted.contains("250"))
    }

    @Test
    fun `missing currency falls back to MAD`() {
        assertTrue(AtlasFormat.money(10.0, null).endsWith(" MAD"))
        assertTrue(AtlasFormat.money(10.0, "  ").endsWith(" MAD"))
    }

    @Test
    fun `whole quantities have no decimals`() {
        assertEquals("2", AtlasFormat.quantity(2.0))
        assertEquals("0", AtlasFormat.quantity(0.0))
    }

    @Test
    fun `fractional quantities keep two digits`() {
        assertEquals("1,5", AtlasFormat.quantity(1.5))
    }

    @Test
    fun `non finite amounts are displayed as zero`() {
        assertTrue(AtlasFormat.money(Double.NaN, "MAD").startsWith("0"))
    }

    @Test
    fun `dates accept ISO and plain dates`() {
        assertTrue(AtlasFormat.date("2026-10-21T10:30:00Z").contains("2026"))
        assertTrue(AtlasFormat.date("2026-10-21").contains("2026"))
        assertEquals("—", AtlasFormat.date(null))
    }
}

class MarkdownParserTest {

    @Test
    fun parsesAgentDailySalesTableAndActionListFromRealPrompt() {
        val sample = """
            Voici le résumé des ventes et impayés pour aujourd'hui, le 22 octobre 2026 :

            | Indicateur | Valeur | Commentaire |
            | :--- | :---: | ---: |
            | **Ventes totales** | 0,00 MAD | Aucune vente enregistrée aujourd'hui. |
            | **Impayés (clients)** | 2 459,64 MAD | 5 factures en attente de paiement, dont 3 en retard. |
            | **Achats impayés** | 12 000,00 MAD | 1 facture fournisseur à régler. |

            **À faire :**
            1. **Relancer les factures impayées** : Concentrez-vous sur les 3 factures en retard.
            2. **Suivi des achats** : Vérifiez l'échéance de la facture de 12 000,00 MAD.
        """.trimIndent()

        val blocks = AtlasMarkdownParser.parse(sample)
        assertEquals(4, blocks.size)

        assertTrue(blocks[0] is MarkdownBlock.Paragraph)

        val table = blocks[1] as MarkdownBlock.Table
        assertEquals(listOf("Indicateur", "Valeur", "Commentaire"), table.headers)
        assertEquals(
            listOf(
                MarkdownBlock.ColumnAlign.LEFT,
                MarkdownBlock.ColumnAlign.CENTER,
                MarkdownBlock.ColumnAlign.RIGHT,
            ),
            table.alignments,
        )
        assertEquals(3, table.rows.size)
        assertEquals("**Ventes totales**", table.rows[0][0])
        assertEquals("0,00 MAD", table.rows[0][1])

        assertTrue(blocks[2] is MarkdownBlock.Paragraph)

        val list = blocks[3] as MarkdownBlock.NumberedList
        assertEquals(2, list.items.size)
        assertEquals(1, list.items[0].number)
        assertTrue(list.items[0].text.contains("**Relancer les factures impayées**"))
    }

    @Test
    fun parsesHeadingsCodeBlocksBlockquotesAndHorizontalDividers() {
        val markdown = """
            ## Rapport Stock
            > Alerte réapprovisionnement prioritaire

            ---

            ```sql
            SELECT sku, stock_quantity FROM products WHERE stock_quantity <= 5;
            ```
        """.trimIndent()

        val blocks = AtlasMarkdownParser.parse(markdown)
        assertEquals(4, blocks.size)

        val heading = blocks[0] as MarkdownBlock.Heading
        assertEquals(2, heading.level)
        assertEquals("Rapport Stock", heading.text)

        val quote = blocks[1] as MarkdownBlock.Blockquote
        assertTrue(quote.text.contains("Alerte"))

        assertEquals(MarkdownBlock.Divider, blocks[2])

        val code = blocks[3] as MarkdownBlock.CodeBlock
        assertEquals("sql", code.language)
        assertTrue(code.code.contains("SELECT sku"))
    }

    @Test
    fun bottomBarHidesSettingsAndCamerasDefaultToClosed() {
        assertFalse(
            "Settings must not appear in BottomBarTabs",
            MainTab.BottomBarTabs.contains(MainTab.SETTINGS),
        )
        assertEquals(
            listOf(MainTab.HOME, MainTab.PRODUCTS, MainTab.SCAN, MainTab.SALE, MainTab.AGENT),
            MainTab.BottomBarTabs,
        )
        assertFalse(
            "ScanViewModel camera must stay closed by default",
            ScanViewModel.UiState().cameraEnabled,
        )
        assertFalse(
            "QrDeviceViewModel camera must stay closed by default",
            QrDeviceViewModel.UiState().cameraEnabled,
        )
    }
}
