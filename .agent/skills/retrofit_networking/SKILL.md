---
name: Retrofit Networking
description: HTTP client cho Android với type-safe API calls.
---

# Retrofit Networking

## Dependencies

```groovy
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
```

## API Interface

```kotlin
interface ExpenseApi {
    
    @GET("transactions")
    suspend fun getTransactions(): Response<List<TransactionDto>>
    
    @GET("transactions/{id}")
    suspend fun getTransaction(@Path("id") id: Long): Response<TransactionDto>
    
    @POST("transactions")
    suspend fun createTransaction(@Body transaction: TransactionDto): Response<TransactionDto>
    
    @PUT("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body transaction: TransactionDto
    ): Response<TransactionDto>
    
    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long): Response<Unit>
    
    @GET("transactions")
    suspend fun getByDateRange(
        @Query("start") startDate: Long,
        @Query("end") endDate: Long
    ): Response<List<TransactionDto>>
}
```

## Retrofit Instance

```kotlin
object RetrofitClient {
    
    private const val BASE_URL = "https://api.example.com/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val api: ExpenseApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExpenseApi::class.java)
    }
}
```

## Repository với Network + Local

```kotlin
class ExpenseRepository(
    private val api: ExpenseApi,
    private val dao: TransactionDao
) {
    suspend fun syncTransactions(): Result<Unit> {
        return try {
            val response = api.getTransactions()
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    val entities = dtos.map { it.toEntity() }
                    dao.insertAll(entities)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Sealed Result Class

```kotlin
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val message: String, val code: Int? = null) : NetworkResult<T>()
    class Loading<T> : NetworkResult<T>()
}
```
