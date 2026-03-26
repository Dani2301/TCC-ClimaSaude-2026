package com.climasaude.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission() || !isLocationEnabled()) {
            if (!continuation.isCompleted) continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && (System.currentTimeMillis() - location.time) < 600000) {
                    if (!continuation.isCompleted) continuation.resume(location)
                } else {
                    requestNewLocation(continuation)
                }
            }.addOnFailureListener {
                requestNewLocation(continuation)
            }
        } catch (e: Exception) {
            if (!continuation.isCompleted) continuation.resume(null)
        }
    }

    private fun requestNewLocation(continuation: kotlinx.coroutines.CancellableContinuation<Location?>) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!continuation.isCompleted) continuation.resume(result.lastLocation)
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
            // Timeout de segurança para não travar o app. Modificado por: Daniel
            android.os.Handler(context.mainLooper).postDelayed({
                if (!continuation.isCompleted) {
                    continuation.resume(null)
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }, 8000)
        } catch (e: Exception) {
            if (!continuation.isCompleted) continuation.resume(null)
        }
    }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun getCityFromCoordinates(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            // Senior Fix: Compatibilidade com Android 13+ e tratamento de erro assíncrono. Modificado por: Daniel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val city = addresses.firstOrNull()?.locality ?: addresses.firstOrNull()?.subAdminArea ?: "Cidade Atual"
                        cont.resume(city)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { it.locality ?: it.subAdminArea } ?: "Cidade Atual"
            }
        } catch (e: Exception) {
            "Cidade Atual"
        }
    }
}
