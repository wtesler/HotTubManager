package tesler.will.hottubmanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tesler.will.hottubmanager.domain.model.TaskType
import tesler.will.hottubmanager.scheduling.NotificationHelper
import tesler.will.hottubmanager.ui.navigation.DeepLinkRequest
import tesler.will.hottubmanager.ui.navigation.HotTubAppRoot
import tesler.will.hottubmanager.ui.theme.HotTubTheme

class MainActivity : ComponentActivity() {

    private var pendingDeepLink by mutableStateOf<DeepLinkRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        val appContainer = (application as HotTubApplication).container
        setContent {
            HotTubTheme {
                HotTubAppRoot(
                    container = appContainer,
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val navTarget = intent?.getStringExtra(NotificationHelper.EXTRA_NAV_TARGET) ?: return
        val taskTypeName = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TYPE)
        val taskType = taskTypeName?.let { runCatching { TaskType.valueOf(it) }.getOrNull() }
        pendingDeepLink = DeepLinkRequest(navTarget = navTarget, taskType = taskType)
    }
}
