package com.climasaude.ui.health

import android.app.TimePickerDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.climasaude.databinding.FragmentHealthBinding
import com.climasaude.databinding.DialogAddMedicationBinding
import com.climasaude.databinding.DialogAddSymptomBinding
import com.climasaude.presentation.viewmodels.HealthViewModel
import com.climasaude.utils.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class HealthFragment : Fragment() {

    private var _binding: FragmentHealthBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HealthViewModel by viewModels()
    private lateinit var medicationAdapter: MedicationAdapter
    private lateinit var symptomAdapter: SymptomAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Medicamentos
        medicationAdapter = MedicationAdapter(onDeleteClick = { medication ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remover Medicamento")
                .setMessage("Deseja remover ${medication.name}?")
                .setPositiveButton("Remover") { _, _ -> viewModel.deleteMedication(medication) }
                .setNegativeButton("Cancelar", null)
                .show()
        })
        binding.recyclerMedications.layoutManager = LinearLayoutManager(context)
        binding.recyclerMedications.adapter = medicationAdapter

        // Sintomas. Agora com opção de remoção por clique longo. Modificado por: Daniel
        symptomAdapter = SymptomAdapter(onDeleteClick = { symptom ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remover Sintoma")
                .setMessage("Deseja remover o registro de ${symptom.name}?")
                .setPositiveButton("Remover") { _, _ -> viewModel.deleteSymptom(symptom) }
                .setNegativeButton("Cancelar", null)
                .show()
        })
        binding.recyclerSymptoms.layoutManager = LinearLayoutManager(context)
        binding.recyclerSymptoms.adapter = symptomAdapter
    }

    private fun setupClickListeners() {
        binding.btnAddMedication.setOnClickListener {
            showAddMedicationDialog()
        }
        
        binding.btnAddSymptom.setOnClickListener {
            showAddSymptomDialog()
        }
    }

    private fun showAddSymptomDialog() {
        val dialogBinding = DialogAddSymptomBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Registrar") { _, _ ->
                val name = dialogBinding.editSymptomName.text.toString()
                val intensity = dialogBinding.sliderIntensity.value.toInt()
                val notes = dialogBinding.editNotes.text.toString()
                
                if (name.isNotBlank()) {
                    viewModel.recordSymptom(name, intensity, notes)
                } else {
                    Toast.makeText(context, "Informe o nome do sintoma", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddMedicationDialog() {
        val dialogBinding = DialogAddMedicationBinding.inflate(layoutInflater)
        var selectedTime = "08:00"

        dialogBinding.editTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(context, { _, hour, minute ->
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                dialogBinding.editTime.setText(selectedTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Novo Medicamento")
            .setView(dialogBinding.root)
            .setPositiveButton("Salvar") { _, _ ->
                val name = dialogBinding.editName.text.toString()
                val dosage = dialogBinding.editDosage.text.toString()
                if (name.isNotEmpty() && dosage.isNotEmpty()) {
                    viewModel.addMedication(name, dosage, selectedTime)
                } else {
                    Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.medications.collect { list ->
                    medicationAdapter.submitList(list)
                    binding.layoutEmptyMeds.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.symptoms.collect { list ->
                    symptomAdapter.submitList(list)
                    binding.layoutEmptySymptoms.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.operationResult.collectLatest { resource ->
                    if (resource is Resource.Success) {
                        Toast.makeText(context, resource.data, Toast.LENGTH_SHORT).show()
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
