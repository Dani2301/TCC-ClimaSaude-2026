package com.climasaude.ui.emergency

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.climasaude.data.database.entities.EmergencyContact
import com.climasaude.databinding.FragmentEmergencyContactsBinding
import com.climasaude.presentation.viewmodels.EmergencyContactsViewModel
import com.climasaude.utils.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmergencyContactsFragment : Fragment() {

    private var _binding: FragmentEmergencyContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EmergencyContactsViewModel by viewModels()
    private lateinit var contactAdapter: EmergencyContactAdapter

    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
        contactUri?.let { uri ->
            retrieveContactDetails(uri)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickContactLauncher.launch(null)
        } else {
            Toast.makeText(requireContext(), "Permissão para ler contatos é necessária", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmergencyContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        contactAdapter = EmergencyContactAdapter(
            onCallClick = { contact -> makePhoneCall(contact.phone) },
            onDeleteClick = { contact -> showDeleteConfirmation(contact) }
        )
        binding.recyclerContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddContact.setOnClickListener {
            checkPermissionAndPickContact()
        }
    }

    private fun checkPermissionAndPickContact() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                pickContactLauncher.launch(null)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun retrieveContactDetails(contactUri: Uri) {
        val cursor = requireContext().contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                
                val contactId = it.getString(idIndex)
                val contactName = it.getString(nameIndex)

                val hasPhoneNumberIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val hasPhoneNumber = it.getString(hasPhoneNumberIndex).toInt()

                if (hasPhoneNumber > 0) {
                    val phoneCursor = requireContext().contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            val phoneIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val phoneNumber = pCursor.getString(phoneIndex)
                            
                            // Adicionar o contato selecionado
                            viewModel.addContact(contactName, phoneNumber, "Agenda")
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Este contato não possui número de telefone", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteConfirmation(contact: EmergencyContact) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remover Contato")
            .setMessage("Deseja remover ${contact.name} da sua lista de emergência?")
            .setPositiveButton("Remover") { _, _ ->
                viewModel.removeContact(contact.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun makePhoneCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o discador", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.contacts.collect { contacts ->
                    contactAdapter.submitList(contacts)
                    binding.layoutEmptyContacts.isVisible = contacts.isEmpty()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.operationResult.collectLatest { resource ->
                    if (resource is Resource.Success) {
                        Toast.makeText(context, resource.data, Toast.LENGTH_SHORT).show()
                    } else if (resource is Resource.Error) {
                        Toast.makeText(context, "Erro: ${resource.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
