package com.example.myapplication.data

import com.example.myapplication.bluetoot.BluetoothManager
import com.example.myapplication.data.db.MedicineDao
import com.example.myapplication.data.model.Medicine
import kotlinx.coroutines.flow.Flow

class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val bluetoothManager: BluetoothManager
) {
    
    /**
     * Получает все записи из базы данных
     */
    fun getAllMedicines(): Flow<List<Medicine>> {
        return medicineDao.getAll()
    }
    
    /**
     * Отправляет данные на Arduino и сохраняет в базу данных
     * @param dateTime Строка в формате "YYYY-MM-DD HH:MM"
     * @return Результат операции (true если успешно)
     */
    suspend fun sendMedicineTime(eventDateTime: String, sendDateTime: String): Boolean {
        // Проверяем подключение
        if (!bluetoothManager.isConnected()) {
            return false
        }

        val dataToSend = "$eventDateTime;$sendDateTime"
        val success = bluetoothManager.sendData(dataToSend)
        
        // Сохраняем в базу данных
        if (success) {
            val medicine = Medicine(
                dateTime = eventDateTime,      // Время приёма лекарства
                sentAt = System.currentTimeMillis(), // Время отправки (timestamp)
                deviceName = bluetoothManager.getConnectedDeviceName()
            )
            medicineDao.insert(medicine)
        }
        
        return success
    }
    
    /**
     * Обновляет запись в базе данных
     */
    suspend fun updateMedicine(medicine: Medicine) {
        medicineDao.update(medicine)
    }
    
    /**
     * Удаляет запись из базы данных
     */
    suspend fun deleteMedicine(medicine: Medicine) {
        medicineDao.delete(medicine)
    }

    /*
        Удалить все записи из бд
     */
    suspend fun deleteAllMedicines() {
        medicineDao.deleteAll();
    }
    
    /**
     * Удаляет запись по ID
     */
    suspend fun deleteMedicineById(id: Long) {
        medicineDao.deleteById(id)
    }
    
    /**
     * Получает запись по ID
     */
    suspend fun getMedicineById(id: Long): Medicine? {
        return medicineDao.getById(id)
    }
    
    /**
     * Проверяет состояние Bluetooth-соединения
     */
    fun isBluetoothConnected(): Boolean {
        return bluetoothManager.isConnected()
    }
    
    /**
     * Получает имя подключенного устройства
     */
    fun getConnectedDeviceName(): String? {
        return bluetoothManager.getConnectedDeviceName()
    }
}
