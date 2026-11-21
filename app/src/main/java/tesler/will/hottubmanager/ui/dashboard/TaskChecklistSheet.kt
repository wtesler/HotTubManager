package tesler.will.hottubmanager.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tesler.will.hottubmanager.data.prefs.ActiveTimerState
import tesler.will.hottubmanager.domain.model.TaskStatus
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.model.TimerPreset
import tesler.will.hottubmanager.ui.components.ActiveTimerBanner
import tesler.will.hottubmanager.ui.components.ChecklistItemRow
import tesler.will.hottubmanager.ui.components.SafetyBadgeRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskChecklistSheet(
    task: TaskStatus,
    activeTimer: ActiveTimerState?,
    onDismiss: () -> Unit,
    onCompleteTask: (LocalDate, String) -> Unit,
    onCompleteRefillAndStartStartup: (String) -> Unit,
    onSnoozeDays: (Long) -> Unit,
    onSnoozeUntilDate: (LocalDate) -> Unit,
    onStartTimer: (TimerPreset) -> Unit,
    onCancelTimer: () -> Unit,
    onOpenTimersTab: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val checkedSteps = remember(task.spec.taskType) { mutableStateMapOf<String, Boolean>() }
    var notes by remember(task.spec.taskType) { mutableStateOf("") }
    var showPastDatePicker by remember { mutableStateOf(false) }
    var showSnoozeDatePicker by remember { mutableStateOf(false) }

    val allChecked = task.spec.checklistSteps.all { checkedSteps[it.id] == true }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = task.spec.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "${task.spec.intervalLabel} • Due ${task.effectiveDueDate.format(dateFormatter)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SafetyBadgeRow(badges = task.spec.badges)

            Text(
                text = task.spec.summary,
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            Text(
                text = "Safety & Procedure Checklist",
                style = MaterialTheme.typography.titleMedium
            )

            task.spec.checklistSteps.forEach { step ->
                ChecklistItemRow(
                    label = step.label,
                    detail = step.detail,
                    critical = step.critical,
                    checked = checkedSteps[step.id] == true,
                    onCheckedChange = { checkedSteps[step.id] = it }
                )
            }

            task.spec.linkedTimer?.let { preset ->
                if (activeTimer?.timerId == preset.id) {
                    ActiveTimerBanner(
                        timer = activeTimer,
                        onCancel = onCancelTimer,
                        onClick = onOpenTimersTab
                    )
                } else {
                    FilledTonalButton(
                        onClick = { onStartTimer(preset) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start ${preset.title}")
                    }
                }
            }

            if (task.spec.taskType == TaskType.TEST_WATER_CHEMISTRY) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenTimersTab()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Science, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Water Chemistry Logger")
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Optional notes") },
                placeholder = { Text("e.g., cartridge lot, filter condition...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            if (task.spec.taskType == TaskType.REFILL_TUB) {
                Button(
                    onClick = {
                        onCompleteRefillAndStartStartup(notes)
                        onDismiss()
                    },
                    enabled = allChecked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Drain Complete -> Launch Startup Sequence")
                }
                if (!allChecked) {
                    Text(
                        text = "Check all 4 drain & wipe-down safety boxes above to launch Startup Sequence.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onCompleteTask(LocalDate.now(), notes)
                        onDismiss()
                    },
                    enabled = allChecked || task.spec.taskType != TaskType.REFILL_TUB,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Complete Today")
                }
                OutlinedButton(
                    onClick = { showPastDatePicker = true },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Past Date")
                }
            }

            HorizontalDivider()

            Text(
                text = "Snooze / Reschedule",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onSnoozeDays(1)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("+1 Day")
                }
                OutlinedButton(
                    onClick = {
                        onSnoozeDays(3)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+3 Days")
                }
                OutlinedButton(
                    onClick = { showSnoozeDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pick Date")
                }
            }
        }
    }

    if (showPastDatePicker) {
        val initialMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showPastDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onCompleteTask(picked, notes)
                            showPastDatePicker = false
                            onDismiss()
                        }
                    }
                ) { Text("Save Completed Date") }
            },
            dismissButton = {
                TextButton(onClick = { showPastDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showSnoozeDatePicker) {
        val initialMillis = task.effectiveDueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showSnoozeDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onSnoozeUntilDate(picked)
                            showSnoozeDatePicker = false
                            onDismiss()
                        }
                    }
                ) { Text("Reschedule") }
            },
            dismissButton = {
                TextButton(onClick = { showSnoozeDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
