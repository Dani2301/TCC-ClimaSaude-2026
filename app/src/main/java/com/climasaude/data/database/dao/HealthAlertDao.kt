package com.climasaude.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.climasaude.data.database.entities.HealthAlert
import com.climasaude.data.database.entities.AlertSeverity
import com.climasaude.data.database.entities.AlertType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface HealthAlertDao {

    @Query("SELECT * FROM health_alerts WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveAlertsFlow(userId: String): Flow<List<HealthAlert>>

    @Query("SELECT * FROM health_alerts WHERE userId = :userId AND isRead = 0 AND isActive = 1 ORDER BY severity DESC, createdAt DESC")
    suspend fun getUnreadAlerts(userId: String): List<HealthAlert>

    @Query("SELECT * FROM health_alerts WHERE userId = :userId AND severity = :severity AND isActive = 1 ORDER BY createdAt DESC")
    suspend fun getAlertsBySeverity(userId: String, severity: AlertSeverity): List<HealthAlert>

    @Query("SELECT * FROM health_alerts WHERE userId = :userId AND type = :type AND isActive = 1 ORDER BY createdAt DESC")
    suspend fun getAlertsByType(userId: String, type: AlertType): List<HealthAlert>

    @Query("SELECT * FROM health_alerts WHERE userId = :userId AND createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    suspend fun getAlertsInPeriod(userId: String, startDate: Date, endDate: Date): List<HealthAlert>

    @Query("SELECT COUNT(*) FROM health_alerts WHERE userId = :userId AND isRead = 0 AND isActive = 1")
    fun getUnreadAlertsCountFlow(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: HealthAlert)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<HealthAlert>)

    @Update
    suspend fun updateAlert(alert: HealthAlert)

    @Query("UPDATE health_alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAsRead(alertId: String)

    @Query("UPDATE health_alerts SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)

    @Query("UPDATE health_alerts SET isActive = 0 WHERE id = :alertId")
    suspend fun dismissAlert(alertId: String)

    @Query("DELETE FROM health_alerts WHERE userId = :userId AND createdAt < :cutoffDate")
    suspend fun deleteOldAlerts(userId: String, cutoffDate: Date)
}