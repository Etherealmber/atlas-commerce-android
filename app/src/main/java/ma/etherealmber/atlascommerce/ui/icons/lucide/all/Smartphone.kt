package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val smartphoneDefinition = LucideIconDefinition("Smartphone") { strokeWidth ->
    lucidePath(strokeWidth) {
        moveTo(18f, 2f)
        horizontalLineTo(6f)
        arcTo(2f, 2f, 0f, false, false, 4f, 4f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, false, 6f, 22f)
        horizontalLineTo(18f)
        arcTo(2f, 2f, 0f, false, false, 20f, 20f)
        verticalLineTo(4f)
        arcTo(2f, 2f, 0f, false, false, 18f, 2f)
        close()
        moveTo(12f, 18f)
        horizontalLineTo(12.01f)
    }
}

public val LucideIcons.All.SmartphoneDefinition: LucideIconDefinition
    get() = smartphoneDefinition
