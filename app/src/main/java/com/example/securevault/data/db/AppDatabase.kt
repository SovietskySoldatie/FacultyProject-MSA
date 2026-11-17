package com.example.securevault.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.securevault.data.dao.AuthDao
import com.example.securevault.data.dao.NoteDao
import com.example.securevault.data.entity.AuthEntity
import com.example.securevault.data.entity.NoteEntity

/**
 * Main application database encapsulating 2 separate schemas:
 * 1) app_auth (Login credentials schema)
 * 2) notes (Notes collection schema)
 *
 * Local persistence is managed using SQLite on device.
 */
@Database(
    entities = [AuthEntity::class, NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun authDao(): AuthDao
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "secure_vault.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
