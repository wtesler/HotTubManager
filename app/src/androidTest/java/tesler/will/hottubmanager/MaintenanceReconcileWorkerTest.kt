package tesler.will.hottubmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import tesler.will.hottubmanager.scheduling.MaintenanceReconcileWorker

@RunWith(AndroidJUnit4::class)
class MaintenanceReconcileWorkerTest {

    @Test
    fun workerCompletesSuccessfully() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<MaintenanceReconcileWorker>(context).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
