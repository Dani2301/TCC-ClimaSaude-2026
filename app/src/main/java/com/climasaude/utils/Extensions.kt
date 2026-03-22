package com.climasaude.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.round

// Context Extensions
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

    return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.shareText(text: String, title: String = "Compartilhar") {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    startActivity(Intent.createChooser(intent, title))
}

// View Extensions
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

fun View.showSnackbarWithAction(
    message: String,
    actionText: String,
    action: () -> Unit,
    duration: Int = Snackbar.LENGTH_LONG
) {
    Snackbar.make(this, message, duration)
        .setAction(actionText) { action() }
        .show()
}

// Fragment Extensions
fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    context?.showToast(message, duration)
}

fun Fragment.isNetworkAvailable(): Boolean {
    return context?.isNetworkAvailable() ?: false
}

// String Extensions
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPhone(): Boolean {
    return android.util.Patterns.PHONE.matcher(this).matches()
}

fun String.capitalizeFirstLetter(): String {
    return if (isEmpty()) this else first().uppercaseChar() + drop(1).lowercase()
}

fun String.maskEmail(): String {
    val parts = split("@")
    if (parts.size != 2) return this

    val username = parts[0]
    val domain = parts[1]

    return if (username.length <= 2) {
        "*@$domain"
    } else {
        "${username.take(2)}${"*".repeat(username.length - 2)}@$domain"
    }
}

fun String.maskPhone(): String {
    return if (length >= 4) {
        "*".repeat(length - 4) + takeLast(4)
    } else {
        "*".repeat(length)
    }
}

// Date Extensions
fun Date.formatToString(pattern: String = Constants.DATE_FORMAT_DISPLAY): String {
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return format.format(this)
}

fun Date.isToday(): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { time = this@isToday }

    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}

fun Date.isThisWeek(): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { time = this@isThisWeek }

    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            today.get(Calendar.WEEK_OF_YEAR) == date.get(Calendar.WEEK_OF_YEAR)
}

fun Date.daysBetween(other: Date): Int {
    val diff = kotlin.math.abs(this.time - other.time)
    return (diff / (1000 * 60 * 60 * 24)).toInt()
}

fun Date.addDays(days: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.DAY_OF_MONTH, days)
    return calendar.time
}

fun Date.addHours(hours: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.HOUR_OF_DAY, hours)
    return calendar.time
}

fun Date.getTimeAgo(context: Context): String {
    val now = Date()
    val diff = now.time - this.time

    return when {
        diff < 60 * 1000 -> "Agora"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m atrás"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h atrás"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d atrás"
        else -> formatToString()
    }
}

// Number Extensions
fun Double.roundToDecimals(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals.toDouble())
    return round(this * multiplier) / multiplier
}

fun Float.roundToDecimals(decimals: Int): Float {
    val multiplier = 10.0.pow(decimals.toDouble())
    return (round(this * multiplier) / multiplier).toFloat()
}

fun Double.formatTemperature(): String {
    return "${this.roundToDecimals(1)}°C"
}

fun Double.formatPercentage(): String {
    return "${(this * 100).roundToDecimals(1)}%"
}

fun Int.formatPercentage(): String {
    return "$this%"
}

fun Float.formatWeight(): String {
    return "${this.roundToDecimals(1)} kg"
}

fun Float.formatHeight(): String {
    return "${this.roundToDecimals(2)} m"
}

fun Float.calculateBMI(height: Float): Float {
    val heightInMeters = height / 100
    return this / (heightInMeters * heightInMeters)
}

fun Float.getBMICategory(): String {
    return when {
        this < 18.5 -> "Abaixo do peso"
        this < 25.0 -> "Peso normal"
        this < 30.0 -> "Sobrepeso"
        this < 35.0 -> "Obesidade grau I"
        this < 40.0 -> "Obesidade grau II"
        else -> "Obesidade grau III"
    }
}

// List Extensions
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index >= 0 && index < size) this[index] else null
}

fun <T> MutableList<T>.addIfNotExists(item: T) {
    if (!contains(item)) {
        add(item)
    }
}

fun <T> List<T>.chunkedByDate(dateSelector: (T) -> Date): Map<String, List<T>> {
    return groupBy { item ->
        dateSelector(item).formatToString(Constants.DATE_FORMAT_DISPLAY)
    }
}

// Map Extensions
fun Map<String, Any?>.toJson(): String {
    return com.google.gson.Gson().toJson(this)
}

// Weather-specific Extensions
fun Double.getTemperatureColor(): Int {
    return when {
        this < 0 -> android.graphics.Color.BLUE
        this < 10 -> android.graphics.Color.CYAN
        this < 20 -> android.graphics.Color.GREEN
        this < 30 -> android.graphics.Color.YELLOW
        this < 40 -> android.graphics.Color.rgb(255, 165, 0) // Orange
        else -> android.graphics.Color.RED
    }
}

fun Int.getHumidityLevel(): String {
    return when {
        this < 30 -> "Baixa"
        this < 50 -> "Moderada"
        this < 70 -> "Confortável"
        this < 85 -> "Alta"
        else -> "Muito Alta"
    }
}

fun Double.getUVLevel(): String {
    return when {
        this < 3 -> "Baixo"
        this < 6 -> "Moderado"
        this < 8 -> "Alto"
        this < 11 -> "Muito Alto"
        else -> "Extremo"
    }
}

fun Int.getAirQualityLevel(): String {
    return when (this) {
        1 -> "Boa"
        2 -> "Razoável"
        3 -> "Moderada"
        4 -> "Ruim"
        5 -> "Muito Ruim"
        else -> "Desconhecida"
    }
}

fun Int.getAirQualityColor(): Int {
    return when (this) {
        1 -> android.graphics.Color.GREEN
        2 -> android.graphics.Color.YELLOW
        3 -> android.graphics.Color.rgb(255, 165, 0) // Orange
        4 -> android.graphics.Color.RED
        5 -> android.graphics.Color.rgb(128, 0, 128) // Purple
        else -> android.graphics.Color.GRAY
    }
}

// Health-specific Extensions
fun Int.getSeverityLevel(): String {
    return when {
        this <= 2 -> "Leve"
        this <= 5 -> "Moderada"
        this <= 7 -> "Intensa"
        this <= 9 -> "Severa"
        else -> "Crítica"
    }
}

fun Int.getSeverityColor(): Int {
    return when {
        this <= 2 -> android.graphics.Color.GREEN
        this <= 5 -> android.graphics.Color.YELLOW
        this <= 7 -> android.graphics.Color.rgb(255, 165, 0) // Orange
        this <= 9 -> android.graphics.Color.RED
        else -> android.graphics.Color.rgb(128, 0, 128) // Purple
    }
}

fun Double.getAdherenceLevel(): String {
    return when {
        this >= 0.9 -> "Excelente"
        this >= 0.8 -> "Boa"
        this >= 0.7 -> "Regular"
        this >= 0.5 -> "Ruim"
        else -> "Muito Ruim"
    }
}

fun Double.getAdherenceColor(): Int {
    return when {
        this >= 0.9 -> android.graphics.Color.GREEN
        this >= 0.8 -> android.graphics.Color.rgb(173, 255, 47) // GreenYellow
        this >= 0.7 -> android.graphics.Color.YELLOW
        this >= 0.5 -> android.graphics.Color.rgb(255, 165, 0) // Orange
        else -> android.graphics.Color.RED
    }
}
