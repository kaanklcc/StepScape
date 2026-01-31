package com.example.stepscape.presentation.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
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
    
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>
    
    private var hasRequestedPermission = false
    
    private val periodTabs: List<View> by lazy {
        listOf(
            binding.tabDay,
            binding.tabWeek,
            binding.tabMonth,
            binding.tab6Month,
            binding.tabYear
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionLauncher = registerForActivityResult(
            viewModel.getPermissionContract()
        ) { granted ->
            if (granted.containsAll(viewModel.getPermissions())) {
                viewModel.onPermissionsGranted()
            }
        }
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
            
            val headerView = binding.root.findViewById<View>(
                resources.getIdentifier("headerLayout", "id", requireContext().packageName)
            )
            headerView?.setPadding(
                headerView.paddingLeft,
                insets.top + 16,
                headerView.paddingRight,
                headerView.paddingBottom
            )
            
            v.setPadding(0, 0, 0, insets.bottom)
            
            WindowInsetsCompat.CONSUMED
        }
        
        setupChart()
        setupClickListeners()
        observeUiState()
    }
    
    private fun setupClickListeners() {
        binding.btnLogs.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_logs)
        }
        
        binding.tabDay.setOnClickListener { selectPeriod(ChartPeriod.DAY) }
        binding.tabWeek.setOnClickListener { selectPeriod(ChartPeriod.WEEK) }
        binding.tabMonth.setOnClickListener { selectPeriod(ChartPeriod.MONTH) }
        binding.tab6Month.setOnClickListener { selectPeriod(ChartPeriod.SIX_MONTH) }
        binding.tabYear.setOnClickListener { selectPeriod(ChartPeriod.YEAR) }
    }
    
    private fun selectPeriod(period: ChartPeriod) {
        updateTabsUI(period)
        viewModel.onPeriodSelected(period)
    }
    
    private fun updateTabsUI(period: ChartPeriod) {
        periodTabs.forEach { tab ->
            tab.background = null
            (tab as? android.widget.TextView)?.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_primary)
            )
        }
        
        val selectedTab = when (period) {
            ChartPeriod.DAY -> binding.tabDay
            ChartPeriod.WEEK -> binding.tabWeek
            ChartPeriod.MONTH -> binding.tabMonth
            ChartPeriod.SIX_MONTH -> binding.tab6Month
            ChartPeriod.YEAR -> binding.tabYear
        }
        
        selectedTab.setBackgroundResource(R.drawable.tab_selected)
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                updateUI(state)
                
                // İzin yoksa ve Health Connect mevcutsa izin iste (sadece 1 kez)
                if (!state.hasPermissions && state.isHealthConnectAvailable && !state.isLoading && !hasRequestedPermission) {
                    hasRequestedPermission = true
                    requestHealthConnectPermissions()
                }
            }
        }
    }
    
    private fun requestHealthConnectPermissions() {
        permissionLauncher.launch(viewModel.getPermissions())
    }
    
    private fun updateUI(state: HomeUiState) {
        binding.tvStepCount.text = formatNumber(state.todaySteps)
        binding.tvGoal.text = "/${formatNumber(state.dailyGoal)}"
        
        binding.circularProgress.setProgress(state.progressPercent)
        
        binding.tvMotivation.text = getMotivationMessage(state.progressPercent)
        
        updateTabsUI(state.selectedPeriod)
        
        if (state.chartData.isNotEmpty()) {
            updateChart(state.chartData, state.selectedPeriod)
            
            val totalSteps = state.chartData.sumOf { it.yValue }
            binding.tvRangeSteps.text = formatNumber(totalSteps.toInt())
            
            binding.tvDateRange.text = getDateRangeText(state.selectedPeriod)
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
    
    private fun getDateRangeText(period: ChartPeriod): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        val endDate = dateFormat.format(calendar.time)
        
        val days = when (period) {
            ChartPeriod.DAY -> 0
            ChartPeriod.WEEK -> 6
            ChartPeriod.MONTH -> 29
            ChartPeriod.SIX_MONTH -> 179
            ChartPeriod.YEAR -> 364
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
                gridColor = Color.parseColor("#E0E0E0")
                gridLineWidth = 1f
                enableGridDashedLine(8f, 4f, 0f)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 12f
                granularity = 1f
                setDrawAxisLine(false)
                yOffset = 10f
            }
            
            axisLeft.apply {
                isEnabled = false
            }
            
            axisRight.apply {
                isEnabled = true
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                gridLineWidth = 1f
                enableGridDashedLine(8f, 4f, 0f)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 12f
                axisMinimum = 0f
                setDrawAxisLine(false)
                xOffset = 10f
            }
            
            setExtraOffsets(8f, 8f, 16f, 8f)
        }
    }
    
    private fun updateChart(chartEntries: List<ChartEntry>, period: ChartPeriod) {
        android.util.Log.d("HomeFragment", "updateChart called with ${chartEntries.size} entries for $period")
        
        if (chartEntries.isEmpty()) {
            android.util.Log.w("HomeFragment", "No entries for chart, skipping")
            return
        }
        
        val entries = chartEntries.map { entry ->
            Entry(entry.xValue, entry.yValue.toFloat())
        }
        
        val labels = chartEntries.map { it.label }
        binding.lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < labels.size) labels[index] else ""
            }
        }
        
        val (yMax, labelCount) = when (period) {
            ChartPeriod.DAY -> 150f to 4
            ChartPeriod.WEEK -> 10000f to 5
            ChartPeriod.MONTH -> 15000f to 4
            ChartPeriod.SIX_MONTH -> 100000f to 5
            ChartPeriod.YEAR -> 500000f to 5
        }
        
        binding.lineChart.axisRight.apply {
            axisMaximum = yMax
            setLabelCount(labelCount, true)
        }
        
        val dataSet = LineDataSet(entries, "Steps").apply {
            color = ContextCompat.getColor(requireContext(), R.color.chart_line)
            lineWidth = 4f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawFilled(false)
        }
        
        binding.lineChart.apply {
            data = LineData(dataSet)
            xAxis.labelCount = minOf(labels.size, 5)
            animateX(500)
            invalidate()
        }
        
        android.util.Log.d("HomeFragment", "Chart updated with labels: $labels, yMax: $yMax")
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}