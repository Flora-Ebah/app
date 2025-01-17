package com.example.notificationforwarder.ui.adapter

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationforwarder.data.model.NotificationEntity
import com.example.notificationforwarder.databinding.ItemNotificationBinding

class NotificationsAdapter(
    private val onResendClick: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotificationsAdapter.ViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationEntity) {
            binding.apply {
                appNameTextView.text = notification.appName
                titleTextView.text = notification.title
                contentTextView.text = notification.content
                timestampTextView.text = DateUtils.getRelativeTimeSpanString(
                    notification.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )

                // Charger l'icône de l'application
                try {
                    val packageManager = root.context.packageManager
                    val icon = packageManager.getApplicationIcon(notification.packageName)
                    appIconView.setImageDrawable(icon)
                } catch (e: PackageManager.NameNotFoundException) {
                    appIconView.setImageResource(android.R.mipmap.sym_def_app_icon)
                }

                resendButton.setOnClickListener {
                    onResendClick(notification)
                }
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
        override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity): Boolean {
            return oldItem == newItem
        }
    }
} 