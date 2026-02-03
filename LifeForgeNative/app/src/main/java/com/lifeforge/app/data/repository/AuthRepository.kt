package com.lifeforge.app.data.repository

import com.lifeforge.app.data.remote.UserProfileDto
import com.lifeforge.app.data.remote.UserSettingsDto
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user authentication and profile management.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: io.github.jan.supabase.gotrue.Auth,
    private val postgrest: io.github.jan.supabase.postgrest.Postgrest,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val coinRepository: CoinRepository
) {
    // ... (rest of class) ...

    /**
     * Ensure user has initial coin balance (100 LC).
     * Uses SharedPreferences to track if bonus was locally awarded, 
     * acting as a fail-safe even if row exists with 0 balance.
     */
    suspend fun ensureInitialBalance(userId: String) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("lifeforge_auth_prefs", android.content.Context.MODE_PRIVATE)
        val hasReceivedBonus = prefs.getBoolean("has_received_signup_bonus_$userId", false)
        
        if (!hasReceivedBonus) {
            // Check current local balance. If it's 0, we grant the bonus.
            // This handles fresh installs and "0 balance" issues.
            val currentBalance = coinRepository.getBalance(userId)
            
            if (currentBalance == 0) {
                // Grant 27 bonus coins (user requested this exact amount)
                coinRepository.earnCoins(
                    userId = userId,
                    amount = 27,
                    type = "signup_bonus",
                    description = "Welcome to LifeForge Bonus"
                )
            }

            
            // Mark as received so we don't repeat the check
            prefs.edit().putBoolean("has_received_signup_bonus_$userId", true).apply()
        }
    }
    
    private val _currentUser = MutableStateFlow<UserState>(UserState.Loading)
    val currentUser: StateFlow<UserState> = _currentUser.asStateFlow()
    
    sealed class UserState {
        object Loading : UserState()
        object NotAuthenticated : UserState()
        data class Authenticated(
            val userId: String,
            val email: String?,
            val fullName: String?
        ) : UserState()
        data class Error(val message: String) : UserState()
    }
    
    /**
     * Initialize and check current session.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            // Check for existing session
            val session = auth.currentSessionOrNull()
            if (session != null) {
                // Fetch profile to get full name
                val user = session.user
                val profile = fetchProfile(user?.id ?: "")
                
                _currentUser.value = UserState.Authenticated(
                    userId = user?.id ?: "",
                    email = user?.email,
                    fullName = profile?.fullName
                )
            } else {
                _currentUser.value = UserState.NotAuthenticated
            }
        } catch (e: Exception) {
            _currentUser.value = UserState.Error(e.message ?: "Initialization error")
            _currentUser.value = UserState.NotAuthenticated
        }
    }
    
    /**
     * Sign up with email and password.
     * After successful sign up, automatically signs in the user.
     */
    suspend fun signUp(
        emailAddress: String,
        passwordRaw: String,
        fullName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // First, create the account
            auth.signUpWith(Email) {
                email = emailAddress
                password = passwordRaw
            }
            
            // Now sign in to get the session
            auth.signInWith(Email) {
                email = emailAddress
                password = passwordRaw
            }
            
            // Get the current user after sign in
            val user = auth.currentUserOrNull() 
                ?: throw Exception("Account created but sign in failed. Please try signing in.")
            
            val userId = user.id
            
            // Create profile
            try {
                createProfile(userId, emailAddress, fullName)
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "Profile creation: ${e.message}")
            }
            
            // Give bonus coins (separate try-catch to ensure it runs)
            try {
                ensureInitialBalance(userId)
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Bonus coins error: ${e.message}")
            }
            
            _currentUser.value = UserState.Authenticated(
                userId = userId,
                email = emailAddress,
                fullName = fullName
            )
            
            Result.success(userId)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "SignUp error: ${e.message}", e)
            Result.failure(Exception("Sign up failed: ${e.message}"))
        }
    }
    
    /**
     * Sign in with Google ID Token.
     */
    suspend fun signInWithGoogle(token: String, fullName: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            auth.signInWith(IDToken) {
                this.idToken = token
                provider = Google
            }
            
            val user = auth.currentUserOrNull() ?: throw Exception("Google Login failed")
            val userId = user.id

            // Initialize profile variable
            var profile = fetchProfile(userId)
            
            // Fetch or create profile
            val providedName = if (fullName.isNullOrBlank()) null else fullName
            val metadataName = user.userMetadata?.get("full_name")?.toString()
            
            val initialName = providedName ?: profile?.fullName ?: metadataName
            val nameToUse = if (initialName.isNullOrBlank()) "Warrior" else initialName
            
            if (providedName != null) {
                 // Force update/create with provided name if it exists, regardless of what DB says
                 if (profile == null) {
                     createProfile(userId, user.email ?: "", providedName)
                     profile = fetchProfile(userId)
                 } else if (profile.fullName != providedName) {
                     updateProfileName(userId, providedName)
                     profile = fetchProfile(userId) // specific refresh
                 }
            } else if (profile == null) {
                // Fallback creation
                createProfile(userId, user.email ?: "", metadataName ?: "User")
            }
            
            ensureInitialBalance(userId)
            
            // Re-evaluate name to use after potential updates
            val finalName = providedName ?: profile?.fullName ?: metadataName ?: "Warrior"
            
            _currentUser.value = UserState.Authenticated(
                userId = userId,
                email = user.email,
                fullName = finalName
            )
            Result.success(userId)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Google SignIn error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(
        emailAddress: String,
        passwordRaw: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            auth.signInWith(Email) {
                email = emailAddress
                password = passwordRaw
            }
            
            val user = auth.currentUserOrNull() ?: throw Exception("Login failed")
            val userId = user.id
            
            val profile = fetchProfile(userId)
            
            _currentUser.value = UserState.Authenticated(
                userId = userId,
                email = emailAddress,
                fullName = profile?.fullName
            )
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out.
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
            _currentUser.value = UserState.NotAuthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current user ID if authenticated.
     */
    fun getCurrentUserId(): String? {
        return when (val state = _currentUser.value) {
            is UserState.Authenticated -> state.userId
            else -> {
                // Fallback attempt to get from auth directly (with safety)
                try {
                    auth.currentSessionOrNull()?.user?.id
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    /**
     * Fetch user profile from Supabase.
     */
    private suspend fun fetchProfile(userId: String): UserProfileDto? {
        return try {
            postgrest["profiles"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserProfileDto>()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create user profile.
     */
    private suspend fun createProfile(userId: String, email: String, fullName: String) {
        try {
            postgrest["profiles"]
                .insert(UserProfileDto(
                    id = userId,
                    userId = userId,
                    email = email,
                    fullName = fullName
                ))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Update user profile name.
     */
    suspend fun updateProfileName(userId: String, newName: String) = withContext(Dispatchers.IO) {
        try {
            postgrest["profiles"]
                .update({
                    set("full_name", newName)
                }) {
                    filter {
                        eq("user_id", userId)
                    }
                }
            
            // If current state is Authenticated, update it locally too
            val currentState = _currentUser.value
            if (currentState is UserState.Authenticated && currentState.userId == userId) {
                _currentUser.value = currentState.copy(fullName = newName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Legacy methods removed. ensureInitialBalance is now at the top.
    
    /**
     * Fetch user settings.
     */
    suspend fun fetchSettings(userId: String): UserSettingsDto? = withContext(Dispatchers.IO) {
        try {
            postgrest["user_settings"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserSettingsDto>()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Update user settings.
     */
    suspend fun updateSettings(userId: String, settings: UserSettingsDto): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                postgrest["user_settings"]
                    .upsert(settings)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
    /**
     * Set the timestamp of the last streak reset (e.g., due to Emergency Unlock).
     */
    fun setLastStreakResetTimestamp(timestamp: Long) {
        val prefs = context.getSharedPreferences("lifeforge_auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("last_streak_reset_timestamp", timestamp).apply()
    }

    /**
     * Get the timestamp of the last streak reset.
     */
    fun getLastStreakResetTimestamp(): Long {
        val prefs = context.getSharedPreferences("lifeforge_auth_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getLong("last_streak_reset_timestamp", 0L)
    }
}
