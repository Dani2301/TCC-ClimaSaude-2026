package com.climasaude.ui.emergency

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.climasaude.data.database.entities.EmergencyContact
import com.climasaude.databinding.ItemEmergencyContactBinding

class EmergencyContactAdapter(
    private val onCallClick: (EmergencyContact) -> Unit,
    private val onDeleteClick: (EmergencyContact) -> Unit
) : ListAdapter<EmergencyContact, EmergencyContactAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEmergencyContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemEmergencyContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: EmergencyContact) {
            binding.textContactName.text = "${contact.name} (${contact.relationship})"
            binding.textContactPhone.text = contact.phone
            
            binding.btnCall.setOnClickListener { onCallClick(contact) }
            binding.btnDelete.setOnClickListener { onDeleteClick(contact) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<EmergencyContact>() {
        override fun areItemsTheSame(oldItem: EmergencyContact, newItem: EmergencyContact) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EmergencyContact, newItem: EmergencyContact) = oldItem == newItem
    }
}
