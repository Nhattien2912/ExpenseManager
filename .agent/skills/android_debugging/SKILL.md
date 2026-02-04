---
name: Android Debugging
description: Kỹ năng debug ứng dụng Android, phân tích crash logs.
---

# Kỹ năng: Debugging Android Apps

## Đọc Crash Logs

```
E/AndroidRuntime: FATAL EXCEPTION: main
    java.lang.NullPointerException
        at com.example.app.MainActivity.updateUI(MainActivity.kt:45)
```

Cách đọc: Exception type → Message → Location (file:line)

## Common Crashes

### NullPointerException
```kotlin
// ✅ Handle null
val text = intent.getStringExtra("key") ?: "Default"
```

### Fragment not attached
```kotlin
// ✅ Use viewLifecycleOwner
viewModel.data.observe(viewLifecycleOwner) { }
```

### IndexOutOfBoundsException
```kotlin
// ✅ Safe access
val item = list.getOrNull(position)
```

## Logging
```kotlin
Log.d(TAG, "Debug message")
Log.e(TAG, "Error", exception)
```

## ADB Commands
```bash
adb logcat -s "TAG"
adb logcat *:E
adb install app.apk
```
