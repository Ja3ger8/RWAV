package com.birdsightings.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.birdsightings.app.databinding.FragmentMapBinding
import com.birdsightings.app.util.MapConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BirdViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialise OSMDroid config — sets cache dir and user agent
        MapConfig.init(requireContext())
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(-25.0, 133.0)) // Centre of Australia
        }

        viewModel.allSightings.observe(viewLifecycleOwner) { sightings ->
            binding.mapView.overlays.clear()

            if (sightings.isEmpty()) {
                binding.emptyMapText.visibility = View.VISIBLE
                binding.mapView.invalidate()
                return@observe
            }

            binding.emptyMapText.visibility = View.GONE
            var firstPoint: GeoPoint? = null

            sightings.forEach { sighting ->
                val point = GeoPoint(sighting.latitude, sighting.longitude)
                if (firstPoint == null) firstPoint = point

                val marker = Marker(binding.mapView).apply {
                    position = point
                    title = sighting.birdName
                    snippet = "${sighting.location} · ${sighting.date}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                binding.mapView.overlays.add(marker)
            }

            firstPoint?.let {
                binding.mapView.controller.animateTo(it)
                binding.mapView.controller.setZoom(8.0)
            }

            binding.mapView.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
