package com.example.stepscape.domain.usecase

import com.example.stepscape.data.health.HealthConnectManager
import javax.inject.Inject

/**
 * Health Connect'ten bugünün adım sayısını okuyan UseCase.
 */
class GetHealthConnectStepsUseCase @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {
    suspend operator fun invoke(): Long {
        return healthConnectManager.getTodaySteps()
    }
}
