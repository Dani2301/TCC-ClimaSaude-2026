package com.climasaude.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.climasaude.domain.usecases.WeatherUseCases
import com.climasaude.domain.usecases.RiskLevel
import com.climasaude.utils.LocationUtils
import com.climasaude.utils.NotificationUtils
import com.climasaude.utils.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WeatherAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val weatherUseCases: WeatherUseCases,
    private val locationUtils: LocationUtils,
    private val notificationUtils: NotificationUtils
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val location = locationUtils.getCurrentLocation() ?: return Result.retry()
            
            val weatherResult = weatherUseCases.getCurrentWeather(location.latitude, location.longitude, true)
            
            if (weatherResult is Resource.Success && weatherResult.data != null) {
                val weather = weatherResult.data
                val riskAnalysis = weatherUseCases.analyzeWeatherRisk(weather)
                
                // Sincronizado com a assinatura de NotificationUtils. Modificado por: Daniel
                if (riskAnalysis.overallRisk == RiskLevel.HIGH || riskAnalysis.overallRisk == RiskLevel.CRITICAL) {
                    val message = riskAnalysis.recommendations.firstOrNull() ?: "Condições climáticas extremas detectadas."
                    notificationUtils.showWeatherAlert(
                        title = "Alerta de Saúde e Clima",
                        message = message
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("WeatherAlertWorker", "Erro ao verificar clima em background: ${e.message}")
            Result.failure()
        }
    }
}
