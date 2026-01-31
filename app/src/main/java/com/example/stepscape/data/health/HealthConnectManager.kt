package com.example.stepscape.data.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Connect bağlantısı için sınıf
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HealthConnectManager"
    }

    private var healthConnectClient: HealthConnectClient? = null

    // Sadece okuma izni (yazma gerekmiyor)
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    // Permission request için contract
    val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    // SDK durumunu kontrol et
    fun getSdkStatus(): Int {
        val status = HealthConnectClient.getSdkStatus(context)
        Log.d(TAG, "SDK Status: $status")
        return status
    }

    // Health Connect kullanılabilir mi?
    fun isAvailable(): Boolean {
        val available = getSdkStatus() == HealthConnectClient.SDK_AVAILABLE
        Log.d(TAG, "Health Connect Available: $available")
        return available
    }

    fun needsUpdate(): Boolean {
        return getSdkStatus() == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
    }

    fun openPlayStoreForUpdate() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun getClient(): HealthConnectClient {
        if (healthConnectClient == null) {
            healthConnectClient = HealthConnectClient.getOrCreate(context)
            Log.d(TAG, "HealthConnectClient created")
        }
        return healthConnectClient!!
    }

    suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = getClient().permissionController.getGrantedPermissions()
            val hasAll = permissions.all { it in granted }
            Log.d(TAG, "Has all permissions: $hasAll, Granted: $granted")
            hasAll
        } catch (e: Exception) {
            Log.e(TAG, "Permission check error: ${e.message}")
            false
        }
    }

    suspend fun getTodaySteps(): Long {
        Log.d(TAG, "Getting today's steps...")
        if (!isAvailable()) {
            Log.w(TAG, "Health Connect not available")
            return 0L
        }
        
        return try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val response = getClient().aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )
            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            Log.d(TAG, "Today's steps from Health Connect: $steps")
            steps
        } catch (e: Exception) {
            Log.e(TAG, "Error getting today's steps: ${e.message}")
            e.printStackTrace()
            0L
        }
    }

    suspend fun getStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, Long> {
        Log.d(TAG, "Getting steps for range: $startDate to $endDate")
        if (!isAvailable()) return emptyMap()

        val result = mutableMapOf<LocalDate, Long>()
        
        return try {
            val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val response = getClient().readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                )
            )

            response.records.forEach { record ->
                val date = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                result[date] = (result[date] ?: 0L) + record.count
            }
            Log.d(TAG, "Steps for range: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error getting steps for range: ${e.message}")
            e.printStackTrace()
            emptyMap()
        }
    }

    suspend fun getStepsForLastDays(days: Int): Map<LocalDate, Long> {
        Log.d(TAG, "Getting steps for last $days days")
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong() - 1)
        return getStepsForDateRange(startDate, endDate)
    }


    data class StepSessionData(
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val steps: Long
    )

    suspend fun getTodayStepSessions(): List<StepSessionData> {
        Log.d(TAG, "Getting today's step sessions...")
        if (!isAvailable()) {
            Log.w(TAG, "Health Connect not available")
            return emptyList()
        }

        return try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val response = getClient().readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )

            val sessions = response.records.map { record ->
                StepSessionData(
                    startTimeMillis = record.startTime.toEpochMilli(),
                    endTimeMillis = record.endTime.toEpochMilli(),
                    steps = record.count
                )
            }.sortedByDescending { it.startTimeMillis }

            Log.d(TAG, "Found ${sessions.size} step sessions for today")
            sessions
        } catch (e: Exception) {
            Log.e(TAG, "Error getting step sessions: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getStepSessionsForLastDays(days: Int): List<StepSessionData> {
        Log.d(TAG, "Getting step sessions for last $days days...")
        if (!isAvailable()) return emptyList()

        return try {
            val today = LocalDate.now()
            val startDate = today.minusDays(days.toLong() - 1)
            val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val response = getClient().readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                )
            )

            val sessions = response.records.map { record ->
                StepSessionData(
                    startTimeMillis = record.startTime.toEpochMilli(),
                    endTimeMillis = record.endTime.toEpochMilli(),
                    steps = record.count
                )
            }.sortedByDescending { it.startTimeMillis }

            Log.d(TAG, "Found ${sessions.size} step sessions for last $days days")
            sessions
        } catch (e: Exception) {
            Log.e(TAG, "Error getting step sessions: ${e.message}")
            emptyList()
        }
    }

    suspend fun getStepsGroupedByHour(): Map<Int, Long> {
        Log.d(TAG, "Getting steps grouped by hour...")
        if (!isAvailable()) return emptyMap()

        return try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val response = getClient().readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )

            val result = mutableMapOf<Int, Long>()
            response.records.forEach { record ->
                val hour = record.startTime.atZone(ZoneId.systemDefault()).hour
                result[hour] = (result[hour] ?: 0L) + record.count
            }
            
            Log.d(TAG, "Steps by hour: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error getting steps by hour: ${e.message}")
            emptyMap()
        }
    }
}
