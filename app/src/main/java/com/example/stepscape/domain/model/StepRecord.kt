package com.example.stepscape.domain.model


data class StepRecord(
    val userId: String,
    val date: Long,
    val steps: Int,
    val syncedToFirebase: Boolean = false
)
