package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("Share2") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(15.0f, 5.0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, 6.0f, 0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, -6.0f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(3.0f, 12.0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, 6.0f, 0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, -6.0f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(15.0f, 19.0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, 6.0f, 0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, -6.0f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(8.59f, 13.51f)
                lineTo(15.42f, 17.49f)
            }
            lucidePath(strokeWidth) {
                moveTo(15.41f, 6.51f)
                lineTo(8.59f, 10.49f)
            }
}

public val LucideIcons.All.Share2Definition: LucideIconDefinition
    get() = _ActivityDefinition

