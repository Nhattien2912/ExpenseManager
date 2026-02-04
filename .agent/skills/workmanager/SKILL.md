---
name: WorkManager
description: Xử lý background tasks đáng tin cậy, survive app restarts.
---

# WorkManager

## Setup

```groovy
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

## Tạo Worker

```kotlin
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Thực hiện backup
            val file = BackupUtils.exportData(applicationContext)
            
            if (file != null) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
```

## One-time Work

```kotlin
val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    )
    .build()

WorkManager.getInstance(context).enqueue(backupRequest)
```

## Periodic Work (Auto backup hàng ngày)

```kotlin
val dailyBackup = PeriodicWorkRequestBuilder<BackupWorker>(
    1, TimeUnit.DAYS
)
    .setConstraints(
        Constraints.Builder()
            .setRequiresCharging(true)
            .build()
    )
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "daily_backup",
    ExistingPeriodicWorkPolicy.KEEP,
    dailyBackup
)
```

## Truyền dữ liệu

```kotlin
// Gửi data
val inputData = workDataOf(
    "FILE_PATH" to filePath,
    "BACKUP_TYPE" to "FULL"
)

val request = OneTimeWorkRequestBuilder<BackupWorker>()
    .setInputData(inputData)
    .build()

// Nhận trong Worker
override suspend fun doWork(): Result {
    val filePath = inputData.getString("FILE_PATH")
    val type = inputData.getString("BACKUP_TYPE")
    // ...
}
```

## Observe Work Status

```kotlin
WorkManager.getInstance(context)
    .getWorkInfoByIdLiveData(backupRequest.id)
    .observe(this) { workInfo ->
        when (workInfo.state) {
            WorkInfo.State.RUNNING -> showProgress()
            WorkInfo.State.SUCCEEDED -> showSuccess()
            WorkInfo.State.FAILED -> showError()
            else -> {}
        }
    }
```

## Chained Work

```kotlin
WorkManager.getInstance(context)
    .beginWith(cleanupRequest)
    .then(backupRequest)
    .then(uploadRequest)
    .enqueue()
```
