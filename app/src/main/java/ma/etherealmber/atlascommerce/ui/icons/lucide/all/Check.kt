package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("Check") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(20f, 6f)
                lineTo(9f, 17f)
                lineToRelative(-5f, -5f)
            }
}

public val LucideIcons.All.CheckDefinition: LucideIconDefinition
    get() = _ActivityDefinition

