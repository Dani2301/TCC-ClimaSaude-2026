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
import com.climasaude.presentation.viewmodels.HealthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class HealthFragment : Fragment() {

    private var _binding: FragmentHealthBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HealthViewModel by viewModels()
    private lateinit var adapter: MedicationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = MedicationAdapter(onDeleteClick = { medication ->
            viewModel.deleteMedication(medication)
            Toast.makeText(context, "Medicamento removido", Toast.LENGTH_SHORT).show()
        })
        binding.recyclerMedications.layoutManager = LinearLayoutManager(context)
        binding.recyclerMedications.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.fabAddMedication.setOnClickListener {
            showAddMedicationDialog()
        }
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
                    adapter.submitList(list)
                    binding.layoutEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerMedications.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
