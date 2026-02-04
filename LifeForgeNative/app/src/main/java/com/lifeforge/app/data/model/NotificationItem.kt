package com.lifeforge.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val createdAtEpochMs: Long,
    val category: String = "General",
    val isRead: Boolean = false
)

