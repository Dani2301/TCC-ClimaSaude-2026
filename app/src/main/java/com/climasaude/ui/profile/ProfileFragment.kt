package com.climasaude.ui.profile

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.climasaude.R
import com.climasaude.data.database.entities.EmergencyContact
import com.climasaude.databinding.FragmentProfileBinding
import com.climasaude.databinding.DialogEditHealthProfileBinding
import com.climasaude.presentation.viewmodels.ProfileViewModel
import com.climasaude.domain.models.UserProfile
import com.climasaude.ui.emergency.EmergencyContactAdapter
import com.climasaude.utils.Resource
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.pow

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ProfileViewModel by viewModels()
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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEmergencyRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupEmergencyRecyclerView() {
        contactAdapter = EmergencyContactAdapter(
            onCallClick = { contact -> makePhoneCall(contact.phone) },
            onDeleteClick = { contact -> showDeleteContactConfirmation(contact) }
        )
        binding.recyclerProfileEmergencyContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonLogout.setOnClickListener {
            viewModel.logout()
            activity?.finish()
        }

        binding.buttonEditProfile.setOnClickListener {
            showEditHealthDialog()
        }

        binding.buttonAddEmergencyContact.setOnClickListener {
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
                            
                            viewModel.addEmergencyContact(contactName, phoneNumber, "Agenda")
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Este contato não possui número de telefone", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

    private fun showDeleteContactConfirmation(contact: EmergencyContact) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remover Contato")
            .setMessage("Deseja remover ${contact.name}?")
            .setPositiveButton("Remover") { _, _ ->
                viewModel.removeEmergencyContact(contact.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditHealthDialog() {
        val dialogBinding = DialogEditHealthProfileBinding.inflate(layoutInflater)
        val profile = viewModel.userProfile.value

        profile?.let {
            dialogBinding.editWeight.setText(it.weight?.toString() ?: "")
            dialogBinding.editHeight.setText(it.height?.toString() ?: "")
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnSave.setOnClickListener {
            val weight = dialogBinding.editWeight.text.toString().toFloatOrNull()
            val height = dialogBinding.editHeight.text.toString().toFloatOrNull()
            val condition = dialogBinding.editCondition.text.toString().trim()
            val allergy = dialogBinding.editAllergy.text.toString().trim()

            viewModel.updateFullHealthProfile(
                weight = weight,
                height = height,
                condition = if (condition.isEmpty()) null else condition,
                allergy = if (allergy.isEmpty()) null else allergy
            )

            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userProfile.collect { profile ->
                    profile?.let { updateUI(it) }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.emergencyContacts.collect { contacts ->
                    contactAdapter.submitList(contacts)
                    binding.textNoEmergencyContacts.isVisible = contacts.isEmpty()
                    binding.recyclerProfileEmergencyContacts.isVisible = contacts.isNotEmpty()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateResult.collectLatest { resource ->
                    if (resource is Resource.Success) {
                        Toast.makeText(requireContext(), resource.data, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateUI(profile: UserProfile) {
        binding.textUserName.text = profile.name
        binding.textUserEmail.text = profile.email
        
        val weight = profile.weight ?: 0f
        val height = profile.height ?: 0f
        
        binding.textWeight.text = if (weight > 0) String.format(Locale.getDefault(), "%.1f kg", weight) else "--"
        binding.textHeight.text = if (height > 0) String.format(Locale.getDefault(), "%.0f cm", height) else "--"
        
        if (weight > 0 && height > 0) {
            val heightInMeters = height / 100
            val bmi = weight / heightInMeters.pow(2)
            binding.textBmi.text = String.format(Locale.getDefault(), "%.1f", bmi)
        } else {
            binding.textBmi.text = "--"
        }

        binding.chipGroupConditions.removeAllViews()
        profile.medicalConditions.forEach { condition ->
            val chip = Chip(requireContext()).apply {
                text = condition.name
                isCloseIconVisible = true
                setOnCloseIconClickListener { viewModel.removeMedicalCondition(condition.name) }
            }
            binding.chipGroupConditions.addView(chip)
        }

        binding.chipGroupAllergies.removeAllViews()
        profile.allergies.forEach { allergy ->
            val chip = Chip(requireContext()).apply {
                text = allergy.name
                isCloseIconVisible = true
                setOnCloseIconClickListener { viewModel.removeAllergy(allergy.name) }
            }
            binding.chipGroupAllergies.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
