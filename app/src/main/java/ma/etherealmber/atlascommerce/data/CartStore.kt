package ma.etherealmber.atlascommerce.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ma.etherealmber.atlascommerce.data.model.ProductDto
import javax.inject.Inject
import javax.inject.Singleton

data class CartLine(
    val product: ProductDto,
    val quantity: Double = 1.0,
) {
    val lineTotal: Double get() = product.price * quantity
}

/**
 * In-memory cart for the quick-sale (ticket VR) flow.
 *
 * Deliberately not persisted: a cart is a single counter interaction, and
 * keeping it out of storage avoids leaking half-finished sales across accounts.
 * It is cleared on logout.
 */
@Singleton
class CartStore @Inject constructor() {

    private val _lines = MutableStateFlow<List<CartLine>>(emptyList())
    val lines: StateFlow<List<CartLine>> = _lines.asStateFlow()

    fun add(product: ProductDto, quantity: Double = 1.0) {
        if (quantity <= 0.0) return
        _lines.update { current ->
            val index = current.indexOfFirst { it.product.id == product.id }
            if (index >= 0) {
                current.toMutableList().also {
                    it[index] = it[index].copy(quantity = it[index].quantity + quantity)
                }
            } else {
                current + CartLine(product, quantity)
            }
        }
    }

    fun setQuantity(productId: Long, quantity: Double) {
        _lines.update { current ->
            current.mapNotNull { line ->
                when {
                    line.product.id != productId -> line
                    quantity <= 0.0 -> null
                    else -> line.copy(quantity = quantity)
                }
            }
        }
    }

    fun increment(productId: Long, step: Double = 1.0) {
        _lines.update { current ->
            current.map { line ->
                if (line.product.id == productId) line.copy(quantity = line.quantity + step) else line
            }
        }
    }

    fun decrement(productId: Long, step: Double = 1.0) {
        _lines.update { current ->
            current.mapNotNull { line ->
                when {
                    line.product.id != productId -> line
                    line.quantity - step <= 0.0 -> null
                    else -> line.copy(quantity = line.quantity - step)
                }
            }
        }
    }

    fun remove(productId: Long) {
        _lines.update { current -> current.filterNot { it.product.id == productId } }
    }

    fun clear() {
        _lines.value = emptyList()
    }

    fun snapshot(): List<CartLine> = _lines.value

    fun total(): Double = _lines.value.sumOf { it.lineTotal }

    fun quantityOf(productId: Long): Double =
        _lines.value.firstOrNull { it.product.id == productId }?.quantity ?: 0.0
}
