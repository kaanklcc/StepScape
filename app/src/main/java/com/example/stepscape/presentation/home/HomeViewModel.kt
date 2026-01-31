package com.example.stepscape.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepscape.data.health.HealthConnectManager
import com.example.stepscape.data.local.dao.StepSessionDao
import com.example.stepscape.data.local.entity.StepSession
import com.example.stepscape.data.mapper.toDomain
import com.example.stepscape.data.remote.FirebaseService
import com.example.stepscape.domain.repository.AuthRepository
import com.example.stepscape.domain.usecase.CheckHealthConnectPermissionsUseCase
import com.example.stepscape.domain.usecase.GetHealthConnectStepsUseCase
import com.example.stepscape.domain.usecase.GetStepsForPeriodUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHealthConnectStepsUseCase: GetHealthConnectStepsUseCase,
    private val getStepsForPeriodUseCase: GetStepsForPeriodUseCase,
    private val checkHealthConnectPermissionsUseCase: CheckHealthConnectPermissionsUseCase,
    private val healthConnectManager: HealthConnectManager,
    private val stepSessionDao: StepSessionDao,
    private val firebaseService: FirebaseService,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "HomeViewModel initialized")
        checkHealthConnectStatus()
    }

    private fun checkHealthConnectStatus() {
        Log.d(TAG, "Checking Health Connect status...")
        
        val isAvailable = checkHealthConnectPermissionsUseCase.isAvailable()
        Log.d(TAG, "Health Connect available: $isAvailable")
        
        _uiState.update { it.copy(isHealthConnectAvailable = isAvailable) }
        
        if (isAvailable) {
            checkPermissions()
        } else {
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    error = "Health Connect mevcut değil. Lütfen yükleyin."
                ) 
            }
        }
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            Log.d(TAG, "Checking permissions...")
            val hasPermissions = checkHealthConnectPermissionsUseCase.hasPermissions()
            Log.d(TAG, "Has permissions: $hasPermissions")
            
            _uiState.update { it.copy(hasPermissions = hasPermissions) }
            
            if (hasPermissions) {
                loadTodaySteps()
                loadChartData(ChartPeriod.DAY)
                loadAndSyncSessions()
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onPermissionsGranted() {
        Log.d(TAG, "Permissions granted, loading data...")
        _uiState.update { it.copy(hasPermissions = true) }
        loadTodaySteps()
        loadChartData(_uiState.value.selectedPeriod)
        loadAndSyncSessions()
    }

    private fun loadTodaySteps() {
        viewModelScope.launch {
            Log.d(TAG, "Loading today's steps...")
            try {
                val steps = getHealthConnectStepsUseCase()
                Log.d(TAG, "Today's steps: $steps")
                
                val goal = _uiState.value.goalSteps
                val progress = (steps.toFloat() / goal * 100).coerceAtMost(100f)
                val message = getMotivationalMessage(steps, goal)
                
                _uiState.update { 
                    it.copy(
                        todaySteps = steps,
                        progressPercent = progress,
                        motivationalMessage = message,
                        isLoading = false,
                        error = null
                    ) 
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading today's steps: ${e.message}")
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message) 
                }
            }
        }
    }

    private fun loadAndSyncSessions() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid
            if (userId == null) {
                Log.e(TAG, "User not signed in, cannot sync sessions")
                return@launch
            }
            
            try {
                Log.d(TAG, "Loading step sessions from Health Connect...")
                
                val sessions = healthConnectManager.getStepSessionsForLastDays(30)
                Log.d(TAG, "Found ${sessions.size} sessions from Health Connect")
                
                sessions.forEach { session ->
                    val exists = stepSessionDao.sessionExists(userId, session.startTimeMillis)
                    if (exists == 0) {
                        val entity = StepSession(
                            userId = userId,
                            startTime = session.startTimeMillis,
                            endTime = session.endTimeMillis,
                            steps = session.steps.toInt(),
                            syncedToFirebase = false
                        )
                        stepSessionDao.insert(entity)
                        Log.d(TAG, "Inserted new session: startTime=${session.startTimeMillis}, steps=${session.steps}")
                    }
                }
                
                syncSessionsToFirebase(userId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading/syncing sessions: ${e.message}")
            }
        }
    }

    private suspend fun syncSessionsToFirebase(userId: String) {
        val unsyncedSessions = stepSessionDao.getUnsyncedSessionsByUser(userId)
        Log.d(TAG, "Syncing ${unsyncedSessions.size} unsynced sessions to Firebase...")
        
        unsyncedSessions.forEach { entity ->
            val domainSession = entity.toDomain()
            val success = firebaseService.uploadStepSession(domainSession)
            if (success) {
                stepSessionDao.markAsSynced(userId, entity.startTime)
                Log.d(TAG, "Session synced: startTime=${entity.startTime}")
            }
        }
        
        Log.d(TAG, "Session sync completed")
    }

    fun onPeriodSelected(period: ChartPeriod) {
        Log.d(TAG, "Period selected: ${period.label}")
        _uiState.update { it.copy(selectedPeriod = period) }
        loadChartData(period)
    }

    fun loadChartData(period: ChartPeriod) {
        viewModelScope.launch {
            Log.d(TAG, "Loading chart data for period: ${period.label}")
            try {
                val chartEntries = when (period) {
                    ChartPeriod.DAY -> loadDayChart()
                    ChartPeriod.WEEK -> loadWeekChart()
                    ChartPeriod.MONTH -> loadMonthChart()
                    ChartPeriod.SIX_MONTH -> loadSixMonthChart()
                    ChartPeriod.YEAR -> loadYearChart()
                }
                
                Log.d(TAG, "Chart entries: ${chartEntries.size} points")
                _uiState.update { it.copy(chartData = chartEntries, selectedPeriod = period) }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chart data: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadDayChart(): List<ChartEntry> {
        val hourlyData = healthConnectManager.getStepsGroupedByHour()
        return (0..23).map { hour ->
            val label = String.format("%02d", hour)
            ChartEntry(
                xValue = hour.toFloat(),
                yValue = hourlyData[hour] ?: 0L,
                label = label
            )
        }
    }

    private suspend fun loadWeekChart(): List<ChartEntry> {
        val stepsMap = getStepsForPeriodUseCase(7)
        val today = LocalDate.now()
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        
        return (6 downTo 0).mapIndexed { index, daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            ChartEntry(
                xValue = index.toFloat(),
                yValue = stepsMap[date] ?: 0L,
                label = date.format(dayFormatter)
            )
        }
    }

    private suspend fun loadMonthChart(): List<ChartEntry> {
        val stepsMap = getStepsForPeriodUseCase(30)
        val today = LocalDate.now()
        
        // Son 30 günü 5'er günlük gruplarla göster (1, 5, 10, 15, 20, 25, 30)
        val entries = mutableListOf<ChartEntry>()
        for (i in 0..5) {
            val daysAgo = 29 - (i * 5)
            val date = today.minusDays(daysAgo.toLong())
            entries.add(ChartEntry(
                xValue = i.toFloat(),
                yValue = stepsMap[date] ?: 0L,
                label = date.dayOfMonth.toString()
            ))
        }
        return entries
    }

    private suspend fun loadSixMonthChart(): List<ChartEntry> {
        val stepsMap = getStepsForPeriodUseCase(180)
        val today = LocalDate.now()
        val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
        
        return (5 downTo 0).mapIndexed { index, monthsAgo ->
            val monthStart = today.minusMonths(monthsAgo.toLong()).withDayOfMonth(1)
            val monthEnd = monthStart.plusMonths(1).minusDays(1)
            
            val monthSteps = stepsMap.filter { (date, _) ->
                !date.isBefore(monthStart) && !date.isAfter(monthEnd)
            }.values.sum()
            
            ChartEntry(
                xValue = index.toFloat(),
                yValue = monthSteps,
                label = monthStart.format(monthFormatter)
            )
        }
    }

    private suspend fun loadYearChart(): List<ChartEntry> {
        val stepsMap = getStepsForPeriodUseCase(365)
        val today = LocalDate.now()
        val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
        
        return (11 downTo 0).mapIndexed { index, monthsAgo ->
            val monthStart = today.minusMonths(monthsAgo.toLong()).withDayOfMonth(1)
            val monthEnd = monthStart.plusMonths(1).minusDays(1)
            
            val monthSteps = stepsMap.filter { (date, _) ->
                !date.isBefore(monthStart) && !date.isAfter(monthEnd)
            }.values.sum()
            
            ChartEntry(
                xValue = index.toFloat(),
                yValue = monthSteps,
                label = monthStart.format(monthFormatter)
            )
        }
    }

    fun refresh() {
        Log.d(TAG, "Refreshing data...")
        _uiState.update { it.copy(isLoading = true) }
        loadTodaySteps()
        loadChartData(_uiState.value.selectedPeriod)
        loadAndSyncSessions()
    }

    fun getPermissionContract() = checkHealthConnectPermissionsUseCase.getRequestContract()
    
    fun getPermissions() = checkHealthConnectPermissionsUseCase.getPermissions()

    private fun getMotivationalMessage(steps: Long, goal: Int): String {
        val percent = (steps.toFloat() / goal * 100).toInt()
        return when {
            percent >= 100 -> "Tebrikler! Hedefine ulaştın! 🎉"
            percent >= 90 -> "Harika! Hedefe çok yakınsın!"
            percent >= 75 -> "You're very close to your goal, so keep pushing forward."
            percent >= 50 -> "Yarıyı geçtin, devam et!"
            percent >= 25 -> "İyi başlangıç, devam et!"
            else -> "Haydi, bugün biraz yürüyüş yap!"
        }
    }
}