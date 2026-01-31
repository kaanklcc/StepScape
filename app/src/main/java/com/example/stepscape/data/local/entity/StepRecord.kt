package com.example.stepscape.data.local.entity

import androidx.room.Entity

/**
 * Günlük adım kaydının data classı.
 */
@Entity(
    tableName = "step_records",
    primaryKeys = ["userId", "date"]
)
data class StepRecord(
    val userId: String,
    val date: Long,
    val steps: Int,
    val syncedToFirebase: Boolean = false
)
