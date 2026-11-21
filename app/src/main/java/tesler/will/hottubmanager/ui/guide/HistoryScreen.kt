package tesler.will.hottubmanager.ui.guide

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tesler.will.hottubmanager.domain.ls450dx.Ls450dxSpecs
import tesler.will.hottubmanager.domain.model.TaskType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryRouteScreen(
    viewModel: GuideViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a").withZone(ZoneId.systemDefault())
    }
    val cyclesById = remember(uiState.fillCycles) {
        uiState.fillCycles.associateBy { it.id }
    }

    val filteredLogs = remember(uiState.allLogs, uiState.historySearchQuery, uiState.historyFilterType) {
        val q = uiState.historySearchQuery.trim().lowercase()
        uiState.allLogs.filter { log ->
            val typeOk = uiState.historyFilterType == null || log.taskType == uiState.historyFilterType
            val spec = Ls450dxSpecs.specFor(log.taskType)
            val queryOk = q.isEmpty() ||
                spec.title.lowercase().contains(q) ||
                log.notes.lowercase().contains(q)
            typeOk && queryOk
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        TopAppBar(
            title = { Text("Maintenance History") },
            windowInsets = WindowInsets(0),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.historySearchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search tasks or notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.historyFilterType == null,
                        onClick = { viewModel.setFilterType(null) },
                        label = { Text("All") }
                    )
                    TaskType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.historyFilterType == type,
                            onClick = { viewModel.setFilterType(type) },
                            label = { Text(Ls450dxSpecs.specFor(type).shortTitle) }
                        )
                    }
                }
            }

            if (filteredLogs.isEmpty()) {
                item {
                    Text(
                        text = "No matching maintenance history entries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                items(
                    items = filteredLogs,
                    key = { it.id }
                ) { log ->
                    val spec = Ls450dxSpecs.specFor(log.taskType)
                    val cycleText = log.fillCycleId?.let { cyclesById[it]?.cycleNumber }?.let { "Fill Cycle #$it" }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(spec.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = listOfNotNull(
                                    dateFormatter.format(log.completedAt),
                                    cycleText
                                ).joinToString("  •  "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (log.notes.isNotBlank()) {
                                Text(log.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
