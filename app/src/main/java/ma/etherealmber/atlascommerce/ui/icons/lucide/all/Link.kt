package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("Link") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(10f, 13f)
                arcToRelative(5f, 5f, 0f, false, false, 7.54f, .54f)
                lineToRelative(3f, -3f)
                arcToRelative(5f, 5f, 0f, false, false, -7.07f, -7.07f)
                lineToRelative(-1.72f, 1.71f)
            }
            lucidePath(strokeWidth) {
                moveTo(14f, 11f)
                arcToRelative(5f, 5f, 0f, false, false, -7.54f, -.54f)
                lineToRelative(-3f, 3f)
                arcToRelative(5f, 5f, 0f, false, false, 7.07f, 7.07f)
                lineToRelative(1.71f, -1.71f)
            }
}

public val LucideIcons.All.LinkDefinition: LucideIconDefinition
    get() = _ActivityDefinition

