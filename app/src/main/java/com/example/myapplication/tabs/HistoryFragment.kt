package com.example.myapplication.ui.main

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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

	private lateinit var viewModel: MainViewModel
	private lateinit var historyRecyclerView: RecyclerView
	private lateinit var medicineAdapter: MedicineAdapter
	private var deleteAllButton: Button? = null // Новая кнопка

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_history, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

		historyRecyclerView = view.findViewById(R.id.historyRecyclerView)

		medicineAdapter = MedicineAdapter(
			onEditClick = { medicine -> showEditDialog(medicine) },
			onDeleteClick = { medicine -> showDeleteConfirmationDialog(medicine) },
		)

		historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
		historyRecyclerView.adapter = medicineAdapter

		// Обработчик кнопки "Удалить все"
		deleteAllButton?.setOnClickListener {
			showDeleteAllConfirmationDialog()
		}

		observeViewModel()
	}

	private fun observeViewModel() {
		lifecycleScope.launch {
			viewModel.medicines.collectLatest { medicines ->
				medicineAdapter.submitList(medicines)
			}
		}
	}

	private fun showEditDialog(medicine: Medicine) { // Логика диалога редактирования (аналогична той, что была в MainActivity)
		// Для краткости оставим заглушку или перенесем код из MainActivity
		// Важно: TimeUtils должен быть доступен
		val parts = medicine.dateTime.split(" ")
		if (parts.size != 2) return

		val dateParts = parts[0].split("-")
		val timeParts = parts[1].split(":")
		if (dateParts.size != 3 || timeParts.size != 2) return

		val year = dateParts[0].toInt()
		val month = dateParts[1].toInt() - 1
		val day = dateParts[2].toInt()
		val hour = timeParts[0].toInt()
		val minute = timeParts[1].toInt()

		val dialogView = layoutInflater.inflate(R.layout.dialog_edit_medicine, null)
		val datePicker = dialogView.findViewById<android.widget.DatePicker>(R.id.editDatePicker)
		val timePicker = dialogView.findViewById<android.widget.TimePicker>(R.id.editTimePicker)

		datePicker.updateDate(year, month, day)
		timePicker.hour = hour
		timePicker.minute = minute

		AlertDialog.Builder(requireContext()).setTitle("Редактировать запись").setView(dialogView)
			.setPositiveButton("Сохранить") { _, _ ->
				val newYear = datePicker.year
				val newMonth = datePicker.month
				val newDay = datePicker.dayOfMonth
				val newHour = timePicker.hour
				val newMinute = timePicker.minute

				// Убедитесь, что TimeUtils доступен в этом пакете
				val updatedMedicine = medicine.copy(
					dateTime = com.example.myapplication.utils.TimeUtils.formatDateTime(
						newYear, newMonth, newDay, newHour, newMinute
					)
				)
				viewModel.updateMedicine(updatedMedicine)
			}.setNegativeButton("Отмена", null).show()
	}

	private fun showDeleteConfirmationDialog(medicine: Medicine) {
		AlertDialog.Builder(requireContext()).setTitle("Удалить запись?")
			.setMessage("Вы уверены, что хотите удалить запись ${medicine.dateTime}?")
			.setPositiveButton("Удалить") { _, _ ->
				viewModel.deleteMedicine(medicine)
			}.setNegativeButton("Отмена", null).show()
	}

	private fun showDeleteAllConfirmationDialog() {
		AlertDialog.Builder(requireContext()).setTitle("Удалить всю историю?")
			.setMessage("Вы уверены, что хотите удалить ВСЕ записи истории? Это действие нельзя отменить.")
			.setPositiveButton("Удалить всё") { _, _ ->
				viewModel.deleteAllMedicines() // Нужен метод в ViewModel
			}.setNegativeButton("Отмена", null).show()
	}

}