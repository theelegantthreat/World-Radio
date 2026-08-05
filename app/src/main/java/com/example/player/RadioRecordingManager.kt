package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.data.db.StationEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class RecordingItem(
    val fileName: String,
    val filePath: String,
    val stationName: String,
    val timestamp: Long,
    val dateFormatted: String,
    val sizeBytes: Long,
    val durationSeconds: Int = 0
)

class RadioRecordingManager {
    private val TAG = "RadioRecordingManager"

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingStation = MutableStateFlow<StationEntity?>(null)
    val recordingStation: StateFlow<StationEntity?> = _recordingStation.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var localPlayer: MediaPlayer? = null
    
    private val _isPlayingRecording = MutableStateFlow(false)
    val isPlayingRecording: StateFlow<Boolean> = _isPlayingRecording.asStateFlow()

    private val _playingFile = MutableStateFlow<File?>(null)
    val playingFile: StateFlow<File?> = _playingFile.asStateFlow()

    private val _recordingPlaybackPosition = MutableStateFlow(0)
    val recordingPlaybackPosition: StateFlow<Int> = _recordingPlaybackPosition.asStateFlow()

    private val _recordingPlaybackDuration = MutableStateFlow(0)
    val recordingPlaybackDuration: StateFlow<Int> = _recordingPlaybackDuration.asStateFlow()

    private var playbackProgressJob: Job? = null

    fun getRecordingsDir(context: Context): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun startRecording(context: Context, station: StationEntity) {
        if (_isRecording.value) {
            Log.w(TAG, "Already recording a station")
            return
        }

        _isRecording.value = true
        _recordingStation.value = station
        _recordingDuration.value = 0

        stopPlayingRecording()

        val cleanName = station.name.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val extension = if (station.codec.lowercase().contains("aac")) "aac" else "mp3"
        val fileName = "REC_${cleanName}_${timestamp}.$extension"
        val recordingsDir = getRecordingsDir(context)
        val file = File(recordingsDir, fileName)

        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                _recordingDuration.value += 1
            }
        }

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                val url = station.urlResolved.ifBlank { station.url }
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WorldRadio/1.0")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to connect to stream: ${response.code}")
                        withContext(Dispatchers.Main) {
                            stopRecording()
                        }
                        return@use
                    }

                    val body = response.body
                    if (body == null) {
                        Log.e(TAG, "Empty response body from stream")
                        withContext(Dispatchers.Main) {
                            stopRecording()
                        }
                        return@use
                    }

                    body.byteStream().use { inputStream ->
                        file.outputStream().use { outputStream ->
                            val buffer = ByteArray(32 * 1024)
                            var bytesRead: Int
                            while (isActive && _isRecording.value) {
                                bytesRead = inputStream.read(buffer)
                                if (bytesRead == -1) break
                                outputStream.write(buffer, 0, bytesRead)
                            }
                            outputStream.flush()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recording stream", e)
            } finally {
                withContext(Dispatchers.Main) {
                    if (file.exists() && file.length() == 0L) {
                        file.delete()
                    }
                    _isRecording.value = false
                    _recordingStation.value = null
                    timerJob?.cancel()
                }
            }
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false
        recordingJob?.cancel()
        timerJob?.cancel()
        _recordingStation.value = null
    }

    fun getRecordings(context: Context): List<RecordingItem> {
        val dir = getRecordingsDir(context)
        val files = dir.listFiles { _, name -> name.startsWith("REC_") } ?: return emptyList()
        
        return files.map { file ->
            val parts = file.nameWithoutExtension.split("_")
            val stationName = if (parts.size >= 2) {
                parts.subList(1, parts.size - 2).joinToString(" ").replace("_", " ")
            } else {
                "Unknown Station"
            }.ifBlank { "Recorded Broadcast" }

            val timestamp = file.lastModified()
            val dateFormatted = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

            RecordingItem(
                fileName = file.name,
                filePath = file.absolutePath,
                stationName = stationName,
                timestamp = timestamp,
                dateFormatted = dateFormatted,
                sizeBytes = file.length()
            )
        }.sortedByDescending { it.timestamp }
    }

    fun deleteRecording(file: File): Boolean {
        if (_playingFile.value?.absolutePath == file.absolutePath) {
            stopPlayingRecording()
        }
        return file.delete()
    }

    fun playRecording(file: File) {
        stopPlayingRecording()

        try {
            localPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlayingRecording.value = true
                    _playingFile.value = file
                    _recordingPlaybackDuration.value = mp.duration
                    startPlaybackProgressTracker()
                }
                setOnCompletionListener {
                    stopPlayingRecording()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error playing recording what=$what extra=$extra")
                    stopPlayingRecording()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting local playback", e)
            stopPlayingRecording()
        }
    }

    fun togglePlayPauseRecording() {
        val player = localPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlayingRecording.value = false
        } else {
            player.start()
            _isPlayingRecording.value = true
        }
    }

    fun seekToRecording(positionMs: Int) {
        localPlayer?.seekTo(positionMs)
        _recordingPlaybackPosition.value = positionMs
    }

    fun stopPlayingRecording() {
        playbackProgressJob?.cancel()
        localPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
            } catch (e: Exception) {
                // Ignore state errors
            }
            release()
        }
        localPlayer = null
        _isPlayingRecording.value = false
        _playingFile.value = null
        _recordingPlaybackPosition.value = 0
        _recordingPlaybackDuration.value = 0
    }

    private fun startPlaybackProgressTracker() {
        playbackProgressJob?.cancel()
        playbackProgressJob = scope.launch {
            while (isActive && _isPlayingRecording.value) {
                localPlayer?.let { player ->
                    try {
                        _recordingPlaybackPosition.value = player.currentPosition
                    } catch (e: Exception) {
                        // Ignore state errors
                    }
                }
                delay(250)
            }
        }
    }

    fun release() {
        stopRecording()
        stopPlayingRecording()
        scope.cancel()
    }
}
