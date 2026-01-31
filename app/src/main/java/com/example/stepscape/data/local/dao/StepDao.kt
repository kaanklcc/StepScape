package com.example.stepscape.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stepscape.data.local.entity.StepRecord
import kotlinx.coroutines.flow.Flow

/**
 * Uygulama için DAO interfacesi.
 * Tüm sorgular userId parametresi ile kullanıcıya özel.
 */
@Dao
interface StepDao {

    // Yeni adım kaydı ekle veya güncelle
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stepRecord: StepRecord)

    // Kullanıcının tüm kayıtlarını tarihe göre azalan sırada getir (Logs ekranı için)
    @Query("SELECT * FROM step_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllRecordsByUser(userId: String): Flow<List<StepRecord>>

    // Kullanıcının belirli bir günün adım kaydını getir
    @Query("SELECT * FROM step_records WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getRecordByUserAndDate(userId: String, date: Long): StepRecord?

    // Kullanıcının senkronize edilmemiş kayıtlarını getir
    @Query("SELECT * FROM step_records WHERE userId = :userId AND syncedToFirebase = 0")
    suspend fun getUnsyncedRecordsByUser(userId: String): List<StepRecord>

    // Kullanıcının belirli bir tarihten itibaren kayıtlarını getir
    @Query("SELECT * FROM step_records WHERE userId = :userId AND date >= :startDate ORDER BY date ASC")
    fun getRecordsFromDateByUser(userId: String, startDate: Long): Flow<List<StepRecord>>

    // Kaydı senkronize edildi olarak işaretle
    @Query("UPDATE step_records SET syncedToFirebase = 1 WHERE userId = :userId AND date = :date")
    suspend fun markAsSynced(userId: String, date: Long)
}
