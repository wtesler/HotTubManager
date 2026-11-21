package tesler.will.hottubmanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object StartupRoute : NavKey

@Serializable
data object ChemistryRoute : NavKey

@Serializable
data object GuideRoute : NavKey

@Serializable
data object HistoryRoute : NavKey

data class TopLevelTab(
    val route: NavKey,
    val label: String,
    val icon: ImageVector
)

val TOP_LEVEL_TABS: List<TopLevelTab> = listOf(
    TopLevelTab(
        route = DashboardRoute,
        label = "Dashboard",
        icon = Icons.Default.Home
    ),
    TopLevelTab(
        route = StartupRoute,
        label = "Startup",
        icon = Icons.Default.Build
    ),
    TopLevelTab(
        route = ChemistryRoute,
        label = "Chemistry",
        icon = Icons.Default.Science
    ),
    TopLevelTab(
        route = GuideRoute,
        label = "Guide",
        icon = Icons.AutoMirrored.Filled.MenuBook
    )
)
