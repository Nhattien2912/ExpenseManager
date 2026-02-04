---
name: ExpenseManager Backup
description: Backup và restore dữ liệu trong ExpenseManager sử dụng JSON và Google Drive.
---

# Backup & Restore trong ExpenseManager

## Local Backup (JSON)

### Export
```kotlin
import com.nhattien.expensemanager.utils.BackupUtils

lifecycleScope.launch {
    val file = BackupUtils.exportData(requireContext())
    file?.let {
        // Share file
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            it
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        startActivity(Intent.createChooser(intent, "Chia sẻ backup"))
    }
}
```

### Import
```kotlin
// Mở file picker
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "application/json"
}
filePickerLauncher.launch(intent)

// Xử lý kết quả
private val filePickerLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    result.data?.data?.let { uri ->
        lifecycleScope.launch {
            val success = BackupUtils.importData(requireContext(), uri)
            if (success) {
                Toast.makeText(context, "Khôi phục thành công", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

## CSV Export

```kotlin
import com.nhattien.expensemanager.utils.CsvUtils

val transactions = viewModel.allTransactions.value
val uri = CsvUtils.exportTransactionsToCsv(requireContext(), transactions)
uri?.let {
    // Share CSV file
}
```

## Google Drive Backup

### Setup DriveServiceHelper
```kotlin
import com.nhattien.expensemanager.utils.DriveServiceHelper

val driveHelper = DriveServiceHelper(
    Drive.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    ).build()
)
```

### Upload to Drive
```kotlin
lifecycleScope.launch {
    val localFile = BackupUtils.exportData(requireContext())
    localFile?.let {
        driveHelper.uploadFile(it, "expense_manager_backup.json")
    }
}
```

## Firebase Realtime Database

```kotlin
import com.nhattien.expensemanager.utils.FirebaseUtils

// Sync to Firebase
FirebaseUtils.syncToCloud(transactions)

// Restore from Firebase
FirebaseUtils.fetchFromCloud { data ->
    // Import data
}
```

## Backup Data Structure

```json
[
  {
    "id": 1,
    "amount": 500000.0,
    "categoryId": 1,
    "paymentMethod": "CASH",
    "type": "EXPENSE",
    "note": "Ăn trưa",
    "date": 1704067200000,
    "isRecurring": false

  }
]
```
