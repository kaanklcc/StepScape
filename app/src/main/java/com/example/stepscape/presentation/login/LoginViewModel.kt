package com.example.stepscape.presentation.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepscape.domain.usecase.CheckAuthStateUseCase
import com.example.stepscape.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Login ekranı ViewModel.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val checkAuthStateUseCase: CheckAuthStateUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val isSignedIn = checkAuthStateUseCase()
        Log.d(TAG, "User signed in: $isSignedIn")
        _uiState.update { it.copy(isSignedIn = isSignedIn) }
    }

    fun onGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            Log.d(TAG, "Signing in with Google ID token...")
            val result = signInWithGoogleUseCase(idToken)
            
            result.fold(
                onSuccess = { user ->
                    Log.d(TAG, "Sign in successful: ${user.email}")
                    _uiState.update { 
                        it.copy(isLoading = false, isSignedIn = true, error = null) 
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Sign in failed: ${exception.message}")
                    _uiState.update { 
                        it.copy(isLoading = false, error = exception.message) 
                    }
                }
            )
        }
    }

    fun onSignInError(message: String) {
        _uiState.update { it.copy(isLoading = false, error = message) }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }
}
