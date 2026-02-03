package com.lifeforge.app.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class FeedbackDto(
    val userId: String,
    val message: String,
    val type: String = "general",
    val appVersion: String = "1.0.0"
)

@Singleton
class FeedbackRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    suspend fun sendFeedback(userId: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest["feedback"].insert(
                FeedbackDto(
                    userId = userId,
                    message = message
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
