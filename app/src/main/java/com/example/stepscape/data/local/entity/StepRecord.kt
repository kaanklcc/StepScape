package com.example.stepscape.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Günlük adım kaydının data classı.
 * gün,adım ve firebase senkronizasyonunu içerir.
 */
@Entity(tableName = "step_records")
data class StepRecord(
    @PrimaryKey
    val date: Long,
    val steps: Int,
    val syncedToFirebase: Boolean = false
)
