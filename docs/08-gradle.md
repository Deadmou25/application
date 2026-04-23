# Gradle — система сборки проекта

## Что такое система сборки?

Представь, что приложение — это сложный LEGO-конструктор.
Детали (библиотеки) нужно найти, скачать, сложить в правильном порядке,
склеить (скомпилировать) — и только тогда получится готовое приложение (APK).

Делать всё это вручную — долго и сложно. **Gradle** делает это автоматически.

**Gradle** — это инструмент, который:
- Скачивает нужные библиотеки из интернета
- Компилирует Kotlin-код в байткод
- Упаковывает всё в `.apk` файл, который можно установить на телефон

---

## Структура Gradle-файлов в проекте

```
application/
├── build.gradle.kts          ← настройки для всего проекта
├── settings.gradle.kts       ← имя проекта и модули
├── gradle.properties         ← глобальные параметры сборки
├── gradle/
│   └── libs.versions.toml    ← версии всех библиотек в одном месте
└── app/
    └── build.gradle.kts      ← настройки конкретного модуля (самый важный)
```

---

## Kotlin DSL — Gradle на Kotlin

Раньше Gradle-файлы писали на языке **Groovy** (`.gradle`).
Теперь можно писать на **Kotlin** (`.gradle.kts`).

В нашем проекте используется **Kotlin DSL** (файлы `.kts`).
Это тот же Kotlin, поэтому IDE подсказывает и проверяет ошибки.

---

## `libs.versions.toml` — каталог версий

Это новый способ управления версиями библиотек.
Все версии прописаны в одном месте — удобно обновлять:

```toml
[versions]
kotlin = "2.0.21"      # версия Kotlin
room   = "2.8.4"       # версия Room
material = "1.13.0"    # версия Material Design

[libraries]
# Описываем библиотеки: группа + имя + ссылка на версию
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
google-material       = { group = "com.google.android.material", name = "material", version.ref = "material" }

[plugins]
# Плагины — расширения для Gradle
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android      = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

Чтобы обновить Room с `2.8.4` до `2.9.0` — меняешь одну строчку:
```toml
room = "2.9.0"   # вместо "2.8.4"
```

---

## `app/build.gradle.kts` — главный файл настроек

Это самый важный файл. Разберём его по частям:

### Плагины — расширения Gradle

```kotlin
plugins {
    alias(libs.plugins.android.application)  // "это Android-приложение"
    alias(libs.plugins.kotlin.android)        // "используем Kotlin"
    alias(libs.plugins.kotlin.compose)        // "используем Jetpack Compose"
    id("kotlin-kapt")                         // обработчик аннотаций (нужен Room)
}
```

### android {} — настройки приложения

```kotlin
android {
    namespace = "com.example.myapplication"  // уникальное имя пакета
    compileSdk { version = release(36) }     // компилируем под Android 15

    defaultConfig {
        applicationId = "com.example.myapplication"  // ID в Google Play
        minSdk = 24     // минимальный Android: 7.0
        targetSdk = 36  // целевой Android: 15
        versionCode = 1         // внутренний номер версии (целое число)
        versionName = "1.0"    // версия для пользователей
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // НЕ сжимать код (для отладки лучше включить)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11  // совместимость с Java 11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

### dependencies {} — список библиотек

```kotlin
dependencies {
    // Основные библиотеки Android
    implementation(libs.androidx.core.ktx)              // расширения Kotlin для Android
    implementation(libs.androidx.lifecycle.runtime.ktx) // ViewModel, lifecycleScope

    // Интерфейс
    implementation(libs.google.material)  // кнопки, TabLayout и другие Material-компоненты

    // База данных Room
    implementation(libs.androidx.room.runtime)  // основной модуль Room
    implementation(libs.androidx.room.ktx)      // расширения Room для Kotlin (Flow, suspend)
    kapt(libs.androidx.room.compiler)           // генератор кода (обрабатывает @Entity, @Dao)

    // Тестирование
    testImplementation(libs.junit)              // unit-тесты
    androidTestImplementation(libs.androidx.espresso.core)  // UI-тесты
}
```

---

## Что означают `implementation`, `kapt`, `testImplementation`

| Ключевое слово | Смысл |
|---|---|
| `implementation` | Библиотека нужна в рабочем приложении |
| `kapt` | Инструмент для обработки аннотаций во время компиляции (не входит в APK) |
| `testImplementation` | Библиотека только для unit-тестов |
| `androidTestImplementation` | Библиотека только для инструментальных тестов на устройстве |
| `debugImplementation` | Только для debug-сборки |

---

## Как Gradle скачивает библиотеки

Gradle скачивает библиотеки из **репозиториев** — онлайн-хранилищ кода.
Основной репозиторий для Android — **Maven Central** и **Google Maven**.

Путь библиотеки: `группа:имя:версия`
```
androidx.room : room-runtime : 2.8.4
^^^^^^^^^^^^^   ^^^^^^^^^^^^   ^^^^^
   группа          имя         версия
```

Скачанные библиотеки кэшируются на компьютере — повторная сборка не скачивает их заново.

---

## AGP — Android Gradle Plugin

**AGP (Android Gradle Plugin)** — расширение для Gradle, которое добавляет поддержку
Android-проектов: умеет подписывать APK, работать с ресурсами, собирать под разные архитектуры.

В проекте используется **AGP 8.13.2**.

---

## `gradlew` — запуск Gradle без установки

Файлы `gradlew` (Mac/Linux) и `gradlew.bat` (Windows) — это «упакованный» Gradle,
который не нужно устанавливать отдельно. Android Studio использует его автоматически.

Можно запустить вручную в терминале:
```bash
./gradlew assembleDebug    # собрать debug-APK
./gradlew assembleRelease  # собрать release-APK
./gradlew test             # запустить тесты
./gradlew clean            # очистить папку build/
```

---

## Что происходит при нажатии «Run» в Android Studio

```
1. Gradle читает build.gradle.kts
        │
        ▼
2. Скачивает недостающие библиотеки из репозитория
        │
        ▼
3. KAPT обрабатывает аннотации (@Entity, @Dao) — генерирует код для Room
        │
        ▼
4. Компилятор Kotlin превращает .kt файлы в байткод (.class)
        │
        ▼
5. D8/R8 превращает байткод в DEX-формат для Android
        │
        ▼
6. APK-пакет: DEX + ресурсы (картинки, XML, строки)
        │
        ▼
7. APK подписывается debug-ключом
        │
        ▼
8. APK устанавливается на телефон через ADB
        │
        ▼
9. Приложение запускается!
```

---

## Итого

| Файл/Понятие | Роль |
|---|---|
| `libs.versions.toml` | Один список всех версий библиотек |
| `app/build.gradle.kts` | Настройки приложения, список зависимостей |
| `settings.gradle.kts` | Имя проекта, подключение модулей |
| `gradlew` | Скрипт для запуска Gradle без установки |
| `implementation` | Добавить библиотеку в приложение |
| `kapt` | Запустить генератор кода для библиотеки |
| AGP | Плагин, который делает Gradle «понимающим» Android |
| APK | Готовый файл приложения для установки на телефон |
