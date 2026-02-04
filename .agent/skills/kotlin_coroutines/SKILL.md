---
name: Kotlin Coroutines
description: Hướng dẫn sử dụng Kotlin Coroutines cho async programming trong Android.
---

# Kỹ năng: Kotlin Coroutines trong Android

## Khi nào sử dụng
- Network requests
- Database operations
- File I/O
- Bất kỳ long-running operations nào

## Coroutine Basics

### Scopes

```kotlin
// ViewModel scope - auto cancel khi ViewModel cleared
class MyViewModel : ViewModel() {
    fun loadData() {
        viewModelScope.launch {
            // Coroutine code
        }
    }
}

// Lifecycle scope - auto cancel theo lifecycle
class MyFragment : Fragment() {
    fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Coroutine code
        }
    }
}

// Global scope - KHÔNG recommend trong Android
GlobalScope.launch { } // Avoid!
```

### Dispatchers

```kotlin
viewModelScope.launch {
    // Default: Main thread (UI)
    updateUI()
    
    withContext(Dispatchers.IO) {
        // IO operations: network, database, file
        val data = repository.fetchData()
    }
    
    withContext(Dispatchers.Default) {
        // CPU-intensive work: sorting, parsing
        val processed = processLargeData(data)
    }
    
    // Back to Main thread
    showResult(processed)
}
```

## Common Patterns

### Sequential Execution

```kotlin
viewModelScope.launch {
    val user = repository.getUser()      // Wait
    val orders = repository.getOrders()   // Then this
    updateUI(user, orders)
}
```

### Parallel Execution

```kotlin
viewModelScope.launch {
    // Run in parallel
    val userDeferred = async { repository.getUser() }
    val ordersDeferred = async { repository.getOrders() }
    
    // Wait for both
    val user = userDeferred.await()
    val orders = ordersDeferred.await()
    
    updateUI(user, orders)
}
```

### Error Handling

```kotlin
viewModelScope.launch {
    try {
        val result = repository.fetchData()
        _uiState.value = UiState.Success(result)
    } catch (e: HttpException) {
        _uiState.value = UiState.Error("Network error: ${e.message}")
    } catch (e: IOException) {
        _uiState.value = UiState.Error("Connection error")
    } catch (e: Exception) {
        _uiState.value = UiState.Error("Unknown error")
    }
}

// Hoặc dùng runCatching
viewModelScope.launch {
    repository.fetchData()
        .onSuccess { data -> _uiState.value = UiState.Success(data) }
        .onFailure { e -> _uiState.value = UiState.Error(e.message) }
}
```

### Timeout

```kotlin
viewModelScope.launch {
    try {
        val result = withTimeout(5000L) {
            repository.fetchData()
        }
        handleResult(result)
    } catch (e: TimeoutCancellationException) {
        showError("Request timed out")
    }
}

// Hoặc return null nếu timeout
val result = withTimeoutOrNull(5000L) {
    repository.fetchData()
}
```

## Flow

### Basic Flow

```kotlin
// Repository
fun getTransactions(): Flow<List<Transaction>> = flow {
    while (true) {
        val data = database.getAllTransactions()
        emit(data)
        delay(5000) // Refresh every 5 seconds
    }
}

// ViewModel
private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

init {
    viewModelScope.launch {
        repository.getTransactions().collect { data ->
            _transactions.value = data
        }
    }
}
```

### Flow Operators

```kotlin
repository.getTransactions()
    .map { list -> list.filter { it.amount > 0 } }  // Transform
    .filter { it.isNotEmpty() }                      // Filter
    .distinctUntilChanged()                          // Only emit when changed
    .debounce(300)                                   // Debounce
    .catch { e -> emit(emptyList()) }               // Handle errors
    .flowOn(Dispatchers.IO)                         // Run upstream on IO
    .collect { transactions ->
        // Collect on Main thread
    }
```

### StateFlow vs SharedFlow

```kotlin
// StateFlow: luôn có value, chỉ emit khi value thay đổi
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// SharedFlow: không có initial value, emit mọi lúc (events)
private val _events = MutableSharedFlow<Event>()
val events: SharedFlow<Event> = _events.asSharedFlow()

fun sendEvent() {
    viewModelScope.launch {
        _events.emit(Event.ShowToast("Hello"))
    }
}
```

### Collect trong Fragment

```kotlin
// Cách 1: flowWithLifecycle
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.uiState
        .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
        .collect { state ->
            updateUI(state)
        }
}

// Cách 2: repeatOnLifecycle (recommended cho multiple flows)
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
            viewModel.uiState.collect { updateUI(it) }
        }
        launch {
            viewModel.events.collect { handleEvent(it) }
        }
    }
}
```

## Suspend Functions

```kotlin
// Repository
suspend fun getUser(id: Long): User {
    return withContext(Dispatchers.IO) {
        database.getUserById(id)
    }
}

suspend fun saveUser(user: User) {
    withContext(Dispatchers.IO) {
        database.insert(user)
    }
}

// Combine multiple suspend functions
suspend fun syncData(): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val remoteData = api.fetchData()
            database.saveAll(remoteData)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Dependencies

```groovy
dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    
    // Lifecycle
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
}
```

## Best Practices

1. **Sử dụng structured concurrency** - viewModelScope, lifecycleScope
2. **Không block Main thread** - sử dụng Dispatchers.IO cho I/O
3. **Handle cancellation** - check isActive trong long-running loops
4. **Prefer Flow over LiveData** cho reactive streams
5. **Use repeatOnLifecycle** để collect flows safely
6. **Proper error handling** với try-catch hoặc runCatching
