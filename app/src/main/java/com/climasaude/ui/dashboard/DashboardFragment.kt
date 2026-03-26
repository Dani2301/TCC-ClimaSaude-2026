package com.climasaude.ui.dashboard

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.climasaude.R
import com.climasaude.databinding.FragmentDashboardBinding
import com.climasaude.presentation.viewmodels.DashboardViewModel
import com.climasaude.presentation.viewmodels.DashboardState
import com.climasaude.domain.usecases.HealthRiskLevel
import com.climasaude.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

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

        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.navigation_dashboard, inclusive = false)
            .build()

        binding.cardAddSymptom.setOnClickListener {
            findNavController().navigate(R.id.navigation_health, null, navOptions)
        }

        binding.cardMedicationReminder.setOnClickListener {
            findNavController().navigate(R.id.navigation_health, null, navOptions)
        }

        binding.cardViewReports.setOnClickListener {
            findNavController().navigate(R.id.navigation_reports, null, navOptions)
        }

        binding.cardEmergency.setOnClickListener {
            findNavController().navigate(R.id.navigation_alerts, null, navOptions)
        }

        binding.cardProfilePicture.setOnClickListener {
            findNavController().navigate(R.id.navigation_profile, null, navOptions)
        }
        
        binding.cardCurrentWeather.setOnClickListener {
            findNavController().navigate(R.id.navigation_weather, null, navOptions)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dashboardState.collect { state ->
                when (state) {
                    is DashboardState.Loading -> { }
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isRefreshing.collect { isRefreshing ->
                binding.swipeRefreshLayout.isRefreshing = isRefreshing
            }
        }
    }

    private fun updateWeatherUI() {
        val weather = viewModel.weatherCondition.value ?: return
        
        binding.textviewLocation.text = String.format(Locale.getDefault(), "%s, %s", weather.location.city, weather.location.country)
        binding.textviewTemperature.text = getString(R.string.temp_format, weather.current.temperature.toInt())
        binding.textviewWeatherDescription.text = weather.current.description.replaceFirstChar { it.uppercase() }
        binding.textviewHumidity.text = getString(R.string.humidity_format, weather.current.humidity)
        binding.textviewWindSpeed.text = getString(R.string.wind_format, weather.current.windSpeed.toInt())
        
        val iconRes = when {
            weather.current.condition.contains("Cloud", true) -> R.drawable.ic_weather
            weather.current.condition.contains("Rain", true) -> R.drawable.ic_weather_alert_moderate
            else -> R.drawable.ic_weather_sunny
        }
        binding.imageviewWeatherIcon.setImageResource(iconRes)
    }

    private fun updateHealthRiskUI() {
        val risk = viewModel.healthRiskAssessment.value ?: return
        
        val (label, colorRes, strokeColor) = when(risk.overallRisk) {
            HealthRiskLevel.LOW -> Triple("Baixo", android.R.color.holo_green_dark, android.R.color.transparent)
            HealthRiskLevel.MEDIUM -> Triple("Médio", android.R.color.holo_orange_light, android.R.color.transparent)
            HealthRiskLevel.HIGH -> Triple("Alto!", android.R.color.holo_red_light, android.R.color.holo_red_dark)
            HealthRiskLevel.CRITICAL -> Triple("CRÍTICO", android.R.color.holo_red_dark, android.R.color.black)
        }

        binding.chipRiskLevel.text = label
        binding.chipRiskLevel.setChipBackgroundColorResource(colorRes)
        
        if (risk.overallRisk >= HealthRiskLevel.HIGH) {
            binding.cardHealthRisk.strokeWidth = 4
            binding.cardHealthRisk.setStrokeColor(ContextCompat.getColorStateList(requireContext(), strokeColor))
            binding.textviewRiskDescription.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        } else {
            binding.cardHealthRisk.strokeWidth = 0
            // Resetar cor para o padrão do tema. Modificado por: Daniel
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
            binding.textviewRiskDescription.setTextColor(typedValue.data)
        }
        
        if (risk.recommendations.isNotEmpty()) {
            binding.textviewRiskDescription.text = risk.recommendations.first()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
