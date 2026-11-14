package com.msbytes.statussaver.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodic fallback (~15 min) for when the live service isn't running. */
@HiltWorker
class StatusCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val runner: StatusSyncRunner,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runCatching { runner.sync() }
        return Result.success()
    }
}
