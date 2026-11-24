package com.example.data.repository

import com.example.data.local.dao.InspectionNoteDao
import com.example.data.local.entity.InspectionNoteEntity
import com.example.domain.sample.SampleDataGenerator
import kotlinx.coroutines.flow.Flow

class InspectionRepository(private val inspectionNoteDao: InspectionNoteDao) {

    val allNotes: Flow<List<InspectionNoteEntity>> = inspectionNoteDao.getAllInspectionNotes()
    val draftNotes: Flow<List<InspectionNoteEntity>> = inspectionNoteDao.getDraftNotes()

    suspend fun getNoteById(id: Long): InspectionNoteEntity? = inspectionNoteDao.getNoteById(id)

    suspend fun insertNote(note: InspectionNoteEntity): Long = inspectionNoteDao.insert(note)

    suspend fun updateNote(note: InspectionNoteEntity) = inspectionNoteDao.update(note)

    suspend fun deleteNote(note: InspectionNoteEntity) = inspectionNoteDao.delete(note)

    suspend fun deleteNoteById(id: Long) = inspectionNoteDao.deleteById(id)

    suspend fun loadSyntheticSampleData() {
        val samples = SampleDataGenerator.getSampleInspectionNotes()
        inspectionNoteDao.deleteAll()
        inspectionNoteDao.insertAll(samples)
    }

    suspend fun clearAll() = inspectionNoteDao.deleteAll()

    suspend fun getCount(): Int = inspectionNoteDao.getCount()
}
