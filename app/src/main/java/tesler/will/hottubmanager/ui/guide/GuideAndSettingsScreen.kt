package tesler.will.hottubmanager.ui.guide

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tesler.will.hottubmanager.domain.ls450dx.Ls450dxSpecs
import tesler.will.hottubmanager.domain.model.OverdueFrequency
import tesler.will.hottubmanager.ui.theme.HotTubTheme
import tesler.will.hottubmanager.ui.theme.LocalSpaColors
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GuideRouteScreen(
    viewModel: GuideViewModel,
    onOpenFullHistory: () -> Unit,
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotifPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                hasExactAlarmPermission =
                    context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) viewModel.clearTestFeedback()
    }

    GuideAndSettingsContent(
        uiState = uiState,
        hasNotifPermission = hasNotifPermission,
        hasExactAlarmPermission = hasExactAlarmPermission,
        contentPadding = contentPadding,
        onRequestNotificationPermission = {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onOpenAppNotificationSettings = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        },
        onOpenExactAlarmSettings = {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        },
        onReminderTimeChange = viewModel::setReminderTime,
        onAdvanceDaysChange = viewModel::setAdvanceNoticeDays,
        onOverdueFrequencyChange = viewModel::setOverdueFrequency,
        onSendTestNotification = viewModel::sendTestNotification,
        onOpenFullHistory = onOpenFullHistory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideAndSettingsContent(
    uiState: GuideUiState,
    hasNotifPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    contentPadding: PaddingValues,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onAdvanceDaysChange: (Int) -> Unit,
    onOverdueFrequencyChange: (OverdueFrequency) -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenFullHistory: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val logFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Guide, History & Settings",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // LS450DX Quick Reference Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("LS450DX Quick Reference", style = MaterialTheme.typography.titleLarge)
                    }
                    Ls450dxSpecs.quickReferenceSections.forEachIndexed { index, section ->
                        if (index > 0) HorizontalDivider()
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        section.items.forEach { (label, detail) ->
                            Column {
                                Text(label, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Maintenance History Preview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Maintenance History", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            "${uiState.fillCycles.size} Fill Cycle(s)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.recentLogs.isEmpty()) {
                        Text(
                            "No completed maintenance tasks recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.recentLogs.forEach { log ->
                            val spec = Ls450dxSpecs.specFor(log.taskType)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(spec.shortTitle, fontWeight = FontWeight.Medium)
                                Text(
                                    logFormatter.format(log.completedAt),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenFullHistory,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("View Full Maintenance History (${uiState.allLogs.size})")
                    }
                }
            }
        }

        // Notification & Alarm Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Notifications & Alarms", style = MaterialTheme.typography.titleLarge)
                    }

                    // Permission statuses
                    PermissionStatusRow(
                        title = "Push Notifications (POST_NOTIFICATIONS)",
                        granted = hasNotifPermission,
                        actionLabel = if (hasNotifPermission) "Settings" else "Grant",
                        onClick = if (hasNotifPermission) onOpenAppNotificationSettings else onRequestNotificationPermission
                    )

                    PermissionStatusRow(
                        title = "Exact Alarms (Alarms & Reminders)",
                        granted = hasExactAlarmPermission,
                        actionLabel = if (hasExactAlarmPermission) "Manage" else "Allow",
                        onClick = onOpenExactAlarmSettings
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Reminder Time", style = MaterialTheme.typography.labelLarge)
                            Text(
                                uiState.preferences.reminderTime.format(timeFormatter),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        OutlinedButton(onClick = { showTimePicker = true }) {
                            Text("Change Time")
                        }
                    }

                    Text("Advance Notice", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "None", 1 to "1 day", 3 to "3 days", 7 to "1 wk").forEach { (days, label) ->
                            FilterChip(
                                selected = uiState.preferences.advanceNoticeDays == days,
                                onClick = { onAdvanceDaysChange(days) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Overdue Reminder Frequency", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverdueFrequency.entries.forEach { freq ->
                            FilterChip(
                                selected = uiState.preferences.overdueFrequency == freq,
                                onClick = { onOverdueFrequencyChange(freq) },
                                label = { Text(freq.label) }
                            )
                        }
                    }

                    uiState.testNotificationMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (hasNotifPermission) LocalSpaColors.current.good else MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = onSendTestNotification,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Send Test Notification")
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = uiState.preferences.reminderTime.hour,
            initialMinute = uiState.preferences.reminderTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReminderTimeChange(LocalTime.of(state.hour, state.minute))
                        showTimePicker = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            title = { Text("Preferred Reminder Time") },
            text = { TimePicker(state = state) }
        )
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    val spaColors = LocalSpaColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) spaColors.good else spaColors.warning
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    if (granted) "Granted" else "Not granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (granted) spaColors.good else spaColors.warning
                )
            }
        }
        OutlinedButton(onClick = onClick) {
            Text(actionLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuidePreview() {
    HotTubTheme {
        GuideAndSettingsContent(
            uiState = GuideUiState(),
            hasNotifPermission = true,
            hasExactAlarmPermission = false,
            contentPadding = PaddingValues(0.dp),
            onRequestNotificationPermission = {},
            onOpenAppNotificationSettings = {},
            onOpenExactAlarmSettings = {},
            onReminderTimeChange = {},
            onAdvanceDaysChange = {},
            onOverdueFrequencyChange = {},
            onSendTestNotification = {},
            onOpenFullHistory = {}
        )
    }
}
