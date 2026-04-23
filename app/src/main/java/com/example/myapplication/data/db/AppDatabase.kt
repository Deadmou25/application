package com.example.myapplication.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.model.Medicine

/**
 * Синглтон базы данных Room для хранения истории отправок.
 *
 * Содержит одну таблицу: [Medicine] → `medicines`.
 * Экземпляр создаётся единожды через [getDatabase] с защитой от
 * гонки потоков (double-checked locking с @Volatile).
 *
 * При изменении схемы (добавление полей, таблиц) необходимо увеличить
 * [version] и предоставить миграцию через [RoomDatabase.Builder.addMigrations].
 */
@Database(
    entities = [Medicine::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /** Предоставляет доступ к операциям с таблицей medicines */
    abstract fun medicineDao(): MedicineDao

    companion object {
        // @Volatile гарантирует, что все потоки видят актуальное значение INSTANCE
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Возвращает единственный экземпляр базы данных.
         * При первом вызове создаёт БД; при последующих возвращает кэшированный экземпляр.
         *
         * @param context Используется applicationContext во избежание утечки Activity
         * @return Синглтон [AppDatabase]
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medicine_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
