# Bluetooth — как телефон разговаривает с Arduino

## Что такое Bluetooth?

**Bluetooth** — беспроводная технология для обмена данными на короткие расстояния
(обычно до 10 метров). Как невидимый провод между устройствами.

Телефон и Arduino соединяются через Bluetooth-модуль **HC-05** или **HC-06** —
маленькую платку, которую подключают к Arduino.

```
[Телефон Android]  ~~~Bluetooth~~~  [HC-05/HC-06]──[Arduino]
```

---

## Классический Bluetooth vs BLE

Существует два вида Bluetooth:

| Вид | Полное название | Когда использовать |
|---|---|---|
| Classic BT | Классический Bluetooth | Постоянный поток данных (наушники, мышь, HC-05) |
| BLE | Bluetooth Low Energy | Редкие короткие сигналы (часы, датчики) |

Наш проект использует **классический Bluetooth**, потому что HC-05 и HC-06
работают только с ним.

---

## SPP — Serial Port Profile

**SPP (Serial Port Profile)** — это «договорённость» о том, как передавать данные
по Bluetooth. Работает как виртуальный COM-порт: одна сторона пишет строку,
другая её читает.

Это максимально простой протокол — идеально для Arduino, которая читает данные
через `Serial.readStringUntil('\n')`.

### UUID — «адрес» протокола

Каждый Bluetooth-профиль имеет уникальный идентификатор — **UUID**:

```kotlin
private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
```

Этот UUID стандартный для SPP — его знают все устройства с HC-05/HC-06.
Он нужен при создании соединения, чтобы устройства «договорились» об одном протоколе.

---

## Как работает подключение

### Шаг 1: Сопряжение (Pairing)

Перед первым использованием нужно «познакомить» телефон и Arduino.
Это делается в системных настройках Bluetooth телефона — один раз.
После сопряжения оба устройства запоминают друг друга.

### Шаг 2: Поиск сопряжённых устройств

```kotlin
fun getPairedDevices(): List<BluetoothDevice> {
    return bluetoothAdapter!!.bondedDevices.toList()
}
```

`bondedDevices` — это список всех устройств, с которыми телефон уже знаком.
Наш HC-05 должен быть в этом списке.

### Шаг 3: Установка соединения

```kotlin
fun connect(device: BluetoothDevice): Boolean {
    // Создаём «трубку» (сокет) для разговора
    val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

    // Отменяем поиск новых устройств — он мешает подключению
    bluetoothAdapter?.cancelDiscovery()

    // Устанавливаем соединение — БЛОКИРУЮЩИЙ ВЫЗОВ!
    socket.connect()

    outputStream = socket.outputStream  // канал для отправки данных
    return true
}
```

`createRfcommSocketToServiceRecord` — создаёт виртуальную «трубку» для разговора
по SPP-протоколу. RFCOMM — транспортный уровень классического Bluetooth.

### Шаг 4: Отправка данных

```kotlin
fun sendData(data: String): Boolean {
    val dataToSend = if (!data.endsWith("\n")) "$data\n" else data
    outputStream!!.write(dataToSend.toByteArray())
    outputStream!!.flush()
    return true
}
```

Данные отправляются как массив байтов. `\n` в конце нужен Arduino — это сигнал
«строка закончилась, можно читать».

### Шаг 5: Разрыв соединения

```kotlin
fun disconnect() {
    outputStream?.close()
    bluetoothSocket?.close()
}
```

Всегда закрывай сокет, когда он не нужен — иначе Arduino будет думать,
что соединение ещё активно.

---

## Состояния соединения

В `BluetoothManager` есть три состояния, как светофор:

```kotlin
const val STATE_DISCONNECTED = 0  // красный  — нет связи
const val STATE_CONNECTING   = 1  // жёлтый   — идёт подключение
const val STATE_CONNECTED    = 2  // зелёный  — связь есть
```

---

## Разрешения Bluetooth в Android

Google постоянно ужесточает правила доступа к Bluetooth.
В Android 12 (API 31) всё изменилось:

```
До Android 12 (API < 31):
├── BLUETOOTH        — базовые операции
└── BLUETOOTH_ADMIN  — сканирование и управление

Android 12+ (API ≥ 31):
├── BLUETOOTH_SCAN     — поиск устройств
└── BLUETOOTH_CONNECT  — подключение и имена устройств
```

Поэтому в коде везде проверяется версия Android:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    // Android 12+: проверяем BLUETOOTH_CONNECT
    checkPermission(Manifest.permission.BLUETOOTH_CONNECT)
} else {
    // Старый Android: проверяем BLUETOOTH
    checkPermission(Manifest.permission.BLUETOOTH)
}
```

`Build.VERSION.SDK_INT` — это «номер версии» установленного Android.
`Build.VERSION_CODES.S` — константа, означающая Android 12.

---

## Почему connect() нельзя вызывать в главном потоке?

В Android есть правило: **главный поток** (Main Thread, UI Thread) нельзя блокировать.
Он отвечает за отрисовку экрана — если заблокировать, приложение «заморозится».

`socket.connect()` может занять несколько секунд. Поэтому вызов обёрнут в `Thread {}`:

```kotlin
Thread {
    val success = bluetoothManager.connect(device)  // долгая операция
    runOnUiThread {
        // возвращаемся в главный поток для обновления UI
        if (success) Toast.makeText(...)
    }
}.start()
```

---

## Формат данных, которые отправляются Arduino

```
2026-04-23 14:30;2026-04-23 09:15\n
│               │ │               │└── символ конца строки
│               │ └── время отправки (текущее время телефона)
│               └── разделитель
└── запланированное время приёма лекарства
```

Arduino читает всё до `\n` и разбирает строку.

---

## HC-05 vs HC-06 — в чём разница?

| Параметр | HC-05 | HC-06 |
|---|---|---|
| Режимы | Master + Slave | Только Slave |
| Настройка | Через AT-команды | Через AT-команды |
| Цена | Чуть дороже | Дешевле |
| Подходит нам? | ✓ Да | ✓ Да |

Оба модуля работают по SPP и с нашим приложением совместимы.

---

## Итого

| Понятие | Простыми словами |
|---|---|
| Bluetooth Classic | Беспроводной «провод» для постоянного потока данных |
| SPP | Протокол, как виртуальный COM-порт |
| UUID | Адрес протокола — чтобы устройства говорили «на одном языке» |
| Pairing (сопряжение) | Знакомство устройств — делается один раз |
| RFCOMM Socket | «Трубка» для разговора по Bluetooth |
| `\n` | Сигнал «конец строки» для Arduino |
