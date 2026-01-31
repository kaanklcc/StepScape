package com.example.stepscape.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.stepscape.data.local.dao.StepDao
import com.example.stepscape.data.local.dao.StepSessionDao
import com.example.stepscape.data.local.entity.StepRecord
import com.example.stepscape.data.local.entity.StepSession

/**
 * Uygulama için stepdatabase oluşturma sınıfı.
 */
@Database(
    entities = [StepRecord::class, StepSession::class],
    version = 2,
    exportSchema = false
)
abstract class StepDatabase : RoomDatabase() {
    
    abstract fun stepDao(): StepDao
    abstract fun stepSessionDao(): StepSessionDao

    companion object {
        const val DATABASE_NAME = "stepscape_database"
    }
}
