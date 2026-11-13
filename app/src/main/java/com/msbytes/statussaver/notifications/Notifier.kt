package com.msbytes.statussaver.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.msbytes.statussaver.MainActivity
import com.msbytes.statussaver.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_NEW, context.getString(R.string.channel_new_statuses), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = context.getString(R.string.channel_new_statuses_desc) },
                NotificationChannel(CHANNEL_AUTO_SAVE, context.getString(R.string.channel_auto_save), NotificationManager.IMPORTANCE_LOW)
                    .apply { description = context.getString(R.string.channel_auto_save_desc) },
            ),
        )
    }

    fun canNotify(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showNewStatuses(count: Int) = show(
        id = ID_NEW,
        channel = CHANNEL_NEW,
        title = context.resources.getQuantityString(R.plurals.notif_new_statuses, count, count),
    )

    fun showAutoSaved(count: Int) = show(
        id = ID_AUTO_SAVE,
        channel = CHANNEL_AUTO_SAVE,
        title = context.resources.getQuantityString(R.plurals.notif_auto_saved, count, count),
    )

    private fun show(id: Int, channel: String, title: String) {
        if (!canNotify()) return
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_tap_to_view))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call — ignore.
        }
    }

    private companion object {
        const val CHANNEL_NEW = "new_statuses"
        const val CHANNEL_AUTO_SAVE = "auto_save"
        const val ID_NEW = 1001
        const val ID_AUTO_SAVE = 1002
    }
}
