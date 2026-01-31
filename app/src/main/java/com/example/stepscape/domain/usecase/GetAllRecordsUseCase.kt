package com.example.stepscape.domain.usecase

import com.example.stepscape.domain.model.StepRecord
import com.example.stepscape.domain.repository.StepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * Tüm adım kayıtlarını getiren UseCase.
 */
class GetAllRecordsUseCase @Inject constructor(
    private val repository: StepRepository
) {
    operator fun invoke(): Flow<List<StepRecord>> {
        val userId = repository.getCurrentUserId() ?: return emptyFlow()
        return repository.getAllRecords(userId)
    }
}
