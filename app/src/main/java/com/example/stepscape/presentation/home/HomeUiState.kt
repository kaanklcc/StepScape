package com.example.stepscape.presentation.home

/**
 * Home ekranı UI durumu.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val todaySteps: Long = 0,
    val dailyGoal: Int = 10000,
    val goalSteps: Int = 10000,
    val progressPercent: Float = 0f,
    val motivationalMessage: String = "",
    val chartData: List<ChartEntry> = emptyList(),
    val selectedPeriod: ChartPeriod = ChartPeriod.DAY,
    val hasPermissions: Boolean = false,
    val isHealthConnectAvailable: Boolean = false,
    val error: String? = null
)


data class ChartEntry(
    val xValue: Float,
    val yValue: Long,
    val label: String  // X ekseni etiketi
)


enum class ChartPeriod(val days: Int, val label: String) {
    DAY(1, "Day"),
    WEEK(7, "Week"),
    MONTH(30, "Month"),
    SIX_MONTH(180, "6 Month"),
    YEAR(365, "Year")
}
