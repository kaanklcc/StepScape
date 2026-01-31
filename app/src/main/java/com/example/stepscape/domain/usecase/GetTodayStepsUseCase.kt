package com.example.stepscape.domain.usecase

import com.example.stepscape.domain.repository.StepRepository
import javax.inject.Inject

/**
 * Bugünün adım sayısını getiren UseCase.
 */
class GetTodayStepsUseCase @Inject constructor(
    private val repository: StepRepository
) {
    suspend operator fun invoke(): Int {
        val userId = repository.getCurrentUserId() ?: return 0
        return repository.getTodaySteps(userId)
    }
}
