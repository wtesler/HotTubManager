package tesler.will.hottubmanager.ui.startup

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import tesler.will.hottubmanager.data.db.entity.StartupSessionEntity
import tesler.will.hottubmanager.domain.chemistry.BalanceStep
import tesler.will.hottubmanager.domain.ls450dx.Ls450dxSpecs
import tesler.will.hottubmanager.domain.model.SafetyBadge
import tesler.will.hottubmanager.domain.model.TimerPreset
import tesler.will.hottubmanager.ui.components.ActiveTimerBanner
import tesler.will.hottubmanager.ui.components.ChecklistItemRow
import tesler.will.hottubmanager.ui.components.RangeMeterBar
import tesler.will.hottubmanager.ui.components.SafetyBadgeRow
import tesler.will.hottubmanager.ui.theme.HotTubTheme
import tesler.will.hottubmanager.ui.theme.LocalSpaColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StartupRouteScreen(
    viewModel: StartupViewModel,
    onNavigateToChemistry: () -> Unit,
    onStartupFinished: () -> Unit,
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StartupWizardContent(
        uiState = uiState,
        contentPadding = contentPadding,
        onStartFreshSession = viewModel::startFreshStartupSession,
        onSelectStep = viewModel::selectStep,
        onSetFlag = viewModel::setFlag,
        onAdvanceFromStep = viewModel::advanceFromStep,
        onTaChange = viewModel::updateTaText,
        onPhChange = viewModel::updatePhText,
        onChChange = viewModel::updateChText,
        onManualOverrideChange = viewModel::setManualOverride,
        onLogStep4 = viewModel::logStep4Reading,
        onSetResetAnchors = viewModel::setResetAnchorsOnStep6,
        onCompleteStep6 = viewModel::completeStep6,
        onCompleteStep7 = {
            viewModel.completeStep7()
            onStartupFinished()
        },
        onStartTimer = viewModel::startTimer,
        onCancelTimer = viewModel::cancelTimer,
        onOpenTimers = onNavigateToChemistry
    )
}

@Composable
fun StartupWizardContent(
    uiState: StartupUiState,
    contentPadding: PaddingValues,
    onStartFreshSession: () -> Unit,
    onSelectStep: (Int) -> Unit,
    onSetFlag: (String, Boolean) -> Unit,
    onAdvanceFromStep: (Int) -> Unit,
    onTaChange: (String) -> Unit,
    onPhChange: (String) -> Unit,
    onChChange: (String) -> Unit,
    onManualOverrideChange: (Boolean) -> Unit,
    onLogStep4: (Boolean) -> Unit,
    onSetResetAnchors: (Boolean) -> Unit,
    onCompleteStep6: () -> Unit,
    onCompleteStep7: () -> Unit,
    onStartTimer: (TimerPreset) -> Unit,
    onCancelTimer: () -> Unit,
    onOpenTimers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "LS450DX Startup Sequence",
            style = MaterialTheme.typography.headlineSmall
        )

        uiState.activeTimer?.let { timer ->
            ActiveTimerBanner(
                timer = timer,
                onCancel = onCancelTimer,
                onClick = onOpenTimers
            )
        }

        if (uiState.activeSession == null) {
            NoActiveStartupCard(onStartFreshSession = onStartFreshSession)
            return@Column
        }

        // Stepper pills 1..7
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..7).forEach { step ->
                val done = uiState.flags["step_${step}_done"] == true
                FilterChip(
                    selected = uiState.selectedStep == step,
                    onClick = { onSelectStep(step) },
                    leadingIcon = if (done) {
                        {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = LocalSpaColors.current.good
                            )
                        }
                    } else null,
                    label = { Text("Step $step") }
                )
            }
        }

        when (uiState.selectedStep) {
            1 -> Step1BypassSoftener(
                flags = uiState.flags,
                onSetFlag = onSetFlag,
                onNext = { onAdvanceFromStep(1) }
            )
            2 -> Step2FillFilterWell(
                flags = uiState.flags,
                onSetFlag = onSetFlag,
                onNext = { onAdvanceFromStep(2) }
            )
            3 -> Step3PowerAndFilterCycles(
                flags = uiState.flags,
                onSetFlag = onSetFlag,
                onStart15SecTimer = { onStartTimer(TimerPreset.FILTER_SAVE_15S) },
                onNext = { onAdvanceFromStep(3) }
            )
            4 -> Step4BalanceWater(
                uiState = uiState,
                onTaChange = onTaChange,
                onPhChange = onPhChange,
                onChChange = onChChange,
                onManualOverrideChange = onManualOverrideChange,
                onLogStep4 = onLogStep4,
                onNext = { onAdvanceFromStep(4) }
            )
            5 -> Step5StartupShock(
                flags = uiState.flags,
                onSetFlag = onSetFlag,
                onStart30MinTimer = { onStartTimer(TimerPreset.STARTUP_SHOCK_30M) },
                onNext = { onAdvanceFromStep(5) }
            )
            6 -> Step6InsertCartridges(
                flags = uiState.flags,
                resetAnchors = uiState.resetAnchorsOnStep6,
                onSetFlag = onSetFlag,
                onSetResetAnchors = onSetResetAnchors,
                onCompleteStep6 = onCompleteStep6
            )
            7 -> Step7FilterClean24h(
                session = uiState.activeSession,
                flags = uiState.flags,
                onSetFlag = onSetFlag,
                onCompleteStep7 = onCompleteStep7
            )
        }
    }
}

@Composable
private fun NoActiveStartupCard(onStartFreshSession: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("No Startup Sequence In Progress", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = "Start the guided 7-step LS450DX fresh-fill sequence whenever you drain & refill your spa. Progress is saved after every tap—even across chemical wait timers and the 24-hour filter rinse.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onStartFreshSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start 7-Step Startup Sequence")
            }
        }
    }
}

@Composable
private fun Step1BypassSoftener(
    flags: Map<String, Boolean>,
    onSetFlag: (String, Boolean) -> Unit,
    onNext: () -> Unit
) {
    val checked = flags["s1_bypass"] == true
    StepCard(
        stepNumber = 1,
        title = "Bypass Water Softener",
        badges = listOf(SafetyBadge.BYPASS_SOFTENER)
    ) {
        Text(
            text = "Ensure your home water softener bypass valve is engaged before attaching the hose so calcium hardness stays controllable.",
            style = MaterialTheme.typography.bodyLarge
        )
        ChecklistItemRow(
            label = "Water softener bypass is active",
            critical = true,
            checked = checked,
            onCheckedChange = { onSetFlag("s1_bypass", it) }
        )
        Button(
            onClick = onNext,
            enabled = checked,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Continue to Step 2")
        }
    }
}

@Composable
private fun Step2FillFilterWell(
    flags: Map<String, Boolean>,
    onSetFlag: (String, Boolean) -> Unit,
    onNext: () -> Unit
) {
    val c1 = flags["s2_basket"] == true
    val c2 = flags["s2_hose_well"] == true
    val c3 = flags["s2_water_level"] == true
    StepCard(
        stepNumber = 2,
        title = "Fill Water Directly into Filter Well",
        badges = listOf(SafetyBadge.GARDEN_HOSE)
    ) {
        Text(
            text = "Filling directly down the filter well prevents air-locks in the LS450DX pump.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ChecklistItemRow(
            label = "Pull out top skimmer basket",
            checked = c1,
            onCheckedChange = { onSetFlag("s2_basket", it) }
        )
        ChecklistItemRow(
            label = "Push garden hose straight down into open filter well",
            checked = c2,
            onCheckedChange = { onSetFlag("s2_hose_well", it) }
        )
        ChecklistItemRow(
            label = "Stop halfway between top rim & grey filter ring (above all jets)",
            critical = true,
            checked = c3,
            onCheckedChange = { onSetFlag("s2_water_level", it) }
        )
        Button(
            onClick = onNext,
            enabled = c1 && c2 && c3,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Continue to Step 3")
        }
    }
}

@Composable
private fun Step3PowerAndFilterCycles(
    flags: Map<String, Boolean>,
    onSetFlag: (String, Boolean) -> Unit,
    onStart15SecTimer: () -> Unit,
    onNext: () -> Unit
) {
    val plugged = flags["s3_plugged"] == true
    val fcKeys = Ls450dxSpecs.filterCycleWalkthrough.map { "s3_fc_${it.number}" }
    val allFcChecked = fcKeys.all { flags[it] == true }

    StepCard(
        stepNumber = 3,
        title = "Plug In, Set Temp & Filter Cycles (F1/F2 = 2 hr)",
        badges = emptyList()
    ) {
        ChecklistItemRow(
            label = "Plug in 110V spa and set target water temperature",
            checked = plugged,
            onCheckedChange = { onSetFlag("s3_plugged", it) }
        )
        HorizontalDivider()
        Text(
            text = "Configure Filter Cycles (override defaults F1=1 hr, F2=30 min):",
            style = MaterialTheme.typography.titleMedium
        )
        Ls450dxSpecs.filterCycleWalkthrough.forEach { step ->
            val key = "s3_fc_${step.number}"
            ChecklistItemRow(
                label = "${step.number}. ${step.instruction}",
                checked = flags[key] == true,
                onCheckedChange = { onSetFlag(key, it) }
            )
        }
        FilledTonalButton(
            onClick = onStart15SecTimer,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Timer, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start 15-Sec Hands-Off Save Timer")
        }
        Button(
            onClick = onNext,
            enabled = plugged && allFcChecked,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Continue to Step 4")
        }
    }
}

@Composable
private fun Step4BalanceWater(
    uiState: StartupUiState,
    onTaChange: (String) -> Unit,
    onPhChange: (String) -> Unit,
    onChChange: (String) -> Unit,
    onManualOverrideChange: (Boolean) -> Unit,
    onLogStep4: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    val input = uiState.step4Input
    val advice = uiState.step4Advice

    StepCard(
        stepNumber = 4,
        title = "Balance Water in Strict Order",
        badges = listOf(SafetyBadge.JETS_HIGH, SafetyBadge.AIR_VALVES_CLOSED)
    ) {
        Text(
            text = "Adjust ONE chemical at a time in strict order: 1) Total Alkalinity -> 2) pH -> 3) Calcium Hardness. Wait 15 min with jets HIGH and air valves CLOSED between additions.",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Manual out-of-order override", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = input.manualOverrideOrder,
                onCheckedChange = onManualOverrideChange
            )
        }

        // Order 1: TA
        OutlinedTextField(
            value = input.taText,
            onValueChange = onTaChange,
            label = { Text("1. Total Alkalinity (ppm) — Target 80–120") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        RangeMeterBar(
            label = "Total Alkalinity",
            valueText = input.taText.ifBlank { "--" } + " ppm",
            targetText = "80–120",
            status = advice.taStatus,
            fractionInScale = input.taText.toFloatOrNull()?.let { it / 200f }
        )

        // Order 2: pH
        OutlinedTextField(
            value = input.phText,
            onValueChange = onPhChange,
            enabled = !advice.isPhLocked,
            label = {
                Text(
                    if (advice.isPhLocked) "2. pH (Locked until TA is 80–120 ppm)"
                    else "2. pH — Target 7.2–7.8"
                )
            },
            leadingIcon = if (advice.isPhLocked) {
                { Icon(Icons.Default.Lock, contentDescription = "Locked") }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        RangeMeterBar(
            label = "pH",
            valueText = input.phText.ifBlank { "--" },
            targetText = "7.2–7.8",
            status = advice.phStatus,
            fractionInScale = input.phText.toFloatOrNull()?.let { ((it - 6.2f) / 2.4f) }
        )

        // Order 3: CH
        OutlinedTextField(
            value = input.chText,
            onValueChange = onChChange,
            enabled = !advice.isChLocked,
            label = {
                Text(
                    if (advice.isChLocked) "3. Calcium Hardness (Locked until TA & pH balanced)"
                    else "3. Calcium Hardness (ppm) — Target 150–250"
                )
            },
            leadingIcon = if (advice.isChLocked) {
                { Icon(Icons.Default.Lock, contentDescription = "Locked") }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        RangeMeterBar(
            label = "Calcium Hardness",
            valueText = input.chText.ifBlank { "--" } + " ppm",
            targetText = "150–250",
            status = advice.chStatus,
            fractionInScale = input.chText.toFloatOrNull()?.let { it / 400f }
        )

        // Advisor Recommendation Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (advice.isFullyBalanced) {
                    LocalSpaColors.current.goodContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(advice.headline, style = MaterialTheme.typography.titleMedium)
                if (advice.chemicalName != null && advice.doseText != null) {
                    Text(
                        text = "Nudge Dose: ${advice.doseText} of ${advice.chemicalName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(advice.instructions, style = MaterialTheme.typography.bodyMedium)
            }
        }

        input.lastLoggedMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.labelLarge,
                color = LocalSpaColors.current.good
            )
        }

        if (advice.needs15MinWaitTimer) {
            FilledTonalButton(
                onClick = { onLogStep4(true) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log Addition & Start 15-Min Jets Timer")
            }
        } else {
            OutlinedButton(
                onClick = { onLogStep4(false) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Save Test Strip Reading")
            }
        }

        Button(
            onClick = onNext,
            enabled = advice.activeStep == BalanceStep.ALL_BALANCED || input.manualOverrideOrder,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Continue to Step 5 (Startup Shock)")
        }
    }
}

@Composable
private fun Step5StartupShock(
    flags: Map<String, Boolean>,
    onSetFlag: (String, Boolean) -> Unit,
    onStart30MinTimer: () -> Unit,
    onNext: () -> Unit
) {
    val c1 = flags["s5_cover_open"] == true
    val c2 = flags["s5_air_closed"] == true
    val c3 = flags["s5_shock_added"] == true

    StepCard(
        stepNumber = 5,
        title = "Startup Shock Packet (30 Minutes)",
        badges = listOf(SafetyBadge.JETS_HIGH, SafetyBadge.AIR_VALVES_CLOSED, SafetyBadge.COVER_OPEN)
    ) {
        ChecklistItemRow(
            label = "Keep spa cover OPEN",
            critical = true,
            checked = c1,
            onCheckedChange = { onSetFlag("s5_cover_open", it) }
        )
        ChecklistItemRow(
            label = "Close air valves / filter and turn jets ON HIGH",
            critical = true,
            checked = c2,
            onCheckedChange = { onSetFlag("s5_air_closed", it) }
        )
        ChecklistItemRow(
            label = "Add startup shock packet and circulate 30 minutes",
            checked = c3,
            onCheckedChange = { onSetFlag("s5_shock_added", it) }
        )
        FilledTonalButton(
            onClick = onStart30MinTimer,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Timer, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start 30-Min Startup Shock Timer")
        }
        Button(
            onClick = onNext,
            enabled = c1 && c2 && c3,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Continue to Step 6 (Insert Cartridges)")
        }
    }
}

@Composable
private fun Step6InsertCartridges(
    flags: Map<String, Boolean>,
    resetAnchors: Boolean,
    onSetFlag: (String, Boolean) -> Unit,
    onSetResetAnchors: (Boolean) -> Unit,
    onCompleteStep6: () -> Unit
) {
    val c1 = flags["s6_jets_off"] == true
    val c2 = flags["s6_blue_6"] == true
    val c3 = flags["s6_silver_2"] == true

    StepCard(
        stepNumber = 6,
        title = "Insert Cartridges",
        badges = listOf(SafetyBadge.JETS_OFF, SafetyBadge.SETTING_6, SafetyBadge.SETTING_2)
    ) {
        ChecklistItemRow(
            label = "Ensure jets are OFF (NOT running)",
            detail = "No need to run jets after inserting cartridges.",
            critical = true,
            checked = c1,
            onCheckedChange = { onSetFlag("s6_jets_off", it) }
        )
        ChecklistItemRow(
            label = "Insert Blue Mineral Cartridge on setting 6",
            checked = c2,
            onCheckedChange = { onSetFlag("s6_blue_6", it) }
        )
        ChecklistItemRow(
            label = "Insert Silver Chlorine Cartridge on setting 2",
            checked = c3,
            onCheckedChange = { onSetFlag("s6_silver_2", it) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Reset recurring schedules (4 mo / 4 wk / 1 mo) to start from today",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )
            Switch(
                checked = resetAnchors,
                onCheckedChange = onSetResetAnchors
            )
        }
        Button(
            onClick = onCompleteStep6,
            enabled = c1 && c2 && c3,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Complete Step 6 & Schedule 24h Filter Clean")
        }
    }
}

@Composable
private fun Step7FilterClean24h(
    session: StartupSessionEntity,
    flags: Map<String, Boolean>,
    onSetFlag: (String, Boolean) -> Unit,
    onCompleteStep7: () -> Unit
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

    val dueAt = session.filterCleanDueAt
    val formatter = remember {
        DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(ZoneId.systemDefault())
    }
    val statusText = if (dueAt == null) {
        "Complete Step 6 first to schedule the 24-hour filter rinse."
    } else {
        val diff = dueAt.toEpochMilli() - nowMillis
        if (diff <= 0L) {
            "24 hours elapsed (${formatter.format(dueAt)}) — Ready to rinse filter!"
        } else {
            val hours = diff / 3_600_000L
            val mins = (diff % 3_600_000L) / 60_000L
            "Scheduled for ${formatter.format(dueAt)} (${hours}h ${mins}m remaining)"
        }
    }

    val c1 = flags["s7_jets_off"] == true
    val c2 = flags["s7_hose_rinse"] == true

    StepCard(
        stepNumber = 7,
        title = "Clean Filter After 24 Hours",
        badges = listOf(SafetyBadge.JETS_OFF, SafetyBadge.GARDEN_HOSE)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(14.dp)
            )
        }
        ChecklistItemRow(
            label = "Ensure jets are OFF (NOT running)",
            critical = true,
            checked = c1,
            onCheckedChange = { onSetFlag("s7_jets_off", it) }
        )
        ChecklistItemRow(
            label = "Rinse filter pleats thoroughly with garden hose & reinstall",
            checked = c2,
            onCheckedChange = { onSetFlag("s7_hose_rinse", it) }
        )
        Button(
            onClick = onCompleteStep7,
            enabled = c1 && c2 && dueAt != null,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Complete 24h Filter Clean & Finish Startup")
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: Int,
    title: String,
    badges: List<SafetyBadge>,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "STEP $stepNumber OF 7",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            if (badges.isNotEmpty()) {
                SafetyBadgeRow(badges = badges)
            }
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartupStep4Preview() {
    val reading = tesler.will.hottubmanager.domain.chemistry.WaterReadingInput(totalAlkalinityPpm = 60)
    HotTubTheme {
        StartupWizardContent(
            uiState = StartupUiState(
                activeSession = StartupSessionEntity(
                    id = 1,
                    fillCycleId = 1,
                    startedAt = Instant.now(),
                    currentStep = 4
                ),
                selectedStep = 4,
                step4Input = Step4InputState(taText = "60"),
                step4Advice = tesler.will.hottubmanager.domain.chemistry.WaterBalanceAdvisor().evaluate(reading)
            ),
            contentPadding = PaddingValues(0.dp),
            onStartFreshSession = {},
            onSelectStep = {},
            onSetFlag = { _, _ -> },
            onAdvanceFromStep = {},
            onTaChange = {},
            onPhChange = {},
            onChChange = {},
            onManualOverrideChange = {},
            onLogStep4 = {},
            onSetResetAnchors = {},
            onCompleteStep6 = {},
            onCompleteStep7 = {},
            onStartTimer = {},
            onCancelTimer = {},
            onOpenTimers = {}
        )
    }
}
