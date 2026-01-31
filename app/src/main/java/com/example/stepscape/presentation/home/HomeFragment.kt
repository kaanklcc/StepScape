package com.example.stepscape.presentation.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.stepscape.R
import com.example.stepscape.databinding.FragmentHomeBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()
    
    private var selectedPeriod = Period.DAY
    
    private val periodTabs: List<View> by lazy {
        listOf(
            binding.tabDay,
            binding.tabWeek,
            binding.tabMonth,
            binding.tab6Month,
            binding.tabYear
        )
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, 0)
            val headerView = binding.root.findViewById<View>(
                resources.getIdentifier("tvTitle", "id", requireContext().packageName)
            ).parent as? View
            headerView?.setPadding(
                headerView.paddingLeft,
                insets.top + 16,
                headerView.paddingRight,
                headerView.paddingBottom
            )
            WindowInsetsCompat.CONSUMED
        }
        
        setupChart()
        setupClickListeners()
        observeUiState()
        
        selectPeriod(Period.DAY)
    }
    
    private fun setupClickListeners() {
        binding.btnLogs.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_logs)
        }
        
        binding.tabDay.setOnClickListener { selectPeriod(Period.DAY) }
        binding.tabWeek.setOnClickListener { selectPeriod(Period.WEEK) }
        binding.tabMonth.setOnClickListener { selectPeriod(Period.MONTH) }
        binding.tab6Month.setOnClickListener { selectPeriod(Period.SIX_MONTH) }
        binding.tabYear.setOnClickListener { selectPeriod(Period.YEAR) }
    }
    
    private fun selectPeriod(period: Period) {
        selectedPeriod = period
        updateTabsUI()
        
        val days = when (period) {
            Period.DAY -> 1
            Period.WEEK -> 7
            Period.MONTH -> 30
            Period.SIX_MONTH -> 180
            Period.YEAR -> 365
        }
        
        viewModel.loadChartData(days)
    }
    
    private fun updateTabsUI() {
        periodTabs.forEach { tab ->
            tab.setBackgroundColor(Color.TRANSPARENT)
            (tab as? android.widget.TextView)?.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_primary)
            )
        }
        
        val selectedTab = when (selectedPeriod) {
            Period.DAY -> binding.tabDay
            Period.WEEK -> binding.tabWeek
            Period.MONTH -> binding.tabMonth
            Period.SIX_MONTH -> binding.tab6Month
            Period.YEAR -> binding.tabYear
        }
        
        selectedTab.setBackgroundResource(R.drawable.tab_selected)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                updateUI(state)
            }
        }
    }
    
    private fun updateUI(state: HomeUiState) {
        binding.tvStepCount.text = formatNumber(state.todaySteps)
        binding.tvGoal.text = "/${formatNumber(state.dailyGoal)}"
        
        binding.circularProgress.setProgress(state.progressPercent)
        
        binding.tvMotivation.text = getMotivationMessage(state.progressPercent)
        
        if (state.chartData.isNotEmpty()) {
            updateChart(state.chartData)
            
            val totalSteps = state.chartData.values.sum()
            binding.tvRangeSteps.text = formatNumber(totalSteps.toInt())
            
            binding.tvDateRange.text = getDateRangeText()
        }
    }
    
    private fun formatNumber(number: Number): String {
        return String.format(Locale.getDefault(), "%,d", number).replace(",", "")
    }
    
    private fun getMotivationMessage(progress: Float): String {
        return when {
            progress >= 100 -> "Amazing! You've reached your goal! 🎉"
            progress >= 75 -> "You're very close to your goal,\nso keep pushing forward."
            progress >= 50 -> "Great progress! You're halfway there!"
            progress >= 25 -> "Good start! Keep moving!"
            else -> "Every step counts. Let's get moving!"
        }
    }
    
    private fun getDateRangeText(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        val endDate = dateFormat.format(calendar.time)
        
        val days = when (selectedPeriod) {
            Period.DAY -> 0
            Period.WEEK -> 6
            Period.MONTH -> 29
            Period.SIX_MONTH -> 179
            Period.YEAR -> 364
        }
        
        if (days == 0) {
            return endDate
        }
        
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
        
        return "$startDate - $endDate"
    }
    
    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E8E8E8")
                gridLineWidth = 0.5f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 11f
                granularity = 1f
                setDrawAxisLine(false)
            }
            
            axisLeft.apply {
                isEnabled = false
            }
            
            axisRight.apply {
                isEnabled = true
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E8E8E8")
                gridLineWidth = 0.5f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 11f
                axisMinimum = 0f
                setDrawAxisLine(false)
            }
            
            setExtraOffsets(8f, 8f, 8f, 8f)
        }
    }
    
    private fun updateChart(data: Map<Int, Long>) {
        val entries = data.map { (day, steps) ->
            Entry(day.toFloat(), steps.toFloat())
        }.sortedBy { it.x }
        
        if (entries.isEmpty()) return
        
        val dataSet = LineDataSet(entries, "Steps").apply {
            color = ContextCompat.getColor(requireContext(), R.color.chart_line)
            lineWidth = 4f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            
            setDrawFilled(false)
        }
        
        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.animateX(500)
        binding.lineChart.invalidate()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    enum class Period {
        DAY, WEEK, MONTH, SIX_MONTH, YEAR
    }
}