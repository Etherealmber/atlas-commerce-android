package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("Eye") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(2.062f, 12.348f)
                arcToRelative(1f, 1f, 0f, false, true, 0f, -.696f)
                arcToRelative(10.75f, 10.75f, 0f, false, true, 19.876f, 0f)
                arcToRelative(1f, 1f, 0f, false, true, 0f, .696f)
                arcToRelative(10.75f, 10.75f, 0f, false, true, -19.876f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(9.0f, 12.0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, 6.0f, 0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, -6.0f, 0f)
            }
}

public val LucideIcons.All.EyeDefinition: LucideIconDefinition
    get() = _ActivityDefinition

