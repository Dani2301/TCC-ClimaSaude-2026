package com.climasaude.ui.health

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.climasaude.data.database.entities.Medication
import com.climasaude.databinding.ItemMedicationBinding

class MedicationAdapter(
    private val onItemClick: (Medication) -> Unit,
    private val onDeleteClick: (Medication) -> Unit
) : ListAdapter<Medication, MedicationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMedicationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(medication: Medication) {
            binding.textMedicationName.text = medication.name
            
            // Exibir horários formatados
            val timesInfo = if (medication.times.size > 1) {
                "${medication.times.size} vezes ao dia (${medication.times.joinToString(", ")})"
            } else {
                medication.times.firstOrNull() ?: "--:--"
            }
            
            binding.textMedicationDetails.text = "${medication.dosage} - $timesInfo"
            
            binding.root.setOnClickListener {
                onItemClick(medication)
            }
            
            binding.btnDeleteMedication.setOnClickListener {
                onDeleteClick(medication)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Medication>() {
        override fun areItemsTheSame(oldItem: Medication, newItem: Medication) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Medication, newItem: Medication) = oldItem == newItem
    }
}
