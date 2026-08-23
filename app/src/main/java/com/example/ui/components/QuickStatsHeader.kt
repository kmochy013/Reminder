package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedLight
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FilterTab
import com.example.ui.viewmodel.ReminderUiState

@Composable
fun QuickStatsHeader(
    uiState: ReminderUiState,
    onTabSelected: (FilterTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Active Alert",
                count = uiState.naggingCount,
                icon = Icons.Default.Warning,
                tintColor = AlertRed,
                bgColor = if (uiState.naggingCount > 0) AlertRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                isSelected = uiState.selectedTab == FilterTab.NAGGING,
                onClick = { onTabSelected(FilterTab.NAGGING) },
                modifier = Modifier.weight(1f).testTag("stat_card_nagging"),
                badge = if (uiState.naggingCount > 0) "1-min nag" else null
            )

            StatCard(
                title = "Upcoming",
                count = uiState.upcomingCount,
                icon = Icons.Default.Schedule,
                tintColor = PrimaryIndigo,
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                isSelected = uiState.selectedTab == FilterTab.UPCOMING,
                onClick = { onTabSelected(FilterTab.UPCOMING) },
                modifier = Modifier.weight(1f).testTag("stat_card_upcoming")
            )

            StatCard(
                title = "Read / Done",
                count = uiState.completedCount,
                icon = Icons.Default.CheckCircle,
                tintColor = SuccessGreen,
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                isSelected = uiState.selectedTab == FilterTab.COMPLETED,
                onClick = { onTabSelected(FilterTab.COMPLETED) },
                modifier = Modifier.weight(1f).testTag("stat_card_completed")
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    tintColor: Color,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) tintColor.copy(alpha = 0.18f) else bgColor
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, tintColor) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(tintColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tintColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (badge != null) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = tintColor,
                        modifier = Modifier
                            .background(tintColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
