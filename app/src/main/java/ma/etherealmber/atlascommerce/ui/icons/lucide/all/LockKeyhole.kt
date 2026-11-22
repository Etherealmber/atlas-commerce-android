package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("LockKeyhole") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(11.0f, 16.0f)
                arcToRelative(1.0f, 1.0f, 0f, true, false, 2.0f, 0f)
                arcToRelative(1.0f, 1.0f, 0f, true, false, -2.0f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(5.0f, 10.0f)
                horizontalLineToRelative(14.0f)
                arcToRelative(2.0f, 0.0f, 0f, false, true, 2.0f, 0.0f)
                verticalLineToRelative(12.0f)
                arcToRelative(2.0f, 0.0f, 0f, false, true, -2.0f, 0.0f)
                horizontalLineToRelative(-14.0f)
                arcToRelative(2.0f, 0.0f, 0f, false, true, -2.0f, -0.0f)
                verticalLineToRelative(-12.0f)
                arcToRelative(2.0f, 0.0f, 0f, false, true, 2.0f, -0.0f)
                close()
            }
            lucidePath(strokeWidth) {
                moveTo(7f, 10f)
                verticalLineTo(7f)
                arcToRelative(5f, 5f, 0f, false, true, 10f, 0f)
                verticalLineToRelative(3f)
            }
}

public val LucideIcons.All.LockKeyholeDefinition: LucideIconDefinition
    get() = _ActivityDefinition

