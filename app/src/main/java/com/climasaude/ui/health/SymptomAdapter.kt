package com.climasaude.ui.health

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.climasaude.data.database.entities.Symptom
import com.climasaude.databinding.ItemSymptomBinding
import java.text.SimpleDateFormat
import java.util.*

class SymptomAdapter(
    private val onDeleteClick: (Symptom) -> Unit
) : ListAdapter<Symptom, SymptomAdapter.SymptomViewHolder>(SymptomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymptomViewHolder {
        val binding = ItemSymptomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SymptomViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(parent: SymptomViewHolder, position: Int) {
        parent.bind(getItem(position))
    }

    class SymptomViewHolder(
        private val binding: ItemSymptomBinding,
        private val onDeleteClick: (Symptom) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(symptom: Symptom) {
            binding.textSymptomName.text = symptom.name
            binding.chipIntensity.text = "Intensidade: ${symptom.intensity}"
            
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.textTimestamp.text = sdf.format(symptom.timestamp)
            
            if (!symptom.notes.isNullOrBlank()) {
                binding.textNotes.visibility = View.VISIBLE
                binding.textNotes.text = symptom.notes
            } else {
                binding.textNotes.visibility = View.GONE
            }

            // Permitir remover ao clicar longo ou adicionar um botão se preferir. 
            // Para manter o visual limpo, usaremos clique longo no card. Modificado por: Daniel
            binding.root.setOnLongClickListener {
                onDeleteClick(symptom)
                true
            }
        }
    }

    class SymptomDiffCallback : DiffUtil.ItemCallback<Symptom>() {
        override fun areItemsTheSame(oldItem: Symptom, newItem: Symptom): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Symptom, newItem: Symptom): Boolean = oldItem == newItem
    }
}
