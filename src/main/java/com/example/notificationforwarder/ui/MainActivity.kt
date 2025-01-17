package com.example.notificationforwarder.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notificationforwarder.R
import com.example.notificationforwarder.data.database.AppDatabase
import com.example.notificationforwarder.data.preferences.UserPreferences
import com.example.notificationforwarder.data.repository.NotificationRepository
import com.example.notificationforwarder.databinding.ActivityMainBinding
import com.example.notificationforwarder.service.email.EmailService
import com.example.notificationforwarder.ui.adapter.NotificationsAdapter
import com.example.notificationforwarder.ui.settings.SettingsActivity
import com.example.notificationforwarder.util.AppError
import com.example.notificationforwarder.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import android.view.animation.AnimationUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationsAdapter: NotificationsAdapter
    private lateinit var userPreferences: UserPreferences

    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = NotificationRepository(database.notificationDao(), EmailService())
        MainViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        userPreferences = UserPreferences(this)
        setupRecyclerView()
        setupFab()
        checkNotificationListenerPermission()
        setupObservers()
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter { notification ->
            if (!userPreferences.isConfigured()) {
                showConfigurationError()
                return@NotificationsAdapter
            }
            viewModel.resendNotification(notification, userPreferences.loadPreferences().emailAddress)
        }
        
        binding.notificationsRecyclerView.apply {
            adapter = notificationsAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.notifications.collect { notifications ->
                notificationsAdapter.submitList(notifications)
                updateViewVisibility(notifications.isEmpty())
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let { handleError(it) }
            }
        }
    }

    private fun handleError(error: AppError) {
        val message = when (error) {
            is AppError.ConfigurationError -> getString(R.string.error_configuration)
            is AppError.EmailError -> getString(R.string.error_email_sending)
            is AppError.NetworkError -> getString(R.string.error_network)
            is AppError.DatabaseError -> getString(R.string.error_database)
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showConfigurationError() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error_configuration_title)
            .setMessage(R.string.error_configuration_message)
            .setPositiveButton(R.string.settings) { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun checkNotificationListenerPermission() {
        if (!isNotificationServiceEnabled()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.notification_permission_message)
                .setPositiveButton(R.string.settings) { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_clear -> {
                showClearDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_notifications)
            .setMessage(R.string.clear_notifications_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearOldNotifications(7) // Nettoie les notifications de plus de 7 jours
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateViewVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            binding.notificationsRecyclerView.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.fade_out)
            )
            binding.emptyState.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.fade_in)
            )
        } else {
            binding.emptyState.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.fade_out)
            )
            binding.notificationsRecyclerView.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.fade_in)
            )
        }
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.notificationsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
} 