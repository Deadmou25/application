package com.example.myapplication.ui.main

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.R
import com.example.myapplication.bluetoot.BluetoothManager
import com.example.myapplication.data.MedicineRepository
import com.example.myapplication.data.db.AppDatabase
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Делаем bluetoothManager public, чтобы ControlFragment мог проверить статус
    lateinit var bluetoothManager: BluetoothManager
    private lateinit var repository: MedicineRepository
    private lateinit var viewModel: MainViewModel

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapter

    // Request permissions
    private val requestBluetoothPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initializeBluetooth()
        } else {
            Toast.makeText(this, "Необходимы разрешения для работы с Bluetooth", Toast.LENGTH_LONG).show()
        }
    }

    private val requestEnableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (bluetoothManager.isBluetoothEnabled()) {
            showDeviceSelectionDialog()
        } else {
            Toast.makeText(this, "Bluetooth должен быть включен", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация данных
        bluetoothManager = BluetoothManager(this)
        val database = AppDatabase.getDatabase(this)
        repository = MedicineRepository(database.medicineDao(), bluetoothManager)

        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Инициализация UI (Tabs)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()

        // Проверка разрешений
        checkAndRequestPermissions()

        // Глобальные сообщения (успех)
        observeGlobalMessages()
    }

    // Метод, который вызывает ControlFragment для начала подключения
    fun startBluetoothConnectionFlow() {
        if (bluetoothManager.isBluetoothEnabled()) {
            showDeviceSelectionDialog()
        } else {
            requestEnableBluetooth()
        }
    }

    private fun observeGlobalMessages() {
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
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

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
            Toast.makeText(this, "Устройство не поддерживает Bluetooth", Toast.LENGTH_LONG).show()
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
        requestEnableBluetoothLauncher.launch(android.content.Intent(enableBluetoothIntent))
    }

    private fun showDeviceSelectionDialog() {
        val devices = bluetoothManager.getPairedDevices()
        if (devices.isEmpty()) {
            Toast.makeText(this, "Нет сопряженных устройств", Toast.LENGTH_LONG).show()
            return
        }

        val deviceNames = devices.map { device ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
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
        Thread {
            val success = bluetoothManager.connect(device)
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Подключено к ${getDeviceName(device)}", Toast.LENGTH_SHORT).show()
                    viewModel.updateConnectionState()
                } else {
                    Toast.makeText(this, "Ошибка подключения", Toast.LENGTH_LONG).show()
                    viewModel.updateConnectionState()
                }
            }
        }.start()
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                device.name ?: device.address
            } else {
                device.address
            }
        } else {
            @Suppress("DEPRECATION")
            device.name ?: device.address
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}