package com.example.myapplication.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.MedicineRepository
import com.example.myapplication.data.model.Medicine
import com.example.myapplication.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddMedicineViewModel(private val repository: MedicineRepository) : ViewModel() {

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun saveMedicine(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                repository.insertMedicine(
                    Medicine(
                        dateTime = TimeUtils.formatDateTime(year, month, day, hour, minute),
                        sentAt = System.currentTimeMillis(),
                        deviceName = null
                    )
                )
                _saved.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка сохранения: ${e.message}"
            }
        }
    }
}

class AddMedicineViewModelFactory(
    private val repository: MedicineRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddMedicineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return AddMedicineViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
