package com.example.stepscape.domain.repository

import com.example.stepscape.domain.model.StepRecord
import kotlinx.coroutines.flow.Flow

/**
 * Domain katmanı Repository interface.
 */
interface StepRepository {


    fun getCurrentUserId(): String?

    fun getAllRecords(userId: String): Flow<List<StepRecord>>

    fun getRecordsFromDate(userId: String, startDate: Long): Flow<List<StepRecord>>

    suspend fun getTodaySteps(userId: String): Int


    suspend fun saveSteps(userId: String, date: Long, steps: Int)

    suspend fun syncToFirebase(userId: String)
}
