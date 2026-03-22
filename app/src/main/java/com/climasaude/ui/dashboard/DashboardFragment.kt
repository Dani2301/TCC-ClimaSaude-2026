package com.climasaude.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.climasaude.R
import com.climasaude.databinding.FragmentDashboardBinding
import com.climasaude.presentation.viewmodels.DashboardViewModel
import com.climasaude.presentation.viewmodels.DashboardState
import com.climasaude.domain.usecases.RiskLevel
import com.climasaude.domain.usecases.HealthRiskLevel
import com.climasaude.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }

        binding.cardAddSymptom.setOnClickListener {
            findNavController().navigate(R.id.navigation_health)
        }

        binding.cardMedicationReminder.setOnClickListener {
            findNavController().navigate(R.id.navigation_health)
        }

        binding.cardViewReports.setOnClickListener {
            findNavController().navigate(R.id.navigation_reports)
        }

        binding.cardEmergency.setOnClickListener {
            findNavController().navigate(R.id.navigation_alerts)
        }

        binding.cardProfilePicture.setOnClickListener {
            findNavController().navigate(R.id.navigation_profile)
        }
        
        binding.cardCurrentWeather.setOnClickListener {
            findNavController().navigate(R.id.navigation_weather)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.dashboardState.collect { state ->
                when (state) {
                    is DashboardState.Loading -> {
                        // Show loading if needed (SwipeRefresh handles it mostly)
                    }
                    is DashboardState.Success -> {
                        updateWeatherUI()
                        updateHealthRiskUI()
                    }
                    is DashboardState.Error -> {
                        binding.root.showSnackbar(state.message)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isRefreshing.collect { isRefreshing ->
                binding.swipeRefreshLayout.isRefreshing = isRefreshing
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    binding.root.showSnackbar(it)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun updateWeatherUI() {
        val weather = viewModel.weatherCondition.value ?: return
        
        binding.textviewLocation.text = "${weather.location.city}, ${weather.location.country}"
        binding.textviewTemperature.text = getString(R.string.temp_format, weather.current.temperature.toInt())
        binding.textviewWeatherDescription.text = weather.current.description.replaceFirstChar { it.uppercase() }
        binding.textviewHumidity.text = getString(R.string.humidity_format, weather.current.humidity)
        binding.textviewWindSpeed.text = getString(R.string.wind_format, weather.current.windSpeed.toInt())
        
        // Icon selection
        val iconRes = when {
            weather.current.condition.contains("Cloud", true) -> R.drawable.ic_weather
            weather.current.condition.contains("Rain", true) -> R.drawable.ic_weather_alert_moderate
            else -> R.drawable.ic_weather_sunny
        }
        binding.imageviewWeatherIcon.setImageResource(iconRes)
    }

    private fun updateHealthRiskUI() {
        val risk = viewModel.healthRiskAssessment.value ?: return
        
        binding.chipRiskLevel.text = when(risk.overallRisk) {
            HealthRiskLevel.LOW -> "Baixo"
            HealthRiskLevel.MEDIUM -> "Médio"
            HealthRiskLevel.HIGH -> "Alto"
            HealthRiskLevel.CRITICAL -> "Crítico"
        }
        
        val colorRes = when(risk.overallRisk) {
            HealthRiskLevel.LOW -> android.R.color.holo_green_dark
            HealthRiskLevel.MEDIUM -> android.R.color.holo_orange_light
            HealthRiskLevel.HIGH -> android.R.color.holo_orange_dark
            HealthRiskLevel.CRITICAL -> android.R.color.holo_red_dark
        }
        binding.chipRiskLevel.setChipBackgroundColorResource(colorRes)
        
        if (risk.recommendations.isNotEmpty()) {
            binding.textviewRiskDescription.text = risk.recommendations.first()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
