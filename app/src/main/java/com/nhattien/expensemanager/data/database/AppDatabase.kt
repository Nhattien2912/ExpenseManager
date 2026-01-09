package com.nhattien.expensemanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhattien.expensemanager.data.converter.Converters
import com.nhattien.expensemanager.data.dao.DebtDao
import com.nhattien.expensemanager.data.dao.TransactionDao
import com.nhattien.expensemanager.data.entity.DebtEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, DebtEntity::class],
    version = 3, // <--- TĂNG TỪ 2 LÊN 3
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun debtDao(): DebtDao

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
                    // Dòng này cực quan trọng: Khi đổi cấu trúc Enum, nó sẽ xóa DB cũ đi làm lại
                    // Sau này release app thật cho người dùng thì phải viết Migration, còn giờ cứ xóa cho nhanh.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}