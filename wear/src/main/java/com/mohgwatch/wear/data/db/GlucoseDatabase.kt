package com.mohgwatch.wear.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GlucoseReadingEntity::class], version = 1, exportSchema = false)
abstract class GlucoseDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao

    companion object {
        @Volatile private var INSTANCE: GlucoseDatabase? = null

        fun getInstance(context: Context): GlucoseDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GlucoseDatabase::class.java,
                    "mohgwatch_glucose.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
