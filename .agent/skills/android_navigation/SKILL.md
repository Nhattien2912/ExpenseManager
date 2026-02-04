---
name: Android Navigation Component
description: Hướng dẫn sử dụng Jetpack Navigation Component để điều hướng giữa các màn hình.
---

# Kỹ năng: Navigation Component trong Android

## Khi nào sử dụng
- Điều hướng giữa các Fragment
- Bottom Navigation với multiple destinations
- Deep linking
- Safe Args để truyền data

## Setup

### 1. Dependencies

```groovy
// build.gradle (project)
buildscript {
    dependencies {
        classpath "androidx.navigation:navigation-safe-args-gradle-plugin:2.7.6"
    }
}

// build.gradle (app)
plugins {
    id 'androidx.navigation.safeargs.kotlin'
}

dependencies {
    def nav_version = "2.7.6"
    implementation "androidx.navigation:navigation-fragment-ktx:$nav_version"
    implementation "androidx.navigation:navigation-ui-ktx:$nav_version"
}
```

### 2. Navigation Graph (nav_graph.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/homeFragment">

    <fragment
        android:id="@+id/homeFragment"
        android:name="com.example.ui.home.HomeFragment"
        android:label="Home">
        
        <action
            android:id="@+id/action_home_to_detail"
            app:destination="@id/detailFragment"
            app:enterAnim="@anim/slide_in_right"
            app:exitAnim="@anim/slide_out_left"
            app:popEnterAnim="@anim/slide_in_left"
            app:popExitAnim="@anim/slide_out_right" />
    </fragment>

    <fragment
        android:id="@+id/detailFragment"
        android:name="com.example.ui.detail.DetailFragment"
        android:label="Detail">
        
        <argument
            android:name="itemId"
            app:argType="long" />
        
        <argument
            android:name="itemName"
            app:argType="string"
            android:defaultValue="" />
    </fragment>

    <fragment
        android:id="@+id/settingsFragment"
        android:name="com.example.ui.settings.SettingsFragment"
        android:label="Settings" />

</navigation>
```

### 3. Activity Layout với NavHostFragment

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/bottom_nav"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:menu="@menu/bottom_nav_menu"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

## Điều hướng cơ bản

### Navigate đến destination

```kotlin
// Cách 1: Sử dụng action ID
findNavController().navigate(R.id.action_home_to_detail)

// Cách 2: Sử dụng Safe Args (Recommended)
val action = HomeFragmentDirections.actionHomeToDetail(
    itemId = 123L,
    itemName = "Example"
)
findNavController().navigate(action)

// Cách 3: Navigate trực tiếp đến destination
findNavController().navigate(R.id.detailFragment)
```

### Nhận arguments với Safe Args

```kotlin
class DetailFragment : Fragment() {
    
    private val args: DetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val itemId = args.itemId
        val itemName = args.itemName
        
        viewModel.loadItem(itemId)
    }
}
```

### Navigate back

```kotlin
// Pop back stack
findNavController().navigateUp()
// hoặc
findNavController().popBackStack()

// Pop đến destination cụ thể
findNavController().popBackStack(R.id.homeFragment, inclusive = false)
```

## Bottom Navigation Setup

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup Bottom Navigation
        binding.bottomNav.setupWithNavController(navController)

        // Setup ActionBar với Navigation
        setupActionBarWithNavController(navController)

        // Ẩn/hiện bottom nav theo destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.detailFragment, R.id.addTransactionFragment -> {
                    binding.bottomNav.visibility = View.GONE
                }
                else -> {
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
```

## Deep Links

```xml
<!-- Trong nav_graph.xml -->
<fragment
    android:id="@+id/detailFragment"
    android:name="com.example.ui.DetailFragment">
    
    <deepLink
        android:id="@+id/deepLink"
        app:uri="example://detail/{itemId}" />
</fragment>

<!-- Trong AndroidManifest.xml -->
<activity android:name=".MainActivity">
    <nav-graph android:value="@navigation/nav_graph" />
</activity>
```

## Nested Navigation Graphs

```xml
<navigation
    android:id="@+id/nav_graph"
    app:startDestination="@id/homeFragment">

    <!-- Nested graph cho Authentication -->
    <navigation
        android:id="@+id/auth_graph"
        app:startDestination="@id/loginFragment">
        
        <fragment
            android:id="@+id/loginFragment"
            android:name=".LoginFragment" />
            
        <fragment
            android:id="@+id/registerFragment"
            android:name=".RegisterFragment" />
    </navigation>

    <fragment
        android:id="@+id/homeFragment"
        android:name=".HomeFragment">
        
        <action
            android:id="@+id/action_home_to_auth"
            app:destination="@id/auth_graph" />
    </fragment>
</navigation>
```

## Best Practices

1. **Sử dụng Safe Args** để truyền data type-safe
2. **Đặt action animations** cho smooth transitions
3. **Nested graphs** cho related screens
4. **Consistent back behavior** với popUpTo và popUpToInclusive
5. **Một Activity, nhiều Fragments** pattern
