package com.example.stepscape.presentation.logs

import com.example.stepscape.domain.model.StepSession


data class LogsUiState(
    val isLoading: Boolean = true,
    val sessions: List<StepSession> = emptyList(),
    val userName: String = "",
    val error: String? = null
)
