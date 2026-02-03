package com.lifeforge.app.ui.screens.guardian

data class ManagedApp(
    val id: String,
    val name: String,
    val packageName: String,
    val isBlocked: Boolean,
    val isCurrentlyUnlocked: Boolean = false,
    val unlockExpiresAt: Long? = null
)
