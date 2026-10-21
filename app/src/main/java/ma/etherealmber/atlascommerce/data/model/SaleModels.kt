package ma.etherealmber.atlascommerce.data.model

data class SaleLineDto(
    val product_id: Long,
    val quantity: Double,
    val unit_price: Double? = null,
)

data class CreateSaleRequest(
    val lines: List<SaleLineDto>,
    val client_id: Long? = null,
    val payment_method: String? = null,
    val note: String? = null,
)

data class SaleResponse(
    val id: Long? = null,
    val number: String? = null,
    val total: Double? = null,
)
