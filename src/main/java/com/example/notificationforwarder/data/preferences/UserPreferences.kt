package com.example.notificationforwarder.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _prefsFlow = MutableStateFlow(loadPreferences())
    val prefsFlow: Flow<EmailConfig> = _prefsFlow.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _prefsFlow.value = loadPreferences()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun loadPreferences(): EmailConfig {
        return EmailConfig(
            emailAddress = prefs.getString("email_address", "") ?: "",
            smtpServer = prefs.getString("smtp_server", "") ?: "",
            smtpPort = prefs.getString("smtp_port", "587")?.toIntOrNull() ?: 587,
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: "",
            forwardAll = prefs.getBoolean("forward_all", true),
            excludedApps = prefs.getString("excluded_apps", "")?.split(",")?.map { it.trim() }
                ?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }

    fun shouldForwardNotification(packageName: String): Boolean {
        val config = loadPreferences()
        return if (config.forwardAll) {
            !config.excludedApps.contains(packageName)
        } else {
            config.excludedApps.contains(packageName)
        }
    }

    fun isConfigured(): Boolean {
        val config = loadPreferences()
        return config.emailAddress.isNotEmpty() &&
                config.smtpServer.isNotEmpty() &&
                config.username.isNotEmpty() &&
                config.password.isNotEmpty()
    }
}

data class EmailConfig(
    val emailAddress: String,
    val smtpServer: String,
    val smtpPort: Int,
    val username: String,
    val password: String,
    val forwardAll: Boolean,
    val excludedApps: List<String>
) 