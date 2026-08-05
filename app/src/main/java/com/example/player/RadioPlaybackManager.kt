/*
 * Copyright (C) 2026 The Elegant Threat (theelegantthreat)
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.example.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.data.db.StationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PlaybackState {
    object Idle : PlaybackState()
    data class Loading(val station: StationEntity) : PlaybackState()
    data class Playing(val station: StationEntity) : PlaybackState()
    data class Paused(val station: StationEntity) : PlaybackState()
    data class Error(val station: StationEntity, val message: String) : PlaybackState()
}

class RadioPlaybackManager {

    private val TAG = "RadioPlaybackManager"
    private var mediaPlayer: MediaPlayer? = null
    var currentStation: StationEntity? = null
        private set

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _volume = MutableStateFlow(0.8f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setVolume(_volume.value, _volume.value)
                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer Prepared")
                    it.start()
                    currentStation?.let { station ->
                        _playbackState.value = PlaybackState.Playing(station)
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer Error what=$what extra=$extra")
                    currentStation?.let { station ->
                        _playbackState.value = PlaybackState.Error(
                            station,
                            "Unable to stream this station. The stream may be offline or temporary busy."
                        )
                    }
                    true
                }
            }
        }
    }

    fun setVolume(vol: Float) {
        val clampedVol = vol.coerceIn(0f, 1f)
        _volume.value = clampedVol
        mediaPlayer?.setVolume(clampedVol, clampedVol)
    }

    fun play(station: StationEntity) {
        initializePlayer()
        currentStation = station
        _playbackState.value = PlaybackState.Loading(station)

        try {
            mediaPlayer?.apply {
                reset()
                // Radio browser's resolved URL is optimal for streaming
                setDataSource(station.urlResolved.ifBlank { station.url })
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing stream", e)
            _playbackState.value = PlaybackState.Error(
                station,
                "Error initializing source: ${e.localizedMessage}"
            )
        }
    }

    fun togglePlayPause() {
        val s = currentStation ?: return
        val current = _playbackState.value

        when (current) {
            is PlaybackState.Playing -> {
                mediaPlayer?.pause()
                _playbackState.value = PlaybackState.Paused(s)
            }
            is PlaybackState.Paused -> {
                mediaPlayer?.start()
                _playbackState.value = PlaybackState.Playing(s)
            }
            is PlaybackState.Error -> {
                play(s)
            }
            else -> {}
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        currentStation = null
