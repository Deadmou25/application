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

class MainViewModel(private val repository: MedicineRepository) : ViewModel() {
    
    // Состояние подключения Bluetooth
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Имя подключенного устройства
    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()
    
    // Состояние отправки данных
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    // Сообщение об ошибке
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Сообщение об успехе
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // История записей
    val medicines: Flow<List<Medicine>> = repository.getAllMedicines()
    
    init {
        updateConnectionState()
    }
    
    /**
     * Обновляет состояние подключения
     */
    fun updateConnectionState() {
        _isConnected.value = repository.isBluetoothConnected()
        _connectedDeviceName.value = repository.getConnectedDeviceName()
    }
    
    /**
     * Отправляет данные о времени приёма лекарства
     * @param year Год
     * @param month Месяц (0-11)
     * @param day День
     * @param hour Час (0-23)
     * @param minute Минута (0-59)
     */
    fun sendMedicineTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                // Форматируем дату и время
                val dateTime = TimeUtils.formatDateTime(year, month, day, hour, minute)
                
                // Валидируем формат
                if (!TimeUtils.isValidFormat(dateTime)) {
                    _errorMessage.value = "Неверный формат даты и времени"
                    _isSending.value = false
                    return@launch
                }
                
                // Отправляем данные
                val success = repository.sendMedicineTime(dateTime)
                
                if (success) {
                    _successMessage.value = "Данные успешно отправлены"
                } else {
                    _errorMessage.value = "Ошибка отправки данных. Проверьте подключение."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
            } finally {
                _isSending.value = false
                updateConnectionState()
            }
        }
    }
    
    /**
     * Удаляет запись из истории
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
     * Обновляет запись в истории
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
     * Очищает сообщения об ошибках и успехе
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

class MainViewModelFactory(
    private val repository: MedicineRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
