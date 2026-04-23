# MVVM — как код делится на части

## Зачем вообще делить код на части?

Представь, что весь код приложения — это один огромный ящик, в котором
вперемешку лежат кнопки, данные, настройки Bluetooth и база данных.
Нужно что-то найти? Придётся перерыть весь ящик.

Чтобы не было такого беспорядка, программисты придумали **архитектурные паттерны** —
правила, как раскладывать код по «полочкам».

В нашем проекте используется паттерн **MVVM**.

---

## MVVM расшифровывается как:

- **M** — Model (Модель) — данные и бизнес-логика
- **V** — View (Вид) — то, что видит пользователь (экраны)
- **VM** — ViewModel (Модель Вида) — мост между данными и экраном

---

## Простая аналогия: ресторан

Представь ресторан:

```
Гость (пользователь)
    │  смотрит меню, делает заказ
    ▼
Официант (ViewModel)
    │  принимает заказ, передаёт на кухню, приносит еду
    ▼
Кухня (Model/Repository)
    │  готовит, хранит продукты
    ▼
Холодильник (База данных)
```

- **Гость не ходит на кухню** — он не должен знать, как готовится еда.
  Так и экран (View) не должен лезть прямо в базу данных.
- **Официант знает и гостя, и кухню** — это ViewModel, связующее звено.
- **Кухня не знает, как выглядит зал** — данные не зависят от экрана.

---

## View — что видит пользователь

**View** в нашем проекте — это фрагменты и Activity:
- `MainActivity`
- `ControlFragment`
- `HistoryFragment`

View отвечает только за **отображение** данных и **реакцию на нажатия**.
Она не должна думать, куда сохранять данные или как работает Bluetooth.

```kotlin
// ControlFragment просто говорит ViewModel: "пользователь нажал кнопку"
sendButton.setOnClickListener {
    viewModel.sendMedicineTime(year, month, day, hour, minute)
}
```

---

## ViewModel — умный помощник

**ViewModel** (`MainViewModel`) — посредник между экраном и данными.

Самое важное свойство ViewModel: она **переживает поворот экрана**.
Когда пользователь поворачивает телефон, Activity пересоздаётся с нуля,
но ViewModel остаётся живой и данные не теряются.

```kotlin
class MainViewModel(private val repository: MedicineRepository) : ViewModel() {

    // Хранит статус соединения — экран подписывается и обновляется сам
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Экран говорит: "отправь данные" — ViewModel решает, как
    fun sendMedicineTime(year: Int, month: Int, ...) {
        viewModelScope.launch {
            repository.sendMedicineTime(...)
        }
    }
}
```

---

## Model — данные и правила

**Model** — это:

### Repository (Репозиторий) — `MedicineRepository`
Единственная точка доступа к данным для ViewModel.
ViewModel не знает, откуда берутся данные — из БД или Bluetooth.
Она просто просит Repository, а тот разбирается.

```kotlin
class MedicineRepository(
    private val medicineDao: MedicineDao,          // доступ к базе данных
    private val bluetoothManager: BluetoothManager  // доступ к Bluetooth
) {
    suspend fun sendMedicineTime(...): Boolean {
        bluetoothManager.sendData(...)  // отправляем
        medicineDao.insert(...)         // сохраняем
    }
}
```

### Data (Данные) — `Medicine`, `AppDatabase`, `MedicineDao`
Сама структура данных и способы работы с ней (база данных Room).

---

## Как всё связано в проекте

```
┌─────────────────────────────────────────────┐
│  VIEW (экраны)                              │
│  MainActivity                               │
│  ControlFragment   ──наблюдает StateFlow──► │
│  HistoryFragment                            │
└────────────────────┬────────────────────────┘
                     │ вызывает методы
┌────────────────────▼────────────────────────┐
│  VIEWMODEL                                  │
│  MainViewModel                              │
│  - isConnected: StateFlow<Boolean>          │
│  - medicines: Flow<List<Medicine>>          │
│  - sendMedicineTime()                       │
└────────────────────┬────────────────────────┘
                     │ вызывает suspend-функции
┌────────────────────▼────────────────────────┐
│  MODEL                                      │
│  MedicineRepository                         │
│  ├── MedicineDao (Room)                     │
│  └── BluetoothManager                      │
└─────────────────────────────────────────────┘
```

---

## ViewModelFactory — фабрика для ViewModel

ViewModel нельзя просто создать через `MainViewModel()` — Android сам управляет
её созданием. Но наша ViewModel требует `repository` в конструкторе.

**ViewModelFactory** — это инструкция для Android, как создать нужную ViewModel:

```kotlin
class MainViewModelFactory(private val repository: MedicineRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}

// Использование в Activity:
val factory = MainViewModelFactory(repository)
val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
```

Как рецепт: «Чтобы приготовить `MainViewModel`, возьми `repository` и...»

---

## Плюсы MVVM

| Проблема | Как MVVM решает |
|---|---|
| Код-спагетти — всё в одном месте | Каждый слой знает только своё дело |
| Данные теряются при повороте | ViewModel переживает поворот экрана |
| Сложно тестировать | Каждый слой можно протестировать отдельно |
| Трудно менять UI | View легко заменить — данные не изменятся |

---

## Итого

| Слой | Класс в проекте | Что делает |
|---|---|---|
| View | `MainActivity`, `ControlFragment`, `HistoryFragment` | Показывает UI, реагирует на нажатия |
| ViewModel | `MainViewModel` | Хранит состояние, обрабатывает команды от UI |
| Repository | `MedicineRepository` | Координирует БД и Bluetooth |
| Model | `Medicine`, `MedicineDao`, `AppDatabase` | Хранит и описывает данные |
