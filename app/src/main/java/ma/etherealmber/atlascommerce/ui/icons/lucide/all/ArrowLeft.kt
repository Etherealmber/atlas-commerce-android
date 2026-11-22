package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("ArrowLeft") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveToRelative(12f, 19f)
                lineToRelative(-7f, -7f)
                lineToRelative(7f, -7f)
            }
            lucidePath(strokeWidth) {
                moveTo(19f, 12f)
                horizontalLineTo(5f)
            }
}

public val LucideIcons.All.ArrowLeftDefinition: LucideIconDefinition
    get() = _ActivityDefinition

