# Передача времени приёма лекарств (Android → Arduino по Bluetooth)

## Оглавление

- [Описание проекта](#описание-проекта)
- [Технологии и зависимости](#технологии-и-зависимости)
- [Архитектура приложения](#архитектура-приложения)
- [Структура проекта](#структура-проекта)
- [Модули и классы](#модули-и-классы)
- [Пользовательский интерфейс](#пользовательский-интерфейс)
- [Поток данных](#поток-данных)
- [Bluetooth и разрешения](#bluetooth-и-разрешения)
- [База данных](#база-данных)
- [Формат передаваемых данных](#формат-передаваемых-данных)
- [Сборка и запуск](#сборка-и-запуск)
- [Чек-лист возможных улучшений](#чек-лист-возможных-улучшений)

---

## Описание проекта

Android-приложение для передачи запланированного времени приёма лекарств на Arduino-устройство по классическому Bluetooth (SPP — Serial Port Profile). Приложение формирует строку с датой и временем, отправляет её на Arduino через модуль HC-05 или HC-06, и сохраняет историю отправок в локальной базе данных.

**Основные возможности:**
- Выбор даты и времени через встроенные виджеты `DatePicker` / `TimePicker`
- Подключение к сопряжённым Bluetooth-устройствам
- Отображение статуса соединения в реальном времени
- Сохранение истории отправок в базе данных Room
- Редактирование и удаление записей из истории

---

## Технологии и зависимости

| Категория | Технология |
|---|---|
| Язык | Kotlin |
| Архитектурный паттерн | MVVM (Model-View-ViewModel) |
| Навигация между экранами | ViewPager2 + TabLayout |
| База данных | Room (SQLite) |
| Реактивное состояние | Kotlin StateFlow / Flow |
| Bluetooth | Android Bluetooth API (классический, SPP) |
| Минимальный SDK | API 24 (Android 7.0 Nougat) |
| Целевой SDK | API 36 (Android 15) |
| Версия JVM | Java 11 |
| Build-система | Gradle (Kotlin DSL) |

---

## Архитектура приложения

Приложение построено на паттерне **MVVM** с разделением на слои:

```
┌─────────────────────────────────────────┐
│               UI Layer                  │
│  MainActivity · ControlFragment         │
│  HistoryFragment · MedicineAdapter      │
└──────────────┬──────────────────────────┘
               │ наблюдает StateFlow
┌──────────────▼──────────────────────────┐
│            ViewModel Layer              │
│         MainViewModel                   │
└──────────────┬──────────────────────────┘
               │ вызывает suspend-функции
┌──────────────▼──────────────────────────┐
│           Repository Layer              │
│         MedicineRepository              │
└──────┬───────────────────┬──────────────┘
       │                   │
┌──────▼──────┐   ┌────────▼───────────┐
│  Room DAO   │   │  BluetoothManager  │
│ MedicineDao │   │  (Bluetooth SPP)   │
└─────────────┘   └────────────────────┘
```

---

## Структура проекта

```
app/src/main/
├── AndroidManifest.xml                  # Разрешения и объявление Activity
├── java/com/example/myapplication/
│   ├── bluetoot/
│   │   └── BluetoothManager.kt          # Управление Bluetooth-соединением
│   ├── data/
│   │   ├── MedicineRepository.kt        # Репозиторий (Bluetooth + БД)
│   │   ├── db/
│   │   │   ├── AppDatabase.kt           # Синглтон базы данных Room
│   │   │   └── MedicineDao.kt           # DAO — запросы к таблице medicines
│   │   └── model/
│   │       └── Medicine.kt              # Сущность (Entity) записи о лекарстве
│   ├── tabs/
│   │   ├── ControlFragment.kt           # Вкладка "Управление" (выбор даты/времени, отправка)
│   │   ├── HistoryFragment.kt           # Вкладка "История" (список отправленных записей)
│   │   └── ViewPagerAdapter.kt          # Адаптер для ViewPager2
│   ├── ui/
│   │   ├── add/
│   │   │   ├── AddMedicineActivity.kt   # Заглушка (не реализована)
│   │   │   └── AddMedicineViewModel.kt  # Заглушка (не реализована)
│   │   └── main/
│   │       ├── MainActivity.kt          # Точка входа, управление Bluetooth-диалогами
│   │       ├── MainViewModel.kt         # ViewModel главного экрана
│   │       ├── MainViewModelFactory.kt  # Фабрика ViewModel (вложена в MainViewModel.kt)
│   │       └── MedicineAdapter.kt       # RecyclerView-адаптер для истории
│   ├── utils/
│   │   └── TimeUtils.kt                 # Утилиты форматирования даты/времени
│   └── worker/
│       └── CheckMedicineWorker.kt       # Заглушка воркера (не реализована)
└── res/
    ├── layout/
    │   ├── activity_main.xml            # Разметка главного экрана (TabLayout + ViewPager2)
    │   ├── fragment_control.xml         # Разметка вкладки "Управление"
    │   ├── fragment_history.xml         # Разметка вкладки "История"
    │   ├── item_medicine.xml            # Элемент списка истории (CardView)
    │   └── dialog_edit_medicine.xml     # Диалог редактирования записи
    └── values/
        ├── strings.xml                  # Строковые ресурсы (на русском)
        ├── colors.xml                   # Цветовые ресурсы
        └── themes.xml                   # Тема приложения
```

---

## Модули и классы

### `BluetoothManager` (`bluetoot/BluetoothManager.kt`)

Управляет жизненным циклом Bluetooth-соединения. Работает с классическим Bluetooth через **SPP (Serial Port Profile)**.

**Константы состояния соединения:**

| Константа | Значение | Описание |
|---|---|---|
| `STATE_DISCONNECTED` | 0 | Нет соединения |
| `STATE_CONNECTING` | 1 | Установка соединения |
| `STATE_CONNECTED` | 2 | Соединение установлено |

**Ключевые методы:**

| Метод | Описание |
|---|---|
| `isBluetoothEnabled()` | Проверяет, включён ли Bluetooth |
| `isBluetoothSupported()` | Проверяет поддержку Bluetooth на устройстве |
| `getPairedDevices()` | Возвращает список сопряжённых устройств |
| `connect(device)` | Устанавливает соединение с устройством (блокирующий вызов!) |
| `disconnect()` | Разрывает текущее соединение |
| `sendData(data)` | Отправляет строку данных, автоматически добавляя `\n` |
| `isConnected()` | Возвращает `true`, если соединение активно |
| `getConnectedDeviceName()` | Возвращает имя подключённого устройства |

> **Важно:** метод `connect()` является блокирующим — его нельзя вызывать в главном потоке. В `MainActivity` вызов обёрнут в `Thread {}`.

---

### `Medicine` (`data/model/Medicine.kt`)

Сущность (Entity) для Room. Представляет одну запись об отправленном времени приёма.

| Поле | Тип | Описание |
|---|---|---|
| `id` | `Long` | Первичный ключ (автоинкремент) |
| `dateTime` | `String` | Запланированное время приёма в формате `YYYY-MM-DD HH:MM` |
| `sentAt` | `Long` | Unix-timestamp момента отправки |
| `deviceName` | `String?` | Имя Bluetooth-устройства (может быть `null`) |

---

### `MedicineDao` (`data/db/MedicineDao.kt`)

DAO-интерфейс для операций с таблицей `medicines`.

| Метод | SQL / Тип | Описание |
|---|---|---|
| `getAll()` | `SELECT * ORDER BY sentAt DESC` | Все записи в виде `Flow` (реактивно) |
| `insert(medicine)` | `INSERT OR REPLACE` | Вставка записи, возвращает `id` |
| `update(medicine)` | `UPDATE` | Обновление записи |
| `delete(medicine)` | `DELETE` | Удаление конкретной записи |
| `deleteAll()` | `DELETE FROM medicines` | Очистка всей таблицы |
| `deleteById(id)` | `DELETE WHERE id = :id` | Удаление по первичному ключу |
| `getById(id)` | `SELECT WHERE id = :id` | Получение записи по `id` |

---

### `AppDatabase` (`data/db/AppDatabase.kt`)

Синглтон базы данных Room. Обеспечивает потокобезопасное создание единственного экземпляра через `@Volatile` + `synchronized`.

- **Имя файла БД:** `medicine_database`
- **Версия схемы:** 1
- **Таблицы:** `medicines`

---

### `MedicineRepository` (`data/MedicineRepository.kt`)

Единая точка доступа к данным. Координирует операции Bluetooth и Room.

**Ключевой метод `sendMedicineTime`:**
1. Проверяет активность Bluetooth-соединения
2. Формирует строку для отправки: `eventDateTime;sendDateTime`
3. Отправляет строку через `BluetoothManager.sendData()`
4. При успехе сохраняет запись в Room

---

### `MainViewModel` (`ui/main/MainViewModel.kt`)

ViewModel главного экрана. Хранит и предоставляет UI-состояние через `StateFlow`.

**StateFlow-потоки:**

| Поток | Тип | Описание |
|---|---|---|
| `isConnected` | `StateFlow<Boolean>` | Флаг активного Bluetooth-соединения |
| `connectedDeviceName` | `StateFlow<String?>` | Имя подключённого устройства |
| `isSending` | `StateFlow<Boolean>` | Флаг активной отправки данных |
| `errorMessage` | `StateFlow<String?>` | Сообщение об ошибке (одноразовое) |
| `successMessage` | `StateFlow<String?>` | Сообщение об успехе (одноразовое) |
| `medicines` | `Flow<List<Medicine>>` | Реактивный список из Room |

---

### `MainActivity` (`ui/main/MainActivity.kt`)

Главная Activity. Управляет:
- Запросом Bluetooth-разрешений (с учётом API 31+)
- Диалогом выбора Bluetooth-устройства
- Установкой соединения в фоновом потоке
- Инициализацией ViewPager2 с вкладками

Предоставляет `ControlFragment` публичный доступ к `bluetoothManager` и методу `startBluetoothConnectionFlow()`.

---

### `ControlFragment` (`tabs/ControlFragment.kt`)

Вкладка **"Управление"**. Отображает:
- Текущий статус соединения (зелёный/красный цвет)
- `DatePicker` и `TimePicker` для выбора даты/времени
- Кнопку подключения к Bluetooth-устройству
- Кнопку **"ОК (Отправить)"** для отправки данных

Получает `BluetoothManager` напрямую через `MainActivity` (каст `requireActivity() as MainActivity`).

---

### `HistoryFragment` (`tabs/HistoryFragment.kt`)

Вкладка **"История"**. Отображает список отправленных записей через `RecyclerView`. Поддерживает:
- Редактирование записи через диалог (`dialog_edit_medicine.xml`)
- Удаление конкретной записи с подтверждением
- Кнопку **"Удалить все"** с диалогом подтверждения

> **Известная проблема:** кнопка `deleteAllButton` инициализируется через `?` (nullable) и сразу же используется до вызова `view.findViewById()` — обработчик клика никогда не будет привязан.

---

### `MedicineAdapter` (`ui/main/MedicineAdapter.kt`)

`ListAdapter` для `RecyclerView`. Использует `DiffUtil` для эффективного обновления списка.

Каждый элемент отображает:
- Дата/время приёма (крупно)
- Имя Bluetooth-устройства (или "Неизвестно")
- Дату и время отправки в формате `dd.MM.yyyy HH:mm`
- Кнопки "Редактировать" и "Удалить"

---

### `TimeUtils` (`utils/TimeUtils.kt`)

Утилитный объект (`object`) для работы с датой и временем.

| Метод | Описание |
|---|---|
| `formatDateTime(year, month, day, hour, minute)` | Форматирует компоненты даты в строку `YYYY-MM-DD HH:MM` (month = 0..11) |
| `formatCurrentDateTime()` | Возвращает текущие дату и время в формате `YYYY-MM-DD HH:MM` |
| `isValidFormat(dateTime)` | Проверяет строку на соответствие формату `YYYY-MM-DD HH:MM` |

---

### `ViewPagerAdapter` (`tabs/ViewPagerAdapter.kt`)

Адаптер для `ViewPager2`. Содержит две вкладки:

| Индекс | Класс | Заголовок |
|---|---|---|
| 0 | `ControlFragment` | "Управление" |
| 1 | `HistoryFragment` | "История" |

---

## Пользовательский интерфейс

### Главный экран (`activity_main.xml`)
- `TabLayout` (вкладки «Управление» / «История»)
- `ViewPager2` (свайп между вкладками)

### Вкладка «Управление» (`fragment_control.xml`)
- Заголовок приложения
- Статус Bluetooth-соединения (текст меняет цвет: зелёный/красный)
- Кнопка «Выбрать устройство»
- `DatePicker` (режим spinner)
- `TimePicker` (режим spinner)
- Кнопка «ОК (Отправить)»

### Вкладка «История» (`fragment_history.xml`)
- Заголовок «История отправок»
- Кнопка «Удалить все»
- Разделитель
- `RecyclerView` со списком записей

### Элемент истории (`item_medicine.xml`)
- `CardView` с тенью и скруглёнными углами
- Дата/время приёма (жирный шрифт 18sp)
- Имя устройства (14sp, серый)
- Дата отправки (12sp, серый)
- Кнопки «Редактировать» / «Удалить»

---

## Поток данных

```
Пользователь выбирает дату/время
         │
         ▼
ControlFragment.handleSendButtonClick()
         │  проверяет bluetooth перед вызовом
         ▼
MainViewModel.sendMedicineTime(year, month, day, hour, minute)
         │  viewModelScope.launch{}
         ▼
TimeUtils.formatDateTime()  →  "YYYY-MM-DD HH:MM"
TimeUtils.formatCurrentDateTime()  →  текущее время
         │
         ▼
MedicineRepository.sendMedicineTime(eventDateTime, sendDateTime)
         │  проверяет isConnected()
         │  формирует строку: "eventDateTime;sendDateTime"
         ▼
BluetoothManager.sendData(data)
         │  добавляет \n, отправляет через OutputStream
         ▼
Arduino (HC-05 / HC-06)
         │
         ▼ (при успехе)
MedicineDao.insert(Medicine(...))  →  Room SQLite
         │
         ▼
Flow<List<Medicine>>  →  HistoryFragment  →  RecyclerView
```

---

## Bluetooth и разрешения

### Запрашиваемые разрешения

| Разрешение | API | Назначение |
|---|---|---|
| `BLUETOOTH` | < 31 | Базовые Bluetooth-операции |
| `BLUETOOTH_ADMIN` | < 31 | Управление Bluetooth (поиск устройств) |
| `BLUETOOTH_SCAN` | ≥ 31 | Поиск и сопряжение устройств |
| `BLUETOOTH_CONNECT` | ≥ 31 | Подключение к устройствам |
| `BLUETOOTH_ADVERTISE` | ≥ 31 | BLE-реклама (не используется) |
| `ACCESS_FINE_LOCATION` | ≥ 23 | Требуется для сканирования Bluetooth |
| `ACCESS_COARSE_LOCATION` | ≥ 23 | Требуется для сканирования Bluetooth |

### UUID SPP-профиля

```
00001101-0000-1000-8000-00805F9B34FB
```

Это стандартный UUID Serial Port Profile, совместимый с HC-05 и HC-06.

---

## База данных

**Схема таблицы `medicines`:**

```sql
CREATE TABLE medicines (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    dateTime  TEXT    NOT NULL,   -- "YYYY-MM-DD HH:MM"
    sentAt    INTEGER NOT NULL,   -- Unix timestamp в миллисекундах
    deviceName TEXT              -- Имя BT-устройства, может быть NULL
);
```

---

## Формат передаваемых данных

На устройство Arduino отправляется строка вида:

```
<время_приёма>;<время_отправки>\n
```

**Пример:**
```
2026-04-23 14:30;2026-04-23 09:15\n
```

Где:
- `<время_приёма>` — выбранные пользователем дата и время в формате `YYYY-MM-DD HH:MM`
- `<время_отправки>` — текущие дата и время устройства в формате `YYYY-MM-DD HH:MM`
- Строка завершается символом новой строки `\n`

> **Примечание:** Формат с разделителем `;` отличается от исходного ТЗ, где предполагалась только одна строка `YYYY-MM-DD HH:MM`. Следует синхронизировать с командой, разрабатывающей прошивку Arduino.

---

## Сборка и запуск

### Требования
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 11+
- Реальное Android-устройство с Bluetooth (эмулятор не поддерживает Classic Bluetooth)
- Arduino с Bluetooth-модулем HC-05 или HC-06

### Шаги
1. Клонировать репозиторий
2. Открыть проект в Android Studio
3. Синхронизировать Gradle (`File → Sync Project with Gradle Files`)
4. Подключить Android-устройство по USB с включённой отладкой
5. Запустить через `Run → Run 'app'`

### Перед первым использованием
1. Убедитесь, что Bluetooth включён на Android-устройстве
2. Предварительно сопрягите Arduino (HC-05/HC-06) с телефоном через системные настройки Bluetooth
3. Откройте приложение → вкладка «Управление» → «Выбрать устройство»
4. Выберите Arduino из списка сопряжённых устройств

---

## Чек-лист возможных улучшений

### Критические баги

- [ ] **`HistoryFragment`: кнопка "Удалить все" не работает** — `deleteAllButton` получает `null` через `view.findViewById<Button?>(R.id.deleteAllButton)` и обработчик клика привязывается до инициализации View. Нужно добавить `deleteAllButton = view.findViewById(R.id.deleteAllButton)` в `onViewCreated` и перенести `setOnClickListener` туда же.
- [ ] **Опечатка в пакете** — пакет `bluetoot` вместо `bluetooth`. Следует переименовать для читаемости кода.
- [ ] **Bluetooth-подключение в главном потоке невозможно** — `BluetoothManager.connect()` вызывается в `Thread {}`, а не в корутине. При повороте экрана Activity пересоздаётся, а поток продолжает работу, что может вызвать утечку.

### Архитектурные улучшения

- [ ] **Заменить `Thread {}` на корутины** — использовать `viewModelScope.launch(Dispatchers.IO)` для Bluetooth-соединения вместо ручного управления потоками.
- [ ] **Внедрить Dependency Injection (Hilt/Koin)** — сейчас зависимости создаются вручную в `MainActivity`. DI упростит тестирование и масштабирование.
- [ ] **`ControlFragment` напрямую знает о `MainActivity`** — каст `requireActivity() as MainActivity` нарушает принцип инкапсуляции. Лучше использовать общий `ViewModel` или интерфейс.
- [ ] **`BluetoothManager` — не синглтон** — создаётся в `MainActivity` и передаётся вручную. При перестройке Activity создаётся новый экземпляр и соединение теряется.
- [ ] **`MainViewModelFactory` вложена в файл `MainViewModel.kt`** — лучше вынести в отдельный файл или использовать Hilt.

### Безопасность и корректность

- [ ] **Включить ProGuard/R8 в release-сборке** — `isMinifyEnabled = false` в `buildTypes.release`. Без минификации APK больше и код приложения легко декомпилировать.
- [ ] **Разрешение `BLUETOOTH_ADVERTISE` не используется** — в `AndroidManifest.xml` объявлено разрешение для BLE-рекламы, которое не нужно для SPP-профиля. Следует удалить.
- [ ] **Нет обработки потери соединения** — если Bluetooth отключается во время работы, `connectionState` остаётся `STATE_CONNECTED` до следующей попытки отправки. Нужна периодическая проверка состояния сокета.
- [ ] **Нет валидации данных перед отправкой** — `TimeUtils.isValidFormat()` существует, но нигде не вызывается перед отправкой.

### Функциональные улучшения

- [ ] **Реализовать `CheckMedicineWorker`** — заглушка воркера для фоновых проверок. Если нужны напоминания или автоматическая отправка по расписанию — реализовать через `WorkManager`.
- [ ] **Реализовать `AddMedicineActivity`** — класс создан, но пуст. Можно использовать для добавления записей без отправки по Bluetooth.
- [ ] **Отображение пустого состояния** — когда история пуста, `RecyclerView` ничего не показывает. Добавить `TextView` с подсказкой «История пуста».
- [ ] **Подтверждение отправки от Arduino** — сейчас передача однонаправленная. Можно реализовать чтение ответа от Arduino для подтверждения успешного приёма.
- [ ] **Поиск новых устройств** — `startDiscovery()` в `BluetoothManager` реализован, но нигде не вызывается. Можно добавить UI для поиска несопряжённых устройств.

### UX-улучшения

- [ ] **Индикатор загрузки при подключении** — при нажатии «Выбрать устройство» нет визуального фидбека о том, что идёт подключение (только Toast «Подключение...»). Добавить `ProgressDialog` или блокировку кнопки.
- [ ] **Кнопка «Отключиться»** — нет возможности явно отключиться от устройства в UI. Соединение разрывается только при закрытии приложения.
- [ ] **Жёстко заданные строки в коде** — часть строк (например, «Устройство: ${medicine.deviceName ?: "Неизвестно"}», «Отправлено: $sentDate») не вынесена в `strings.xml`, что затрудняет локализацию.
- [ ] **Фиксированные размеры в `dialog_edit_medicine.xml`** — ширина `DatePicker` и `TimePicker` задана как `403dp` и `404dp` явно, что не будет корректно отображаться на всех экранах. Заменить на `match_parent`.
- [ ] **Нет тёмной темы для диалогового окна** — разделитель в `fragment_history.xml` использует `@android:color/holo_red_dark` вместо атрибута темы.

### Тестирование

- [ ] **Нет unit-тестов** — добавить тесты для `TimeUtils`, `MedicineRepository`, `MainViewModel`.
- [ ] **Нет интеграционных тестов для Room** — протестировать `MedicineDao` с использованием `Room.inMemoryDatabaseBuilder`.
- [ ] **Мокирование `BluetoothManager`** — вынести за интерфейс для возможности тестирования `MedicineRepository` без реального Bluetooth.

### Техническая задолженность

- [ ] **Синхронизировать формат данных с ТЗ** — в ТЗ (`tz.md`) указан формат `YYYY-MM-DD HH:MM`, а в коде отправляется `eventDateTime;sendDateTime`. Уточнить итоговый протокол с командой Arduino.
- [ ] **`applicationId` содержит `com.example`** — перед публикацией сменить на уникальный идентификатор (например, `com.yourname.medicinetracker`).
- [ ] **`app_name` в `strings.xml`** — имя приложения «Передача времени приёма лекарств» слишком длинное для лаунчера. Рекомендуется сократить.
- [ ] **Версия приложения** — `versionCode = 1`, `versionName = "1.0"` — настроить систему управления версиями при переходе к релизу.
