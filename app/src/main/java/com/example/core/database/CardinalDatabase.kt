package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.dao.EventDao
import com.example.core.database.dao.MissionDao
import com.example.core.database.dao.StepDao
import com.example.core.database.entity.ExecutionEventEntity
import com.example.core.database.entity.MissionEntity
import com.example.core.database.entity.StepEntity

@Database(
    entities = [MissionEntity::class, StepEntity::class, ExecutionEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CardinalDatabase : RoomDatabase() {

    abstract fun missionDao(): MissionDao
    abstract fun stepDao(): StepDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: CardinalDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructive migration template preserving existing tables and data.
            }
        }

        fun getInstance(context: Context): CardinalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CardinalDatabase::class.java,
                    "cardinal_database.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
