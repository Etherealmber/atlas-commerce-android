package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("Search") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveToRelative(21f, 21f)
                lineToRelative(-4.34f, -4.34f)
            }
            lucidePath(strokeWidth) {
                moveTo(3.0f, 11.0f)
                arcToRelative(8.0f, 8.0f, 0f, true, false, 16.0f, 0f)
                arcToRelative(8.0f, 8.0f, 0f, true, false, -16.0f, 0f)
            }
}

public val LucideIcons.All.SearchDefinition: LucideIconDefinition
    get() = _ActivityDefinition

