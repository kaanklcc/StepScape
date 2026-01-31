package com.example.stepscape.domain.model


data class StepSession(
    val userId: String,
    val startTime: Long,
    val endTime: Long,
    val steps: Int,           // Bu session'daki adım sayıs
    val syncedToFirebase: Boolean = false
)
