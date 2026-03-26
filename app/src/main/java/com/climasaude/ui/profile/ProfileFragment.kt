package com.climasaude.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.climasaude.databinding.FragmentProfileBinding
import com.climasaude.databinding.DialogEditHealthProfileBinding
import com.climasaude.presentation.viewmodels.ProfileViewModel
import com.climasaude.domain.models.UserProfile
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.pow

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ProfileViewModel by viewModels()

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
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonLogout.setOnClickListener {
            viewModel.logout()
            activity?.finish()
        }

        binding.buttonEditProfile.setOnClickListener {
            showEditHealthDialog()
        }
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
            val newCondition = dialogBinding.editCondition.text.toString().trim()
            val newAllergy = dialogBinding.editAllergy.text.toString().trim()

            // Salvar Biometria. Modificado por: Daniel
            viewModel.updatePersonalInfo(
                name = profile?.name ?: "",
                birthDate = profile?.birthDate,
                gender = profile?.gender,
                weight = weight,
                height = height
            )

            if (newCondition.isNotEmpty()) viewModel.addMedicalCondition(newCondition)
            if (newAllergy.isNotEmpty()) viewModel.addAllergy(newAllergy)

            dialog.dismiss()
            Toast.makeText(requireContext(), "Perfil atualizado!", Toast.LENGTH_SHORT).show()
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
    }

    private fun updateUI(profile: UserProfile) {
        binding.textUserName.text = profile.name
        binding.textUserEmail.text = profile.email
        
        val weight = profile.weight ?: 0f
        val height = profile.height ?: 0f
        
        binding.textWeight.text = if (weight > 0) String.format(Locale.getDefault(), "%.1f kg", weight) else "--"
        binding.textHeight.text = if (height > 0) String.format(Locale.getDefault(), "%.0f cm", height) else "--"
        
        // Exibição de IMC. Modificado por: Daniel
        if (weight > 0 && height > 0) {
            val heightInMeters = height / 100
            val bmi = weight / heightInMeters.pow(2)
            binding.textBmi.text = String.format(Locale.getDefault(), "%.1f", bmi)
        } else {
            binding.textBmi.text = "--"
        }

        // Re-populando Chips com funcionalidade de remoção. Modificado por: Daniel
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
