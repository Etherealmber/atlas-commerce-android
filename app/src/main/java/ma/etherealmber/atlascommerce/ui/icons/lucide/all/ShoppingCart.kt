package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("ShoppingCart") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(7.0f, 21.0f)
                arcToRelative(1.0f, 1.0f, 0f, true, false, 2.0f, 0f)
                arcToRelative(1.0f, 1.0f, 0f, true, false, -2.0f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(18.0f, 21.0f)
                arcToRelative(1.0f, 1.0f, 0f, true, false, 2.0f, 0f)
                arcToRelative(1.0f, 1.0f, 0f, true, false, -2.0f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(2.05f, 2.05f)
                horizontalLineToRelative(2f)
                lineToRelative(2.66f, 12.42f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 1.58f)
                horizontalLineToRelative(9.78f)
                arcToRelative(2f, 2f, 0f, false, false, 1.95f, -1.57f)
                lineToRelative(1.65f, -7.43f)
                horizontalLineTo(5.12f)
            }
}

public val LucideIcons.All.ShoppingCartDefinition: LucideIconDefinition
    get() = _ActivityDefinition

