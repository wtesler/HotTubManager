package tesler.will.hottubmanager.ui.onboarding

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.ui.theme.HotTubTheme
import tesler.will.hottubmanager.ui.theme.LocalSpaColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun OnboardingRouteScreen(
    viewModel: OnboardingViewModel,
    onCompletedFreshFill: () -> Unit,
    onCompletedExistingTub: () -> Unit
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
    }

    OnboardingScreenContent(
        uiState = uiState,
        hasNotifPermission = hasNotifPermission,
        hasExactAlarmPermission = hasExactAlarmPermission,
        onRequestNotificationPermission = {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onRequestExactAlarmPermission = {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        },
        onReminderTimeChange = viewModel::setReminderTime,
        onAdvanceDaysChange = viewModel::setAdvanceNoticeDays,
        onStepChange = viewModel::setStep,
        onSetExistingMode = viewModel::setExistingTubMode,
        onDateSelected = viewModel::setTaskLastCompletedDate,
        previewNextDue = viewModel::computePreviewNextDue,
        onStartFreshFill = { viewModel.startFreshFillToday(onCompletedFreshFill) },
        onSaveExistingTub = { viewModel.saveExistingTubDates(onCompletedExistingTub) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreenContent(
    uiState: OnboardingUiState,
    hasNotifPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onAdvanceDaysChange: (Int) -> Unit,
    onStepChange: (Int) -> Unit,
    onSetExistingMode: (Boolean) -> Unit,
    onDateSelected: (TaskType, LocalDate) -> Unit,
    previewNextDue: (TaskType) -> LocalDate,
    onStartFreshFill: () -> Unit,
    onSaveExistingTub: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var editingTaskDate by remember { mutableStateOf<TaskType?>(null) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HotTub,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "LS450DX Spa Manager",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "100% offline care, dosing & reminder assistant",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.stepIndex == 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Push Notifications", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Notifications power your 4-month, 4-week, and 1-month maintenance reminders, 24-hour filter clean follow-up, and 15/20/30-min treatment timer alerts.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (hasNotifPermission) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LocalSpaColors.current.good
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Notifications Enabled",
                                    color = LocalSpaColors.current.good,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Button(onClick = onRequestNotificationPermission) {
                                Text("Enable Notifications")
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Alarms & Reminders", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Granting Alarms & Reminders lets countdown timers and daily maintenance alerts fire at the exact minute. If skipped, the app falls back to best-effort scheduling.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (hasExactAlarmPermission) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LocalSpaColors.current.good
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Exact Alarms Allowed",
                                    color = LocalSpaColors.current.good,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            OutlinedButton(onClick = onRequestExactAlarmPermission) {
                                Text("Allow Exact Alarms (Optional)")
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reminder Schedule", style = MaterialTheme.typography.titleMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daily Reminder Time", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    uiState.reminderTime.format(timeFormatter),
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
                                    selected = uiState.advanceNoticeDays == days,
                                    onClick = { onAdvanceDaysChange(days) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { onStepChange(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Continue to Tub Setup")
                }
            } else {
                Text(
                    text = "Where is your LS450DX today?",
                    style = MaterialTheme.typography.titleLarge
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Starting a Fresh Fill Today", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Launches straight into the 7-step LS450DX Startup Sequence (bypass softener, fill via filter well, F1/F2 = 2 hr, strict-order TA -> pH -> CH balancing, 30-min shock, cartridges, and 24-hr filter rinse).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onStartFreshFill,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start 7-Step Startup Sequence")
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Hot Tub Is Already Running", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Enter when you last completed each maintenance item so your calendar reminders start on the right cadence immediately.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (!uiState.selectedExistingTubMode) {
                            OutlinedButton(
                                onClick = { onSetExistingMode(true) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enter Last Completed Dates")
                            }
                        } else {
                            Ls450dxSpecs.recurringTasks.forEach { spec ->
                                val lastDone = uiState.lastCompletedDates[spec.taskType] ?: LocalDate.now()
                                val nextDue = previewNextDue(spec.taskType)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(spec.shortTitle, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Next due: ${nextDue.format(dateFormatter)} (${spec.intervalLabel})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(onClick = { editingTaskDate = spec.taskType }) {
                                        Text(lastDone.format(dateFormatter))
                                    }
                                }
                            }
                            Button(
                                onClick = onSaveExistingTub,
                                enabled = !uiState.isSaving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Schedule & Open Dashboard")
                            }
                        }
                    }
                }

                TextButton(onClick = { onStepChange(0) }) {
                    Text("Back to Permissions & Reminder Time")
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.reminderTime.hour,
            initialMinute = uiState.reminderTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReminderTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            title = { Text("Preferred Reminder Time") },
            text = { TimePicker(state = timePickerState) }
        )
    }

    editingTaskDate?.let { taskType ->
        val current = uiState.lastCompletedDates[taskType] ?: LocalDate.now()
        val initialMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { editingTaskDate = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDateSelected(taskType, picked)
                        }
                        editingTaskDate = null
                    }
                ) {
                    Text("Set Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTaskDate = null }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingStep0Preview() {
    HotTubTheme {
        OnboardingScreenContent(
            uiState = OnboardingUiState(stepIndex = 0),
            hasNotifPermission = false,
            hasExactAlarmPermission = false,
            onRequestNotificationPermission = {},
            onRequestExactAlarmPermission = {},
            onReminderTimeChange = {},
            onAdvanceDaysChange = {},
            onStepChange = {},
            onSetExistingMode = {},
            onDateSelected = { _, _ -> },
            previewNextDue = { LocalDate.of(2026, 12, 21) },
            onStartFreshFill = {},
            onSaveExistingTub = {}
        )
    }
}
