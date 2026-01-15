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

class BluetoothManager(private val context: Context) {
    
    companion object {
        // UUID для SPP (Serial Port Profile) - стандартный для HC-05/HC-06
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
    }
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectedDevice: BluetoothDevice? = null
    private var connectionState = STATE_DISCONNECTED
    
    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }
    
    /**
     * Проверяет, включен ли Bluetooth
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Проверяет, поддерживается ли Bluetooth на устройстве
     */
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }
    
    /**
     * Получает список сопряженных устройств
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        if (bluetoothAdapter == null) return emptyList()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+
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
            // API < 31
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
     * Подключается к указанному устройству
     */
    fun connect(device: BluetoothDevice): Boolean {
        if (connectionState == STATE_CONNECTED) {
            disconnect()
        }
        
        return try {
            connectionState = STATE_CONNECTING
            
            // Создаем сокет
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
            
            // Отменяем поиск устройств для ускорения подключения
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
            
            // Подключаемся
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
     * Отключается от текущего устройства
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
     * Отправляет данные на подключенное устройство
     * @param data Строка данных (должна заканчиваться \n)
     * @return true если отправка успешна
     */
    fun sendData(data: String): Boolean {
        if (connectionState != STATE_CONNECTED || outputStream == null) {
            return false
        }
        
        return try {
            val dataToSend = if (!data.endsWith("\n")) {
                "$data\n"
            } else {
                data
            }
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
     * Получает текущее состояние соединения
     */
    fun getConnectionState(): Int {
        return connectionState
    }
    
    /**
     * Проверяет, подключено ли устройство
     */
    fun isConnected(): Boolean {
        return connectionState == STATE_CONNECTED
    }
    
    /**
     * Получает имя подключенного устройства
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
     * Начинает поиск устройств (для будущего использования)
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
