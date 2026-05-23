package com.birdsightings.app.util

import android.content.Context
import android.net.Uri
import com.birdsightings.app.data.BirdSighting
import com.opencsv.CSVReaderBuilder
import java.io.InputStreamReader

data class ImportResult(
    val sightings: List<BirdSighting>,
    val errors: List<String>
)

object CsvImporter {

    /**
     * Parses a CSV file from the given URI.
     * Expected columns (case-insensitive): location, bird name, date, x coordinate, y coordinate
     */
    fun importFromUri(context: Context, uri: Uri): ImportResult {
        val sightings = mutableListOf<BirdSighting>()
        val errors = mutableListOf<String>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult(emptyList(), listOf("Could not open file"))

            val reader = CSVReaderBuilder(InputStreamReader(inputStream))
                .withSkipLines(0)
                .build()

            val allRows = reader.readAll()
            reader.close()

            if (allRows.isEmpty()) {
                return ImportResult(emptyList(), listOf("File is empty"))
            }

            // Parse header row — normalize to lowercase, trim whitespace
            val headers = allRows[0].map { it.trim().lowercase() }

            // Find column indices flexibly
            val locationIdx = findColumnIndex(headers, listOf("location"))
            val birdNameIdx = findColumnIndex(headers, listOf("bird name", "birdname", "bird"))
            val dateIdx = findColumnIndex(headers, listOf("date"))
            val xIdx = findColumnIndex(headers, listOf("x coordinate", "x", "longitude", "lon", "lng"))
            val yIdx = findColumnIndex(headers, listOf("y coordinate", "y", "latitude", "lat"))

            // Validate that required columns were found
            val missing = mutableListOf<String>()
            if (locationIdx == -1) missing.add("Location")
            if (birdNameIdx == -1) missing.add("Bird Name")
            if (dateIdx == -1) missing.add("Date")
            if (xIdx == -1) missing.add("X Coordinate")
            if (yIdx == -1) missing.add("Y Coordinate")

            if (missing.isNotEmpty()) {
                return ImportResult(
                    emptyList(),
                    listOf("Missing required columns: ${missing.joinToString(", ")}\nFound columns: ${headers.joinToString(", ")}")
                )
            }

            // Parse data rows
            for ((rowIndex, row) in allRows.drop(1).withIndex()) {
                val lineNumber = rowIndex + 2 // +2 because we skipped header and are 1-indexed

                if (row.all { it.isBlank() }) continue // skip blank rows

                try {
                    val location = row.getOrNull(locationIdx)?.trim() ?: ""
                    val birdName = row.getOrNull(birdNameIdx)?.trim() ?: ""
                    val date = row.getOrNull(dateIdx)?.trim() ?: ""
                    val xStr = row.getOrNull(xIdx)?.trim() ?: ""
                    val yStr = row.getOrNull(yIdx)?.trim() ?: ""

                    if (location.isEmpty() || birdName.isEmpty()) {
                        errors.add("Row $lineNumber: Missing location or bird name — skipped")
                        continue
                    }

                    val x = xStr.toDoubleOrNull()
                    val y = yStr.toDoubleOrNull()

                    if (x == null || y == null) {
                        errors.add("Row $lineNumber: Invalid coordinates '$xStr', '$yStr' — skipped")
                        continue
                    }

                    // X = longitude, Y = latitude (standard GIS convention)
                    sightings.add(
                        BirdSighting(
                            location = location,
                            birdName = birdName,
                            date = date,
                            longitude = x,
                            latitude = y
                        )
                    )
                } catch (e: Exception) {
                    errors.add("Row $lineNumber: ${e.message} — skipped")
                }
            }

        } catch (e: Exception) {
            errors.add("Failed to read file: ${e.message}")
        }

        return ImportResult(sightings, errors)
    }

    private fun findColumnIndex(headers: List<String>, candidates: List<String>): Int {
        for (candidate in candidates) {
            val idx = headers.indexOfFirst { it == candidate }
            if (idx != -1) return idx
        }
        return -1
    }
}
