package com.msbytes.statussaver.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Enables the 15-minute check only while a feature needs it. */
    fun sync(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<StatusCheckWorker>(15, TimeUnit.MINUTES).build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }

    private companion object {
        const val WORK_NAME = "status_check"
    }
}
