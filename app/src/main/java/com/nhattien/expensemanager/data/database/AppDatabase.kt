package com.nhattien.expensemanager.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nhattien.expensemanager.data.converter.Converters
import com.nhattien.expensemanager.data.dao.CategoryDao
import com.nhattien.expensemanager.data.dao.DebtDao
import com.nhattien.expensemanager.data.dao.NotificationDao
import com.nhattien.expensemanager.data.dao.PlannedExpenseDao
import com.nhattien.expensemanager.data.dao.TagDao
import com.nhattien.expensemanager.data.dao.TransactionDao
import com.nhattien.expensemanager.data.dao.WalletDao
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.DebtEntity
import com.nhattien.expensemanager.data.entity.NotificationEntity
import com.nhattien.expensemanager.data.entity.PlannedExpenseEntity
import com.nhattien.expensemanager.data.entity.TagEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.entity.TransactionTagCrossRef
import com.nhattien.expensemanager.data.entity.WalletEntity
import com.nhattien.expensemanager.data.entity.SearchHistoryEntity
import com.nhattien.expensemanager.data.dao.SearchHistoryDao

@Database(
    entities = [
        TransactionEntity::class,
        DebtEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        NotificationEntity::class,
        WalletEntity::class,
        PlannedExpenseEntity::class,
        SearchHistoryEntity::class
    ],
    version = 9, // Bumped for SearchHistory
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5)
        // Manual migration is used for 6 -> 7 and 7 -> 8.
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun debtDao(): DebtDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun notificationDao(): NotificationDao
    abstract fun walletDao(): WalletDao
    abstract fun plannedExpenseDao(): PlannedExpenseDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `search_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `query` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }


        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `search_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `query` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dongtien_db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_8_9, *DatabaseMigrations.getAllMigrations())
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Keep a default wallet available for all flows.
                            db.execSQL(
                                "INSERT OR IGNORE INTO wallets (id, name, initialBalance, icon, color, isArchived) " +
                                    "VALUES (1, 'Tiền mặt', 0.0, 'W', -16776961, 0)"
                            )
                            db.execSQL(
                                "UPDATE wallets SET icon = 'W' WHERE id = 1 AND (icon IS NULL OR TRIM(icon) = '')"
                            )
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
