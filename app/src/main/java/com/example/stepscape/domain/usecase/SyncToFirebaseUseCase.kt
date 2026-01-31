package com.example.stepscape.domain.usecase

import android.util.Log
import com.example.stepscape.domain.repository.StepRepository
import javax.inject.Inject

/**
 * Firebase'e senkronizasyon yapan UseCase.
 */
class SyncToFirebaseUseCase @Inject constructor(
    private val repository: StepRepository
) {
    companion object {
        private const val TAG = "SyncToFirebaseUseCase"
    }

    suspend operator fun invoke() {
        val userId = repository.getCurrentUserId()
        Log.d(TAG, "Current userId for sync: $userId")
        
        if (userId == null) {
            Log.e(TAG, "User not signed in, cannot sync to Firebase!")
            return
        }
        
        Log.d(TAG, "Syncing to Firebase for userId: $userId")
        repository.syncToFirebase(userId)
    }
}
