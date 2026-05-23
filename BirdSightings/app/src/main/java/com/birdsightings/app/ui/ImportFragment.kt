package com.birdsightings.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.birdsightings.app.databinding.FragmentImportBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ImportFragment : Fragment() {

    private var _binding: FragmentImportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BirdViewModel by activityViewModels()

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Show file name
                val fileName = uri.lastPathSegment ?: "selected file"
                binding.selectedFileText.text = "Importing: $fileName"
                viewModel.importCsv(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pick file button
        binding.pickFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // Accept CSV and text files
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/plain", "application/csv"))
            }
            filePickerLauncher.launch(intent)
        }

        // Clear data button
        binding.clearDataButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("This will delete all sightings from the database. Are you sure?")
                .setPositiveButton("Delete All") { _, _ ->
                    viewModel.clearAllData()
                    binding.statusText.text = "All data cleared."
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Observe import state
        viewModel.importState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ImportState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = "Select a CSV file to import bird sightings."
                }
                is ImportState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.statusText.text = "Importing..."
                    binding.pickFileButton.isEnabled = false
                }
                is ImportState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.pickFileButton.isEnabled = true
                    val msg = StringBuilder("✓ Imported ${state.count} sighting${if (state.count != 1) "s" else ""}.")
                    if (state.errors.isNotEmpty()) {
                        msg.append("\n\nWarnings (${state.errors.size}):\n")
                        msg.append(state.errors.joinToString("\n"))
                    }
                    binding.statusText.text = msg.toString()
                    viewModel.resetImportState()
                }
                is ImportState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.pickFileButton.isEnabled = true
                    binding.statusText.text = "✗ Import failed:\n${state.message}"
                    viewModel.resetImportState()
                }
            }
        }

        // CSV format hint
        binding.formatHintText.text = """
            Expected CSV columns:
            • Location
            • Bird Name
            • Date
            • X Coordinate (longitude)
            • Y Coordinate (latitude)
            
            Column names are case-insensitive.
            The first row must be a header row.
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
