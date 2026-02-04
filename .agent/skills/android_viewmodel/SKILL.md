---
name: Android ViewModel & LiveData
description: Hướng dẫn sử dụng ViewModel và LiveData trong Android với MVVM pattern.
---

# Kỹ năng: ViewModel và LiveData trong Android

## Khi nào sử dụng
- Cần lưu trữ và quản lý UI-related data
- Muốn data survive configuration changes (xoay màn hình)
- Implement MVVM architecture

## Cấu trúc ViewModel chuẩn

```kotlin
class ExampleViewModel(
    private val repository: ExampleRepository
) : ViewModel() {

    // Private MutableLiveData để sửa đổi nội bộ
    private val _items = MutableLiveData<List<Item>>()
    // Public LiveData để expose ra ngoài (immutable)
    val items: LiveData<List<Item>> = _items

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getItems()
                _items.value = result
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addItem(item: Item) {
        viewModelScope.launch {
            repository.insertItem(item)
            loadItems()  // Refresh list
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItem(item)
            loadItems()
        }
    }
}
```

## ViewModel Factory (khi cần inject dependencies)

```kotlin
class ExampleViewModelFactory(
    private val repository: ExampleRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExampleViewModel::class.java)) {
            return ExampleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Sử dụng trong Activity/Fragment
private val viewModel: ExampleViewModel by viewModels {
    ExampleViewModelFactory(repository)
}
```

## Sử dụng trong Activity/Fragment

```kotlin
class ExampleFragment : Fragment() {

    private val viewModel: ExampleViewModel by viewModels()
    // Hoặc share với Activity
    // private val viewModel: SharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Observe LiveData
        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
```

## StateFlow vs LiveData (Modern approach)

```kotlin
class ModernViewModel : ViewModel() {

    // StateFlow - luôn có giá trị
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // SharedFlow - events (one-time)
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun updateState(newData: String) {
        _uiState.update { currentState ->
            currentState.copy(data = newData)
        }
    }

    fun sendEvent(event: UiEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}

// Sử dụng trong Fragment
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            // Update UI
        }
    }
}
```

## UI State Pattern

```kotlin
data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)

sealed class TransactionEvent {
    data class ShowError(val message: String) : TransactionEvent()
    data class NavigateToDetail(val id: Long) : TransactionEvent()
    object TransactionAdded : TransactionEvent()
}
```

## Dependencies cần thiết

```groovy
// build.gradle (app)
dependencies {
    // ViewModel
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
    // LiveData
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.7.0"
    // Fragment KTX (for by viewModels())
    implementation "androidx.fragment:fragment-ktx:1.6.2"
    // Activity KTX
    implementation "androidx.activity:activity-ktx:1.8.2"
}
```

## Best Practices

1. **Không giữ reference đến View/Context** trong ViewModel
2. **Sử dụng viewModelScope** cho coroutines
3. **Expose immutable LiveData/StateFlow** ra ngoài
4. **Tách UI State và Events**
5. **ViewModel không nên biết về Android framework** (trừ Application context nếu cần)
6. **Unit test ViewModel** một cách độc lập
