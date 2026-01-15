package com.example.myapplication.data.db

import androidx.room.*
import com.example.myapplication.data.model.Medicine
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    
    @Query("SELECT * FROM medicines ORDER BY sentAt DESC")
    fun getAll(): Flow<List<Medicine>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: Medicine): Long
    
    @Update
    suspend fun update(medicine: Medicine)
    
    @Delete
    suspend fun delete(medicine: Medicine)
    
    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getById(id: Long): Medicine?
}
