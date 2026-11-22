package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("ChevronUp") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveToRelative(18f, 15f)
                lineToRelative(-6f, -6f)
                lineToRelative(-6f, 6f)
            }
}

public val LucideIcons.All.ChevronUpDefinition: LucideIconDefinition
    get() = _ActivityDefinition

