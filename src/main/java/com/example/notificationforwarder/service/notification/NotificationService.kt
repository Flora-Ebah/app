package com.example.notificationforwarder.service.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notificationforwarder.data.database.AppDatabase
import com.example.notificationforwarder.data.model.NotificationEntity
import com.example.notificationforwarder.data.preferences.UserPreferences
import com.example.notificationforwarder.service.email.EmailService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var database: AppDatabase
    private lateinit var userPreferences: UserPreferences
    private lateinit var emailService: EmailService

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        userPreferences = UserPreferences(this)
        emailService = EmailService()

        // Configuration initiale du service email
        serviceScope.launch {
            userPreferences.prefsFlow.collect { config ->
                emailService.configure(
                    host = config.smtpServer,
                    port = config.smtpPort,
                    username = config.username,
                    password = config.password
                )
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!shouldProcessNotification(sbn)) return

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val appName = getApplicationName(sbn.packageName)

        val notificationEntity = NotificationEntity(
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            content = text,
            timestamp = System.currentTimeMillis()
        )

        serviceScope.launch {
            database.notificationDao().insert(notificationEntity)
            if (userPreferences.isConfigured()) {
                try {
                    userPreferences.prefsFlow.collect { config ->
                        emailService.sendEmail(
                            to = config.emailAddress,
                            subject = "Notification de $appName",
                            content = """
                                Application: $appName
                                Titre: $title
                                Message: $text
                            """.trimIndent()
                        )
                        database.notificationDao().update(notificationEntity.copy(isEmailSent = true))
                    }
                } catch (e: Exception) {
                    // TODO: Gérer les erreurs d'envoi d'email
                }
            }
        }
    }

    private fun shouldProcessNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.isOngoing) return false
        if (sbn.packageName == packageName) return false
        return userPreferences.shouldForwardNotification(sbn.packageName)
    }

    private fun getApplicationName(packageName: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
} 