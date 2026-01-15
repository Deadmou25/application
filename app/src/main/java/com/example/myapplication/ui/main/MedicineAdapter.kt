package com.example.myapplication.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.model.Medicine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicineAdapter(
    private val onEditClick: (Medicine) -> Unit,
    private val onDeleteClick: (Medicine) -> Unit
) : ListAdapter<Medicine, MedicineAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTimeText: TextView = itemView.findViewById(R.id.dateTimeText)
        private val deviceNameText: TextView = itemView.findViewById(R.id.deviceNameText)
        private val sentAtText: TextView = itemView.findViewById(R.id.sentAtText)
        private val editButton: Button = itemView.findViewById(R.id.editButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(medicine: Medicine) {
            dateTimeText.text = medicine.dateTime

            deviceNameText.text = if (medicine.deviceName != null) {
                "Устройство: ${medicine.deviceName}"
            } else {
                "Устройство: Неизвестно"
            }

            // Форматируем timestamp отправки
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val sentAtFormatted = dateFormat.format(Date(medicine.sentAt))
            sentAtText.text = "Отправлено: $sentAtFormatted"

            editButton.setOnClickListener {
                onEditClick(medicine)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(medicine)
            }
        }
    }

    class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
}
