package com.example.notificationforwarder.data.repository

import com.example.notificationforwarder.data.dao.NotificationDao
import com.example.notificationforwarder.data.model.NotificationEntity
import com.example.notificationforwarder.service.email.EmailService
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class NotificationRepository(
    private val notificationDao: NotificationDao,
    private val emailService: EmailService
) {
    fun getAllNotifications(): Flow<List<NotificationEntity>> {
        return notificationDao.getAllNotifications()
    }

    fun getPendingNotifications(): Flow<List<NotificationEntity>> {
        return notificationDao.getPendingNotifications()
    }

    suspend fun insertNotification(notification: NotificationEntity) {
        notificationDao.insert(notification)
    }

    suspend fun markNotificationAsSent(notification: NotificationEntity) {
        notificationDao.update(notification.copy(isEmailSent = true))
    }

    suspend fun cleanOldNotifications(days: Int) {
        val timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        notificationDao.deleteOldNotifications(timestamp)
    }

    suspend fun sendEmailNotification(notification: NotificationEntity, recipientEmail: String) {
        val subject = "Notification de ${notification.appName}"
        val content = """
            Application: ${notification.appName}
            Titre: ${notification.title}
            Message: ${notification.content}
            Date: ${java.util.Date(notification.timestamp)}
        """.trimIndent()

        emailService.sendEmail(recipientEmail, subject, content)
        markNotificationAsSent(notification)
    }
} 