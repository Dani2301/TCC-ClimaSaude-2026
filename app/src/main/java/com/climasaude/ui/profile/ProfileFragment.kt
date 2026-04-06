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
            val condition = dialogBinding.editCondition.text.toString().trim()
            val allergy = dialogBinding.editAllergy.text.toString().trim()

            // Chamada atômica: salva tudo em uma única transação no repositório. Modificado por: Daniel
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

        // Observar resultado do salvamento para dar feedback ao usuário. Modificado por: Daniel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateResult.collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            Toast.makeText(requireContext(), resource.data, Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Error -> {
                            Toast.makeText(requireContext(), "Erro: ${resource.message}", Toast.LENGTH_LONG).show()
                        }
                        else -> {}
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
