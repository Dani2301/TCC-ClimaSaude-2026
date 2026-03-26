package com.climasaude.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.climasaude.data.database.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("UPDATE users SET isActive = 0 WHERE id = :userId")
    suspend fun deactivateUser(userId: String)

    @Query("SELECT * FROM users WHERE isActive = 1")
    suspend fun getAllActiveUsers(): List<User>

    @Query("UPDATE users SET theme = :theme WHERE id = :userId")
    suspend fun updateTheme(userId: String, theme: String)

    @Query("UPDATE users SET language = :language WHERE id = :userId")
    suspend fun updateLanguage(userId: String, language: String)

    @Query("UPDATE users SET notificationPreferences = :preferences WHERE id = :userId")
    suspend fun updateNotificationPreferences(userId: String, preferences: Map<String, Boolean>)

    @Query("UPDATE users SET photoUrl = :photoUrl WHERE id = :userId")
    suspend fun updatePhotoUrl(userId: String, photoUrl: String)

    @Query("UPDATE users SET privacySettings = :settings WHERE id = :userId")
    suspend fun updatePrivacySettings(userId: String, settings: Map<String, Boolean>)
}
