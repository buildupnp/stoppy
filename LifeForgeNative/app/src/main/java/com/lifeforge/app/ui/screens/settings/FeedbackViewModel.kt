package com.lifeforge.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeforge.app.data.repository.AuthRepository
import com.lifeforge.app.data.repository.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun submitFeedback(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            
            val userId = authRepository.getCurrentUserId() ?: "anonymous"
            val result = feedbackRepository.sendFeedback(userId, message)
            
            if (result.isSuccess) {
                _submitSuccess.value = true
            } else {
                _errorMessage.value = "Failed to send feedback. Please try again."
            }
            _isSubmitting.value = false
        }
    }
    
    fun resetState() {
        _submitSuccess.value = false
        _errorMessage.value = null
    }
}
