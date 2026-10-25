package com.mohgwatch.wear.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [GlucoseReadingEntity::class], version = 2, exportSchema = false)
abstract class GlucoseDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao

    companion object {
        @Volatile private var INSTANCE: GlucoseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM readings WHERE id NOT IN (SELECT MAX(id) FROM readings GROUP BY timestamp)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_readings_timestamp ON readings(timestamp)")
            }
        }

        fun getInstance(context: Context): GlucoseDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GlucoseDatabase::class.java,
                    "mohgwatch_glucose.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
