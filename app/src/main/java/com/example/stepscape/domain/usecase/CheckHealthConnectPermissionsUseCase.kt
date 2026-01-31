package com.example.stepscape.domain.usecase

import com.example.stepscape.data.health.HealthConnectManager
import javax.inject.Inject

/**
 * Health Connect izinlerini kontrol eden UseCase.
 */
class CheckHealthConnectPermissionsUseCase @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {

    suspend fun hasPermissions(): Boolean {
        return healthConnectManager.hasAllPermissions()
    }

    fun isAvailable(): Boolean {
        return healthConnectManager.isAvailable()
    }

    fun needsUpdate(): Boolean {
        return healthConnectManager.needsUpdate()
    }

    fun openPlayStore() {
        healthConnectManager.openPlayStoreForUpdate()
    }

    fun getPermissions() = healthConnectManager.permissions

    fun getRequestContract() = healthConnectManager.requestPermissionActivityContract
}
