package ma.etherealmber.atlascommerce.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductRequestWireContractTest {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Test
    fun nonAdminActionCarriesOnlySessionAttributedMinimalProductBody() {
        val action = CreateMobileActionRequest(
            type = "new_product",
            title = "Nouveau produit à créer : Marbre",
            meta = MobileActionMetaDto(
                targetRole = "admin",
                category = "product_creation_request",
                createProductBody = ProductRequestBody(
                    name = "Marbre",
                    sku = "MAR-1",
                    unit = "m²",
                    stockQuantity = 4.0,
                ),
            ),
        )
        val json = JSONObject(moshi.adapter(CreateMobileActionRequest::class.java).toJson(action))
        val body = json.getJSONObject("meta").getJSONObject("create_product_body")

        assertEquals(
            setOf("name", "sku", "unit", "stock_quantity"),
            body.keys().asSequence().toSet(),
        )
        assertFalse(body.has("price"))
        assertFalse(body.has("barcode"))
        assertFalse(body.has("created_by"))
        assertFalse(json.has("created_by"))
        assertFalse(json.has("barcode"))
    }

    @Test
    fun nonAdminActionIncludesImageDataUrlOnlyWhenPresent() {
        val json = JSONObject(
            moshi.adapter(ProductRequestBody::class.java).toJson(
                ProductRequestBody("Marbre", "MAR-1", "m²", 4.0, "data:image/png;base64,abc"),
            ),
        )
        assertEquals("data:image/png;base64,abc", json.getString("image_data_url"))
        assertEquals(
            setOf("name", "sku", "unit", "stock_quantity", "image_data_url"),
            json.keys().asSequence().toSet(),
        )
    }

    @Test
    fun directCreationSerializesExplicitVatPriceMode() {
        val adapter = moshi.adapter(CreateProductRequest::class.java)
        val exclusive = JSONObject(adapter.toJson(CreateProductRequest("Table", "T-1", price = 100.0)))
        val inclusive = JSONObject(adapter.toJson(CreateProductRequest("Table", "T-1", price = 120.0, invoicePriceMode = "inclusive")))

        assertEquals("exclusive", exclusive.getString("invoice_price_mode"))
        assertEquals("inclusive", inclusive.getString("invoice_price_mode"))
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
