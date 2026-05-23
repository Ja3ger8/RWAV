package com.birdsightings.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.birdsightings.app.databinding.FragmentOfflineMapBinding
import com.birdsightings.app.util.MapConfig
import com.birdsightings.app.util.TileDownloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Rectangle

class OfflineMapFragment : Fragment() {

    private var _binding: FragmentOfflineMapBinding? = null
    private val binding get() = _binding!!

    // The two corners the user taps to define the download region
    private var corner1: GeoPoint? = null
    private var corner2: GeoPoint? = null
    private var selectionOverlay: Rectangle? = null

    private var downloadJob: Job? = null
    private var maxZoom: Int = 14  // default max zoom to download

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        MapConfig.init(requireContext())
        _binding = FragmentOfflineMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Map setup ────────────────────────────────────────────────────────
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(-25.0, 133.0))
        }

        // ── Zoom level slider ────────────────────────────────────────────────
        binding.zoomSeekBar.apply {
            min = 10
            max = 16
            progress = 14
        }
        updateZoomLabel(14)

        binding.zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                maxZoom = progress
                updateZoomLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // ── Tap to set bounding box corners ──────────────────────────────────
        binding.mapView.setOnClickListener { /* handled by overlay below */ }

        binding.mapView.overlayManager.addAll(
            mutableListOf(
                object : org.osmdroid.views.overlay.Overlay() {
                    override fun onSingleTapConfirmed(
                        e: android.view.MotionEvent,
                        mapView: org.osmdroid.views.MapView
                    ): Boolean {
                        val proj = mapView.projection
                        val tapped = proj.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                        if (corner1 == null || (corner1 != null && corner2 != null)) {
                            // Start a new selection
                            corner1 = tapped
                            corner2 = null
                            removeSelectionOverlay()
                            setStatus("Tap a second point to complete the selection area.")
                        } else {
                            // Complete the selection
                            corner2 = tapped
                            drawSelectionBox()
                            val bbox = buildBoundingBox()
                            val estimate = TileDownloader.estimateTileCount(bbox, 5, maxZoom)
                            setStatus("Area selected. ~$estimate tiles to download (zoom 5–$maxZoom).\nTap Download to proceed.")
                            binding.downloadButton.isEnabled = true
                        }
                        return true
                    }
                }
            )
        )

        // ── Buttons ──────────────────────────────────────────────────────────
        binding.clearSelectionButton.setOnClickListener {
            corner1 = null
            corner2 = null
            removeSelectionOverlay()
            binding.downloadButton.isEnabled = false
            setStatus("Tap two points on the map to select a download area.")
        }

        binding.downloadButton.setOnClickListener {
            val bbox = buildBoundingBox() ?: return@setOnClickListener
            startDownload(bbox)
        }

        binding.cancelButton.setOnClickListener {
            downloadJob?.cancel()
            setDownloadingUi(false)
            setStatus("Download cancelled.")
        }

        // ── Cache info ───────────────────────────────────────────────────────
        binding.clearCacheButton.setOnClickListener {
            MapConfig.clearCache(requireContext())
            updateCacheSize()
            setStatus("Tile cache cleared.")
        }

        updateCacheSize()
        setStatus("Tap two points on the map to select a download area.")
        binding.downloadButton.isEnabled = false
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun updateZoomLabel(zoom: Int) {
        // Rough scale descriptions
        val desc = when {
            zoom <= 10 -> "country"
            zoom <= 12 -> "region"
            zoom <= 14 -> "city/town"
            zoom <= 15 -> "suburb"
            else -> "street level"
        }
        binding.zoomLabel.text = "Max zoom: $zoom ($desc)"
    }

    private fun drawSelectionBox() {
        val c1 = corner1 ?: return
        val c2 = corner2 ?: return
        removeSelectionOverlay()

        val rect = Rectangle().apply {
            points = listOf(
                GeoPoint(c1.latitude, c1.longitude),
                GeoPoint(c1.latitude, c2.longitude),
                GeoPoint(c2.latitude, c2.longitude),
                GeoPoint(c2.latitude, c1.longitude)
            )
            fillPaint.apply {
                color = android.graphics.Color.argb(40, 33, 150, 243)
            }
            outlinePaint.apply {
                color = android.graphics.Color.argb(200, 33, 150, 243)
                strokeWidth = 4f
            }
        }
        selectionOverlay = rect
        binding.mapView.overlays.add(rect)
        binding.mapView.invalidate()
    }

    private fun removeSelectionOverlay() {
        selectionOverlay?.let { binding.mapView.overlays.remove(it) }
        selectionOverlay = null
        binding.mapView.invalidate()
    }

    private fun buildBoundingBox(): BoundingBox? {
        val c1 = corner1 ?: return null
        val c2 = corner2 ?: return null
        return BoundingBox(
            maxOf(c1.latitude, c2.latitude),   // north
            maxOf(c1.longitude, c2.longitude),  // east
            minOf(c1.latitude, c2.latitude),    // south
            minOf(c1.longitude, c2.longitude)   // west
        )
    }

    private fun startDownload(bbox: BoundingBox) {
        setDownloadingUi(true)
        binding.progressBar.progress = 0
        binding.progressText.text = "Starting…"

        downloadJob = viewLifecycleOwner.lifecycleScope.launch {
            TileDownloader.downloadTiles(
                context = requireContext(),
                bbox = bbox,
                minZoom = 5,
                maxZoom = maxZoom,
                onProgress = { done, total ->
                    val pct = if (total > 0) (done * 100 / total) else 0
                    binding.progressBar.progress = pct
                    binding.progressText.text = "$done / $total tiles ($pct%)"
                },
                onComplete = { downloaded, skipped, failed ->
                    setDownloadingUi(false)
                    updateCacheSize()
                    val cacheStr = formatBytes(MapConfig.cacheSizeBytes(requireContext()))
                    setStatus("✓ Done! $downloaded downloaded, $skipped already cached, $failed failed.\nCache size: $cacheStr")
                },
                onError = { msg ->
                    setDownloadingUi(false)
                    setStatus("✗ Download error: $msg")
                }
            )
        }
    }

    private fun setDownloadingUi(downloading: Boolean) {
        binding.downloadButton.isEnabled = !downloading
        binding.cancelButton.visibility = if (downloading) View.VISIBLE else View.GONE
        binding.progressBar.visibility = if (downloading) View.VISIBLE else View.GONE
        binding.progressText.visibility = if (downloading) View.VISIBLE else View.GONE
        binding.zoomSeekBar.isEnabled = !downloading
    }

    private fun setStatus(msg: String) {
        binding.statusText.text = msg
    }

    private fun updateCacheSize() {
        val bytes = MapConfig.cacheSizeBytes(requireContext())
        binding.cacheSizeText.text = "Cache: ${formatBytes(bytes)}"
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        updateCacheSize()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        downloadJob?.cancel()
        _binding = null
    }
}
