package com.birdsightings.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bird_sightings")
data class BirdSighting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val location: String,
    val birdName: String,
    val date: String,
    val latitude: Double,
    val longitude: Double
)
