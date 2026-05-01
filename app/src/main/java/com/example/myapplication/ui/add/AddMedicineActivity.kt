package com.example.myapplication.ui.add

import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.bluetoot.BluetoothManager
import com.example.myapplication.data.MedicineRepository
import com.example.myapplication.data.db.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddMedicineActivity : AppCompatActivity() {

    private lateinit var viewModel: AddMedicineViewModel
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        val database = AppDatabase.getDatabase(this)
        val repository = MedicineRepository(database.medicineDao(), BluetoothManager(this))
        viewModel = ViewModelProvider(this, AddMedicineViewModelFactory(repository))[AddMedicineViewModel::class.java]

        datePicker = findViewById(R.id.datePicker)
        timePicker = findViewById(R.id.timePicker)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        saveButton.setOnClickListener {
            viewModel.saveMedicine(
                datePicker.year,
                datePicker.month,
                datePicker.dayOfMonth,
                timePicker.hour,
                timePicker.minute
            )
        }

        cancelButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            viewModel.saved.collectLatest { saved ->
                if (saved) {
                    Toast.makeText(this@AddMedicineActivity, "Запись сохранена", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@AddMedicineActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
