package com.nhattien.expensemanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhattien.expensemanager.data.converter.Converters
import com.nhattien.expensemanager.data.dao.CategoryDao
import com.nhattien.expensemanager.data.dao.DebtDao
import com.nhattien.expensemanager.data.dao.TransactionDao
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.DebtEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity

import androidx.room.AutoMigration
import com.nhattien.expensemanager.data.dao.NotificationDao
import com.nhattien.expensemanager.data.dao.TagDao
import com.nhattien.expensemanager.data.dao.WalletDao // Added
import com.nhattien.expensemanager.data.entity.NotificationEntity
import com.nhattien.expensemanager.data.entity.TagEntity
import com.nhattien.expensemanager.data.entity.TransactionTagCrossRef
import com.nhattien.expensemanager.data.entity.WalletEntity // Added

@Database(
    entities = [
        TransactionEntity::class, 
        DebtEntity::class, 
        CategoryEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        NotificationEntity::class,
        WalletEntity::class // Added
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6)
        // Manual Migration 6->7 due to complex data migration
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun debtDao(): DebtDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun notificationDao(): NotificationDao
    abstract fun walletDao(): WalletDao // Added

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dongtien_db"
                )
                    // Use proper migrations instead of destructive migration
                    .addMigrations(*DatabaseMigrations.getAllMigrations())
                    // Fallback ONLY for development - remove in production if needed
                    // .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
