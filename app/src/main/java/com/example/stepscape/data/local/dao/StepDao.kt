package com.example.stepscape.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stepscape.data.local.entity.StepRecord
import kotlinx.coroutines.flow.Flow

/**
 * Uygulama için DAO interfacesi.
 */
@Dao
interface StepDao {

    // Yeni adım kaydı ekle veya güncelle
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stepRecord: StepRecord)

    // Tüm kayıtları tarihe göre azalan sırada getir (Logs ekranı için)
    @Query("SELECT * FROM step_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<StepRecord>>

    // Bugünün adım kaydını getir
    @Query("SELECT * FROM step_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: Long): StepRecord?

    // Senkronize edilmemiş kayıtları getir
    @Query("SELECT * FROM step_records WHERE syncedToFirebase = 0")
    suspend fun getUnsyncedRecords(): List<StepRecord>

    // Haftalık kayıtları getir
    @Query("SELECT * FROM step_records WHERE date >= :startDate ORDER BY date ASC")
    fun getRecordsFromDate(startDate: Long): Flow<List<StepRecord>>

    // Kaydı senkronize edildi olarak işaretle
    @Query("UPDATE step_records SET syncedToFirebase = 1 WHERE date = :date")
    suspend fun markAsSynced(date: Long)
}
