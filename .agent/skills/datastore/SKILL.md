---
name: DataStore Preferences
description: Thay thế SharedPreferences với Kotlin Coroutines và Flow.
---

# DataStore Preferences

## Setup

```groovy
dependencies {
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
```

## Tạo DataStore

```kotlin
// Trong file riêng hoặc Extension
val Context.dataStore by preferencesDataStore(name = "settings")

object PreferenceKeys {
    val CURRENCY_TYPE = intPreferencesKey("currency_type")
    val SPENDING_LIMIT = doublePreferencesKey("spending_limit")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val USER_NAME = stringPreferencesKey("user_name")
}
```

## Đọc dữ liệu (Flow)

```kotlin
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    
    val currencyType: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.CURRENCY_TYPE] ?: 0
        }
    
    val spendingLimit: Flow<Double> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.SPENDING_LIMIT] ?: 5000000.0
        }
    
    val darkMode: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.DARK_MODE] ?: false
        }
}
```

## Ghi dữ liệu

```kotlin
suspend fun setCurrencyType(type: Int) {
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.CURRENCY_TYPE] = type
    }
}

suspend fun setSpendingLimit(limit: Double) {
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.SPENDING_LIMIT] = limit
    }
}
```

## Sử dụng trong ViewModel

```kotlin
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val darkMode = settingsRepository.darkMode
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    fun toggleDarkMode() {
        viewModelScope.launch {
            val current = darkMode.value
            settingsRepository.setDarkMode(!current)
        }
    }
}
```

## Collect trong Fragment

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.darkMode.collect { isDark ->
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
```

## So sánh với SharedPreferences

| SharedPreferences | DataStore |
|-------------------|-----------|
| Synchronous API | Async với Coroutines |
| Có thể block UI thread | Không bao giờ block |
| Callback listeners | Flow reactive |
| Không type-safe | Type-safe với keys |
