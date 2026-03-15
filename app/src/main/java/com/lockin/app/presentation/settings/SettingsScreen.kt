package com.lockin.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lockin.app.ui.theme.*

/**
 * SettingsScreen – App preferences, integrations, and developer tools.
 */
@Composable
fun SettingsScreen(
    onNavigateToHealthPermissions: () -> Unit = {},
    onNavigateToHealthDebug: () -> Unit = {},
    onNavigateToWatchDebug: () -> Unit = {},
    onNavigateToHealthDiag: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LockinBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = LockinTextPrimary
            )
        }

        item { Spacer(Modifier.height(8.dp)) }

        // ── Profile section ──
        item { SectionLabel("Profile") }
        item {
            SettingsRow(
                icon = Icons.Outlined.Person,
                title = "Edit Profile",
                subtitle = "Name, goal, experience",
                onClick = { /* TODO */ }
            )
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Health & Devices ──
        item { SectionLabel("Health & Devices") }
        item {
            SettingsRow(
                icon = Icons.Outlined.HealthAndSafety,
                title = "Health Permissions",
                subtitle = "Manage Health Connect access",
                onClick = onNavigateToHealthPermissions
            )
        }
        item {
            SettingsRow(
                icon = Icons.Outlined.Watch,
                title = "Watch",
                subtitle = "Galaxy Watch connection",
                onClick = { /* TODO */ }
            )
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── App ──
        item { SectionLabel("App") }
        item {
            SettingsRow(
                icon = Icons.Outlined.Palette,
                title = "Appearance",
                subtitle = "Theme, units",
                onClick = { /* TODO */ }
            )
        }
        item {
            SettingsRow(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                subtitle = "Reminders and alerts",
                onClick = { /* TODO */ }
            )
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Developer / Debug ──
        item { SectionLabel("Developer") }
        item {
            SettingsRow(
                icon = Icons.Outlined.BugReport,
                title = "Health Debug",
                subtitle = "Test steps read",
                iconColor = LockinWarning,
                onClick = onNavigateToHealthDebug
            )
        }
        item {
            SettingsRow(
                icon = Icons.Outlined.BugReport,
                title = "Watch Tracking Debug",
                subtitle = "Start/stop service, view HR",
                iconColor = LockinWarning,
                onClick = onNavigateToWatchDebug
            )
        }
        item {
            SettingsRow(
                icon = Icons.Outlined.BugReport,
                title = "Health Connect Diagnostic",
                subtitle = "SDK status & permission audit",
                iconColor = LockinWarning,
                onClick = onNavigateToHealthDiag
            )
        }

        item { Spacer(Modifier.height(40.dp)) }

        item {
            Text(
                "Lockin v1.0 · Built for S24 Ultra",
                style = MaterialTheme.typography.bodySmall,
                color = LockinTextDisabled,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = LockinPrimaryAccent,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = LockinTextSecondary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = LockinTextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = LockinTextDisabled,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
