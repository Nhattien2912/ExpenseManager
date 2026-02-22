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
import com.nhattien.expensemanager.data.entity.RecurringTransactionEntity
import com.nhattien.expensemanager.data.dao.RecurringTransactionDao

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
        SearchHistoryEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 13, // Bank Loan separation + installment tracking
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5)
        // Manual migration is used for 6 -> 7, 7 -> 8, 8 -> 9, 9 -> 10.
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
    abstract fun recurringTransactionDao(): RecurringTransactionDao

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
        
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create index for tagId on transaction_tag_cross_ref table
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tag_cross_ref_tagId` ON `transaction_tag_cross_ref` (`tagId`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `walletId` INTEGER NOT NULL,
                        `recurrencePeriod` TEXT NOT NULL,
                        `nextRunDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_walletId` ON `recurring_transactions` (`walletId`)")
            }
        }
        
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `walletId` INTEGER NOT NULL,
                        `recurrencePeriod` TEXT NOT NULL,
                        `nextRunDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_walletId` ON `recurring_transactions` (`walletId`)")
            }
        }
        
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recurring_transactions ADD COLUMN loanSource TEXT NOT NULL DEFAULT 'PERSONAL'")
                database.execSQL("ALTER TABLE recurring_transactions ADD COLUMN totalInstallments INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE recurring_transactions ADD COLUMN completedInstallments INTEGER NOT NULL DEFAULT 0")
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
                    .addMigrations(MIGRATION_5_6, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, *DatabaseMigrations.getAllMigrations())
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
