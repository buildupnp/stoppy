package com.lifeforge.app.data.repository

import android.content.Context
import com.lifeforge.app.data.model.NotificationItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("lifeforge_notifications_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _notifications = MutableStateFlow(loadNotifications())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(_notifications.value.count { !it.isRead })
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun markAllRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        persist(updated)
        _unreadCount.value = 0
    }

    fun addNotification(
        title: String,
        description: String,
        icon: String,
        category: String
    ) {
        val item = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            icon = icon,
            createdAtEpochMs = System.currentTimeMillis(),
            category = category,
            isRead = false
        )
        addNotification(item)
    }

    fun addNotification(item: NotificationItem) {
        val current = _notifications.value.toMutableList()
        current.add(0, item)

        val trimmed = current.take(50)
        _notifications.value = trimmed
        persist(trimmed)
        _unreadCount.value = trimmed.count { !it.isRead }
    }

    /** Adds a notification once per unique key (anti-spam). */
    fun addOnce(key: String, item: NotificationItem) {
        val flagKey = "once_$key"
        if (prefs.getBoolean(flagKey, false)) return
        prefs.edit().putBoolean(flagKey, true).apply()
        addNotification(item)
    }

    /** Adds a notification only if enough time passed since last time for this key (anti-spam). */
    fun addDebounced(key: String, minIntervalMs: Long, item: NotificationItem) {
        val tsKey = "debounce_$key"
        val now = System.currentTimeMillis()
        val last = prefs.getLong(tsKey, 0L)
        if (now - last < minIntervalMs) return
        prefs.edit().putLong(tsKey, now).apply()
        addNotification(item)
    }

    private fun loadNotifications(): List<NotificationItem> {
        val raw = prefs.getString("notifications_json", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<NotificationItem>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<NotificationItem>) {
        val raw = try {
            json.encodeToString(list)
        } catch (_: Exception) {
            "[]"
        }
        prefs.edit().putString("notifications_json", raw).apply()
    }
}
