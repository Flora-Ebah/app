package com.example.notificationforwarder.service.notification

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notificationforwarder.data.database.AppDatabase
import com.example.notificationforwarder.data.model.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListenerServiceImpl : NotificationListenerService() {
    private lateinit var database: AppDatabase
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras

        val packageName = sbn.packageName
        val appName = try {
            packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        if (shouldProcessNotification(packageName)) {
            val notificationEntity = NotificationEntity(
                packageName = packageName,
                appName = appName,
                title = title,
                content = text,
                timestamp = System.currentTimeMillis()
            )

            serviceScope.launch {
                database.notificationDao().insert(notificationEntity)
            }
        }
    }

    private fun shouldProcessNotification(packageName: String): Boolean {
        // TODO: Implémenter la logique de filtrage des notifications
        return true
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optionnel : gérer la suppression des notifications
    }
} 