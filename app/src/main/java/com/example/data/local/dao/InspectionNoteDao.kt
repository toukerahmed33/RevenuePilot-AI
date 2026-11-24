package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.InspectionNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionNoteDao {
    @Query("SELECT * FROM inspection_notes ORDER BY inspectionDate DESC, id DESC")
    fun getAllInspectionNotes(): Flow<List<InspectionNoteEntity>>

    @Query("SELECT * FROM inspection_notes WHERE isDraft = 1 ORDER BY updatedAt DESC")
    fun getDraftNotes(): Flow<List<InspectionNoteEntity>>

    @Query("SELECT * FROM inspection_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): InspectionNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: InspectionNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<InspectionNoteEntity>)

    @Update
    suspend fun update(note: InspectionNoteEntity)

    @Delete
    suspend fun delete(note: InspectionNoteEntity)

    @Query("DELETE FROM inspection_notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM inspection_notes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM inspection_notes")
    suspend fun getCount(): Int
}
