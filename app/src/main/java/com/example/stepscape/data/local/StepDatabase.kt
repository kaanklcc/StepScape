package com.example.stepscape.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.stepscape.data.local.dao.StepDao
import com.example.stepscape.data.local.entity.StepRecord

/**
 * Uygulama için stepdatabase oluşturma sınıfı.
 */
@Database(
    entities = [StepRecord::class],
    version = 1,
    exportSchema = false
)
abstract class StepDatabase : RoomDatabase() {
    
    abstract fun stepDao(): StepDao

    companion object {
        const val DATABASE_NAME = "stepscape_database"
    }
}
