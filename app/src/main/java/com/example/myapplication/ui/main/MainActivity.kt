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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.R
import com.example.myapplication.bluetoot.BluetoothManager
import com.example.myapplication.data.MedicineRepository
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.worker.CheckMedicineWorker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Главная Activity приложения. Точка входа и «хост» для двух фрагментов:
 * [ControlFragment] (вкладка «Управление») и [HistoryFragment] (вкладка «История»).
 *
 * Обязанности MainActivity:
 * - Инициализация и жизненный цикл [BluetoothManager] и [MedicineRepository]
 * - Запрос Bluetooth-разрешений (с учётом модели прав Android 12+)
 * - Показ диалога выбора Bluetooth-устройства
 * - Установка соединения в фоновом потоке
 * - Отображение глобальных сообщений об успехе из [MainViewModel]
 *
 * [bluetoothManager] намеренно объявлен public — [ControlFragment] обращается
 * к нему напрямую для проверки статуса перед отправкой данных.
 */
class MainActivity : AppCompatActivity() {

	lateinit var bluetoothManager: BluetoothManager
	private lateinit var repository: MedicineRepository
	private lateinit var viewModel: MainViewModel

	private lateinit var tabLayout: TabLayout
	private lateinit var viewPager: ViewPager2
	private lateinit var adapter: ViewPagerAdapter

	/**
	 * Launcher для запроса сразу нескольких Bluetooth-разрешений.
	 * При получении всех разрешений инициализирует Bluetooth,
	 * иначе показывает Toast с объяснением.
	 */
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

	/**
	 * Launcher для системного диалога включения Bluetooth.
	 * Если пользователь включил BT — сразу открывает диалог выбора устройства.
	 */
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

		// Инициализация слоя данных
		bluetoothManager = BluetoothManager(this)
		val database = AppDatabase.getDatabase(this)
		repository = MedicineRepository(database.medicineDao(), bluetoothManager)

		val factory = MainViewModelFactory(repository)
		viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

		// Инициализация ViewPager2 с TabLayout
		tabLayout = findViewById(R.id.tabLayout)
		viewPager = findViewById(R.id.viewPager)

		adapter = ViewPagerAdapter(this)
		viewPager.adapter = adapter

		TabLayoutMediator(tabLayout, viewPager) { tab, position ->
			tab.text = adapter.getPageTitle(position)
		}.attach()

		checkAndRequestPermissions()
		observeGlobalMessages()
		scheduleCheckWorker()
	}

	/**
	 * Публичный метод для фрагментов: запускает полный флоу подключения —
	 * сначала проверяет, включён ли BT, затем показывает диалог выбора устройства.
	 * Вызывается из [ControlFragment] по нажатию кнопки «Выбрать устройство».
	 */
	fun startBluetoothConnectionFlow() {
		if (bluetoothManager.isBluetoothEnabled()) {
			showDeviceSelectionDialog()
		} else {
			requestEnableBluetooth()
		}
	}

	/**
	 * Подписывается на глобальные сообщения об успехе из ViewModel
	 * и отображает их Toast-уведомлениями.
	 */
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

	private fun scheduleCheckWorker() {
		val request = PeriodicWorkRequestBuilder<CheckMedicineWorker>(15, TimeUnit.MINUTES).build()
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
			CheckMedicineWorker.WORK_NAME,
			ExistingPeriodicWorkPolicy.KEEP,
			request
		)
	}

	/**
	 * Формирует список необходимых разрешений в зависимости от версии Android
	 * и запрашивает те, которые ещё не выданы.
	 *
	 * API 33+ (Android 13): POST_NOTIFICATIONS
	 * API 31+ (Android 12): BLUETOOTH_SCAN + BLUETOOTH_CONNECT
	 * API < 31: BLUETOOTH + BLUETOOTH_ADMIN
	 * Всегда: ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION
	 */
	private fun checkAndRequestPermissions() {
		val permissions = mutableListOf<String>()

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			permissions.add(Manifest.permission.POST_NOTIFICATIONS)
		}

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

	/**
	 * Проверяет поддержку и статус Bluetooth.
	 * Если BT выключен — предлагает включить его через системный диалог.
	 */
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

	/** Запускает системный диалог включения Bluetooth */
	private fun requestEnableBluetooth() {
		val enableBluetoothIntent = android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
		requestEnableBluetoothLauncher.launch(android.content.Intent(enableBluetoothIntent))
	}

	/**
	 * Показывает AlertDialog со списком сопряжённых Bluetooth-устройств.
	 * При выборе устройства запускает процесс подключения.
	 */
	private fun showDeviceSelectionDialog() {
		val devices = bluetoothManager.getPairedDevices()
		if (devices.isEmpty()) {
			Toast.makeText(this, "Нет сопряженных устройств", Toast.LENGTH_LONG).show()
			return
		}

		val deviceNames = devices.map { device ->
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if (ActivityCompat.checkSelfPermission(
						this,
						Manifest.permission.BLUETOOTH_CONNECT
					) == PackageManager.PERMISSION_GRANTED) {
					device.name ?: device.address
				} else {
					device.address
				}
			} else {
				@Suppress("DEPRECATION") device.name ?: device.address
			}
		}.toTypedArray()

		AlertDialog.Builder(this).setTitle("Выберите устройство").setItems(deviceNames) { _, which ->
				connectToDevice(devices[which])
			}.setNegativeButton("Отмена", null).show()
	}

	private fun connectToDevice(device: BluetoothDevice) {
		Toast.makeText(this, "Подключение...", Toast.LENGTH_SHORT).show()
		lifecycleScope.launch(Dispatchers.IO) {
			val success = bluetoothManager.connect(device)
			withContext(Dispatchers.Main) {
				val message = if (success) "Подключено к ${getDeviceName(device)}" else "Ошибка подключения"
				Toast.makeText(this@MainActivity, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
				viewModel.updateConnectionState()
			}
		}
	}

	/** Возвращает имя устройства с учётом проверки разрешений API 31+ */
	private fun getDeviceName(device: BluetoothDevice): String {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			if (ActivityCompat.checkSelfPermission(
					this,
					Manifest.permission.BLUETOOTH_CONNECT
				) == PackageManager.PERMISSION_GRANTED) {
				device.name ?: device.address
			} else {
				device.address
			}
		} else {
			@Suppress("DEPRECATION") device.name ?: device.address
		}
	}

	/** Разрывает Bluetooth-соединение при закрытии Activity */
	override fun onDestroy() {
		super.onDestroy()
		bluetoothManager.disconnect()
	}
}
