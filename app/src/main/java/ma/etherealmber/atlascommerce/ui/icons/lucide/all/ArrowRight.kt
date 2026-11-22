package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("ArrowRight") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(5f, 12f)
                horizontalLineToRelative(14f)
            }
            lucidePath(strokeWidth) {
                moveToRelative(12f, 5f)
                lineToRelative(7f, 7f)
                lineToRelative(-7f, 7f)
            }
}

public val LucideIcons.All.ArrowRightDefinition: LucideIconDefinition
    get() = _ActivityDefinition

