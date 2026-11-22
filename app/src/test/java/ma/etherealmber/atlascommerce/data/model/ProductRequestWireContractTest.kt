package ma.etherealmber.atlascommerce.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductRequestWireContractTest {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val jsonMapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )

    private fun parseMap(json: String): Map<String, Any?> = requireNotNull(jsonMapAdapter.fromJson(json))

    @Test
    fun nonAdminActionCarriesOnlySessionAttributedMinimalProductBody() {
        val action = CreateMobileActionRequest(
            type = "new_product",
            title = "Nouveau produit à créer : Marbre",
            meta = MobileActionMetaDto(
                targetRole = "admin",
                needsApproval = true,
                category = "product_creation_request",
                createProductBody = ProductRequestBody(
                    name = "Marbre",
                    sku = "MAR-1",
                    unit = "carton",
                    secondaryUnit = "m²",
                    secondaryUnitFactor = 1.59,
                    stockQuantity = 4.0,
                ),
            ),
        )
        val json = parseMap(moshi.adapter(CreateMobileActionRequest::class.java).toJson(action))
        val meta = json["meta"] as Map<*, *>
        val body = meta["create_product_body"] as Map<*, *>

        assertEquals(
            setOf("name", "sku", "unit", "secondary_unit", "secondary_unit_factor", "stock_quantity"),
            body.keys,
        )
        assertFalse(body.containsKey("price"))
        assertFalse(body.containsKey("barcode"))
        assertFalse(body.containsKey("created_by"))
        assertFalse(json.containsKey("created_by"))
        assertFalse(json.containsKey("barcode"))
    }

    @Test
    fun nonAdminActionIncludesImageDataUrlOnlyWhenPresent() {
        val json = parseMap(
            moshi.adapter(ProductRequestBody::class.java).toJson(
                ProductRequestBody(
                    name = "Marbre",
                    sku = "MAR-1",
                    unit = "carton",
                    secondaryUnit = "m²",
                    secondaryUnitFactor = 1.59,
                    stockQuantity = 4.0,
                    imageDataUrl = "data:image/png;base64,abc",
                ),
            ),
        )
        assertEquals("data:image/png;base64,abc", json["image_data_url"])
        assertEquals(
            setOf("name", "sku", "unit", "secondary_unit", "secondary_unit_factor", "stock_quantity", "image_data_url"),
            json.keys,
        )
    }

    @Test
    fun directCreationSerializesExplicitVatPriceMode() {
        val adapter = moshi.adapter(CreateProductRequest::class.java)
        val exclusive = parseMap(adapter.toJson(CreateProductRequest("Table", "T-1", price = 100.0)))
        val inclusive = parseMap(adapter.toJson(CreateProductRequest("Table", "T-1", price = 120.0, invoicePriceMode = "inclusive")))

        assertEquals("exclusive", exclusive["invoice_price_mode"])
        assertEquals("inclusive", inclusive["invoice_price_mode"])
    }

    @Test
    fun productDetailAndCatalogPayloadsReadSecondaryUnits() {
        val payload = """{"id":7,"name":"Tile","unit":"carton","secondary_unit":"m²","secondary_unit_factor":1.59,"stock_quantity":6,"price":20}"""
        val product = requireNotNull(moshi.adapter(ProductDto::class.java).fromJson(payload))
        assertEquals("carton", product.unit)
        assertEquals("m²", product.secondaryUnit)
        assertEquals(1.59, product.secondaryUnitFactor!!, 0.0)
        assertEquals(6.0, product.stock, 0.0)
    }

    @Test
    fun unitCatalogMergesServerOptionsWithTheFullFallbackSet() {
        val units = ProductUnitDefaults.merge(
            listOf(
                ProductUnitDto(id = 5, name = "m²"),
                ProductUnitDto(id = null, name = "yard"),
                ProductUnitDto(id = 6, name = "YARD"),
            ),
        )
        assertEquals("m²", units.first())
        assertEquals("yard", units[1])
        assertEquals(1, units.count { it.equals("yard", ignoreCase = true) })
        assertTrue(units.containsAll(listOf("unité", "pièce", "g", "mL", "cm", "m³", "boîte", "carton", "paquet", "lot", "sac", "bouteille", "pack", "douzaine", "heure", "jour", "forfait", "service")))
    }

    @Test
    fun productEditSerializesEmptyDualUnitsToClearExistingValues() {
        val body = CreateProductRequest("Tile", "T-1").toUpdateProductRequest()
        val json = parseMap(moshi.adapter(UpdateProductRequest::class.java).toJson(body))
        assertEquals("", json["secondary_unit"])
        assertEquals("", json["secondary_unit_factor"])
    }

    @Test
    fun productEditKeepsPositiveFactorNumeric() {
        val body = CreateProductRequest(
            name = "Tile",
            sku = "T-1",
            unit = "carton",
            secondaryUnit = "m²",
            secondaryUnitFactor = 1.59,
        ).toUpdateProductRequest()
        val json = parseMap(moshi.adapter(UpdateProductRequest::class.java).toJson(body))
        assertEquals("m²", json["secondary_unit"])
        assertEquals(1.59, json["secondary_unit_factor"])
    }

    @Test
    fun directCreationSerializesSecondaryUnitsAlongsidePriceMode() {
        val json = parseMap(
            moshi.adapter(CreateProductRequest::class.java).toJson(
                CreateProductRequest(
                    name = "Tile",
                    sku = "T-1",
                    unit = "carton",
                    secondaryUnit = "m²",
                    secondaryUnitFactor = 1.59,
                    price = 20.0,
                    invoicePriceMode = "inclusive",
                ),
            ),
        )
        assertEquals("carton", json["unit"])
        assertEquals("m²", json["secondary_unit"])
        assertEquals(1.59, json["secondary_unit_factor"])
        assertEquals("inclusive", json["invoice_price_mode"])
    }

    @Test
    fun quickSaleRemainsVatFree() {
        val quickSale = QuickSaleRequest(
            clientName = "Comptoir",
            issueDate = "2026-10-21",
            items = listOf(QuickSaleItemDto(1, "Table", 1.0, 100.0)),
        )
        assertTrue(quickSale.taxExempt)
        assertEquals(0.0, quickSale.items.single().taxRate, 0.0)
    }
}
