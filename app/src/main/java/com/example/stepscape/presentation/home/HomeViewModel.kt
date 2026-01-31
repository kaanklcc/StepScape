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
                loadChartData(1)
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
        loadChartData(_uiState.value.selectedPeriod.days)
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
        loadChartData(period.days)
    }

    fun loadChartData(days: Int) {
        viewModelScope.launch {
            Log.d(TAG, "Loading chart data for $days days...")
            try {
                val stepsMap = getStepsForPeriodUseCase(days)
                val chartData = stepsMap.mapKeys { it.key.dayOfMonth }
                
                Log.d(TAG, "Chart data loaded: $chartData")
                _uiState.update { it.copy(chartData = chartData) }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chart data: ${e.message}")
            }
        }
    }

    fun refresh() {
        Log.d(TAG, "Refreshing data...")
        _uiState.update { it.copy(isLoading = true) }
        loadTodaySteps()
        loadChartData(_uiState.value.selectedPeriod.days)
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