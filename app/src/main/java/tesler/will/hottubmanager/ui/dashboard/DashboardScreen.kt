package tesler.will.hottubmanager.ui.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import tesler.will.hottubmanager.data.db.entity.FillCycleEntity
import tesler.will.hottubmanager.data.db.entity.StartupSessionEntity
import tesler.will.hottubmanager.domain.ls450dx.Ls450dxSpecs
import tesler.will.hottubmanager.domain.model.TaskStatus
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.domain.model.TimerPreset
import tesler.will.hottubmanager.domain.model.UrgencyLevel
import tesler.will.hottubmanager.ui.components.ActiveTimerBanner
import tesler.will.hottubmanager.ui.components.SafetyBadgeRow
import tesler.will.hottubmanager.ui.theme.HotTubTheme
import tesler.will.hottubmanager.ui.theme.LocalSpaColors
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun DashboardRouteScreen(
    viewModel: DashboardViewModel,
    initialSelectedTaskType: TaskType?,
    onClearInitialTask: () -> Unit,
    onNavigateToStartup: () -> Unit,
    onNavigateToChemistry: () -> Unit,
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTaskType by remember { mutableStateOf<TaskType?>(null) }

    LaunchedEffect(initialSelectedTaskType, uiState.tasks) {
        if (initialSelectedTaskType != null && uiState.tasks.isNotEmpty()) {
            selectedTaskType = initialSelectedTaskType
            onClearInitialTask()
        }
    }

    DashboardScreenContent(
        uiState = uiState,
        contentPadding = contentPadding,
        onTaskClick = { selectedTaskType = it.spec.taskType },
        onOpenStartup = onNavigateToStartup,
        onOpenTimers = onNavigateToChemistry,
        onCancelTimer = viewModel::cancelTimer
    )

    val selectedTask = selectedTaskType?.let { type ->
        uiState.tasks.firstOrNull { it.spec.taskType == type }
    }
    if (selectedTask != null) {
        TaskChecklistSheet(
            task = selectedTask,
            activeTimer = uiState.activeTimer,
            onDismiss = { selectedTaskType = null },
            onCompleteTask = { date, notes ->
                viewModel.completeTask(selectedTask.spec.taskType, date, notes)
            },
            onCompleteRefillAndStartStartup = { notes ->
                viewModel.completeRefillAndStartStartup(notes, onNavigateToStartup)
            },
            onSnoozeDays = { days -> viewModel.snoozeTask(selectedTask.spec.taskType, days) },
            onSnoozeUntilDate = { date -> viewModel.snoozeTaskUntil(selectedTask.spec.taskType, date) },
            onStartTimer = viewModel::startTimer,
            onCancelTimer = viewModel::cancelTimer,
            onOpenTimersTab = {
                selectedTaskType = null
                onNavigateToChemistry()
            }
        )
    }
}

@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    contentPadding: PaddingValues,
    onTaskClick: (TaskStatus) -> Unit,
    onOpenStartup: () -> Unit,
    onOpenTimers: () -> Unit,
    onCancelTimer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TubStatusBanner(
                tasks = uiState.tasks,
                fillCycle = uiState.activeFillCycle,
                fillCycleDayCount = uiState.fillCycleDayCount
            )
        }

        uiState.activeTimer?.let { timer ->
            item {
                ActiveTimerBanner(
                    timer = timer,
                    onCancel = onCancelTimer,
                    onClick = onOpenTimers
                )
            }
        }

        uiState.activeStartupSession?.let { session ->
            item {
                ActiveStartupFollowupCard(
                    session = session,
                    onOpenStartup = onOpenStartup
                )
            }
        }

        item {
            Text(
                text = "Maintenance Schedule",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(
            items = uiState.tasks,
            key = { it.spec.taskType.name }
        ) { task ->
            MaintenanceTaskCard(
                task = task,
                onClick = { onTaskClick(task) }
            )
        }
    }
}

@Composable
private fun TubStatusBanner(
    tasks: List<TaskStatus>,
    fillCycle: FillCycleEntity?,
    fillCycleDayCount: Long?
) {
    val spaColors = LocalSpaColors.current
    val overdueCount = tasks.count { it.urgency == UrgencyLevel.OVERDUE }
    val dueTodayOrSoonCount = tasks.count {
        it.urgency == UrgencyLevel.DUE_TODAY || it.urgency == UrgencyLevel.DUE_SOON
    }

    val (containerColor, contentColor, icon, headline) = when {
        overdueCount > 0 -> Quad(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Warning,
            "$overdueCount Overdue Action${if (overdueCount > 1) "s" else ""} Needed"
        )
        dueTodayOrSoonCount > 0 -> Quad(
            spaColors.warningContainer,
            MaterialTheme.colorScheme.onSurface,
            Icons.Default.Schedule,
            "$dueTodayOrSoonCount Task${if (dueTodayOrSoonCount > 1) "s" else ""} Due Soon"
        )
        else -> Quad(
            spaColors.goodContainer,
            MaterialTheme.colorScheme.onSurface,
            Icons.Default.CheckCircle,
            "All Caught Up"
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HotTub,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "LS450DX Plug-and-Play Spa",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor
                )
            }
            val cycleLine = if (fillCycle != null && fillCycleDayCount != null) {
                "Fill Cycle #${fillCycle.cycleNumber} • Day $fillCycleDayCount of ~${Ls450dxSpecs.TYPICAL_FILL_CYCLE_DAYS}"
            } else {
                "Tap any maintenance item below for checklist & safety rules"
            }
            Text(
                text = cycleLine,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun ActiveStartupFollowupCard(
    session: StartupSessionEntity,
    onOpenStartup: () -> Unit
) {
    var nowMillis by remember(session.filterCleanDueAt) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(session.filterCleanDueAt) {
        if (session.filterCleanDueAt != null) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(30_000L)
            }
        }
    }

    val waitingFor24hFilter = session.currentStep == 7 && session.filterCleanDueAt != null
    val subtitle = if (waitingFor24hFilter) {
        val diffMillis = session.filterCleanDueAt!!.toEpochMilli() - nowMillis
        if (diffMillis <= 0L) {
            "24 hours elapsed — Rinse filter with garden hose (Jets OFF)"
        } else {
            val hours = diffMillis / 3_600_000L
            val mins = (diffMillis % 3_600_000L) / 60_000L
            "24-hr filter rinse due in ${hours}h ${mins}m"
        }
    } else {
        "In progress: Step ${session.currentStep} of 7"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (waitingFor24hFilter) "24-Hour Post-Startup Follow-Up" else "Startup Sequence Active",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onOpenStartup) {
                Text(if (waitingFor24hFilter) "Open Step 7" else "Resume")
            }
        }
    }
}

@Composable
private fun MaintenanceTaskCard(
    task: TaskStatus,
    onClick: () -> Unit
) {
    val spaColors = LocalSpaColors.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    val countdownText = when {
        task.daysUntilDue < -1 -> "${abs(task.daysUntilDue)} days overdue"
        task.daysUntilDue == -1L -> "1 day overdue"
        task.daysUntilDue == 0L -> "Due today"
        task.daysUntilDue == 1L -> "Due tomorrow"
        else -> "In ${task.daysUntilDue} days"
    }

    val pillColors = when (task.urgency) {
        UrgencyLevel.OVERDUE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        UrgencyLevel.DUE_TODAY, UrgencyLevel.DUE_SOON -> spaColors.warningContainer to MaterialTheme.colorScheme.onSurface
        UrgencyLevel.UPCOMING -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.spec.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${task.spec.intervalLabel} • Due ${task.effectiveDueDate.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = pillColors.first,
                    contentColor = pillColors.second
                ) {
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open checklist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SafetyBadgeRow(badges = task.spec.badges)
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    val today = LocalDate.of(2026, 11, 21)
    val previewTasks = listOf(
        TaskStatus(
            id = 2,
            spec = Ls450dxSpecs.specFor(TaskType.SILVER_CHLORINE),
            lastCompletedDate = today.minusDays(30),
            nextDueDate = today.minusDays(2),
            snoozedUntilDate = null,
            effectiveDueDate = today.minusDays(2),
            daysUntilDue = -2,
            urgency = UrgencyLevel.OVERDUE,
            isEnabled = true
        ),
        TaskStatus(
            id = 3,
            spec = Ls450dxSpecs.specFor(TaskType.MONTHLY_SHOCK),
            lastCompletedDate = today.minusMonths(1),
            nextDueDate = today,
            snoozedUntilDate = null,
            effectiveDueDate = today,
            daysUntilDue = 0,
            urgency = UrgencyLevel.DUE_TODAY,
            isEnabled = true
        ),
        TaskStatus(
            id = 1,
            spec = Ls450dxSpecs.specFor(TaskType.BLUE_MINERAL),
            lastCompletedDate = today.minusMonths(2),
            nextDueDate = today.plusMonths(2),
            snoozedUntilDate = null,
            effectiveDueDate = today.plusMonths(2),
            daysUntilDue = 61,
            urgency = UrgencyLevel.UPCOMING,
            isEnabled = true
        )
    )
    HotTubTheme {
        DashboardScreenContent(
            uiState = DashboardUiState(
                tasks = previewTasks,
                activeFillCycle = FillCycleEntity(id = 1, cycleNumber = 1, startedAt = Instant.now()),
                fillCycleDayCount = 42,
                activeTimer = null
            ),
            contentPadding = PaddingValues(0.dp),
            onTaskClick = {},
            onOpenStartup = {},
            onOpenTimers = {},
            onCancelTimer = {}
        )
    }
}
