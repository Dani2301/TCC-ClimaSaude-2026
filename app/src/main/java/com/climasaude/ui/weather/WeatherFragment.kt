package com.climasaude.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.climasaude.R
import com.climasaude.databinding.FragmentWeatherBinding
import com.climasaude.presentation.viewmodels.WeatherViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WeatherViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.loadWeatherData(forceRefresh = true)
        } else {
            Toast.makeText(context, "Permissão de localização negada. O clima não pode ser atualizado.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        checkLocationPermission()
    }

    private fun setupUI() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadWeatherData(forceRefresh = true)
        }

        binding.editSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val cityName = v.text.toString()
                if (cityName.isNotBlank()) {
                    viewModel.searchByCity(cityName)
                }
                true
            } else {
                false
            }
        }

        binding.layoutSearch.setEndIconOnClickListener {
            val cityName = binding.editSearch.text.toString()
            if (cityName.isNotBlank()) {
                viewModel.searchByCity(cityName)
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadWeatherData()
            }
            else -> {
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.currentWeather.collect { weather ->
                weather?.let {
                    binding.textCity.text = it.location.city
                    binding.textTemp.text = getString(R.string.temp_format, it.current.temperature.toInt())
                    binding.textDescription.text = it.current.description.replaceFirstChar { char -> char.uppercase() }
                    binding.textHumidityVal.text = getString(R.string.humidity_format, it.current.humidity)
                    binding.textWindVal.text = getString(R.string.wind_format, it.current.windSpeed.toInt())
                    
                    // Definir ícone baseado na condição (simplificado)
                    if (it.current.condition.contains("Cloud", ignoreCase = true)) {
                        binding.imageWeatherLarge.setImageResource(R.drawable.ic_weather)
                    } else if (it.current.condition.contains("Rain", ignoreCase = true)) {
                        binding.imageWeatherLarge.setImageResource(R.drawable.ic_weather_alert_moderate)
                    } else {
                        binding.imageWeatherLarge.setImageResource(R.drawable.ic_weather_sunny)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.isRefreshing.collect { isRefreshing ->
                binding.swipeRefresh.isRefreshing = isRefreshing
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
