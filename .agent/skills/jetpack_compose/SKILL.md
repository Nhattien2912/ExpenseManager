---
name: Jetpack Compose
description: UI toolkit hiện đại cho Android, declarative UI thay thế XML layouts.
---

# Jetpack Compose

## Setup

```groovy
// build.gradle (app)
android {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

## Basic Composable

```kotlin
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = transaction.category.icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.category.name)
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = CurrencyUtils.toCurrency(transaction.amount),
                color = if (transaction.type == TransactionType.EXPENSE) 
                    Color.Red else Color.Green
            )
        }
    }
}
```

## State Management

```kotlin
@Composable
fun TransactionListScreen(viewModel: MainViewModel = viewModel()) {
    val transactions by viewModel.allTransactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    if (isLoading) {
        CircularProgressIndicator()
    } else {
        LazyColumn {
            items(transactions) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onClick = { /* navigate */ }
                )
            }
        }
    }
}
```

## Navigation Compose

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            DetailScreen(id)
        }
    }
}
```

## Interop với View (XML)

```kotlin
// Compose trong Activity/Fragment
setContent {
    MaterialTheme {
        TransactionListScreen()
    }
}

// Compose trong XML layout
<androidx.compose.ui.platform.ComposeView
    android:id="@+id/composeView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />

binding.composeView.setContent {
    MyComposable()
}
```
