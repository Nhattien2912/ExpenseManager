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
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Historical migration
        }
    }
    
    /**
     * Get all migrations for AppDatabase
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4
        )
    }
}
