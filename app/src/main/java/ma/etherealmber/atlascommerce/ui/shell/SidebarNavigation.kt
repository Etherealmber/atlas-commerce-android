package ma.etherealmber.atlascommerce.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasMark
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.i18n.atlasRoleLabel
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AtlasSidebarContent(
    user: SessionUser,
    serverBaseUrl: String?,
    roleTabs: List<MainTab>,
    selectedTab: MainTab,
    cartCount: Int,
    notificationCount: Int,
    updateState: UpdateState,
    expanded: Boolean,
    onOpenDataStorageManager: () -> Unit,
    dataStorageSelected: Boolean,
    canCollapse: Boolean,
    onToggleCollapse: () -> Unit,
    onNavigate: (MainTab) -> Unit,
    onOpenNotifications: () -> Unit,
    onRequestSignOut: () -> Unit,
    onAvatarSurprise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logoDescription = when {
        !canCollapse -> atlasText("Logo Atlas Commerce", "Atlas Commerce logo", "شعار أطلس كوميرس")
        expanded -> atlasText(
            "Logo Atlas Commerce. Appuyez deux fois pour réduire le menu",
            "Atlas Commerce logo. Double-tap to collapse navigation",
            "شعار أطلس كوميرس. انقر مرتين لتصغير القائمة",
        )
        else -> atlasText(
            "Logo Atlas Commerce. Appuyez deux fois pour développer le menu",
            "Atlas Commerce logo. Double-tap to expand navigation",
            "شعار أطلس كوميرس. انقر مرتين لتوسيع القائمة",
        )
    }
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = if (expanded) 12.dp else 8.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AtlasMark(
                    modifier = if (canCollapse) {
                        Modifier
                            .combinedClickable(onClick = {}, onDoubleClick = onToggleCollapse)
                            .semantics { contentDescription = logoDescription }
                    } else {
                        Modifier
                    },
                    size = 42.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    "Atlas Commerce",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                contentAlignment = Alignment.Center,
            ) {
                AtlasMark(
                    modifier = if (canCollapse) {
                        Modifier
                            .combinedClickable(onClick = {}, onDoubleClick = onToggleCollapse)
                            .semantics { contentDescription = logoDescription }
                    } else {
                        Modifier
                    },
                    size = 32.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            roleTabs.forEach { item ->
                SidebarNavigationItem(
                    icon = item.icon,
                    label = item.localizedLabel(),
                    isSelected = selectedTab == item && !dataStorageSelected,
                    expanded = expanded,
                    badgeCount = if (item == MainTab.SALE) cartCount else 0,
                    onClick = { onNavigate(item) },
                )
            }
            SidebarNavigationItem(
                icon = MainTab.AGENT.icon,
                label = MainTab.AGENT.localizedLabel(),
                isSelected = selectedTab == MainTab.AGENT && !dataStorageSelected,
                expanded = expanded,
                onClick = { onNavigate(MainTab.AGENT) },
            )

            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SidebarNavigationItem(
                icon = Icons.Outlined.Notifications,
                label = atlasText("Notifications", "Notifications", "الإشعارات"),
                isSelected = selectedTab == MainTab.NOTIFICATIONS && !dataStorageSelected,
                expanded = expanded,
                badgeCount = notificationCount,
                onClick = onOpenNotifications,
            )
            if (Roles.canCreateDocuments(user.role)) {
                SidebarNavigationItem(
                    icon = Icons.Outlined.ShoppingCart,
                    label = atlasText("Nouvelle vente", "New sale", "بيع جديد"),
                    isSelected = selectedTab == MainTab.SALE && !dataStorageSelected,
                    expanded = expanded,
                    badgeCount = cartCount,
                    onClick = { onNavigate(MainTab.SALE) },
                )
            }
            SidebarNavigationItem(
                icon = MainTab.SETTINGS.icon,
                label = MainTab.SETTINGS.localizedLabel(),
                isSelected = selectedTab == MainTab.SETTINGS && !dataStorageSelected,
                expanded = expanded,
                onClick = { onNavigate(MainTab.SETTINGS) },
            )
            SidebarNavigationItem(
                icon = Icons.Outlined.Inventory2,
                label = atlasText("Stockage des données", "Data storage", "تخزين البيانات"),
                isSelected = dataStorageSelected,
                expanded = expanded,
                onClick = onOpenDataStorageManager,
            )
        }

        SidebarUpdateStatus(
            state = updateState,
            expanded = expanded,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    onClick = { onNavigate(MainTab.PROFILE) },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtlasAvatar(
                            avatarUrl = user.avatarUrl,
                            label = user.displayName.ifBlank { user.username },
                            size = 40.dp,
                            serverBaseUrl = serverBaseUrl,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            onClick = { onNavigate(MainTab.PROFILE) },
                            onLongClick = onAvatarSurprise,
                            longClickLabel = atlasText("Afficher une surprise", "Show a surprise", "إظهار مفاجأة"),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                user.displayName.ifBlank { user.username },
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                atlasRoleLabel(user.role),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                IconButton(onClick = onRequestSignOut) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = atlasText("Se déconnecter", "Sign out", "تسجيل الخروج"),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    onClick = { onNavigate(MainTab.PROFILE) },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AtlasAvatar(
                            avatarUrl = user.avatarUrl,
                            label = user.displayName.ifBlank { user.username },
                            size = 40.dp,
                            serverBaseUrl = serverBaseUrl,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            onClick = { onNavigate(MainTab.PROFILE) },
                            onLongClick = onAvatarSurprise,
                            longClickLabel = atlasText("Afficher une surprise", "Show a surprise", "إظهار مفاجأة"),
                        )
                    }
                }
                IconButton(onClick = onRequestSignOut) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = atlasText("Se déconnecter", "Sign out", "تسجيل الخروج"),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarUpdateStatus(
    state: UpdateState,
    expanded: Boolean,
) {
    if (state != UpdateState.Checking) return
    Surface(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdateAlt,
                    contentDescription = atlasText("Vérification des mises à jour", "Checking for updates", "التحقق من التحديثات"),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
                Text(atlasText("Vérification", "Checking", "التحقق"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun SidebarNavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    expanded: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
) {
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            pressed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(if (motionEnabled) 150 else 0),
        label = "sidebar-item-color-$label",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.72f, stiffness = 620f) else tween(0),
        label = "sidebar-item-press-$label",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { selected = isSelected }
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale },
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = null,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (expanded) 14.dp else 0.dp, vertical = 11.dp),
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) Badge {
                        Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                    }
                },
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.size(30.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                        ) {}
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = if (expanded) null else label,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.width(14.dp))
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
