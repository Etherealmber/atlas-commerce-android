package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("ArrowDownLeft") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(17f, 7f)
                lineTo(7f, 17f)
            }
            lucidePath(strokeWidth) {
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                verticalLineTo(7f)
            }
}

public val LucideIcons.All.ArrowDownLeftDefinition: LucideIconDefinition
    get() = _ActivityDefinition

