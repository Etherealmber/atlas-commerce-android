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
                    unit = "m²",
                    stockQuantity = 4.0,
                ),
            ),
        )
        val json = parseMap(moshi.adapter(CreateMobileActionRequest::class.java).toJson(action))
        val meta = json["meta"] as Map<*, *>
        val body = meta["create_product_body"] as Map<*, *>

        assertEquals(
            setOf("name", "sku", "unit", "stock_quantity"),
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
                ProductRequestBody("Marbre", "MAR-1", "m²", 4.0, "data:image/png;base64,abc"),
            ),
        )
        assertEquals("data:image/png;base64,abc", json["image_data_url"])
        assertEquals(
            setOf("name", "sku", "unit", "stock_quantity", "image_data_url"),
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
