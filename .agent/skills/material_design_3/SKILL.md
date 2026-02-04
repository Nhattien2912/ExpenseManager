---
name: Material Design 3
description: Thiết kế UI theo Material You với Dynamic Colors và các components mới.
---

# Material Design 3 (Material You)

## Setup

```groovy
dependencies {
    implementation("com.google.android.material:material:1.11.0")
}
```

## Theme (themes.xml)

```xml
<style name="Theme.ExpenseManager" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="colorPrimary">@color/primary</item>
    <item name="colorOnPrimary">@color/on_primary</item>
    <item name="colorPrimaryContainer">@color/primary_container</item>
    <item name="colorSecondary">@color/secondary</item>
    <item name="colorSurface">@color/surface</item>
    <item name="colorSurfaceVariant">@color/surface_variant</item>
    <item name="colorError">@color/error</item>
</style>
```

## Dynamic Colors (Android 12+)

```kotlin
class ExpenseManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Áp dụng dynamic colors
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
```

## Material 3 Components

### Card
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardElevation="2dp"
    app:strokeWidth="0dp"
    style="@style/Widget.Material3.CardView.Filled">
    
    <!-- Content -->
    
</com.google.android.material.card.MaterialCardView>
```

### FloatingActionButton
```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:src="@drawable/ic_add"
    style="@style/Widget.Material3.FloatingActionButton.Primary" />
```

### Bottom Navigation
```xml
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:menu="@menu/bottom_nav"
    style="@style/Widget.Material3.BottomNavigationView" />
```

### Top App Bar
```xml
<com.google.android.material.appbar.MaterialToolbar
    android:layout_width="match_parent"
    android:layout_height="?attr/actionBarSize"
    style="@style/Widget.Material3.Toolbar" />
```

### Button Styles
```xml
<!-- Filled -->
<Button style="@style/Widget.Material3.Button" />

<!-- Outlined -->
<Button style="@style/Widget.Material3.Button.OutlinedButton" />

<!-- Text -->
<Button style="@style/Widget.Material3.Button.TextButton" />

<!-- Tonal -->
<Button style="@style/Widget.Material3.Button.TonalButton" />
```

## Color Roles

```kotlin
// Programmatically get Material colors
val primary = MaterialColors.getColor(view, R.attr.colorPrimary)
val surface = MaterialColors.getColor(view, R.attr.colorSurface)
val error = MaterialColors.getColor(view, R.attr.colorError)
```

## Shape

```xml
<style name="ShapeAppearance.ExpenseManager.SmallComponent" parent="ShapeAppearance.Material3.SmallComponent">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">12dp</item>
</style>
```
