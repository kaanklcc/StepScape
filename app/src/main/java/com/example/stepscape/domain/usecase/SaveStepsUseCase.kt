package com.example.stepscape.domain.usecase

import android.util.Log
import com.example.stepscape.domain.repository.StepRepository
import javax.inject.Inject

/**
 * Adım sayısını kaydeden UseCase.
 */
class SaveStepsUseCase @Inject constructor(
    private val repository: StepRepository
) {
    companion object {
        private const val TAG = "SaveStepsUseCase"
    }

    suspend operator fun invoke(date: Long, steps: Int) {
        val userId = repository.getCurrentUserId()
        Log.d(TAG, "Current userId: $userId")
        
        if (userId == null) {
            Log.e(TAG, "User not signed in, cannot save steps!")
            return
        }
        
        Log.d(TAG, "Saving steps: date=$date, steps=$steps, userId=$userId")
        repository.saveSteps(userId, date, steps)
    }
}
