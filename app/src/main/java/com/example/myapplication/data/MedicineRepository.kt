package com.example.myapplication.data

import com.example.myapplication.bluetoot.BluetoothManager
import com.example.myapplication.data.db.MedicineDao
import com.example.myapplication.data.model.Medicine
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий — единственная точка доступа к данным для ViewModel.
 *
 * Координирует две подсистемы:
 * - [BluetoothManager] — отправка данных на Arduino
 * - [MedicineDao] — сохранение и чтение истории в Room
 *
 * ViewModel не должна знать об источниках данных напрямую —
 * только через методы этого репозитория.
 *
 * @param medicineDao DAO для работы с таблицей medicines
 * @param bluetoothManager Менеджер Bluetooth-соединения
 */
class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val bluetoothManager: BluetoothManager
) {

    /**
     * Реактивный поток всех записей истории (новые сверху).
     * Автоматически обновляется при изменении таблицы.
     */
    fun getAllMedicines(): Flow<List<Medicine>> {
        return medicineDao.getAll()
    }

    /**
     * Отправляет данные о времени приёма на Arduino и, при успехе,
     * сохраняет запись в локальную базу данных.
     *
     * Формат отправляемой строки: `eventDateTime;sendDateTime\n`
     * Пример: `2026-04-23 14:30;2026-04-23 09:00\n`
     *
     * @param eventDateTime Запланированное время приёма в формате "YYYY-MM-DD HH:MM"
     * @param sendDateTime Текущее время устройства в формате "YYYY-MM-DD HH:MM"
     * @return true — данные успешно отправлены и сохранены
     */
    suspend fun sendMedicineTime(eventDateTime: String, sendDateTime: String): Boolean {
        if (!bluetoothManager.isConnected()) {
            return false
        }

        // Формируем строку с двумя временными метками, разделёнными ";"
        val dataToSend = "$eventDateTime;$sendDateTime"
        val success = bluetoothManager.sendData(dataToSend)

        if (success) {
            // Сохраняем запись только при подтверждённой отправке
            val medicine = Medicine(
                dateTime = eventDateTime,
                sentAt = System.currentTimeMillis(),
                deviceName = bluetoothManager.getConnectedDeviceName()
            )
            medicineDao.insert(medicine)
        }

        return success
    }

    /**
     * Обновляет существующую запись в базе данных.
     * Используется при редактировании времени из истории.
     */
    suspend fun updateMedicine(medicine: Medicine) {
        medicineDao.update(medicine)
    }

    /**
     * Удаляет конкретную запись из базы данных.
     */
    suspend fun deleteMedicine(medicine: Medicine) {
        medicineDao.delete(medicine)
    }

    /**
     * Удаляет все записи из базы данных (очистка истории).
     */
    suspend fun deleteAllMedicines() {
        medicineDao.deleteAll()
    }

    /**
     * Удаляет запись по идентификатору.
     * @param id Первичный ключ записи
     */
    suspend fun deleteMedicineById(id: Long) {
        medicineDao.deleteById(id)
    }

    /**
     * Возвращает запись по идентификатору или null.
     * @param id Первичный ключ записи
     */
    suspend fun getMedicineById(id: Long): Medicine? {
        return medicineDao.getById(id)
    }

    /**
     * Проверяет, активно ли Bluetooth-соединение прямо сейчас.
     */
    fun isBluetoothConnected(): Boolean {
        return bluetoothManager.isConnected()
    }

    /**
     * Возвращает имя подключённого Bluetooth-устройства или null.
     */
    fun getConnectedDeviceName(): String? {
        return bluetoothManager.getConnectedDeviceName()
    }
}
