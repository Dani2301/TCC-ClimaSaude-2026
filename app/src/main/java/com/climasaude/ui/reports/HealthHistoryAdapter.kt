package com.climasaude.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.climasaude.R
import com.climasaude.data.database.entities.MedicationLog
import com.climasaude.data.database.entities.Symptom
import com.climasaude.databinding.ItemHealthHistoryBinding
import com.climasaude.presentation.viewmodels.HealthHistoryItem
import java.text.SimpleDateFormat
import java.util.*

class HealthHistoryAdapter : ListAdapter<HealthHistoryItem, HealthHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHealthHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemHealthHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateTimeFormat = SimpleDateFormat("dd/MM/yy, HH:mm", Locale.getDefault())

        fun bind(item: HealthHistoryItem) {
            when (item) {
                is HealthHistoryItem.SymptomEntry -> bindSymptom(item.symptom)
                is HealthHistoryItem.MedicationEntry -> bindMedication(item.log)
            }
        }

        private fun bindSymptom(symptom: Symptom) {
            binding.imageTypeIcon.setImageResource(R.drawable.ic_health)
            binding.textItemTitle.text = symptom.name
            binding.textItemSubtitle.text = "Intensidade: ${symptom.intensity}/10${if (symptom.notes.isNullOrBlank()) "" else " - ${symptom.notes}"}"
            binding.textItemTime.text = dateTimeFormat.format(symptom.timestamp)
            binding.chipStatus.visibility = View.GONE
        }

        private fun bindMedication(log: MedicationLog) {
            binding.imageTypeIcon.setImageResource(R.drawable.ic_health)
            binding.textItemTitle.text = "Remédio: ${log.dosage}"
            val status = if (log.isTaken) "Tomado às ${log.takenTime?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) }}" else "Pendente"
            binding.textItemSubtitle.text = status
            binding.textItemTime.text = dateTimeFormat.format(log.scheduledTime)
            
            binding.chipStatus.apply {
                visibility = View.VISIBLE
                text = if (log.isTaken) "Tomado" else "Pendente"
                setChipBackgroundColorResource(if (log.isTaken) android.R.color.holo_green_light else android.R.color.holo_orange_light)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HealthHistoryItem>() {
        override fun areItemsTheSame(oldItem: HealthHistoryItem, newItem: HealthHistoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HealthHistoryItem, newItem: HealthHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}
