package com.example.myapplication.ui.add

/**
 * Заглушка Activity для добавления записей вручную (без отправки по Bluetooth).
 *
 * Предполагаемое назначение: добавление или редактирование записи истории
 * без активного Bluetooth-соединения — например, для ручной коррекции расписания.
 *
 * Для реализации необходимо:
 * 1. Унаследоваться от AppCompatActivity
 * 2. Создать layout с DatePicker, TimePicker и кнопкой сохранения
 * 3. Подключить [AddMedicineViewModel] для сохранения записи в Room
 * 4. Зарегистрировать Activity в AndroidManifest.xml
 *
 * TODO: реализовать при необходимости независимого добавления записей
 */
class AddMedicineActivity {
}
