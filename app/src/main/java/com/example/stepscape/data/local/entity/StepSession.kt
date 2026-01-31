package com.example.stepscape.data.local.entity

import androidx.room.Entity


@Entity(
    tableName = "step_sessions",
    primaryKeys = ["userId", "startTime"]
)
data class StepSession(
    val userId: String,
    val startTime: Long,
    val endTime: Long,
    val steps: Int,
    val syncedToFirebase: Boolean = false
)
