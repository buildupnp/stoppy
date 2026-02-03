package com.lifeforge.app.data.repository

import com.lifeforge.app.ui.screens.notifications.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor() {
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()
    
    // Simple in-memory unread count for now
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        // Load initial dummy data (Simulate "Real" notifications)
        _notifications.value = listOf(
            NotificationItem("Welcome to LifeForge", "Start your journey by setting up your Guardian.", "👋", "Just now", "General"),
            NotificationItem("Daily Streak Active", "You are on a 1-day streak!", "🔥", "Today", "Streak")
        )
        _unreadCount.value = 2
    }
    
    fun markAllRead() {
        _unreadCount.value = 0
    }
    
    fun addNotification(item: NotificationItem) {
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, item)
        _notifications.value = currentList
        _unreadCount.value += 1
    }
}
