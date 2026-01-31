package com.example.stepscape.presentation.logs

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepscape.data.local.dao.StepSessionDao
import com.example.stepscape.data.mapper.toSessionDomain
import com.example.stepscape.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LogsViewModel @Inject constructor(
    private val stepSessionDao: StepSessionDao,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LogsViewModel"
    }

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "LogsViewModel initialized")
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            val userId = user?.uid ?: ""
            val userName = user?.displayName ?: "User"
            
            Log.d(TAG, "Loading step sessions from Room for userId: $userId")
            _uiState.update { it.copy(userName = userName) }
            
            stepSessionDao.getAllSessionsByUser(userId)
                .catch { e ->
                    Log.e(TAG, "Error loading sessions: ${e.message}")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { sessions ->
                    Log.d(TAG, "Loaded ${sessions.size} sessions from Room")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            sessions = sessions.toSessionDomain(),
                            error = null
                        ) 
                    }
                }
        }
    }

    fun refresh() {
        Log.d(TAG, "Refreshing sessions...")
        _uiState.update { it.copy(isLoading = true) }
        loadSessions()
    }
}