package com.example.myapplication.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.model.Medicine
import com.example.myapplication.ui.add.AddMedicineActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Фрагмент вкладки «История».
 *
 * Отображает все отправленные записи в виде прокручиваемого списка [RecyclerView].
 * Список автоматически обновляется при изменении данных в Room (через Flow).
 *
 * Поддерживаемые операции:
 * - Редактирование записи через диалог с [DatePicker] и [TimePicker]
 * - Удаление одной записи с диалогом подтверждения
 * - Удаление всей истории с диалогом подтверждения
 *
 */
class HistoryFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var medicineAdapter: MedicineAdapter
    private var deleteAllButton: Button? = null
    private var addMedicineFab: FloatingActionButton? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel разделяется с MainActivity
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        historyRecyclerView = view.findViewById(R.id.historyRecyclerView)

        medicineAdapter = MedicineAdapter(
            onEditClick = { medicine -> showEditDialog(medicine) },
            onDeleteClick = { medicine -> showDeleteConfirmationDialog(medicine) },
        )

        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyRecyclerView.adapter = medicineAdapter

        deleteAllButton = view.findViewById(R.id.deleteAllButton)
        deleteAllButton?.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }

        addMedicineFab = view.findViewById(R.id.addMedicineFab)
        addMedicineFab?.setOnClickListener {
            startActivity(Intent(requireContext(), AddMedicineActivity::class.java))
        }

        observeViewModel()
    }

    /**
     * Подписывается на поток записей из ViewModel.
     * При каждом изменении БД список обновляется автоматически через DiffUtil.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.medicines.collectLatest { medicines ->
                medicineAdapter.submitList(medicines)
            }
        }
    }

    /**
     * Показывает диалог редактирования выбранной записи.
     * Парсит dateTime из формата "YYYY-MM-DD HH:MM" в компоненты DatePicker/TimePicker.
     * При сохранении создаёт обновлённую копию через copy() и передаёт в ViewModel.
     *
     * @param medicine Запись для редактирования
     */
    private fun showEditDialog(medicine: Medicine) {
        // Разбираем строку "YYYY-MM-DD HH:MM" на компоненты для DatePicker/TimePicker
        val parts = medicine.dateTime.split(" ")
        if (parts.size != 2) return

        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(":")
        if (dateParts.size != 3 || timeParts.size != 2) return

        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1  // DatePicker использует 0-based месяцы
        val day = dateParts[2].toInt()
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_medicine, null)
        val datePicker = dialogView.findViewById<android.widget.DatePicker>(R.id.editDatePicker)
        val timePicker = dialogView.findViewById<android.widget.TimePicker>(R.id.editTimePicker)

        datePicker.updateDate(year, month, day)
        timePicker.hour = hour
        timePicker.minute = minute

        AlertDialog.Builder(requireContext())
            .setTitle("Редактировать запись")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedMedicine = medicine.copy(
                    dateTime = com.example.myapplication.utils.TimeUtils.formatDateTime(
                        datePicker.year,
                        datePicker.month,
                        datePicker.dayOfMonth,
                        timePicker.hour,
                        timePicker.minute
                    )
                )
                viewModel.updateMedicine(updatedMedicine)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Показывает диалог подтверждения удаления конкретной записи.
     * @param medicine Запись, которую хочет удалить пользователь
     */
    private fun showDeleteConfirmationDialog(medicine: Medicine) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить запись?")
            .setMessage("Вы уверены, что хотите удалить запись ${medicine.dateTime}?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteMedicine(medicine)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Показывает диалог подтверждения очистки всей истории.
     * Действие необратимо — все записи будут удалены из базы данных.
     */
    private fun showDeleteAllConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить всю историю?")
            .setMessage("Вы уверены, что хотите удалить ВСЕ записи истории? Это действие нельзя отменить.")
            .setPositiveButton("Удалить всё") { _, _ ->
                viewModel.deleteAllMedicines()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
