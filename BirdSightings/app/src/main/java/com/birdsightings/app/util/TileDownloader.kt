package com.birdsightings.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.PI
import kotlin.math.tan

/**
 * Downloads OpenStreetMap tile images for a given bounding box and zoom range,
 * storing them in OSMDroid's standard cache directory so MapView uses them offline.
 *
 * Tile URL pattern: https://tile.openstreetmap.org/{z}/{x}/{y}.png
 * OSMDroid cache path: <cacheDir>/tiles/Mapnik/{z}/{x}/{y}.tile
 */
object TileDownloader {

    private const val TILE_URL = "https://tile.openstreetmap.org"
    // OSM tile usage policy: max 2 parallel downloads, identify with a real user agent
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000

    /**
     * Returns a rough estimate of how many tiles cover [bbox] from [minZoom] to [maxZoom].
     */
    fun estimateTileCount(bbox: BoundingBox, minZoom: Int, maxZoom: Int): Long {
        var count = 0L
        for (zoom in minZoom..maxZoom) {
            val (x1, y1) = latLonToTile(bbox.latNorth, bbox.lonWest, zoom)
            val (x2, y2) = latLonToTile(bbox.latSouth, bbox.lonEast, zoom)
            count += (Math.abs(x2 - x1) + 1L) * (Math.abs(y2 - y1) + 1L)
        }
        return count
    }

    /**
     * Downloads all tiles for [bbox] between [minZoom] and [maxZoom].
     * Already-cached tiles are skipped. Runs on IO dispatcher.
     *
     * Calls [onProgress] periodically from the main thread.
     * Calls [onComplete] or [onError] when finished.
     */
    suspend fun downloadTiles(
        context: Context,
        bbox: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
        onProgress: suspend (done: Int, total: Int) -> Unit,
        onComplete: suspend (downloaded: Int, skipped: Int, failed: Int) -> Unit,
        onError: suspend (message: String) -> Unit
    ) {
        // Build full tile list first so we can show accurate progress
        data class Tile(val z: Int, val x: Int, val y: Int)
        val tiles = mutableListOf<Tile>()

        for (zoom in minZoom..maxZoom) {
            val (x1, y1) = latLonToTile(bbox.latNorth, bbox.lonWest, zoom)
            val (x2, y2) = latLonToTile(bbox.latSouth, bbox.lonEast, zoom)
            val xMin = minOf(x1, x2)
            val xMax = maxOf(x1, x2)
            val yMin = minOf(y1, y2)
            val yMax = maxOf(y1, y2)
            for (x in xMin..xMax) for (y in yMin..yMax) tiles.add(Tile(zoom, x, y))
        }

        val total = tiles.size
        var downloaded = 0
        var skipped = 0
        var failed = 0

        // OSMDroid cache directory (set by MapConfig.init)
        val tileCache = File(context.filesDir, "osmdroid_tiles/tiles/Mapnik")

        withContext(Dispatchers.IO) {
            for ((index, tile) in tiles.withIndex()) {
                if (!isActive) break  // cancelled

                val cacheFile = File(tileCache, "${tile.z}/${tile.x}/${tile.y}.tile")

                if (cacheFile.exists() && cacheFile.length() > 0) {
                    skipped++
                } else {
                    try {
                        cacheFile.parentFile?.mkdirs()
                        val url = URL("$TILE_URL/${tile.z}/${tile.x}/${tile.y}.png")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = CONNECT_TIMEOUT_MS
                        conn.readTimeout = READ_TIMEOUT_MS
                        // OSM requires a descriptive User-Agent
                        conn.setRequestProperty("User-Agent", "BirdSightingsApp/1.0 Android")
                        conn.connect()

                        if (conn.responseCode == 200) {
                            conn.inputStream.use { input ->
                                cacheFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            downloaded++
                        } else {
                            failed++
                        }
                        conn.disconnect()

                        // Respect OSM's tile usage policy — small delay between requests
                        Thread.sleep(50)

                    } catch (e: Exception) {
                        failed++
                    }
                }

                // Report progress every 10 tiles or at end
                if (index % 10 == 0 || index == total - 1) {
                    val done = downloaded + skipped + failed
                    withContext(Dispatchers.Main) { onProgress(done, total) }
                }
            }

            withContext(Dispatchers.Main) { onComplete(downloaded, skipped, failed) }
        }
    }

    // ── Coordinate conversion ────────────────────────────────────────────────

    /** Converts a lat/lon to OSM tile x/y at the given zoom level. */
    private fun latLonToTile(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val n = 1 shl zoom  // 2^zoom
        val x = floor((lon + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat)
        val y = floor((1.0 - ln(tan(latRad) + 1.0 / Math.cos(latRad)) / PI) / 2.0 * n).toInt()
        return Pair(x.coerceIn(0, n - 1), y.coerceIn(0, n - 1))
    }
}
