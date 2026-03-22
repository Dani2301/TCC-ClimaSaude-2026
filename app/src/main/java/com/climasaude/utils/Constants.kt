package com.climasaude.utils

object Constants {

    // API Configuration
    const val WEATHER_API_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    const val HEALTH_API_BASE_URL = "https://api.climasaude.com/v1/"

    // Database Configuration
    const val DATABASE_NAME = "clima_saude_database"
    const val DATABASE_VERSION = 1

    // SharedPreferences
    const val PREFS_NAME = "clima_saude_prefs"
    const val ENCRYPTED_PREFS_NAME = "clima_saude_encrypted_prefs"

    // Notification Channels
    const val WEATHER_ALERTS_CHANNEL_ID = "weather_alerts"
    const val MEDICATION_REMINDERS_CHANNEL_ID = "medication_reminders"
    const val HEALTH_TIPS_CHANNEL_ID = "health_tips"
    const val EMERGENCY_ALERTS_CHANNEL_ID = "emergency_alerts"

    // Request Codes
    const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    const val CAMERA_PERMISSION_REQUEST_CODE = 1002
    const val STORAGE_PERMISSION_REQUEST_CODE = 1003
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1004

    // Intent Extras
    const val EXTRA_USER_ID = "extra_user_id"
    const val EXTRA_WEATHER_DATA = "extra_weather_data"
    const val EXTRA_ALERT_ID = "extra_alert_id"
    const val EXTRA_MEDICATION_ID = "extra_medication_id"

    // Work Manager Tags
    const val WEATHER_SYNC_WORK_TAG = "weather_sync_work"
    const val MEDICATION_REMINDER_WORK_TAG = "medication_reminder_work"
    const val HEALTH_CHECK_WORK_TAG = "health_check_work"

    // Cache Settings
    const val WEATHER_CACHE_DURATION_MINUTES = 30
    const val USER_DATA_CACHE_DURATION_HOURS = 24

    // Validation Constants
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_NAME_LENGTH = 100
    const val MAX_DESCRIPTION_LENGTH = 500
    const val MIN_AGE = 13
    const val MAX_AGE = 120

    // Weather Thresholds
    const val HIGH_TEMPERATURE_THRESHOLD = 30.0
    const val LOW_TEMPERATURE_THRESHOLD = 5.0
    const val HIGH_HUMIDITY_THRESHOLD = 80
    const val LOW_HUMIDITY_THRESHOLD = 30
    const val HIGH_UV_INDEX_THRESHOLD = 6.0
    const val POOR_AIR_QUALITY_THRESHOLD = 3

    // Health Thresholds
    const val HIGH_SYMPTOM_SEVERITY_THRESHOLD = 7
    const val CRITICAL_SYMPTOM_SEVERITY_THRESHOLD = 9
    const val LOW_MEDICATION_ADHERENCE_THRESHOLD = 0.8
    const val STRONG_CORRELATION_THRESHOLD = 0.6

    // Sync Settings
    const val DEFAULT_SYNC_INTERVAL_MINUTES = 30
    const val BACKGROUND_SYNC_INTERVAL_HOURS = 6
    const val EMERGENCY_SYNC_TIMEOUT_SECONDS = 10

    // File Paths
    const val EXPORT_DIRECTORY = "ClimaSaude/Exports"
    const val PHOTOS_DIRECTORY = "ClimaSaude/Photos"
    const val REPORTS_DIRECTORY = "ClimaSaude/Reports"

    // Date Formats
    const val DATE_FORMAT_DISPLAY = "dd/MM/yyyy"
    const val DATE_TIME_FORMAT_DISPLAY = "dd/MM/yyyy HH:mm"
    const val DATE_FORMAT_API = "yyyy-MM-dd"
    const val TIME_FORMAT_DISPLAY = "HH:mm"

    // Error Messages
    const val ERROR_NETWORK_UNAVAILABLE = "Conexão com a internet não disponível"
    const val ERROR_LOCATION_UNAVAILABLE = "Localização não disponível"
    const val ERROR_AUTHENTICATION_FAILED = "Falha na autenticação"
    const val ERROR_DATA_SYNC_FAILED = "Falha na sincronização de dados"
    const val ERROR_INVALID_INPUT = "Dados inválidos fornecidos"

    // Success Messages
    const val SUCCESS_DATA_SAVED = "Dados salvos com sucesso"
    const val SUCCESS_DATA_SYNCED = "Dados sincronizados com sucesso"
    const val SUCCESS_PROFILE_UPDATED = "Perfil atualizado com sucesso"
    const val SUCCESS_SETTINGS_SAVED = "Configurações salvas com sucesso"

    // Feature Flags
    const val FEATURE_BIOMETRIC_AUTH = true
    const val FEATURE_OFFLINE_MODE = true
    const val FEATURE_ANALYTICS = true
    const val FEATURE_CRASH_REPORTING = true
    const val FEATURE_BETA_FEATURES = false

    // URLs
    const val PRIVACY_POLICY_URL = "https://climasaude.com/privacy"
    const val TERMS_OF_SERVICE_URL = "https://climasaude.com/terms"
    const val SUPPORT_URL = "https://climasaude.com/support"
    const val HELP_URL = "https://climasaude.com/help"

    // Contact Information
    const val SUPPORT_EMAIL = "suporte@climasaude.com"
    const val SUPPORT_PHONE = "+55 11 99999-9999"
    const val EMERGENCY_NUMBER = "192" // SAMU Brasil
}