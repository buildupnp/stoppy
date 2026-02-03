package com.lifeforge.app.data.model

data class DailyQuest(
    val id: String,
    val title: String,
    val description: String,
    val reward: Int,
    val progress: Float, // 0.0 to 1.0
    val isCompleted: Boolean,
    val type: String = "generic"
)
