package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("Settings") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(9.671f, 4.136f)
                arcToRelative(2.34f, 2.34f, 0f, false, true, 4.659f, 0f)
                arcToRelative(2.34f, 2.34f, 0f, false, false, 3.319f, 1.915f)
                arcToRelative(2.34f, 2.34f, 0f, false, true, 2.33f, 4.033f)
                arcToRelative(2.34f, 2.34f, 0f, false, false, 0f, 3.831f)
                arcToRelative(2.34f, 2.34f, 0f, false, true, -2.33f, 4.033f)
                arcToRelative(2.34f, 2.34f, 0f, false, false, -3.319f, 1.915f)
                arcToRelative(2.34f, 2.34f, 0f, false, true, -4.659f, 0f)
                arcToRelative(2.34f, 2.34f, 0f, false, false, -3.32f, -1.915f)
                arcToRelative(2.34f, 2.34f, 0f, false, true, -2.33f, -4.033f)
                arcToRelative(2.34f, 2.34f, 0f, false, false, 0f, -3.831f)
                arcTo(2.34f, 2.34f, 0f, false, true, 6.35f, 6.051f)
                arcToRelative(2.34f, 2.34f, 0f, false, false, 3.319f, -1.915f)
            }
            lucidePath(strokeWidth) {
                moveTo(9.0f, 12.0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, 6.0f, 0f)
                arcToRelative(3.0f, 3.0f, 0f, true, false, -6.0f, 0f)
            }
}

public val LucideIcons.All.SettingsDefinition: LucideIconDefinition
    get() = _ActivityDefinition

