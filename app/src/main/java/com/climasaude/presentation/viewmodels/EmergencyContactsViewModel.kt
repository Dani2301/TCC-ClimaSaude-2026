package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.database.entities.EmergencyContact
import com.climasaude.data.repository.UserRepository
import com.climasaude.data.preferences.AppPreferences
import com.climasaude.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EmergencyContactsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private val _operationResult = MutableSharedFlow<Resource<String>>()
    val operationResult = _operationResult.asSharedFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            userRepository.getEmergencyContactsFlow(userId).collect {
                _contacts.value = it
            }
        }
    }

    fun addContact(name: String, phone: String, relationship: String) {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            val contact = EmergencyContact(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                phone = phone,
                relationship = relationship
            )
            val result = userRepository.addEmergencyContact(contact)
            _operationResult.emit(result)
        }
    }

    fun removeContact(contactId: String) {
        viewModelScope.launch {
            val result = userRepository.removeEmergencyContact(contactId)
            _operationResult.emit(result)
        }
    }
}
