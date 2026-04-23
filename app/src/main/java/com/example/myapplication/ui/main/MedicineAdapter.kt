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

/**
 * Адаптер RecyclerView для отображения истории отправок в [HistoryFragment].
 *
 * Использует [ListAdapter] + [DiffUtil] для эффективного обновления списка:
 * изменяются только те элементы, которые действительно изменились,
 * без перерисовки всего списка.
 *
 * @param onEditClick Колбэк нажатия кнопки «Редактировать»
 * @param onDeleteClick Колбэк нажатия кнопки «Удалить»
 */
class MedicineAdapter(
    private val onEditClick: (Medicine) -> Unit,
    private val onDeleteClick: (Medicine) -> Unit
) : ListAdapter<Medicine, MedicineAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine, parent, false)
        return MedicineViewHolder(view, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder для одной записи истории.
     * Отображает время приёма, имя устройства и дату отправки.
     */
    class MedicineViewHolder(
        itemView: View,
        private val onEditClick: (Medicine) -> Unit,
        private val onDeleteClick: (Medicine) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val dateTimeText: TextView = itemView.findViewById(R.id.dateTimeText)
        private val deviceNameText: TextView = itemView.findViewById(R.id.deviceNameText)
        private val sentAtText: TextView = itemView.findViewById(R.id.sentAtText)
        private val editButton: Button = itemView.findViewById(R.id.editButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        /**
         * Привязывает данные записи к View-элементам.
         * [Medicine.sentAt] (Unix timestamp мс) форматируется в "dd.MM.yyyy HH:mm".
         */
        fun bind(medicine: Medicine) {
            dateTimeText.text = medicine.dateTime
            deviceNameText.text = "Устройство: ${medicine.deviceName ?: "Неизвестно"}"
            val sentDate =
                java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(medicine.sentAt))
            sentAtText.text = "Отправлено: $sentDate"

            editButton.setOnClickListener { onEditClick(medicine) }
            deleteButton.setOnClickListener { onDeleteClick(medicine) }
        }
    }

    /**
     * Callback для DiffUtil — определяет, изменились ли элементы списка.
     * Позволяет RecyclerView обновлять только изменённые карточки.
     */
    class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {

        /** Два объекта представляют одну и ту же запись, если совпадают их id */
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.id == newItem.id
        }

        /** Содержимое карточки не изменилось, если оба объекта полностью равны */
        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
}
