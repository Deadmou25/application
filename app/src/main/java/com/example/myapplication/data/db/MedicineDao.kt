package com.example.myapplication.data.db

import androidx.room.*
import com.example.myapplication.data.model.Medicine
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) для операций с таблицей `medicines`.
 *
 * Все методы изменения данных (insert/update/delete) являются suspend-функциями
 * и должны вызываться из корутины или другой suspend-функции.
 *
 * [getAll] возвращает реактивный [Flow], который автоматически переиздаёт
 * новый список каждый раз, когда данные в таблице изменяются.
 */
@Dao
interface MedicineDao {

    /**
     * Возвращает все записи, отсортированные по времени отправки (новые сверху).
     * Flow переиздаёт данные при каждом изменении таблицы.
     */
    @Query("SELECT * FROM medicines ORDER BY sentAt DESC")
    fun getAll(): Flow<List<Medicine>>

    /**
     * Вставляет новую запись. При конфликте первичного ключа заменяет существующую.
     * @return id вставленной или заменённой строки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: Medicine): Long

    /**
     * Обновляет существующую запись по первичному ключу [Medicine.id].
     */
    @Update
    suspend fun update(medicine: Medicine)

    /**
     * Удаляет конкретную запись по объекту (сравнение по первичному ключу).
     */
    @Delete
    suspend fun delete(medicine: Medicine)

    /**
     * Удаляет все записи из таблицы. Действие необратимо.
     */
    @Query("DELETE FROM medicines")
    suspend fun deleteAll()

    /**
     * Удаляет запись по идентификатору.
     * @param id Первичный ключ удаляемой записи
     */
    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Возвращает запись по идентификатору или null, если запись не найдена.
     * @param id Первичный ключ искомой записи
     */
    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getById(id: Long): Medicine?
}
