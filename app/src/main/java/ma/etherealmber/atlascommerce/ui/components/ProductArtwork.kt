package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

/**
 * Product artwork is loaded only while a lazy card/detail page is composed and reuses Atlas's
 * persistent stale-while-revalidate bitmap cache. Products without a photo retain the inventory glyph.
 */
@Composable
internal fun ProductArtwork(
    imageUrl: String?,
    serverBaseUrl: String?,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cleanUrl = imageUrl?.trim().orEmpty()
    val cacheKey = remember(cleanUrl, serverBaseUrl) { "${serverBaseUrl.orEmpty()}|$cleanUrl" }
    var bitmap by remember(cacheKey) { mutableStateOf(AvatarBitmapCache.get(cacheKey)) }

    LaunchedEffect(context, cleanUrl, serverBaseUrl) {
        if (cleanUrl.isNotEmpty()) {
            val cached = AvatarBitmapCache.load(context, cleanUrl, serverBaseUrl)
            bitmap = cached
            if (cached != null && AvatarBitmapCache.shouldRefresh(context, cleanUrl, serverBaseUrl)) {
                AvatarBitmapCache.refresh(context, cleanUrl, serverBaseUrl)?.let { updated -> bitmap = updated }
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val loaded = bitmap
            if (loaded != null) {
                Image(
                    bitmap = loaded,
                    contentDescription = atlasText(
                        "Photo de $label",
                        "Photo of $label",
                        "صورة $label",
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxSize(0.48f),
                )
            }
        }
    }
}
