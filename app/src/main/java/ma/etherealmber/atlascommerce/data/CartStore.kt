package ma.etherealmber.atlascommerce.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ma.etherealmber.atlascommerce.data.model.Product
import javax.inject.Inject
import javax.inject.Singleton

data class CartLine(
    val product: Product,
    val quantity: Double = 1.0,
)

@Singleton
class CartStore @Inject constructor() {
    private val _lines = MutableStateFlow<List<CartLine>>(emptyList())
    val lines: StateFlow<List<CartLine>> = _lines.asStateFlow()

    fun add(product: Product, qty: Double = 1.0) {
        _lines.update { list ->
            val i = list.indexOfFirst { it.product.id == product.id }
            if (i >= 0) {
                list.toMutableList().also {
                    it[i] = it[i].copy(quantity = it[i].quantity + qty)
                }
            } else {
                list + CartLine(product, qty)
            }
        }
    }

    fun setQty(productId: Long, qty: Double) {
        _lines.update { list ->
            list.mapNotNull { line ->
                when {
                    line.product.id != productId -> line
                    qty <= 0 -> null
                    else -> line.copy(quantity = qty)
                }
            }
        }
    }

    fun remove(productId: Long) {
        _lines.update { list -> list.filter { it.product.id != productId } }
    }

    fun clear() {
        _lines.value = emptyList()
    }

    fun snapshot(): List<CartLine> = _lines.value
}
