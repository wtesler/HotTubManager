package tesler.will.hottubmanager.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import tesler.will.hottubmanager.di.AppContainer
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.scheduling.NotificationHelper
import tesler.will.hottubmanager.ui.AppViewModelFactory
import tesler.will.hottubmanager.ui.chemistry.ChemistryRouteScreen
import tesler.will.hottubmanager.ui.chemistry.ChemistryViewModel
import tesler.will.hottubmanager.ui.dashboard.DashboardRouteScreen
import tesler.will.hottubmanager.ui.dashboard.DashboardViewModel
import tesler.will.hottubmanager.ui.guide.GuideRouteScreen
import tesler.will.hottubmanager.ui.guide.GuideViewModel
import tesler.will.hottubmanager.ui.guide.HistoryRouteScreen
import tesler.will.hottubmanager.ui.onboarding.OnboardingRouteScreen
import tesler.will.hottubmanager.ui.onboarding.OnboardingViewModel
import tesler.will.hottubmanager.ui.startup.StartupRouteScreen
import tesler.will.hottubmanager.ui.startup.StartupViewModel

data class DeepLinkRequest(
    val navTarget: String,
    val taskType: TaskType? = null
)

class NavigationState(
    val startRoute: NavKey,
    topLevelRouteState: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRouteState

    @Composable
    fun toDecoratedEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>
    ): List<NavEntry<NavKey>> {
        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>()
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider
            )
        }
        return getTopLevelRoutesInUse().flatMap { decoratedEntries[it] ?: emptyList() }
    }

    private fun getTopLevelRoutesInUse(): List<NavKey> =
        if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

class Navigator(val state: NavigationState) {
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return
        val currentRoute = currentStack.lastOrNull()
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}

@Composable
fun rememberHotTubNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val tabIndex = rememberSaveable { mutableStateOf(0) }
    val routesList = remember(topLevelRoutes) { topLevelRoutes.toList() }
    val topLevelRouteState = remember(routesList) {
        object : MutableState<NavKey> {
            override var value: NavKey
                get() = routesList.getOrElse(tabIndex.value) { startRoute }
                set(value) {
                    val idx = routesList.indexOf(value)
                    if (idx >= 0) tabIndex.value = idx
                }
            override fun component1(): NavKey = value
            override fun component2(): (NavKey) -> Unit = { value = it }
        }
    }
    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }
    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRouteState = topLevelRouteState,
            backStacks = backStacks
        )
    }
}

@Composable
fun HotTubAppRoot(
    container: AppContainer,
    pendingDeepLink: DeepLinkRequest?,
    onDeepLinkConsumed: () -> Unit
) {
    val prefs by container.preferencesStore.preferencesFlow.collectAsStateWithLifecycle(initialValue = null)

    val currentPrefs = prefs
    if (currentPrefs == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var startTabOverride by remember { mutableStateOf<NavKey?>(null) }

    if (!currentPrefs.onboardingCompleted) {
        val onboardingViewModel: OnboardingViewModel = viewModel(
            factory = AppViewModelFactory {
                OnboardingViewModel(
                    preferencesStore = container.preferencesStore,
                    maintenanceRepository = container.maintenanceRepository,
                    startupRepository = container.startupRepository,
                    scheduleCalculator = container.scheduleCalculator
                )
            }
        )
        OnboardingRouteScreen(
            viewModel = onboardingViewModel,
            onCompletedFreshFill = { startTabOverride = StartupRoute },
            onCompletedExistingTub = { startTabOverride = DashboardRoute }
        )
        return
    }

    HotTubMainShell(
        container = container,
        initialTab = startTabOverride,
        onClearInitialTab = { startTabOverride = null },
        pendingDeepLink = pendingDeepLink,
        onDeepLinkConsumed = onDeepLinkConsumed
    )
}

@Composable
private fun HotTubMainShell(
    container: AppContainer,
    initialTab: NavKey?,
    onClearInitialTab: () -> Unit,
    pendingDeepLink: DeepLinkRequest?,
    onDeepLinkConsumed: () -> Unit
) {
    val topLevelKeys = remember { TOP_LEVEL_TABS.map { it.route }.toSet() }
    val navigationState = rememberHotTubNavigationState(
        startRoute = DashboardRoute,
        topLevelRoutes = topLevelKeys
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    var deepLinkTaskType by remember { mutableStateOf<TaskType?>(null) }

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            navigator.navigate(initialTab)
            onClearInitialTab()
        }
    }

    LaunchedEffect(pendingDeepLink) {
        if (pendingDeepLink != null) {
            when (pendingDeepLink.navTarget) {
                NotificationHelper.NAV_STARTUP -> navigator.navigate(StartupRoute)
                NotificationHelper.NAV_CHEMISTRY -> navigator.navigate(ChemistryRoute)
                NotificationHelper.NAV_GUIDE -> navigator.navigate(GuideRoute)
                else -> {
                    navigator.navigate(DashboardRoute)
                    deepLinkTaskType = pendingDeepLink.taskType
                }
            }
            onDeepLinkConsumed()
        }
    }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = AppViewModelFactory {
            DashboardViewModel(
                maintenanceRepository = container.maintenanceRepository,
                startupRepository = container.startupRepository,
                timerManager = container.timerManager,
                scheduleCalculator = container.scheduleCalculator
            )
        }
    )
    val startupViewModel: StartupViewModel = viewModel(
        factory = AppViewModelFactory {
            StartupViewModel(
                startupRepository = container.startupRepository,
                chemistryRepository = container.chemistryRepository,
                timerManager = container.timerManager,
                waterBalanceAdvisor = container.waterBalanceAdvisor
            )
        }
    )
    val chemistryViewModel: ChemistryViewModel = viewModel(
        factory = AppViewModelFactory {
            ChemistryViewModel(
                chemistryRepository = container.chemistryRepository,
                maintenanceRepository = container.maintenanceRepository,
                timerManager = container.timerManager,
                waterBalanceAdvisor = container.waterBalanceAdvisor
            )
        }
    )
    val guideViewModel: GuideViewModel = viewModel(
        factory = AppViewModelFactory {
            GuideViewModel(
                preferencesStore = container.preferencesStore,
                maintenanceRepository = container.maintenanceRepository,
                alarmScheduler = container.alarmScheduler,
                notificationHelper = container.notificationHelper
            )
        }
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar {
                TOP_LEVEL_TABS.forEach { tab ->
                    val isSelected = tab.route == navigationState.topLevelRoute
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { navigator.navigate(tab.route) },
                        icon = {
                            Icon(imageVector = tab.icon, contentDescription = tab.label)
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val entriesProvider = entryProvider {
            entry<DashboardRoute> {
                DashboardRouteScreen(
                    viewModel = dashboardViewModel,
                    initialSelectedTaskType = deepLinkTaskType,
                    onClearInitialTask = { deepLinkTaskType = null },
                    onNavigateToStartup = { navigator.navigate(StartupRoute) },
                    onNavigateToChemistry = { navigator.navigate(ChemistryRoute) },
                    contentPadding = innerPadding
                )
            }
            entry<StartupRoute> {
                StartupRouteScreen(
                    viewModel = startupViewModel,
                    onNavigateToChemistry = { navigator.navigate(ChemistryRoute) },
                    onStartupFinished = { navigator.navigate(DashboardRoute) },
                    contentPadding = innerPadding
                )
            }
            entry<ChemistryRoute> {
                ChemistryRouteScreen(
                    viewModel = chemistryViewModel,
                    contentPadding = innerPadding
                )
            }
            entry<GuideRoute> {
                GuideRouteScreen(
                    viewModel = guideViewModel,
                    onOpenFullHistory = { navigator.navigate(HistoryRoute) },
                    contentPadding = innerPadding
                )
            }
            entry<HistoryRoute> {
                HistoryRouteScreen(
                    viewModel = guideViewModel,
                    onBack = { navigator.goBack() },
                    contentPadding = innerPadding
                )
            }
        }

        NavDisplay(
            entries = navigationState.toDecoratedEntries(entriesProvider),
            onBack = { navigator.goBack() },
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
        )
    }
}
