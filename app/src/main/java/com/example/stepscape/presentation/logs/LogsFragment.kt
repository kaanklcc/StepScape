package com.example.stepscape.presentation.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepscape.databinding.FragmentLogsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LogsViewModel by viewModels()
    
    private lateinit var logsAdapter: LogsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            binding.headerLayout.setPadding(
                binding.headerLayout.paddingLeft,
                insets.top + 16,
                binding.headerLayout.paddingRight,
                binding.headerLayout.paddingBottom
            )
            
            v.setPadding(0, 0, 0, insets.bottom)
            
            WindowInsetsCompat.CONSUMED
        }
        
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
    }
    
    private fun setupRecyclerView() {
        logsAdapter = LogsAdapter()
        binding.rvLogs.apply {
            adapter = logsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.sessions.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvLogs.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvLogs.visibility = View.VISIBLE
                    // Sort by most recent first and submit to adapter
                    val sortedSessions = state.sessions.sortedByDescending { it.startTime }
                    logsAdapter.submitList(sortedSessions)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
