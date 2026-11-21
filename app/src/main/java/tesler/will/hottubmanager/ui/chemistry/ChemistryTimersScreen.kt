package tesler.will.hottubmanager.ui.chemistry

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tesler.will.hottubmanager.data.db.entity.WaterTestEntity
import tesler.will.hottubmanager.domain.model.TimerPreset
import tesler.will.hottubmanager.ui.components.ActiveTimerBanner
import tesler.will.hottubmanager.ui.components.RangeMeterBar
import tesler.will.hottubmanager.ui.theme.HotTubTheme
import tesler.will.hottubmanager.ui.theme.LocalSpaColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChemistryRouteScreen(
    viewModel: ChemistryViewModel,
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChemistryTimersContent(
        uiState = uiState,
        contentPadding = contentPadding,
        onStartTimer = viewModel::startTimer,
        onCancelTimer = viewModel::cancelTimer,
        onTaChange = viewModel::updateTa,
        onPhChange = viewModel::updatePh,
        onChChange = viewModel::updateCh,
        onChlorineChange = viewModel::updateChlorine,
        onTempChange = viewModel::updateTemp,
        onChemicalsChange = viewModel::updateChemicalsAdded,
        onNotesChange = viewModel::updateNotes,
        onSaveTest = viewModel::saveWaterTest
    )
}

@Composable
fun ChemistryTimersContent(
    uiState: ChemistryUiState,
    contentPadding: PaddingValues,
    onStartTimer: (TimerPreset) -> Unit,
    onCancelTimer: () -> Unit,
    onTaChange: (String) -> Unit,
    onPhChange: (String) -> Unit,
    onChChange: (String) -> Unit,
    onChlorineChange: (String) -> Unit,
    onTempChange: (String) -> Unit,
    onChemicalsChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveTest: (Boolean) -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a").withZone(ZoneId.systemDefault())
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
                text = "Water Chemistry & Timers",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        uiState.activeTimer?.let { timer ->
            item {
                ActiveTimerBanner(
                    timer = timer,
                    onCancel = onCancelTimer,
                    onClick = {}
                )
            }
        }

        item {
            QuickTimersCard(onStartTimer = onStartTimer)
        }

        item {
            WaterTestLoggerCard(
                uiState = uiState,
                onTaChange = onTaChange,
                onPhChange = onPhChange,
                onChChange = onChChange,
                onChlorineChange = onChlorineChange,
                onTempChange = onTempChange,
                onChemicalsChange = onChemicalsChange,
                onNotesChange = onNotesChange,
                onSaveTest = onSaveTest
            )
        }

        item {
            Text(
                text = "Chemistry History",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (uiState.tests.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No water tests logged yet. Dip a strip above to record your first reading.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            val grouped = uiState.tests.groupBy { it.fillCycleId }
            grouped.forEach { (cycleId, cycleTests) ->
                val cycle = cycleId?.let { uiState.fillCyclesById[it] }
                item(key = "cycle_header_$cycleId") {
                    Text(
                        text = cycle?.let { "Fill Cycle #${it.cycleNumber}" } ?: "General Readings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(
                    items = cycleTests,
                    key = { it.id }
                ) { test ->
                    WaterTestHistoryCard(test = test, formatter = dateFormatter)
                }
            }
        }
    }
}

@Composable
private fun QuickTimersCard(onStartTimer: (TimerPreset) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Quick-Launch Timers", style = MaterialTheme.typography.titleMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onStartTimer(TimerPreset.CHEMICAL_WAIT_15M) },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("15m Chem Mix")
                }
                FilledTonalButton(
                    onClick = { onStartTimer(TimerPreset.MONTHLY_SHOCK_20M) },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("20m Shock")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onStartTimer(TimerPreset.STARTUP_SHOCK_30M) },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("30m Startup")
                }
                OutlinedButton(
                    onClick = { onStartTimer(TimerPreset.FILTER_SAVE_15S) },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("15s F1/F2 Save")
                }
            }
        }
    }
}

@Composable
private fun WaterTestLoggerCard(
    uiState: ChemistryUiState,
    onTaChange: (String) -> Unit,
    onPhChange: (String) -> Unit,
    onChChange: (String) -> Unit,
    onChlorineChange: (String) -> Unit,
    onTempChange: (String) -> Unit,
    onChemicalsChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveTest: (Boolean) -> Unit
) {
    val input = uiState.input
    val advice = uiState.advice

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row {
                Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Test Strip Logger & Dosing Assistant", style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input.taText,
                    onValueChange = onTaChange,
                    label = { Text("TA (ppm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = input.phText,
                    onValueChange = onPhChange,
                    label = { Text("pH") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = input.chText,
                    onValueChange = onChChange,
                    label = { Text("CH (ppm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input.chlorineText,
                    onValueChange = onChlorineChange,
                    label = { Text("Free Cl (ppm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = input.tempText,
                    onValueChange = onTempChange,
                    label = { Text("Temp (°F)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            RangeMeterBar(
                label = "1. Total Alkalinity",
                valueText = input.taText.ifBlank { "--" } + " ppm",
                targetText = "80–120",
                status = advice.taStatus,
                fractionInScale = input.taText.toFloatOrNull()?.let { it / 200f }
            )
            RangeMeterBar(
                label = "2. pH",
                valueText = input.phText.ifBlank { "--" },
                targetText = "7.2–7.8",
                status = advice.phStatus,
                fractionInScale = input.phText.toFloatOrNull()?.let { (it - 6.2f) / 2.4f }
            )
            RangeMeterBar(
                label = "3. Calcium Hardness",
                valueText = input.chText.ifBlank { "--" } + " ppm",
                targetText = "150–250",
                status = advice.chStatus,
                fractionInScale = input.chText.toFloatOrNull()?.let { it / 400f }
            )

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
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(advice.headline, style = MaterialTheme.typography.titleMedium)
                    if (advice.chemicalName != null && advice.doseText != null) {
                        Text(
                            text = "Strict-Order Dose: ${advice.doseText} of ${advice.chemicalName}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(advice.instructions, style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedTextField(
                value = input.chemicalsAdded,
                onValueChange = onChemicalsChange,
                label = { Text("Chemicals added (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = input.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            input.savedFeedback?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalSpaColors.current.good
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSaveTest(false) },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Save Reading")
                }
                if (advice.needs15MinWaitTimer) {
                    FilledTonalButton(
                        onClick = { onSaveTest(true) },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Save & 15m Timer")
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterTestHistoryCard(
    test: WaterTestEntity,
    formatter: DateTimeFormatter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = formatter.format(test.testedAt),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val metrics = buildList {
                test.totalAlkalinityPpm?.let { add("TA: $it ppm") }
                test.ph?.let { add("pH: $it") }
                test.calciumHardnessPpm?.let { add("CH: $it ppm") }
                test.freeChlorinePpm?.let { add("Cl: $it ppm") }
                test.waterTempF?.let { add("$it°F") }
            }.joinToString("  •  ")
            if (metrics.isNotBlank()) {
                Text(metrics, style = MaterialTheme.typography.titleMedium)
            }
            if (test.chemicalsAddedSummary.isNotBlank()) {
                HorizontalDivider()
                Text(
                    text = "Added: ${test.chemicalsAddedSummary}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            if (test.notes.isNotBlank()) {
                Text(
                    text = test.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChemistryScreenPreview() {
    val reading = tesler.will.hottubmanager.domain.chemistry.WaterReadingInput(
        totalAlkalinityPpm = 60,
        ph = 7.5,
        calciumHardnessPpm = 180
    )
    HotTubTheme {
        ChemistryTimersContent(
            uiState = ChemistryUiState(
                input = ChemistryInputState(taText = "60", phText = "7.5", chText = "180"),
                advice = tesler.will.hottubmanager.domain.chemistry.WaterBalanceAdvisor()
                    .evaluate(reading, manualOverrideOrder = true),
                tests = listOf(
                    WaterTestEntity(
                        id = 1,
                        testedAt = Instant.now(),
                        totalAlkalinityPpm = 90,
                        ph = 7.4,
                        calciumHardnessPpm = 190,
                        freeChlorinePpm = 2.5,
                        waterTempF = 102,
                        chemicalsAddedSummary = "3 tablespoons Alkalinity Increaser"
                    )
                )
            ),
            contentPadding = PaddingValues(0.dp),
            onStartTimer = {},
            onCancelTimer = {},
            onTaChange = {},
            onPhChange = {},
            onChChange = {},
            onChlorineChange = {},
            onTempChange = {},
            onChemicalsChange = {},
            onNotesChange = {},
            onSaveTest = {}
        )
    }
}
