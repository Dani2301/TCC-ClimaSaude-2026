package com.climasaude.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "clima_saude_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LOCATION_PERMISSION_GRANTED = "location_permission_granted"
        private const val KEY_NOTIFICATION_PERMISSION_GRANTED = "notification_permission_granted"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_OFFLINE_MODE = "offline_mode"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_SYNC_FREQUENCY = "sync_frequency"
        private const val KEY_WEATHER_UNITS = "weather_units"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_HOURS_START = "quiet_hours_start"
        private const val KEY_QUIET_HOURS_END = "quiet_hours_end"
        
        // Chaves para persistência de localização. Modificado por: Daniel
        private const val KEY_LAST_LAT = "last_lat"
        private const val KEY_LAST_LON = "last_lon"
    }

    // User Authentication
    fun setUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String {
        return sharedPreferences.getString(KEY_USER_ID, "") ?: ""
    }

    fun setUserLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_USER_LOGGED_IN, isLoggedIn).apply()
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)
    }

    // Biometric Authentication
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun clearBiometricEnabled() {
        sharedPreferences.edit().remove(KEY_BIOMETRIC_ENABLED).apply()
    }

    // Theme and Language
    fun setTheme(theme: String) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(): String {
        return sharedPreferences.getString(KEY_THEME, "auto") ?: "auto"
    }

    fun setLanguage(language: String) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun getLanguage(): String {
        return sharedPreferences.getString(KEY_LANGUAGE, "pt-BR") ?: "pt-BR"
    }

    // App State
    fun setFirstLaunch(isFirstLaunch: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch).apply()
    }

    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    // Permissions
    fun setLocationPermissionGranted(granted: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_LOCATION_PERMISSION_GRANTED, granted).apply()
    }

    fun isLocationPermissionGranted(): Boolean {
        return sharedPreferences.getBoolean(KEY_LOCATION_PERMISSION_GRANTED, false)
    }

    fun setNotificationPermissionGranted(granted: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_GRANTED, granted).apply()
    }

    fun isNotificationPermissionGranted(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_PERMISSION_GRANTED, false)
    }

    // Sync Settings
    fun setLastSyncTime(time: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC_TIME, time).apply()
    }

    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_SYNC_TIME, 0)
    }

    fun setOfflineMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_OFFLINE_MODE, enabled).apply()
    }

    fun isOfflineMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_OFFLINE_MODE, false)
    }

    fun setAutoSync(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun isAutoSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_SYNC, true)
    }

    fun setSyncFrequency(frequency: Int) {
        sharedPreferences.edit().putInt(KEY_SYNC_FREQUENCY, frequency).apply()
    }

    fun getSyncFrequency(): Int {
        return sharedPreferences.getInt(KEY_SYNC_FREQUENCY, 30) // 30 minutes default
    }

    // Weather Settings
    fun setWeatherUnits(units: String) {
        sharedPreferences.edit().putString(KEY_WEATHER_UNITS, units).apply()
    }

    fun getWeatherUnits(): String {
        return sharedPreferences.getString(KEY_WEATHER_UNITS, "metric") ?: "metric"
    }

    // Notification Settings
    fun setQuietHoursEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply()
    }

    fun isQuietHoursEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_QUIET_HOURS_ENABLED, false)
    }

    fun setQuietHoursStart(time: String) {
        sharedPreferences.edit().putString(KEY_QUIET_HOURS_START, time).apply()
    }

    fun getQuietHoursStart(): String {
        return sharedPreferences.getString(KEY_QUIET_HOURS_START, "22:00") ?: "22:00"
    }

    fun setQuietHoursEnd(time: String) {
        sharedPreferences.edit().putString(KEY_QUIET_HOURS_END, time).apply()
    }

    fun getQuietHoursEnd(): String {
        return sharedPreferences.getString(KEY_QUIET_HOURS_END, "07:00") ?: "07:00"
    }

    // Last Searched Location. Modificado por: Daniel
    fun setLastLocation(latitude: Double, longitude: Double) {
        sharedPreferences.edit()
            .putFloat(KEY_LAST_LAT, latitude.toFloat())
            .putFloat(KEY_LAST_LON, longitude.toFloat())
            .apply()
    }

    fun getLastLatitude(): Double {
        return sharedPreferences.getFloat(KEY_LAST_LAT, 0f).toDouble()
    }

    fun getLastLongitude(): Double {
        return sharedPreferences.getFloat(KEY_LAST_LON, 0f).toDouble()
    }

    // Clear all data
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    // Clear user-specific data only
    fun clearUserData() {
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_LOGGED_IN)
            .remove(KEY_BIOMETRIC_ENABLED)
            .remove(KEY_LAST_SYNC_TIME)
            .apply()
    }
}
