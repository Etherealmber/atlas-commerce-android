package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("ChevronRight") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveToRelative(9f, 18f)
                lineToRelative(6f, -6f)
                lineToRelative(-6f, -6f)
            }
}

public val LucideIcons.All.ChevronRightDefinition: LucideIconDefinition
    get() = _ActivityDefinition

