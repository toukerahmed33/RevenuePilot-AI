package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.HoldingDao
import com.example.data.local.dao.InspectionNoteDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.HoldingRecordEntity
import com.example.data.local.entity.InspectionNoteEntity
import com.example.data.local.entity.TaskEntity

@Database(
    entities = [
        HoldingRecordEntity::class,
        TaskEntity::class,
        InspectionNoteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun holdingDao(): HoldingDao
    abstract fun taskDao(): TaskDao
    abstract fun inspectionNoteDao(): InspectionNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inspection_notes ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inspection_notes ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE inspection_notes ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE inspection_notes ADD COLUMN locationLabel TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "revenue_pilot_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
