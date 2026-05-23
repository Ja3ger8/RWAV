package com.birdsightings.app.data

import androidx.lifecycle.LiveData

class BirdRepository(private val dao: BirdSightingDao) {

    val allSightings: LiveData<List<BirdSighting>> = dao.getAllSightings()
    val distinctBirdNames: LiveData<List<String>> = dao.getDistinctBirdNames()

    suspend fun insertAll(sightings: List<BirdSighting>) {
        dao.insertAll(sightings)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun getCount(): Int {
        return dao.getCount()
    }

    fun filterSightings(birdName: String, date: String): LiveData<List<BirdSighting>> {
        return dao.filterSightings(birdName, date)
    }

    suspend fun getAllSightingsOnce(): List<BirdSighting> {
        return dao.getAllSightingsOnce()
    }
}
