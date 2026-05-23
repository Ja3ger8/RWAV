package com.birdsightings.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BirdSightingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sightings: List<BirdSighting>)

    @Query("SELECT * FROM bird_sightings ORDER BY date DESC")
    fun getAllSightings(): LiveData<List<BirdSighting>>

    @Query("SELECT * FROM bird_sightings ORDER BY date DESC")
    suspend fun getAllSightingsOnce(): List<BirdSighting>

    @Query("""
        SELECT * FROM bird_sightings 
        WHERE (:birdName = '' OR birdName LIKE '%' || :birdName || '%')
        AND (:date = '' OR date LIKE '%' || :date || '%')
        ORDER BY date DESC
    """)
    fun filterSightings(birdName: String, date: String): LiveData<List<BirdSighting>>

    @Query("SELECT DISTINCT birdName FROM bird_sightings ORDER BY birdName")
    fun getDistinctBirdNames(): LiveData<List<String>>

    @Query("DELETE FROM bird_sightings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bird_sightings")
    suspend fun getCount(): Int
}
