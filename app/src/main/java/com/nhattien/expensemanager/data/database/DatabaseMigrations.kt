package com.nhattien.expensemanager.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database Migrations
 * 
 * Rule: Every time you change the database schema, you MUST:
 * 1. Increase the version number in AppDatabase
 * 2. Add a new migration here (e.g., MIGRATION_4_5)
 * 3. Add the migration to AppDatabase.getInstance()
 */
object DatabaseMigrations {
    
    /**
     * Migration from version 1 to 2
     * Historical: Added categoryId to transactions (unknown exact changes)
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Historical migration - schema already in place for existing users
            // If user has version 1, they likely have destructive migration already
        }
    }
    
    /**
     * Migration from version 2 to 3
     * Historical: Added debts table and/or categories table
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Historical migration
        }
    }
    
    /**
     * Migration from version 3 to 4
     * Historical: Added debtId to transactions for linking
     */
    /**
     * Migration from version 3 to 4
     * Historical: Added debtId to transactions for linking
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Historical migration
        }
    }

    /**
     * Migration from version 6 to 7
     * Feature: Multi-Wallet Support
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create Wallets table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `wallets` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `initialBalance` REAL NOT NULL, 
                    `icon` TEXT NOT NULL, 
                    `color` INTEGER NOT NULL, 
                    `isArchived` INTEGER NOT NULL
                )
            """)

            // 2. Insert Default "Cash" Wallet (ID = 1)
            // Color: Blue (-16776961 or 0xFF0000FF)
            db.execSQL("INSERT INTO wallets (id, name, initialBalance, icon, color, isArchived) VALUES (1, 'Tiền mặt', 0.0, 'W', -16776961, 0)")

            // 3. Add walletId to transactions (Default 1)
            // SQLite limitation: Cannot add column with Foreign Key constraint easily via ALTER TABLE in some versions
            // But we can add the column first. Room verification might fail if we don't recreate table, 
            // but let's try ALTER TABLE first as it's safer for data.
            // Actually, Room expects exact schema match. 
            // If we use ALTER TABLE, we must ensure the schema matches exactly what Room expects.
            // Room creates indices for Foreign Keys usually.

            // Strategy: Create new table -> Copy -> Drop old -> Rename
            
            // A. Create new table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `transactions_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `categoryId` INTEGER NOT NULL, 
                    `paymentMethod` TEXT NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `note` TEXT NOT NULL, 
                    `date` INTEGER NOT NULL, 
                    `isRecurring` INTEGER NOT NULL, 
                    `debtId` INTEGER, 
                    `walletId` INTEGER NOT NULL DEFAULT 1, 
                    `targetWalletId` INTEGER,
                    FOREIGN KEY(`debtId`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)

            // B. Copy Data
            db.execSQL("""
                INSERT INTO transactions_new (id, amount, categoryId, paymentMethod, type, note, date, isRecurring, debtId, walletId, targetWalletId)
                SELECT id, amount, categoryId, paymentMethod, type, note, date, isRecurring, debtId, 1, NULL FROM transactions
            """)

            // C. Drop Old
            db.execSQL("DROP TABLE transactions")

            // D. Rename
            db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
            
            // E. Create Indices
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_walletId` ON `transactions` (`walletId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_debtId` ON `transactions` (`debtId`)")
        }
    }
    
    /**
     * Migration from version 7 to 8
     * Feature: Planned Expenses (Chi tiêu dự tính)
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `planned_expenses` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    `walletId` INTEGER NOT NULL DEFAULT 1,
                    `note` TEXT NOT NULL,
                    `dueDate` INTEGER NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    `transactionId` INTEGER,
                    `groupName` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_expenses_categoryId` ON `planned_expenses` (`categoryId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_expenses_walletId` ON `planned_expenses` (`walletId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_expenses_groupName` ON `planned_expenses` (`groupName`)")
        }
    }


    /**
     * Get all migrations for AppDatabase
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_6_7,
            MIGRATION_7_8
        )
    }
}
