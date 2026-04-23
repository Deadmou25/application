# Android — как устроено мобильное приложение

## Что такое Android?

**Android** — это операционная система для телефонов, как Windows для компьютера.
Наше приложение работает именно на Android.

Чтобы создать Android-приложение, нужно знать несколько ключевых «кирпичиков»,
из которых оно строится.

---

## Activity — «комната» в приложении

**Activity** — это один экран приложения. Как комната в доме: зашёл — видишь определённые
вещи и кнопки, вышел — перешёл в другую комнату.

В нашем проекте есть одна главная Activity — **MainActivity**.
Она отображает весь интерфейс и управляет Bluetooth-соединением.

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // подключаем разметку экрана
    }
}
```

Важный момент: Activity умирает и пересоздаётся при повороте экрана.
Поэтому данные хранят не в Activity, а в ViewModel (подробнее в разделе 03).

---

## Fragment — «часть комнаты»

**Fragment** — это кусок экрана внутри Activity. Как мебель в комнате:
можно поставить диван слева и кресло справа, а можно переставить или заменить.

В нашем приложении вкладки — это фрагменты:
- **ControlFragment** — вкладка «Управление» (выбор даты/времени)
- **HistoryFragment** — вкладка «История» (список отправок)

Оба фрагмента живут внутри одной MainActivity, но показывают разный контент.

```kotlin
class ControlFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, ...): View? {
        return inflater.inflate(R.layout.fragment_control, ...)  // загружаем разметку
    }
}
```

---

## XML-разметка — «план комнаты»

Как выглядит экран, описывается в XML-файлах в папке `res/layout/`.
XML — это язык тегов (похож на HTML для сайтов).

```xml
<!-- Кнопка на экране -->
<Button
    android:id="@+id/sendButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="ОК (Отправить)" />
```

- `android:id` — имя элемента, чтобы найти его из кода
- `android:layout_width` — ширина: `match_parent` = на всю ширину экрана
- `android:text` — текст на кнопке

Из Kotlin-кода кнопку находят по имени:
```kotlin
val кнопка = view.findViewById<Button>(R.id.sendButton)
```

---

## Жизненный цикл — Activity «живёт» по правилам

Android жёстко управляет тем, когда Activity создаётся, уходит в фон и уничтожается.
Это называется **жизненным циклом**:

```
Создание → Запуск → Пауза → Стоп → Уничтожение
  onCreate   onStart  onPause  onStop  onDestroy
```

В нашем коде `onDestroy` используется для отключения Bluetooth,
чтобы не оставлять «открытое соединение» при закрытии приложения:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    bluetoothManager.disconnect()  // закрываем соединение при выходе
}
```

---

## Intent — «записка» между экранами

**Intent** — способ открыть другой экран или запросить что-то у системы.
Как записка: «Эй, система, включи Bluetooth!»

В проекте Intent используется для открытия системного диалога включения Bluetooth:

```kotlin
val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
startActivityForResult(intent, ...)
```

---

## Разрешения (Permissions) — «спросить у пользователя»

Android не разрешает приложению просто так использовать Bluetooth, камеру или геолокацию.
Сначала нужно спросить у пользователя — показать диалог «Разрешить / Запретить».

В `AndroidManifest.xml` объявляем, какие разрешения нужны:
```xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

А в коде запрашиваем их у пользователя:
```kotlin
requestBluetoothPermissionsLauncher.launch(arrayOf(
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN
))
```

Без разрешения Bluetooth просто не будет работать — это защита пользователя.

---

## AndroidManifest.xml — «паспорт» приложения

Файл `AndroidManifest.xml` — главный документ приложения.
Как паспорт: описывает, кто приложение, что умеет, какие разрешения ему нужны.

```xml
<manifest>
    <!-- Какие разрешения нужны -->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <application android:label="Передача времени приёма лекарств">
        <!-- Какие экраны есть в приложении -->
        <activity android:name=".ui.main.MainActivity" android:exported="true">
            <!-- Этот экран открывается при запуске -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## res/ — папка с ресурсами

Всё, что не является кодом, лежит в папке `res/`:

| Папка | Что хранит |
|---|---|
| `res/layout/` | XML-файлы с разметкой экранов |
| `res/values/strings.xml` | Все тексты приложения |
| `res/values/colors.xml` | Цвета |
| `res/values/themes.xml` | Тема (светлая/тёмная) |
| `res/drawable/` | Иконки и картинки |
| `res/mipmap/` | Иконка приложения |

Почему тексты хранят отдельно в `strings.xml`? Чтобы легко сделать перевод
на другой язык — просто добавить папку `values-en/` с английскими текстами.

---

## SDK — версии Android

- **Min SDK = 24** — приложение работает на Android 7.0 и новее.
  Устройства с Android 6 его не запустят.
- **Target SDK = 36** — приложение разработано под Android 15.
  Это сигнал системе использовать самые новые правила безопасности.

---

## Итого

| Понятие | Простыми словами |
|---|---|
| Activity | Один экран приложения |
| Fragment | Часть экрана внутри Activity |
| XML-разметка | Описание того, как выглядит экран |
| Intent | Записка другому экрану или системе |
| Permission | Разрешение, которое пользователь даёт приложению |
| Manifest | Паспорт приложения |
| res/ | Папка с картинками, текстами, цветами |
