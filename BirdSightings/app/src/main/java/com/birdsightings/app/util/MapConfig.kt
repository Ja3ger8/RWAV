package com.birdsightings.app.util

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

object MapConfig {

    /**
     * Call this once before any MapView is created.
     * Sets the user agent and points OSMDroid at a persistent app-internal cache
     * directory so downloaded tiles survive offline sessions.
     */
    fun init(context: Context) {
        val config = Configuration.getInstance()

        // Required by OSMDroid's tile download policy
        config.userAgentValue = context.packageName

        // Store tiles in app-internal storage — no extra permissions needed on Android 10+
        // and tiles persist until the user clears app data or uses the "Clear tile cache" option
        val cacheDir = File(context.filesDir, "osmdroid_tiles")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        config.osmdroidBasePath = cacheDir
        config.osmdroidTileCache = File(cacheDir, "tiles")

        // Allow a generous cache size (512 MB) so large areas can be stored
        config.tileFileSystemCacheMaxBytes = 512L * 1024 * 1024  // 512 MB
        config.tileFileSystemCacheTrimBytes = 400L * 1024 * 1024 // trim to 400 MB when full
    }

    /**
     * Returns the current size of the tile cache in bytes.
     */
    fun cacheSizeBytes(context: Context): Long {
        val cacheDir = File(context.filesDir, "osmdroid_tiles/tiles")
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Deletes all cached tiles.
     */
    fun clearCache(context: Context) {
        File(context.filesDir, "osmdroid_tiles/tiles").deleteRecursively()
    }
}
