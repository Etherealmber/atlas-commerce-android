package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoneyCompact

/** A labeled amount used by the shared dashboard-style finance chart. */
data class AtlasFinanceBarEntry(
    val label: String,
    val amount: Double,
    val color: Color,
)

/** Compact, accessible amount comparison used by Home, employee payroll and client receivables. */
@Composable
fun AtlasFinanceBarChart(
    title: String,
    entries: List<AtlasFinanceBarEntry>,
    currency: String,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    val peak = entries.maxOfOrNull { entry ->
        entry.amount.takeIf(Double::isFinite)?.let { amount -> kotlin.math.abs(amount) } ?: 0.0
    }?.takeIf { it > 0.0 } ?: 1.0
    AtlasCard(modifier = modifier, padding = 12.dp) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        entries.forEach { entry ->
            val safeAmount = entry.amount.takeIf(Double::isFinite) ?: 0.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = entry.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = atlasMoneyCompact(safeAmount, currency),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((kotlin.math.abs(safeAmount) / peak).toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(entry.color),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
