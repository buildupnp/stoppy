package com.lifeforge.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<NotificationItem>> = notificationRepository.notifications
    val unreadCount: StateFlow<Int> = notificationRepository.unreadCount

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllRead()
        }
    }
    
    fun addNotification(title: String, description: String, icon: String, category: String) {
        viewModelScope.launch {
            val item = NotificationItem(
                title = title,
                description = description,
                icon = icon,
                time = "Just now",
                category = category
            )
            notificationRepository.addNotification(item)
        }
    }
}
