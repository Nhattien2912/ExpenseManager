---
name: ExpenseManager Tutorial
description: Sử dụng TapTargetView để tạo tutorial hướng dẫn người dùng.
---

# Tutorial với TapTargetView

## Dependency
```groovy
implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")
```

## TutorialHelper

```kotlin
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.nhattien.expensemanager.utils.TutorialHelper

// Kiểm tra đã xem tutorial chưa
if (!TutorialHelper.hasSeenTutorial(requireContext(), "main_screen")) {
    showTutorial()
}

// Đánh dấu đã xem
TutorialHelper.markTutorialAsSeen(requireContext(), "main_screen")
```

## Tạo Tutorial Sequence

```kotlin
private fun showTutorial() {
    TapTargetSequence(requireActivity())
        .targets(
            TapTarget.forView(
                binding.fabAdd,
                "Thêm giao dịch",
                "Nhấn vào đây để thêm thu nhập hoặc chi tiêu mới"
            )
            .outerCircleColor(R.color.primary)
            .targetCircleColor(android.R.color.white)
            .titleTextSize(20)
            .descriptionTextSize(14)
            .cancelable(true),
            
            TapTarget.forView(
                binding.tabLayout,
                "Chuyển đổi tab",
                "Xem tổng quan, lịch, hoặc báo cáo"
            )
            .outerCircleColor(R.color.primary),
            
            TapTarget.forView(
                binding.btnFilter,
                "Lọc giao dịch",
                "Lọc theo thu nhập, chi tiêu hoặc tất cả"
            )
            .outerCircleColor(R.color.primary)
        )
        .listener(object : TapTargetSequence.Listener {
            override fun onSequenceFinish() {
                TutorialHelper.markTutorialAsSeen(requireContext(), "main_screen")
            }
            override fun onSequenceStep(target: TapTarget, targetClicked: Boolean) {}
            override fun onSequenceCanceled(target: TapTarget) {}
        })
        .start()
}
```

## Single TapTarget

```kotlin
TapTargetView.showFor(
    requireActivity(),
    TapTarget.forView(
        binding.someView,
        "Tiêu đề",
        "Mô tả chi tiết"
    )
    .cancelable(true)
    .tintTarget(false),
    object : TapTargetView.Listener() {
        override fun onTargetClick(view: TapTargetView) {
            super.onTargetClick(view)
            // Handle click
        }
    }
)
```

## SharedPreferences Helper

```kotlin
object TutorialHelper {
    private const val PREFS_NAME = "tutorial_prefs"
    
    fun hasSeenTutorial(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key, false)
    }
    
    fun markTutorialAsSeen(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, true).apply()
    }
    
    fun resetAllTutorials(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
```
