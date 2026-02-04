---
name: Unit Testing Android
description: Viết unit tests với JUnit, MockK, và Turbine cho Flows.
---

# Unit Testing Android

## Dependencies

```groovy
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.9")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("androidx.arch.core:core-testing:2.2.0")
```

## Test ViewModel

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: MainViewModel
    private val repository: ExpenseRepository = mockk()

    @Before
    fun setup() {
        coEvery { repository.allTransactions } returns flowOf(emptyList())
        viewModel = MainViewModel(repository)
    }

    @Test
    fun `when load transactions, should emit list`() = runTest {
        val transactions = listOf(
            TransactionEntity(id = 1, amount = 100.0, ...)
        )
        coEvery { repository.allTransactions } returns flowOf(transactions)
        
        viewModel.transactions.test {
            assertEquals(transactions, awaitItem())
        }
    }

    @Test
    fun `when delete transaction, should call repository`() = runTest {
        val transaction = TransactionEntity(id = 1, ...)
        coEvery { repository.delete(any()) } just Runs
        
        viewModel.deleteTransaction(transaction)
        
        coVerify { repository.delete(transaction) }
    }
}
```

## MainDispatcherRule

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }
    
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

## Test Repository

```kotlin
class ExpenseRepositoryTest {

    private lateinit var repository: ExpenseRepository
    private val dao: TransactionDao = mockk()

    @Before
    fun setup() {
        repository = ExpenseRepository(dao)
    }

    @Test
    fun `insert should call dao insert`() = runTest {
        val transaction = TransactionEntity(amount = 100.0, ...)
        coEvery { dao.insert(any()) } returns 1L
        
        val result = repository.insert(transaction)
        
        assertEquals(1L, result)
        coVerify { dao.insert(transaction) }
    }
}
```

## Test với Turbine (Flow)

```kotlin
@Test
fun `monthly stats should calculate correctly`() = runTest {
    val transactions = listOf(
        TransactionEntity(type = TransactionType.INCOME, amount = 1000.0),
        TransactionEntity(type = TransactionType.EXPENSE, amount = 500.0)
    )
    
    viewModel.monthlyStats.test {
        val (income, expense, _) = awaitItem()
        assertEquals(1000.0, income, 0.01)
        assertEquals(500.0, expense, 0.01)
    }
}
```

## MockK Cheatsheet

```kotlin
// Mock object
val mock: Repository = mockk()

// Relaxed mock (tự trả về default values)
val relaxedMock: Repository = mockk(relaxed = true)

// Stub suspend function
coEvery { mock.getData() } returns data

// Stub regular function
every { mock.calculate(any()) } returns 42

// Verify
verify { mock.doSomething() }
coVerify { mock.suspendFunction() }

// Argument capture
val slot = slot<Transaction>()
coEvery { mock.insert(capture(slot)) } returns 1L
// Sau đó: slot.captured
```
