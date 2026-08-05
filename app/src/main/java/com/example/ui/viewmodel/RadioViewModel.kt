package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.StationEntity
import com.example.data.repository.RadioRepository
import com.example.player.PlaybackState
import com.example.player.RadioPlaybackManager
import com.example.player.RadioRecordingManager
import com.example.player.RecordingItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RadioViewModel(
    private val repository: RadioRepository,
    val playerManager: RadioPlaybackManager
) : ViewModel() {

    // Recording Manager
    val recordingManager = RadioRecordingManager()

    private val _recordingsList = MutableStateFlow<List<RecordingItem>>(emptyList())
    val recordingsList: StateFlow<List<RecordingItem>> = _recordingsList.asStateFlow()

    // Db collections
    val favoriteStations: StateFlow<List<StationEntity>> = repository.favoriteStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStations: StateFlow<List<StationEntity>> = repository.recentStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingStations: StateFlow<List<StationEntity>> = repository.trendingStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search fields
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _countryQuery = MutableStateFlow("")
    val countryQuery: StateFlow<String> = _countryQuery.asStateFlow()

    private val _tagQuery = MutableStateFlow("")
    val tagQuery: StateFlow<String> = _tagQuery.asStateFlow()

    // Status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchResults = MutableStateFlow<List<StationEntity>>(emptyList())
    val searchResults: StateFlow<List<StationEntity>> = _searchResults.asStateFlow()

    private val _popularStations = MutableStateFlow<List<StationEntity>>(emptyList())
    val popularStations: StateFlow<List<StationEntity>> = _popularStations.asStateFlow()

    // Playback state link
    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val volume: StateFlow<Float> = playerManager.volume

    // Live metadata track title flow
    private val _currentTrackTitle = MutableStateFlow<String?>(null)
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle.asStateFlow()

    private var metadataJob: kotlinx.coroutines.Job? = null

    fun setVolume(vol: Float) {
        playerManager.setVolume(vol)
    }

    init {
        loadPopularStations()
        observePlaybackStateForMetadata()
    }

    private fun observePlaybackStateForMetadata() {
        viewModelScope.launch {
            playbackState.collectLatest { state ->
                metadataJob?.cancel() // Cancel previous polling job if any
                _currentTrackTitle.value = null // Reset title on state change
                
                if (state is PlaybackState.Playing) {
                    val streamUrl = state.station.urlResolved.ifBlank { state.station.url }
                    if (streamUrl.isNotBlank()) {
                        metadataJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            while (true) {
                                try {
                                    val title = com.example.data.api.IcyMetadataFetcher.fetchIcyMetadata(streamUrl)
                                    if (title != null) {
                                        _currentTrackTitle.value = title
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                // Poll every 12 seconds
                                kotlinx.coroutines.delay(12000)
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadPopularStations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apiList = repository.getPopularStations()
                val favoritesList = favoriteStations.value.map { it.stationuuid }.toSet()
                _popularStations.value = apiList.map { station ->
                    val isFav = favoritesList.contains(station.stationuuid)
                    val dbEntry = repository.getStationById(station.stationuuid)
                    station.toEntity(isFavorite = isFav, lastPlayedAt = dbEntry?.lastPlayedAt)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCountryQuery(country: String) {
        _countryQuery.value = country
    }

    fun setTagQuery(tag: String) {
        _tagQuery.value = tag
    }

    fun search() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apiList = repository.searchStations(
                    name = _searchQuery.value,
                    country = _countryQuery.value,
                    tag = _tagQuery.value
                )
                val favoritesList = favoriteStations.value.map { it.stationuuid }.toSet()
                _searchResults.value = apiList.map { station ->
                    val isFav = favoritesList.contains(station.stationuuid)
                    val dbEntry = repository.getStationById(station.stationuuid)
                    station.toEntity(isFavorite = isFav, lastPlayedAt = dbEntry?.lastPlayedAt)
                }
                    .distinctBy { it.stationuuid }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun quickSearchByCountry(country: String) {
        _countryQuery.value = country
        _searchQuery.value = ""
        _tagQuery.value = ""
        search()
    }

    fun quickSearchByTag(tag: String) {
        _tagQuery.value = tag
        _searchQuery.value = ""
        _countryQuery.value = ""
        search()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _countryQuery.value = ""
        _tagQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun playStation(station: StationEntity) {
        viewModelScope.launch {
            recordingManager.stopPlayingRecording()
            repository.markAsPlayed(station)
            playerManager.play(station)
        }
    }

    fun startRecording(context: android.content.Context) {
        val current = playerManager.currentStation ?: return
        recordingManager.startRecording(context, current)
        refreshRecordings(context)
    }

    fun stopRecording(context: android.content.Context) {
        recordingManager.stopRecording()
        refreshRecordings(context)
    }

    fun refreshRecordings(context: android.content.Context) {
        _recordingsList.value = recordingManager.getRecordings(context)
    }

    fun playRecording(item: RecordingItem) {
        playerManager.stop()
        recordingManager.playRecording(java.io.File(item.filePath))
    }

    fun deleteRecording(context: android.content.Context, item: RecordingItem) {
        recordingManager.deleteRecording(java.io.File(item.filePath))
        refreshRecordings(context)
    }

    fun seekNext() {
        val current = playerManager.currentStation ?: return
        val list = getActiveStationsList()
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.stationuuid == current.stationuuid }
        val nextStation = if (index == -1 || index == list.size - 1) {
            list.first()
        } else {
            list[index + 1]
        }
        playStation(nextStation)
    }

    fun seekPrev() {
        val current = playerManager.currentStation ?: return
        val list = getActiveStationsList()
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.stationuuid == current.stationuuid }
        val prevStation = if (index == -1 || index == 0) {
            list.last()
        } else {
            list[index - 1]
        }
        playStation(prevStation)
    }

    fun getActiveStationsList(): List<StationEntity> {
        if (_searchResults.value.isNotEmpty()) return _searchResults.value
        if (_popularStations.value.isNotEmpty()) return _popularStations.value
        if (favoriteStations.value.isNotEmpty()) return favoriteStations.value
        return recentStations.value
    }

    fun toggleFavorite(station: StationEntity) {
        viewModelScope.launch {
            // Toggle in Repository/Db
            repository.toggleFavorite(station)
            
            // Sync current lists with updated favorite status
            val favoritesList = repository.favoriteStations.first().map { it.stationuuid }.toSet()
            
            _popularStations.value = _popularStations.value.map {
                if (it.stationuuid == station.stationuuid) {
                    it.copy(isFavorite = !it.isFavorite)
                } else {
                    it.copy(isFavorite = favoritesList.contains(it.stationuuid))
                }
            }
            _searchResults.value = _searchResults.value.map {
                if (it.stationuuid == station.stationuuid) {
                    it.copy(isFavorite = !it.isFavorite)
                } else {
                    it.copy(isFavorite = favoritesList.contains(it.stationuuid))
                }
            }
        }
    }

    suspend fun exportBackupJson(): String {
        val stations = repository.getAllStations()
        return repository.exportStationsToJson(stations)
    }

    suspend fun importBackupJson(json: String): Boolean {
        val stations = repository.importStationsFromJson(json) ?: return false
        repository.insertStations(stations)
        loadPopularStations()
        return true
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
        recordingManager.release()
    }
}
