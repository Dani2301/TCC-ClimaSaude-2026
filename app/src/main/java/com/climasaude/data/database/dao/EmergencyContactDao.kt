package com.climasaude.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.climasaude.data.database.entities.EmergencyContact
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId AND isActive = 1 ORDER BY priority ASC, name ASC")
    fun getEmergencyContactsFlow(userId: String): Flow<List<EmergencyContact>>

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId AND isActive = 1 ORDER BY priority ASC")
    suspend fun getEmergencyContacts(userId: String): List<EmergencyContact>

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId AND isDoctor = 1 AND isActive = 1 ORDER BY priority ASC")
    suspend fun getDoctorContacts(userId: String): List<EmergencyContact>

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId AND priority = 1 AND isActive = 1 LIMIT 1")
    suspend fun getPrimaryEmergencyContact(userId: String): EmergencyContact?

    @Query("SELECT * FROM emergency_contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: String): EmergencyContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContact>)

    @Update
    suspend fun updateContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)

    @Query("UPDATE emergency_contacts SET isActive = 0 WHERE id = :contactId")
    suspend fun deactivateContact(contactId: String)

    @Query("UPDATE emergency_contacts SET priority = :priority WHERE id = :contactId")
    suspend fun updatePriority(contactId: String, priority: Int)
}