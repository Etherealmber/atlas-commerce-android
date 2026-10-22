package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.ui.components.AtlasMarkdownParser
import ma.etherealmber.atlascommerce.ui.components.MdBlock
import ma.etherealmber.atlascommerce.ui.screens.devices.QrDeviceViewModel
import ma.etherealmber.atlascommerce.ui.screens.scan.ScanViewModel
import ma.etherealmber.atlascommerce.ui.shell.MainTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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

        assertTrue(blocks[0] is MdBlock.Paragraph)

        val table = blocks[1] as MdBlock.Table
        assertEquals(listOf("Indicateur", "Valeur", "Commentaire"), table.headers)
        assertEquals(
            listOf(
                MdBlock.ColumnAlign.START,
                MdBlock.ColumnAlign.CENTER,
                MdBlock.ColumnAlign.END,
            ),
            table.alignments,
        )
        assertEquals(3, table.rows.size)
        assertEquals("**Ventes totales**", table.rows[0][0])
        assertEquals("0,00 MAD", table.rows[0][1])

        assertTrue(blocks[2] is MdBlock.Paragraph)

        val list = blocks[3] as MdBlock.ListItems
        assertEquals(2, list.items.size)
        assertEquals("1.", list.items[0].bullet)
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

        val heading = blocks[0] as MdBlock.Heading
        assertEquals(2, heading.level)
        assertEquals("Rapport Stock", heading.text)

        val quote = blocks[1] as MdBlock.Blockquote
        assertTrue(quote.text.contains("Alerte"))

        assertEquals(MdBlock.Divider, blocks[2])

        val code = blocks[3] as MdBlock.CodeBlock
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
