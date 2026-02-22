package com.nhattien.expensemanager.data.source

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.entity.WalletEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val TAG = "FirestoreDataSource"

    private val userId: String?
        get() = auth.currentUser?.uid

    // --- TRANSACTIONS (enriched with category info) ---
    suspend fun saveTransaction(
        transaction: TransactionEntity,
        categoryName: String = "Khác",
        categoryIcon: String = "💰"
    ) {
        val uid = userId ?: return
        try {
            val data = hashMapOf(
                "id" to transaction.id,
                "amount" to transaction.amount,
                "categoryId" to transaction.categoryId,
                "categoryName" to categoryName,
                "categoryIcon" to categoryIcon,
                "type" to transaction.type.name,
                "note" to transaction.note,
                "date" to transaction.date,
                "paymentMethod" to transaction.paymentMethod,
                "isRecurring" to transaction.isRecurring,
                "walletId" to transaction.walletId,
                "debtId" to transaction.debtId,
                "targetWalletId" to transaction.targetWalletId
            )

            val docRef = db.collection("users").document(uid)
                .collection("transactions").document(transaction.id.toString())

            docRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "Transaction saved: ${transaction.id} ($categoryName)")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction", e)
        }
    }

    suspend fun deleteTransaction(id: Long) {
        val uid = userId ?: return
        try {
            db.collection("users").document(uid)
                .collection("transactions").document(id.toString())
                .delete()
                .await()
            Log.d(TAG, "Transaction deleted: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting transaction", e)
        }
    }

    // --- CATEGORIES ---
    suspend fun saveCategory(category: CategoryEntity) {
        val uid = userId ?: return
        try {
            val data = hashMapOf(
                "id" to category.id,
                "name" to category.name,
                "icon" to category.icon,
                "type" to category.type.name,
                "isDefault" to category.isDefault
            )

            db.collection("users").document(uid)
                .collection("categories").document(category.id.toString())
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Category saved: ${category.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving category", e)
        }
    }

    suspend fun deleteCategory(id: Long) {
        val uid = userId ?: return
        try {
            db.collection("users").document(uid)
                .collection("categories").document(id.toString())
                .delete()
                .await()
            Log.d(TAG, "Category deleted: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting category", e)
        }
    }

    // --- WALLETS ---
    suspend fun saveWallet(wallet: WalletEntity) {
        val uid = userId ?: return
        try {
            val data = hashMapOf(
                "id" to wallet.id,
                "name" to wallet.name,
                "initialBalance" to wallet.initialBalance,
                "icon" to wallet.icon,
                "color" to wallet.color,
                "isArchived" to wallet.isArchived
            )

            db.collection("users").document(uid)
                .collection("wallets").document(wallet.id.toString())
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Wallet saved: ${wallet.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving wallet", e)
        }
    }

    suspend fun deleteWallet(id: Long) {
        val uid = userId ?: return
        try {
            db.collection("users").document(uid)
                .collection("wallets").document(id.toString())
                .delete()
                .await()
            Log.d(TAG, "Wallet deleted: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting wallet", e)
        }
    }

    // --- BATCH UPLOAD (INITIAL SYNC) ---
    data class EnrichedTransaction(
        val transaction: TransactionEntity,
        val categoryName: String,
        val categoryIcon: String
    )

    suspend fun syncAll(
        enrichedTransactions: List<EnrichedTransaction>,
        categories: List<CategoryEntity>,
        wallets: List<WalletEntity>
    ) {
        val uid = userId ?: return

        // Firestore batch has 500 ops limit, split if needed
        val allOps = mutableListOf<suspend () -> Unit>()

        enrichedTransactions.forEach { et ->
            allOps.add {
                saveTransaction(et.transaction, et.categoryName, et.categoryIcon)
            }
        }

        categories.forEach { cat ->
            allOps.add { saveCategory(cat) }
        }

        wallets.forEach { wallet ->
            allOps.add { saveWallet(wallet) }
        }

        try {
            allOps.forEach { it() }
            Log.d(TAG, "Full sync complete: ${enrichedTransactions.size} transactions, ${categories.size} categories, ${wallets.size} wallets")
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
        }
    }

    // --- BIDIRECTIONAL SYNC: Listen for Cloud changes ---
    fun listenForTransactionChanges(
        onUpsert: (TransactionEntity, String, String) -> Unit, // transaction, categoryName, categoryIcon
        onDelete: (String) -> Unit // firestoreDocId
    ): com.google.firebase.firestore.ListenerRegistration? {
        val uid = userId ?: return null
        
        return db.collection("users").document(uid)
            .collection("transactions")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed", error)
                    return@addSnapshotListener
                }
                
                snapshots?.documentChanges?.forEach { change ->
                    val docData = change.document.data
                    val docId = change.document.id
                    
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            try {
                                val amount = (docData["amount"] as? Number)?.toDouble() ?: 0.0
                                val categoryId = (docData["categoryId"] as? Number)?.toLong() ?: 1L
                                val typeStr = when (val t = docData["type"]) {
                                    is String -> t
                                    is Map<*, *> -> (t["name"] as? String) ?: "EXPENSE"
                                    else -> "EXPENSE"
                                }
                                val type = try {
                                    com.nhattien.expensemanager.domain.TransactionType.valueOf(typeStr)
                                } catch (_: Exception) {
                                    com.nhattien.expensemanager.domain.TransactionType.EXPENSE
                                }
                                val note = docData["note"] as? String ?: ""
                                val date = (docData["date"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                val walletId = (docData["walletId"] as? Number)?.toLong() ?: 1L
                                val paymentMethod = docData["paymentMethod"] as? String ?: "CASH"
                                val isRecurring = docData["isRecurring"] as? Boolean ?: false
                                val debtId = (docData["debtId"] as? Number)?.toLong()
                                val targetWalletId = (docData["targetWalletId"] as? Number)?.toLong()
                                val categoryName = docData["categoryName"] as? String ?: "Khác"
                                val categoryIcon = docData["categoryIcon"] as? String ?: "💰"
                                
                                // Try to parse document ID as Long (for Room), else use hash
                                val roomId = docId.toLongOrNull() ?: docId.hashCode().toLong()
                                
                                val transaction = TransactionEntity(
                                    id = roomId,
                                    amount = amount,
                                    categoryId = categoryId,
                                    type = type,
                                    note = note,
                                    date = date,
                                    walletId = walletId,
                                    paymentMethod = paymentMethod,
                                    isRecurring = isRecurring,
                                    debtId = debtId,
                                    targetWalletId = targetWalletId
                                )
                                
                                onUpsert(transaction, categoryName, categoryIcon)
                                Log.d(TAG, "Cloud -> Local: Upsert transaction $docId")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing cloud transaction $docId", e)
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            onDelete(docId)
                            Log.d(TAG, "Cloud -> Local: Delete transaction $docId")
                        }
                    }
                }
            }
    }
}
