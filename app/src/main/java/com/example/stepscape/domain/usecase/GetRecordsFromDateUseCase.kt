package com.example.stepscape.domain.usecase

import com.example.stepscape.domain.model.StepRecord
import com.example.stepscape.domain.repository.StepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * Belirli tarihten itibaren kayıtları getiren UseCase.

 */
class GetRecordsFromDateUseCase @Inject constructor(
    private val repository: StepRepository
) {
    operator fun invoke(startDate: Long): Flow<List<StepRecord>> {
        val userId = repository.getCurrentUserId() ?: return emptyFlow()
        return repository.getRecordsFromDate(userId, startDate)
    }
}
