package com.example.notificationforwarder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notificationforwarder.data.model.NotificationEntity
import com.example.notificationforwarder.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            repository.getAllNotifications()
                .catch { e ->
                    _error.value = "Erreur lors du chargement des notifications: ${e.message}"
                }
                .collect { notifications ->
                    _notifications.value = notifications
                }
        }
    }

    fun resendNotification(notification: NotificationEntity, recipientEmail: String) {
        viewModelScope.launch {
            try {
                repository.sendEmailNotification(notification, recipientEmail)
            } catch (e: Exception) {
                _error.value = "Erreur lors de l'envoi de l'email: ${e.message}"
            }
        }
    }

    fun clearOldNotifications(days: Int) {
        viewModelScope.launch {
            try {
                repository.cleanOldNotifications(days)
            } catch (e: Exception) {
                _error.value = "Erreur lors du nettoyage des notifications: ${e.message}"
            }
        }
    }

    class Factory(private val repository: NotificationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 