package ma.etherealmber.atlascommerce.ui.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasMark
import ma.etherealmber.atlascommerce.ui.i18n.atlasRoleLabel
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

@Composable
internal fun AtlasSidebarContent(
    user: SessionUser,
    serverBaseUrl: String,
    roleTabs: List<MainTab>,
    selectedTab: MainTab,
    cartCount: Int,
    notificationCount: Int,
    expanded: Boolean,
    canCollapse: Boolean,
    onToggleCollapse: () -> Unit,
    onNavigate: (MainTab) -> Unit,
    onOpenNotifications: () -> Unit,
    onAgentHistory: () -> Unit,
    onNewAgentConversation: () -> Unit,
    onRefreshAgentContext: () -> Unit,
    onShowAgentQuota: () -> Unit,
    onRequestSignOut: () -> Unit,
    onAvatarSurprise: () -> Unit,
    modifier: Modifier = Modifier,
    showCloseButton: Boolean = false,
    onClose: () -> Unit = {},
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = if (expanded) 12.dp else 8.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AtlasMark(
                modifier = if (!expanded && canCollapse) Modifier.clickable(onClick = onToggleCollapse) else Modifier,
                size = if (expanded) 46.dp else 28.dp,
            )
            if (expanded) {
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "Atlas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        "Commerce",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            when {
                showCloseButton -> IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = atlasText("Fermer la navigation", "Close navigation", "إغلاق التنقل"))
                }
                canCollapse -> {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) {
                            if (isRtl) -90f else 90f
                        } else {
                            if (isRtl) 90f else -90f
                        },
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                        label = "sidebar-collapse-chevron",
                    )
                    IconButton(onClick = onToggleCollapse, modifier = Modifier.size(if (expanded) 40.dp else 32.dp)) {
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (expanded) atlasText("Réduire le menu", "Collapse sidebar", "تصغير القائمة") else atlasText("Développer le menu", "Expand sidebar", "توسيع القائمة"),
                            modifier = Modifier.graphicsLayer { rotationZ = rotation },
                        )
                    }
                }
            }
        }

        if (expanded) {
            Text(
                text = selectedTab.localizedLabel(),
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            roleTabs.forEach { item ->
                SidebarNavigationItem(
                    icon = item.icon,
                    label = item.localizedLabel(),
                    isSelected = selectedTab == item,
                    expanded = expanded,
                    badgeCount = if (item == MainTab.SALE) cartCount else 0,
                    onClick = { onNavigate(item) },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SidebarNavigationItem(
                icon = Icons.Outlined.Notifications,
                label = atlasText("Notifications", "Notifications", "الإشعارات"),
                isSelected = false,
                expanded = expanded,
                badgeCount = notificationCount,
                onClick = onOpenNotifications,
            )
            if (Roles.canCreateDocuments(user.role)) {
                SidebarNavigationItem(
                    icon = Icons.Outlined.ShoppingCart,
                    label = atlasText("Nouvelle vente", "New sale", "بيع جديد"),
                    isSelected = selectedTab == MainTab.SALE,
                    expanded = expanded,
                    badgeCount = cartCount,
                    onClick = { onNavigate(MainTab.SALE) },
                )
            }
            SidebarNavigationItem(
                icon = MainTab.AGENT.icon,
                label = MainTab.AGENT.localizedLabel(),
                isSelected = selectedTab == MainTab.AGENT,
                expanded = expanded,
                onClick = { onNavigate(MainTab.AGENT) },
            )
            if (selectedTab == MainTab.AGENT) {
                SidebarNavigationItem(
                    icon = Icons.Outlined.History,
                    label = atlasText("Historique", "History", "السجل"),
                    isSelected = false,
                    expanded = expanded,
                    onClick = onAgentHistory,
                )
                SidebarNavigationItem(
                    icon = Icons.Outlined.AddComment,
                    label = atlasText("Nouvelle discussion", "New conversation", "محادثة جديدة"),
                    isSelected = false,
                    expanded = expanded,
                    onClick = onNewAgentConversation,
                )
                SidebarNavigationItem(
                    icon = Icons.Outlined.Refresh,
                    label = atlasText("Actualiser le contexte", "Refresh context", "تحديث السياق"),
                    isSelected = false,
                    expanded = expanded,
                    onClick = onRefreshAgentContext,
                )
                SidebarNavigationItem(
                    icon = Icons.Outlined.HourglassBottom,
                    label = atlasText("Quota IA", "AI quota", "حصة الذكاء الاصطناعي"),
                    isSelected = false,
                    expanded = expanded,
                    onClick = onShowAgentQuota,
                )
            }
            SidebarNavigationItem(
                icon = MainTab.PROFILE.icon,
                label = MainTab.PROFILE.localizedLabel(),
                isSelected = selectedTab == MainTab.PROFILE,
                expanded = expanded,
                onClick = { onNavigate(MainTab.PROFILE) },
            )
            SidebarNavigationItem(
                icon = MainTab.SETTINGS.icon,
                label = MainTab.SETTINGS.localizedLabel(),
                isSelected = selectedTab == MainTab.SETTINGS,
                expanded = expanded,
                onClick = { onNavigate(MainTab.SETTINGS) },
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
private fun SidebarNavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    expanded: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { selected = isSelected },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)) else null,
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
                Icon(
                    imageVector = icon,
                    contentDescription = if (expanded) null else label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(23.dp),
                )
            }
            if (expanded) {
                Spacer(Modifier.width(14.dp))
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AtlasFloatingBottomBar(
