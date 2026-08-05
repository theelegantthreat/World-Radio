/*
 * Copyright (C) 2026 The Elegant Threat (theelegantthreat)
 * SPDX-License-Identifier: GPL-3.0-only
 */

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
