package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("Plus") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(5f, 12f)
                horizontalLineToRelative(14f)
            }
            lucidePath(strokeWidth) {
                moveTo(12f, 5f)
                verticalLineToRelative(14f)
            }
}

public val LucideIcons.All.PlusDefinition: LucideIconDefinition
    get() = _ActivityDefinition

