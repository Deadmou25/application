package com.example.myapplication.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.MedicineRepository
import com.example.myapplication.data.model.Medicine
import com.example.myapplication.utils.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel главного экрана. Переживает повороты экрана и пересоздание Activity.
 *
 * Предоставляет UI-слою реактивные потоки состояния через [StateFlow].
 * Все операции с данными делегируются [MedicineRepository].
 *
 * Сообщения об ошибках и успехе ([errorMessage], [successMessage]) являются
 * "одноразовыми" — после отображения должны быть сброшены вызовом [clearMessages].
 *
 * @param repository Репозиторий, координирующий Bluetooth и Room
 */
class MainViewModel(private val repository: MedicineRepository) : ViewModel() {

    /** Флаг активного Bluetooth-соединения */
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** Имя подключённого Bluetooth-устройства (null — нет соединения) */
    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    /** Флаг активной операции отправки (зарезервирован для индикатора загрузки) */
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    /** Одноразовое сообщение об ошибке; null — ошибок нет */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Одноразовое сообщение об успехе; null — нет нового сообщения */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /** Реактивный список всех записей истории (обновляется автоматически из Room) */
    val medicines: Flow<List<Medicine>> = repository.getAllMedicines()

    init {
        updateConnectionState()
    }

    /**
     * Синхронизирует состояние соединения с реальным состоянием [BluetoothManager].
     * Вызывается после попытки подключения или отключения.
     */
    fun updateConnectionState() {
        _isConnected.value = repository.isBluetoothConnected()
        _connectedDeviceName.value = repository.getConnectedDeviceName()
    }

    /**
     * Отправляет запланированное время приёма лекарства на Arduino и
     * сохраняет запись в историю.
     *
     * Выполняется асинхронно в [viewModelScope]. UI получает результат
     * через [successMessage] или [errorMessage].
     *
     * @param year Год
     * @param month Месяц в формате DatePicker/Calendar (0 = январь, 11 = декабрь)
     * @param day День месяца
     * @param hour Час (0–23)
     * @param minute Минута (0–59)
     */
    fun sendMedicineTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                val eventDateTime = TimeUtils.formatDateTime(year, month, day, hour, minute)
                val sendDateTime = TimeUtils.formatCurrentDateTime()
                val success = repository.sendMedicineTime(eventDateTime, sendDateTime)
                if (success) {
                    _successMessage.value = "Данные отправлены успешно"
                } else {
                    _errorMessage.value = "Ошибка отправки данных"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
            }
        }
    }

    /**
     * Удаляет запись из истории. Ошибки записываются в [errorMessage].
     * @param medicine Запись для удаления
     */
    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            try {
                repository.deleteMedicine(medicine)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    /**
     * Обновляет существующую запись в истории.
     * При успехе устанавливает [successMessage].
     * @param medicine Обновлённая запись (id должен совпадать с сохранённой)
     */
    fun updateMedicine(medicine: Medicine) {
        viewModelScope.launch {
            try {
                repository.updateMedicine(medicine)
                _successMessage.value = "Запись обновлена"
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка обновления: ${e.message}"
            }
        }
    }

    /**
     * Удаляет все записи из истории. Действие необратимо.
     */
    fun deleteAllMedicines() {
        viewModelScope.launch {
            try {
                repository.deleteAllMedicines()
                _successMessage.value = "История очищена"
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    /**
     * Сбрасывает одноразовые сообщения об ошибке и успехе.
     * Должен вызываться после того, как UI отобразил сообщение (например, Toast).
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

/**
 * Фабрика для создания [MainViewModel] с аргументом [repository].
 *
 * Используется вместо стандартного конструктора ViewModel, так как
 * [MainViewModel] требует параметр, который ViewModelProvider не может
 * предоставить самостоятельно без Hilt/Koin.
 */
class MainViewModelFactory(
    private val repository: MedicineRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
