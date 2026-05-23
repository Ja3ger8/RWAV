package com.birdsightings.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BirdSighting::class], version = 1, exportSchema = false)
abstract class BirdSightingDatabase : RoomDatabase() {

    abstract fun birdSightingDao(): BirdSightingDao

    companion object {
        @Volatile
        private var INSTANCE: BirdSightingDatabase? = null

        fun getDatabase(context: Context): BirdSightingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BirdSightingDatabase::class.java,
                    "bird_sightings_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
