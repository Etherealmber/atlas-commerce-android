package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("UserRound") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(7.0f, 8.0f)
                arcToRelative(5.0f, 5.0f, 0f, true, false, 10.0f, 0f)
                arcToRelative(5.0f, 5.0f, 0f, true, false, -10.0f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(20f, 21f)
                arcToRelative(8f, 8f, 0f, false, false, -16f, 0f)
            }
}

public val LucideIcons.All.UserRoundDefinition: LucideIconDefinition
    get() = _ActivityDefinition

