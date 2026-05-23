package com.birdsightings.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.birdsightings.app.data.BirdRepository
import com.birdsightings.app.data.BirdSighting
import com.birdsightings.app.data.BirdSightingDatabase
import com.birdsightings.app.util.CsvImporter
import com.birdsightings.app.util.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class FilterState(val birdName: String = "", val date: String = "")

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val count: Int, val errors: List<String>) : ImportState()
    data class Error(val message: String) : ImportState()
}

class BirdViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BirdRepository

    val allSightings: LiveData<List<BirdSighting>>
    val distinctBirdNames: LiveData<List<String>>

    private val _filterState = MutableLiveData(FilterState())
    val filterState: LiveData<FilterState> = _filterState

    val filteredSightings: LiveData<List<BirdSighting>> = _filterState.switchMap { filter ->
        repository.filterSightings(filter.birdName, filter.date)
    }

    private val _importState = MutableLiveData<ImportState>(ImportState.Idle)
    val importState: LiveData<ImportState> = _importState

    init {
        val dao = BirdSightingDatabase.getDatabase(application).birdSightingDao()
        repository = BirdRepository(dao)
        allSightings = repository.allSightings
        distinctBirdNames = repository.distinctBirdNames
    }

    fun setFilter(birdName: String = _filterState.value?.birdName ?: "",
                  date: String = _filterState.value?.date ?: "") {
        _filterState.value = FilterState(birdName, date)
    }

    fun clearFilter() {
        _filterState.value = FilterState()
    }

    fun importCsv(uri: Uri) {
        _importState.value = ImportState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result: ImportResult = CsvImporter.importFromUri(getApplication(), uri)
            if (result.sightings.isNotEmpty()) {
                repository.insertAll(result.sightings)
                _importState.postValue(
                    ImportState.Success(result.sightings.size, result.errors)
                )
            } else {
                val errorMsg = result.errors.firstOrNull() ?: "No valid records found in file"
                _importState.postValue(ImportState.Error(errorMsg))
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
}
