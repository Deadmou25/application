# Корутины и Flow — как делать несколько дел одновременно

## Проблема: телефон должен оставаться отзывчивым

Представь: ты нажимаешь кнопку «Отправить» в приложении.
Телефон должен отправить данные по Bluetooth — это занимает 1–2 секунды.

Если всё делать в один поток, экран будет заморожен: кнопки не нажимаются,
анимации не идут. Пользователь подумает, что приложение зависло.

Решение — делать долгие операции **в фоне**, не блокируя экран.

---

## Поток (Thread) — как отдельный работник

**Поток (Thread)** — независимый поток выполнения кода.

В Android есть главный поток — **Main Thread** (UI Thread).
Он отвечает за **отрисовку экрана**. Если его занять долгой работой — экран замёрзнет.

Решение: запустить долгую работу в **отдельном потоке**:

```kotlin
Thread {
    // это выполняется в фоне — Main Thread свободен
    val success = bluetoothManager.connect(device)

    runOnUiThread {
        // возвращаемся в Main Thread для обновления UI
        Toast.makeText(...)
    }
}.start()
```

Это работает, но неудобно: нужно вручную переключаться между потоками.
Именно так сделано подключение Bluetooth в нашем проекте.

---

## Корутины — лёгкие «работники»

**Корутины (Coroutines)** — более удобная альтернатива потокам.

Аналогия: поток — это грузовик (тяжёлый, дорогой). Корутина — велосипед (лёгкий, быстрый).
Можно запустить тысячи корутин одновременно, а тысяча потоков — уже проблема.

### `suspend` — функция, которая умеет ждать

Ключевое слово `suspend` превращает обычную функцию в «приостанавливаемую»:

```kotlin
// Обычная функция — выполняется сразу
fun обычная(): Boolean { ... }

// Suspend-функция — может ждать, не блокируя поток
suspend fun медленная(): Boolean {
    delay(1000)  // ждёт 1 секунду, НЕ блокируя Main Thread
    return true
}
```

`suspend`-функции можно вызывать только из другой `suspend`-функции
или из корутины.

### `launch` — запустить корутину

```kotlin
viewModelScope.launch {
    // Этот код выполняется в фоне
    val success = repository.sendMedicineTime(...)  // долгая операция
    // После завершения — автоматически возвращается в нужный поток
    _successMessage.value = "Готово!"  // обновляем UI
}
```

Никакого `runOnUiThread` — корутины сами знают, в каком потоке нужно работать.

---

## Scope — «контейнер» для корутин

Корутины живут внутри **Scope** (области видимости).
Когда Scope уничтожается — все корутины в нём тоже отменяются.

В проекте используются два Scope:

### `viewModelScope`

Корутины, запущенные здесь, живут, пока живёт ViewModel.
При уничтожении ViewModel (например, при закрытии приложения) — всё отменяется автоматически.

```kotlin
class MainViewModel : ViewModel() {
    fun sendMedicineTime(...) {
        viewModelScope.launch {
            repository.sendMedicineTime(...)
        }
    }
}
```

### `lifecycleScope`

Корутины, запущенные здесь, живут, пока живёт Fragment или Activity.
Используется для подписки на данные в UI-слое:

```kotlin
class ControlFragment : Fragment() {
    override fun onViewCreated(...) {
        lifecycleScope.launch {
            viewModel.isConnected.collectLatest { isConnected ->
                updateConnectionStatus(isConnected)  // обновляем UI
            }
        }
    }
}
```

---

## Dispatchers — куда отправить корутину

Корутина может работать в разных «местах»:

| Dispatcher | Для чего |
|---|---|
| `Dispatchers.Main` | Главный поток — обновление UI |
| `Dispatchers.IO` | Сеть, файлы, база данных |
| `Dispatchers.Default` | Тяжёлые вычисления |

Room и большинство библиотек сами переключают диспетчер.
Поэтому в коде ViewModel можно просто написать:

```kotlin
viewModelScope.launch {  // по умолчанию — Main
    repository.sendMedicineTime(...)  // Room сам переключится на IO
}
```

---

## Flow — поток данных

**Flow** — это не одноразовый ответ, а **поток значений во времени**.

### Обычная функция (одноразово):
```
запрос ──► [функция] ──► один ответ
```

### Flow (поток):
```
подписка ──► [Flow] ──► ответ1 ──► ответ2 ──► ответ3 ──► ...
```

**Пример из жизни:** обычная функция — это как спросить «который час?» и получить ответ.
Flow — это как повесить часы на стену: они сами показывают время и обновляются каждую минуту.

В проекте `MedicineDao.getAll()` возвращает `Flow<List<Medicine>>`:

```kotlin
fun getAll(): Flow<List<Medicine>>
```

Это значит: как только в базе что-то изменится — Flow автоматически пришлёт новый список.
Экран обновится сам, не нужно ничего запрашивать повторно.

---

## StateFlow — Flow с памятью

**StateFlow** — это особый вид Flow, который:
1. Всегда хранит **последнее значение** (новый подписчик сразу получит его)
2. Используется для хранения **состояния UI**

```kotlin
// В ViewModel:
private val _isConnected = MutableStateFlow(false)  // изменяемый
val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()  // только для чтения

// Изменить значение (только внутри ViewModel):
_isConnected.value = true

// Подписаться (во Fragment):
lifecycleScope.launch {
    viewModel.isConnected.collectLatest { isConnected ->
        // вызывается каждый раз, когда значение меняется
        updateConnectionStatus(isConnected)
    }
}
```

Это как доска объявлений: ViewModel пишет на доске, все фрагменты смотрят на неё
и реагируют, как только что-то изменится.

---

## `collectLatest` vs `collect`

| Метод | Поведение |
|---|---|
| `collect` | Обрабатывает каждое значение, даже если не успел — ждёт |
| `collectLatest` | Если пришло новое значение — отменяет обработку старого |

В нашем проекте используется `collectLatest` — нам важно показать актуальный статус,
а не обрабатывать все промежуточные состояния.

---

## MutableStateFlow — изменяемый StateFlow

В ViewModel есть два уровня доступа:

```kotlin
// Приватный — только ViewModel может менять
private val _isConnected = MutableStateFlow(false)

// Публичный — только для чтения, для UI
val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
```

Это как рычаги управления самолётом: пилот (ViewModel) управляет,
пассажиры (Fragment) только смотрят на показания приборов.

---

## Корутины в нашем проекте: схема

```
Пользователь нажимает кнопку
         │
         ▼
ControlFragment.handleSendButtonClick()
         │  (Main Thread)
         ▼
viewModel.sendMedicineTime(...)
         │
         ▼
viewModelScope.launch {          ← запуск корутины
    val result = repository      ← suspend-функция (переключается на IO)
        .sendMedicineTime(...)
    _successMessage.value = "OK"  ← обновление StateFlow (Main Thread)
}
         │
         ▼
ControlFragment наблюдает errorMessage / successMessage
через lifecycleScope.launch { collectLatest {...} }
         │
         ▼
Toast.makeText(...)
```

---

## Версии в проекте

- **Kotlin Coroutines 1.10.2** — библиотека корутин
- **Lifecycle Runtime KTX 2.10.0** — содержит `viewModelScope` и `lifecycleScope`

---

## Итого

| Понятие | Простыми словами |
|---|---|
| Main Thread | Единственный поток, рисующий UI — его нельзя блокировать |
| Корутина | Лёгкий «работник» для фоновых задач |
| `suspend fun` | Функция, которая умеет ждать без заморозки |
| `launch` | Запустить корутину |
| `viewModelScope` | Корутины живут столько, сколько ViewModel |
| `lifecycleScope` | Корутины живут столько, сколько Fragment/Activity |
| Flow | Поток значений во времени, как часы на стене |
| StateFlow | Flow с памятью — хранит последнее значение |
| `collectLatest` | Подписаться на Flow, отменяя обработку устаревших значений |
