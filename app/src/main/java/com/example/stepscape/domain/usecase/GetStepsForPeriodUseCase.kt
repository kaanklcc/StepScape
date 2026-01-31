package com.example.stepscape.domain.usecase

import com.example.stepscape.data.health.HealthConnectManager
import java.time.LocalDate
import javax.inject.Inject

/**
 * Health Connect'ten belirli tarih aralığındaki adımları okuyan UseCase.
 */
class GetStepsForPeriodUseCase @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {

    suspend operator fun invoke(days: Int): Map<LocalDate, Long> {
        return healthConnectManager.getStepsForLastDays(days)
    }

    suspend fun forDateRange(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, Long> {
        return healthConnectManager.getStepsForDateRange(startDate, endDate)
    }
}
