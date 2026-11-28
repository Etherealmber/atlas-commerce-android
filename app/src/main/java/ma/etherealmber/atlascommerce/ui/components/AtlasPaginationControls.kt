package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

/** Compact numbered pager for fully loaded directory and catalog lists. */
@Composable
fun AtlasPaginationControls(
    totalItems: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pageSize: Int = 10,
) {
    if (totalItems <= pageSize || pageSize <= 0) return
    val pageCount = ((totalItems + pageSize - 1) / pageSize).coerceAtLeast(1)
    val page = currentPage.coerceIn(1, pageCount)
    val firstItem = (page - 1) * pageSize + 1
    val lastItem = (firstItem + pageSize - 1).coerceAtMost(totalItems)

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onPageSelected((page - 1).coerceAtLeast(1)) },
                    enabled = page > 1,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Page précédente", "Previous page", "الصفحة السابقة"))
                }
                pageSlots(page, pageCount).forEach { slot ->
                    if (slot == null) {
                        Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        TextButton(
                            onClick = { onPageSelected(slot) },
                            enabled = slot != page,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                slot.toString(),
                                fontWeight = if (slot == page) FontWeight.Bold else FontWeight.Medium,
                                color = if (slot == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { onPageSelected((page + 1).coerceAtMost(pageCount)) },
                    enabled = page < pageCount,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = atlasText("Page suivante", "Next page", "الصفحة التالية"))
                }
            }
        }
        Spacer(Modifier.width(1.dp))
        Text(
            atlasText("$firstItem–$lastItem sur $totalItems", "$firstItem–$lastItem of $totalItems", "$firstItem–$lastItem من $totalItems"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Visible page numbers with compact gaps, matching the web list pager. */
private fun pageSlots(current: Int, total: Int): List<Int?> {
    if (total <= 7) return (1..total).map { it }
    val pages = when {
        current <= 4 -> (1..5).toList() + total
        current >= total - 3 -> listOf(1) + (total - 4..total).toList()
        else -> listOf(1, current - 1, current, current + 1, total)
    }
    return buildList {
        pages.forEachIndexed { index, value ->
            if (index > 0 && value - pages[index - 1] > 1) add(null)
            add(value)
        }
    }
}
