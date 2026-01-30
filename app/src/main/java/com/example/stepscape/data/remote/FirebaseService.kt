package com.example.stepscape.data.remote

import android.util.Log
import com.example.stepscape.domain.model.StepRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Firestore işlemleri için servis sınıfı.
 */
@Singleton
class FirebaseService @Inject constructor() {

    companion object {
        private const val TAG = "FirebaseService"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val stepsCollection = firestore.collection("step_logs")


    suspend fun uploadStepRecord(record: StepRecord): Boolean {
        Log.d(TAG, "Uploading to Firebase: date=${record.date}, steps=${record.steps}")
        return try {
            val data = hashMapOf(
                "date" to record.date,
                "steps" to record.steps,
                "timestamp" to System.currentTimeMillis()
            )
            stepsCollection.document(record.date.toString()).set(data).await()
            Log.d(TAG, "Firebase upload SUCCESS for date: ${record.date}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase upload FAILED: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

