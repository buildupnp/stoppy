package com.lifeforge.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.lifeforge.app.data.repository.NotificationRepository
import com.lifeforge.app.ui.screens.notifications.NotificationItem

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    fun signIn(email: String, password: String) {
        if (!validateSignIn(email, password)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.signIn(email, password)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Sign in failed"
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String, userName: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithGoogle(idToken, userName)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Google login failed"
                )
            }
        }
    }
    
    fun signUp(name: String, email: String, password: String, isPolicyAccepted: Boolean) {
        if (!validateSignUp(name, email, password, isPolicyAccepted)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            
            val result = authRepository.signUp(email, password, name)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    successMessage = "Account created successfully! Welcome to LifeForge!",
                    isAuthenticated = true
                )
                notificationRepository.addNotification(
                    NotificationItem(
                        title = "Welcome $name!",
                        description = "Your productivity journey starts now.",
                        icon = "👋",
                        time = "Just now",
                        category = "General"
                    )
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Sign up failed"
                )
            }
        }
    }
    
    private fun validateSignIn(email: String, password: String): Boolean {
        if (email.isBlank()) {
            setError("Please enter your email address")
            return false
        }
        if (password.isBlank()) {
            setError("Please enter your password")
            return false
        }
        return true
    }

    private fun validateSignUp(name: String, email: String, password: String, isPolicyAccepted: Boolean): Boolean {
        if (name.isBlank()) {
            setError("Please enter your name")
            return false
        }
        if (email.isBlank()) {
            setError("Please enter your email address")
            return false
        }
        if (email.contains(" ")) {
            setError("No spaces allowed in email address")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setError("Please enter a valid email address")
            return false
        }
        if (password.isBlank()) {
            setError("Please enter a password")
            return false
        }
        if (password.length < 6) {
            setError("Password must be at least 6 characters")
            return false
        }
        if (!isPolicyAccepted) {
            setError("You must agree to the Privacy Policy")
            return false
        }
        return true
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }
}
