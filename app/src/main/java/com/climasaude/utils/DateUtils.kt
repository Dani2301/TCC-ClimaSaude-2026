package com.climasaude.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayDateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    init {
        apiDateTimeFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun formatDate(date: Date): String {
        return displayDateFormat.format(date)
    }

    fun formatTime(date: Date): String {
        return displayTimeFormat.format(date)
    }

    fun formatDateTime(date: Date): String {
        return displayDateTimeFormat.format(date)
    }

    fun formatForApi(date: Date): String {
        return apiDateFormat.format(date)
    }

    fun formatDateTimeForApi(date: Date): String {
        return apiDateTimeFormat.format(date)
    }

    fun parseApiDate(dateString: String): Date? {
        return try {
            apiDateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun parseApiDateTime(dateTimeString: String): Date? {
        return try {
            apiDateTimeFormat.parse(dateTimeString)
        } catch (e: Exception) {
            null
        }
    }

    fun parseDate(dateString: String): Date? {
        return try {
            displayDateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun parseTime(timeString: String): Date? {
        return try {
            displayTimeFormat.parse(timeString)
        } catch (e: Exception) {
            null
        }
    }

    fun getStartOfDay(date: Date = Date()): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    fun getEndOfDay(date: Date = Date()): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    fun getStartOfWeek(date: Date = Date()): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        return getStartOfDay(calendar.time)
    }

    fun getEndOfWeek(date: Date = Date()): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        return getEndOfDay(calendar.time)
    }

    fun getStartOfMonth(date: Date = Date()): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return getStartOfDay(calendar.time)
    }

    fun getEndOfMonth(date: Date = Date()): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return getEndOfDay(calendar.time)
    }

    fun addDays(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.time
    }

    fun addWeeks(date: Date, weeks: Int): Date {
        return addDays(date, weeks * 7)
    }

    fun addMonths(date: Date, months: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MONTH, months)
        return calendar.time
    }

    fun addYears(date: Date, years: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.YEAR, years)
        return calendar.time
    }

    fun daysBetween(startDate: Date, endDate: Date): Int {
        val diffInMillis = endDate.time - startDate.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    fun hoursBetween(startDate: Date, endDate: Date): Int {
        val diffInMillis = endDate.time - startDate.time
        return (diffInMillis / (1000 * 60 * 60)).toInt()
    }

    fun minutesBetween(startDate: Date, endDate: Date): Int {
        val diffInMillis = endDate.time - startDate.time
        return (diffInMillis / (1000 * 60)).toInt()
    }

    fun isToday(date: Date): Boolean {
        return isSameDay(date, Date())
    }

    fun isYesterday(date: Date): Boolean {
        val yesterday = addDays(Date(), -1)
        return isSameDay(date, yesterday)
    }

    fun isTomorrow(date: Date): Boolean {
        val tomorrow = addDays(Date(), 1)
        return isSameDay(date, tomorrow)
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.time = date1
        val cal2 = Calendar.getInstance()
        cal2.time = date2

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isSameWeek(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.time = date1
        val cal2 = Calendar.getInstance()
        cal2.time = date2

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }

    fun isSameMonth(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.time = date1
        val cal2 = Calendar.getInstance()
        cal2.time = date2

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    fun getAge(birthDate: Date): Int {
        val birth = Calendar.getInstance()
        birth.time = birthDate
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }

    fun getRelativeTimeString(date: Date): String {
        val now = Date()
        val diffInMinutes = minutesBetween(date, now)

        return when {
            diffInMinutes < 1 -> "Agora"
            diffInMinutes < 60 -> "${diffInMinutes}m atrás"
            diffInMinutes < 60 * 24 -> "${diffInMinutes / 60}h atrás"
            diffInMinutes < 60 * 24 * 7 -> "${diffInMinutes / (60 * 24)}d atrás"
            else -> formatDate(date)
        }
    }

    fun getTimeUntilString(date: Date): String {
        val now = Date()
        val diffInMinutes = minutesBetween(now, date)

        return when {
            diffInMinutes < 0 -> "Atrasado"
            diffInMinutes < 1 -> "Agora"
            diffInMinutes < 60 -> "Em ${diffInMinutes}m"
            diffInMinutes < 60 * 24 -> "Em ${diffInMinutes / 60}h"
            diffInMinutes < 60 * 24 * 7 -> "Em ${diffInMinutes / (60 * 24)}d"
            else -> formatDate(date)
        }
    }

    fun createTimeFromString(timeString: String): Date? {
        return try {
            val parts = timeString.split(":")
            if (parts.size != 2) return null

            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            if (hour !in 0..23 || minute !in 0..59) return null

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            calendar.time
        } catch (e: Exception) {
            null
        }
    }

    fun getTimeStringFromDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }

    fun getCurrentSeason(): String {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> "verão"
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> "outono"
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> "inverno"
            else -> "primavera"
        }
    }

    fun isQuietHours(quietStart: String, quietEnd: String, currentTime: Date = Date()): Boolean {
        val startTime = createTimeFromString(quietStart) ?: return false
        val endTime = createTimeFromString(quietEnd) ?: return false

        val currentCalendar = Calendar.getInstance()
        currentCalendar.time = currentTime
        val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentCalendar.get(Calendar.MINUTE)

        val startCalendar = Calendar.getInstance()
        startCalendar.time = startTime
        val startHour = startCalendar.get(Calendar.HOUR_OF_DAY)
        val startMinute = startCalendar.get(Calendar.MINUTE)

        val endCalendar = Calendar.getInstance()
        endCalendar.time = endTime
        val endHour = endCalendar.get(Calendar.HOUR_OF_DAY)
        val endMinute = endCalendar.get(Calendar.MINUTE)

        val currentMinutes = currentHour * 60 + currentMinute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            // Same day quiet hours
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight quiet hours
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}