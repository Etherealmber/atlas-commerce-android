package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

/** Compact, touch-friendly local pager for fully loaded directory and catalog lists. */
@Composable
fun AtlasPaginationControls(
    totalItems: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pageSize: Int = 10,
) {
    if (totalItems <= pageSize) return
    val pageCount = ((totalItems + pageSize - 1) / pageSize).coerceAtLeast(1)
    val page = currentPage.coerceIn(1, pageCount)
    val firstItem = (page - 1) * pageSize + 1
    val lastItem = (firstItem + pageSize - 1).coerceAtMost(totalItems)
    var menuExpanded by remember(pageCount) { mutableStateOf(false) }

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
                androidx.compose.foundation.layout.Box {
                    androidx.compose.material3.TextButton(onClick = { menuExpanded = true }) {
                        Text(
                            atlasText("Page $page sur $pageCount", "Page $page of $pageCount", "الصفحة $page من $pageCount"),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        (1..pageCount).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(atlasText("Page $option", "Page $option", "الصفحة $option")) },
                                onClick = {
                                    menuExpanded = false
                                    onPageSelected(option)
                                },
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
