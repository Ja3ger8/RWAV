package com.birdsightings.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.birdsightings.app.data.BirdSighting
import com.birdsightings.app.databinding.FragmentListBinding
import com.birdsightings.app.databinding.ItemSightingBinding

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BirdViewModel by activityViewModels()
    private lateinit var adapter: SightingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView
        adapter = SightingAdapter()
        binding.recyclerView.apply {
            this.adapter = this@ListFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        // Observe filtered sightings
        viewModel.filteredSightings.observe(viewLifecycleOwner) { sightings ->
            adapter.submitList(sightings)
            binding.emptyText.visibility = if (sightings.isEmpty()) View.VISIBLE else View.GONE
            binding.countText.text = "${sightings.size} sighting${if (sightings.size != 1) "s" else ""}"
        }

        // Bird name autocomplete
        viewModel.distinctBirdNames.observe(viewLifecycleOwner) { names ->
            val autoAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                names
            )
            binding.birdNameFilter.setAdapter(autoAdapter)
        }

        // Filter inputs
        binding.birdNameFilter.doAfterTextChanged { text ->
            viewModel.setFilter(birdName = text.toString())
        }

        binding.dateFilter.doAfterTextChanged { text ->
            viewModel.setFilter(date = text.toString())
        }

        binding.clearFilterButton.setOnClickListener {
            binding.birdNameFilter.text?.clear()
            binding.dateFilter.text?.clear()
            viewModel.clearFilter()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ─── RecyclerView Adapter ────────────────────────────────────────────────────

class SightingAdapter : RecyclerView.Adapter<SightingAdapter.ViewHolder>() {

    private var items: List<BirdSighting> = emptyList()

    fun submitList(list: List<BirdSighting>) {
        items = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemSightingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sighting: BirdSighting) {
            binding.birdNameText.text = sighting.birdName
            binding.locationText.text = sighting.location
            binding.dateText.text = sighting.date
            binding.coordsText.text = "%.5f, %.5f".format(sighting.latitude, sighting.longitude)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSightingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
