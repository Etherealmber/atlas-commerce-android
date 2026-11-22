package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("Database") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(3f, 5f)
                verticalLineTo(19f)
                arcTo(9f, 3f, 0f, false, false, 21f, 19f)
                verticalLineTo(5f)
            }
            lucidePath(strokeWidth) {
                moveTo(3f, 12f)
                arcTo(9f, 3f, 0f, false, false, 21f, 12f)
            }
}

public val LucideIcons.All.DatabaseDefinition: LucideIconDefinition
    get() = _ActivityDefinition

