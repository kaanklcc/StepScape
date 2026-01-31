package com.example.stepscape.data.repository

import android.util.Log
import com.example.stepscape.data.local.dao.StepDao
import com.example.stepscape.data.mapper.toDomain
import com.example.stepscape.data.mapper.toEntity
import com.example.stepscape.data.remote.FirebaseService
import com.example.stepscape.domain.model.StepRecord
import com.example.stepscape.domain.repository.AuthRepository
import com.example.stepscape.domain.repository.StepRepository
import com.example.stepscape.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementasyonu  Room ve Firebase işlemleri.
 */
@Singleton
class StepRepositoryImpl @Inject constructor(
    private val stepDao: StepDao,
    private val firebaseService: FirebaseService,
    private val authRepository: AuthRepository
) : StepRepository {

    companion object {
        private const val TAG = "StepRepository"
    }

    override fun getCurrentUserId(): String? {
        return authRepository.getCurrentUser()?.uid
    }

    override fun getAllRecords(userId: String): Flow<List<StepRecord>> {
        Log.d(TAG, "Getting all records from Room for userId: $userId")
        return stepDao.getAllRecordsByUser(userId)
            .map { it.toDomain() }
            .onEach { records ->
                Log.d(TAG, "Room returned ${records.size} records for user")
            }
    }

    override fun getRecordsFromDate(userId: String, startDate: Long): Flow<List<StepRecord>> {
        Log.d(TAG, "Getting records from date: $startDate for userId: $userId")
        return stepDao.getRecordsFromDateByUser(userId, startDate)
            .map { it.toDomain() }
            .onEach { records ->
                Log.d(TAG, "Room returned ${records.size} records from date")
            }
    }

    override suspend fun getTodaySteps(userId: String): Int {
        val today = DateUtils.getTodayStartMillis()
        val steps = stepDao.getRecordByUserAndDate(userId, today)?.steps ?: 0
        Log.d(TAG, "Today's steps from Room for userId $userId: $steps")
        return steps
    }

    override suspend fun saveSteps(userId: String, date: Long, steps: Int) {
        Log.d(TAG, "Saving to Room: userId=$userId, date=$date, steps=$steps")
        val domainRecord = StepRecord(
            userId = userId,
            date = date,
            steps = steps,
            syncedToFirebase = false
        )
        stepDao.insert(domainRecord.toEntity())
        Log.d(TAG, "Room insert SUCCESS")
    }

    override suspend fun syncToFirebase(userId: String) {
        val unsyncedRecords = stepDao.getUnsyncedRecordsByUser(userId)
        Log.d(TAG, "Syncing ${unsyncedRecords.size} unsynced records to Firebase for userId: $userId")
        
        unsyncedRecords.forEach { entity ->
            val domainRecord = entity.toDomain()
            val success = firebaseService.uploadStepRecord(domainRecord)
            if (success) {
                stepDao.markAsSynced(userId, entity.date)
                Log.d(TAG, "Marked as synced: ${entity.date}")
            }
        }
        Log.d(TAG, "Firebase sync completed")
    }
}
