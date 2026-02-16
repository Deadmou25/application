package com.example.myapplication.ui.main

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.bluetoot.BluetoothManager
import com.example.myapplication.data.MedicineRepository
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.model.Medicine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var repository: MedicineRepository
    private lateinit var viewModel: MainViewModel
    private lateinit var connectionStatusText: TextView
    private lateinit var connectButton: Button
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var sendButton: Button
    private lateinit var historyRecyclerView: RecyclerView

    private lateinit var medicineAdapter: MedicineAdapter

    // Request permissions
    private val requestBluetoothPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initializeBluetooth()
        } else {
            Toast.makeText(
                this,
                "Необходимы разрешения для работы с Bluetooth",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val requestEnableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (bluetoothManager.isBluetoothEnabled()) {
            showDeviceSelectionDialog()
        } else {
            Toast.makeText(
                this,
                "Bluetooth должен быть включен для использования приложения",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация компонентов
        initializeComponents()

        // Проверка и запрос разрешений
        checkAndRequestPermissions()
    }

    private fun initializeComponents() {
        connectionStatusText = findViewById(R.id.connectionStatusText)
        connectButton = findViewById(R.id.connectButton)
        datePicker = findViewById(R.id.datePicker)
        timePicker = findViewById(R.id.timePicker)
        sendButton = findViewById(R.id.sendButton)
        historyRecyclerView = findViewById(R.id.historyRecyclerView)

        // Инициализация BluetoothManager и Repository
        bluetoothManager = BluetoothManager(this)
        val database = AppDatabase.getDatabase(this)
        repository = MedicineRepository(database.medicineDao(), bluetoothManager)

        // Инициализация ViewModel
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Настройка RecyclerView
        medicineAdapter = MedicineAdapter(
            onEditClick = { medicine -> showEditDialog(medicine) },
            onDeleteClick = { medicine -> showDeleteConfirmationDialog(medicine) }
        )
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = medicineAdapter

        // Обработчики событий
        connectButton.setOnClickListener {
            if (bluetoothManager.isBluetoothEnabled()) {
                showDeviceSelectionDialog()
            } else {
                requestEnableBluetooth()
            }
        }

        sendButton.setOnClickListener {
            handleSendButtonClick()
        }

        // Наблюдение за состоянием ViewModel
        observeViewModel()
    }

    private fun observeViewModel() {
        // Состояние подключения
        lifecycleScope.launch {
            viewModel.isConnected.collectLatest { isConnected ->
                updateConnectionStatus(isConnected)
            }
        }

        lifecycleScope.launch {
            viewModel.connectedDeviceName.collectLatest { deviceName ->
                updateConnectionStatus(viewModel.isConnected.value)
            }
        }

        // История записей
        lifecycleScope.launch {
            viewModel.medicines.collectLatest { medicines ->
                medicineAdapter.submitList(medicines)
            }
        }

        // Сообщения об ошибках
        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearMessages()
                }
            }
        }

        // Сообщения об успехе
        lifecycleScope.launch {
            viewModel.successMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessages()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Android 11 и ниже
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // Разрешение на местоположение для поиска устройств (API 23+)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestBluetoothPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            initializeBluetooth()
        }
    }

    private fun initializeBluetooth() {
        if (!bluetoothManager.isBluetoothSupported()) {
            Toast.makeText(
                this,
                "Устройство не поддерживает Bluetooth",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!bluetoothManager.isBluetoothEnabled()) {
            requestEnableBluetooth()
        } else {
            viewModel.updateConnectionState()
        }
    }

    private fun requestEnableBluetooth() {
        val enableBluetoothIntent = android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
        requestEnableBluetoothLauncher.launch(
            android.content.Intent(enableBluetoothIntent)
        )
    }

    private fun showDeviceSelectionDialog() {
        val devices = bluetoothManager.getPairedDevices()

        if (devices.isEmpty()) {
            Toast.makeText(
                this,
                "Нет сопряженных устройств. Сопрягите устройство в настройках Bluetooth.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val deviceNames = devices.map { device ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    device.name ?: device.address
                } else {
                    device.address
                }
            } else {
                @Suppress("DEPRECATION")
                device.name ?: device.address
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Выберите устройство")
            .setItems(deviceNames) { _, which ->
                connectToDevice(devices[which])
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Toast.makeText(this, "Подключение...", Toast.LENGTH_SHORT).show()

        // Подключение в фоновом потоке
        Thread {
            val success = bluetoothManager.connect(device)
            runOnUiThread {
                if (success) {
                    Toast.makeText(
                        this,
                        "Подключено к ${getDeviceName(device)}",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.updateConnectionState()
                } else {
                    Toast.makeText(
                        this,
                        "Ошибка подключения. Убедитесь, что устройство включено и находится рядом.",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.updateConnectionState()
                }
            }
        }.start()
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name ?: device.address
            } else {
                device.address
            }
        } else {
            @Suppress("DEPRECATION")
            device.name ?: device.address
        }
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        if (isConnected) {
            val deviceName = viewModel.connectedDeviceName.value
            connectionStatusText.text = if (deviceName != null) {
                "Статус: Подключено ($deviceName)"
            } else {
                "Статус: Подключено"
            }
            connectionStatusText.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            )
        } else {
            connectionStatusText.text = "Статус: Не подключено"
            connectionStatusText.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
            )
        }
    }

    private fun handleSendButtonClick() {
        // Проверка Bluetooth
        if (!bluetoothManager.isBluetoothEnabled()) {
            Toast.makeText(
                this,
                "Bluetooth выключен. Включите Bluetooth для отправки данных.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Проверка подключения
        if (!bluetoothManager.isConnected()) {
            Toast.makeText(
                this,
                "Нет подключения к устройству. Выберите устройство для отправки данных.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Получение даты и времени
        val year = datePicker.year
        val month = datePicker.month
        val day = datePicker.dayOfMonth
        val hour =
            timePicker.hour
        val minute =
            timePicker.minute

        // Отправка данных
        viewModel.sendMedicineTime(year, month, day, hour, minute)
    }

    private fun showEditDialog(medicine: Medicine) {
        // Парсим дату и время из строки
        val parts = medicine.dateTime.split(" ")
        if (parts.size != 2) return

        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(":")
        if (dateParts.size != 3 || timeParts.size != 2) return

        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1 // Month is 0-based
        val day = dateParts[2].toInt()
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        // Создаем диалог с DatePicker и TimePicker
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_medicine, null)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.editDatePicker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.editTimePicker)

        datePicker.updateDate(year, month, day)
        timePicker.hour = hour
        timePicker.minute = minute

        AlertDialog.Builder(this)
            .setTitle("Редактировать запись")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newYear = datePicker.year
                val newMonth = datePicker.month
                val newDay = datePicker.dayOfMonth
                val newHour =
                    timePicker.hour
                val newMinute =
                    timePicker.minute

                val updatedMedicine = medicine.copy(
                    dateTime = com.example.myapplication.utils.TimeUtils.formatDateTime(
                        newYear, newMonth, newDay, newHour, newMinute
                    )
                )
                viewModel.updateMedicine(updatedMedicine)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(medicine: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("Удалить запись?")
            .setMessage("Вы уверены, что хотите удалить запись ${medicine.dateTime}?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteMedicine(medicine)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}
