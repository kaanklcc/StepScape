package com.example.stepscape.data.remote

import android.util.Log
import com.example.stepscape.domain.model.StepRecord
import com.example.stepscape.domain.model.StepSession
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FirebaseService @Inject constructor() {

    companion object {
        private const val TAG = "FirebaseService"
    }

    private val firestore = FirebaseFirestore.getInstance()


    suspend fun uploadStepRecord(record: StepRecord): Boolean {
        Log.d(TAG, "Uploading to Firebase: userId=${record.userId}, date=${record.date}, steps=${record.steps}")
        return try {
            val data = hashMapOf(
                "date" to record.date,
                "steps" to record.steps,
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore
                .collection("users")
                .document(record.userId)
                .collection("steps")
                .document(record.date.toString())
                .set(data)
                .await()
            
            Log.d(TAG, "Firebase upload SUCCESS for userId: ${record.userId}, date: ${record.date}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase upload FAILED: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadStepSession(session: StepSession): Boolean {
        Log.d(TAG, "Uploading session to Firebase: userId=${session.userId}, startTime=${session.startTime}, steps=${session.steps}")
        return try {
            val data = hashMapOf(
                "startTime" to session.startTime,
                "endTime" to session.endTime,
                "steps" to session.steps,
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore
                .collection("users")
                .document(session.userId)
                .collection("sessions")
                .document(session.startTime.toString())
                .set(data)
                .await()
            
            Log.d(TAG, "Firebase session upload SUCCESS: startTime=${session.startTime}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase session upload FAILED: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun getStepRecords(userId: String): List<StepRecord> {
        Log.d(TAG, "Fetching step records for userId: $userId")
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("steps")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                val date = doc.getLong("date") ?: return@mapNotNull null
                val steps = doc.getLong("steps")?.toInt() ?: return@mapNotNull null
                StepRecord(
                    userId = userId,
                    date = date,
                    steps = steps,
                    syncedToFirebase = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase fetch FAILED: ${e.message}")
            emptyList()
        }
    }
}
