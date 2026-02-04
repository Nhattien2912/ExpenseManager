---
name: Hilt Dependency Injection
description: Dependency Injection framework chính thức của Android, đơn giản hóa Dagger.
---

# Hilt Dependency Injection

## Setup

```groovy
// build.gradle (project)
plugins {
    id("com.google.dagger.hilt.android") version "2.50" apply false
}

// build.gradle (app)
plugins {
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
}
```

## Application class

```kotlin
@HiltAndroidApp
class ExpenseManagerApp : Application()
```

## Module cung cấp dependencies

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expense_db"
        ).build()
    }
    
    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao {
        return db.transactionDao()
    }
    
    @Provides
    @Singleton
    fun provideRepository(dao: TransactionDao): ExpenseRepository {
        return ExpenseRepository(dao)
    }
}
```

## Inject vào ViewModel

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {
    
    val transactions = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

## Inject vào Activity/Fragment

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val viewModel: MainViewModel by viewModels()
}

@AndroidEntryPoint
class HomeFragment : Fragment() {
    
    private val viewModel: MainViewModel by viewModels()
    
    // Shared với Activity
    private val sharedViewModel: SharedViewModel by activityViewModels()
}
```

## Scopes

```kotlin
@Singleton          // Application scope
@ActivityScoped     // Activity lifecycle
@FragmentScoped     // Fragment lifecycle
@ViewModelScoped    // ViewModel lifecycle
```
