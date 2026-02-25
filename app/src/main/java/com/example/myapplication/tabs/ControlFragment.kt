package com.example.myapplication.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.bluetoot.BluetoothManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ControlFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var bluetoothManager: BluetoothManager

    private lateinit var connectionStatusText: TextView
    private lateinit var connectButton: Button
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var sendButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем ViewModel и BluetoothManager из Activity
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        // Предполагается, что в MainActivity есть публичный доступ к bluetoothManager или метод для его получения
        // Для простоты, давайте предположим, что MainActivity реализует интерфейс или мы кастуем контекст
        val activity = requireActivity() as MainActivity
        bluetoothManager = activity.bluetoothManager

        initializeViews(view)
        setupObservers()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        connectionStatusText = view.findViewById(R.id.connectionStatusText)
        connectButton = view.findViewById(R.id.connectButton)
        datePicker = view.findViewById(R.id.datePicker)
        timePicker = view.findViewById(R.id.timePicker)
        sendButton = view.findViewById(R.id.sendButton)
    }

    private fun setupClickListeners() {
        connectButton.setOnClickListener {
            // Делегируем обработку Bluetooth диалогов MainActivity
            (requireActivity() as MainActivity).startBluetoothConnectionFlow()
        }

        sendButton.setOnClickListener {
            handleSendButtonClick()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.isConnected.collectLatest { isConnected ->
                updateConnectionStatus(isConnected)
            }
        }

        lifecycleScope.launch {
            viewModel.connectedDeviceName.collectLatest { _ ->
                updateConnectionStatus(viewModel.isConnected.value)
            }
        }

        // Ошибки и успех можно показывать и здесь, или оставить в Activity
        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearMessages()
                }
            }
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
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )
        } else {
            connectionStatusText.text = "Статус: Не подключено"
            connectionStatusText.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
        }
    }

    private fun handleSendButtonClick() {
        if (!bluetoothManager.isBluetoothEnabled()) {
            Toast.makeText(requireContext(), "Bluetooth выключен.", Toast.LENGTH_LONG).show()
            return
        }
        if (!bluetoothManager.isConnected()) {
            Toast.makeText(requireContext(), "Нет подключения к устройству.", Toast.LENGTH_LONG)
                .show()
            return
        }

        val year = datePicker.year
        val month = datePicker.month
        val day = datePicker.dayOfMonth
        val hour = timePicker.hour
        val minute = timePicker.minute

        viewModel.sendMedicineTime(year, month, day, hour, minute)
    }
}