package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("X") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
            lucidePath(strokeWidth) {
                moveToRelative(6f, 6f)
                lineToRelative(12f, 12f)
            }
}

public val LucideIcons.All.XDefinition: LucideIconDefinition
    get() = _ActivityDefinition

