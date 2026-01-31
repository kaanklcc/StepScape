package com.example.stepscape.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stepscape.data.local.entity.StepSession
import kotlinx.coroutines.flow.Flow

/**
 * StepSession için DAO.
 */
@Dao
interface StepSessionDao {

    // Session ekle veya güncelle
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StepSession)

    // Birden fazla session ekle
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<StepSession>)

    // Kullanıcının tüm session'larını tarihe göre azalan sırada getir
    @Query("SELECT * FROM step_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllSessionsByUser(userId: String): Flow<List<StepSession>>

    // Kullanıcının belirli bir tarihten sonraki session'larını getir
    @Query("SELECT * FROM step_sessions WHERE userId = :userId AND startTime >= :startTime ORDER BY startTime DESC")
    fun getSessionsFromTime(userId: String, startTime: Long): Flow<List<StepSession>>

    // Kullanıcının senkronize edilmemiş session'larını getir
    @Query("SELECT * FROM step_sessions WHERE userId = :userId AND syncedToFirebase = 0")
    suspend fun getUnsyncedSessionsByUser(userId: String): List<StepSession>

    // Session'ı senkronize edildi olarak işaretle
    @Query("UPDATE step_sessions SET syncedToFirebase = 1 WHERE userId = :userId AND startTime = :startTime")
    suspend fun markAsSynced(userId: String, startTime: Long)

    // Belirli bir session var mı kontrol et
    @Query("SELECT COUNT(*) FROM step_sessions WHERE userId = :userId AND startTime = :startTime")
    suspend fun sessionExists(userId: String, startTime: Long): Int
}
