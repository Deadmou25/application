package com.example.myapplication.bluetoot

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Управляет Bluetooth-соединением с Arduino через классический Bluetooth (SPP).
 *
 * Поддерживает API 24+ с учётом новой модели разрешений Android 12 (API 31+):
 * - До API 31: требуются BLUETOOTH и BLUETOOTH_ADMIN
 * - API 31+: требуются BLUETOOTH_CONNECT и BLUETOOTH_SCAN
 *
 * Важно: метод [connect] является блокирующим и должен вызываться
 * в фоновом потоке (Thread, Dispatchers.IO и т.д.).
 *
 * @param context Контекст приложения для проверки разрешений
 */
class BluetoothManager(private val context: Context) {

    companion object {
        // Стандартный UUID для SPP (Serial Port Profile) — совместим с HC-05 и HC-06
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /** Устройство не подключено */
        const val STATE_DISCONNECTED = 0

        /** Идёт процесс установки соединения */
        const val STATE_CONNECTING = 1

        /** Соединение успешно установлено */
        const val STATE_CONNECTED = 2
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectedDevice: BluetoothDevice? = null
    private var connectionState = STATE_DISCONNECTED

    init {
        // Получаем системный BluetoothAdapter; null — если устройство не поддерживает BT
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    /**
     * Проверяет, включён ли Bluetooth на устройстве.
     * @return true — Bluetooth включён и готов к работе
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Проверяет, поддерживает ли устройство Bluetooth вообще.
     * @return false — на устройстве нет Bluetooth-адаптера
     */
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Возвращает список Bluetooth-устройств, сопряжённых с телефоном.
     * Список формируется из системного кэша — сканирования не происходит.
     *
     * @return Список [BluetoothDevice] или пустой список, если нет разрешений
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        if (bluetoothAdapter == null) return emptyList()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: необходимо разрешение BLUETOOTH_CONNECT
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                emptyList()
            } else {
                bluetoothAdapter!!.bondedDevices.toList()
            }
        } else {
            // API < 31: проверяем устаревшее разрешение BLUETOOTH
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                emptyList()
            } else {
                bluetoothAdapter!!.bondedDevices.toList()
            }
        }
    }

    /**
     * Устанавливает RFCOMM-соединение с указанным устройством по SPP UUID.
     *
     * ВНИМАНИЕ: метод блокирует текущий поток до завершения соединения
     * или возникновения ошибки. Вызывайте только из фонового потока.
     *
     * При повторном вызове автоматически разрывает текущее соединение.
     *
     * @param device Устройство из списка [getPairedDevices]
     * @return true — соединение установлено успешно
     */
    fun connect(device: BluetoothDevice): Boolean {
        if (connectionState == STATE_CONNECTED) {
            disconnect()
        }

        return try {
            connectionState = STATE_CONNECTING

            // Создаём RFCOMM-сокет для SPP-профиля
            val socket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } else {
                @Suppress("DEPRECATION")
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            }

            // Отменяем активное сканирование: оно замедляет установку соединения
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothAdapter?.cancelDiscovery()
                }
            } else {
                @Suppress("DEPRECATION")
                bluetoothAdapter?.cancelDiscovery()
            }

            // Блокирующий вызов — ожидает установки соединения или бросает IOException
            socket.connect()

            bluetoothSocket = socket
            outputStream = socket.outputStream
            connectedDevice = device
            connectionState = STATE_CONNECTED

            true
        } catch (e: IOException) {
            e.printStackTrace()
            connectionState = STATE_DISCONNECTED
            bluetoothSocket = null
            outputStream = null
            connectedDevice = null
            false
        }
    }

    /**
     * Закрывает текущее Bluetooth-соединение и освобождает ресурсы.
     * Безопасен для вызова, даже если соединения нет.
     */
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            outputStream = null
            bluetoothSocket = null
            connectedDevice = null
            connectionState = STATE_DISCONNECTED
        }
    }

    /**
     * Отправляет строку данных на подключённое устройство.
     *
     * Если строка не заканчивается на `\n` — символ добавляется автоматически.
     * Это необходимо для корректного парсинга на стороне Arduino
     * (команда `readStringUntil('\n')`).
     *
     * При ошибке ввода-вывода автоматически вызывает [disconnect].
     *
     * @param data Строка для отправки
     * @return true — данные успешно записаны в поток
     */
    fun sendData(data: String): Boolean {
        if (connectionState != STATE_CONNECTED || outputStream == null) {
            return false
        }

        return try {
            // Гарантируем завершающий \n для корректного парсинга на Arduino
            val dataToSend = if (!data.endsWith("\n")) "$data\n" else data
            outputStream!!.write(dataToSend.toByteArray())
            outputStream!!.flush()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            disconnect()
            false
        }
    }

    /**
     * Возвращает текущее состояние соединения.
     * @return Одна из констант: [STATE_DISCONNECTED], [STATE_CONNECTING], [STATE_CONNECTED]
     */
    fun getConnectionState(): Int {
        return connectionState
    }

    /**
     * Проверяет, активно ли соединение прямо сейчас.
     * @return true — устройство подключено
     */
    fun isConnected(): Boolean {
        return connectionState == STATE_CONNECTED
    }

    /**
     * Возвращает имя подключённого Bluetooth-устройства.
     * @return Имя устройства или null, если нет соединения / нет разрешений
     */
    fun getConnectedDeviceName(): String? {
        return if (connectionState == STATE_CONNECTED && connectedDevice != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    connectedDevice!!.name
                } else {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                connectedDevice!!.name
            }
        } else {
            null
        }
    }

    /**
     * Запускает активное сканирование Bluetooth-устройств поблизости.
     * Метод реализован, но в текущей версии UI не используется.
     * Может применяться для поиска несопряжённых устройств в будущем.
     *
     * @return true — сканирование запущено успешно
     */
    fun startDiscovery(): Boolean {
        if (bluetoothAdapter == null) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                false
            } else {
                bluetoothAdapter!!.startDiscovery()
            }
        } else {
            @Suppress("DEPRECATION")
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                false
            } else {
                bluetoothAdapter!!.startDiscovery()
            }
        }
    }
}
