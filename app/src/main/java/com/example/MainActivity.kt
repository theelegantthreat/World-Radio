/*
 * Copyright (C) 2026 The Elegant Threat (theelegantthreat)
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.db.AppDatabase
import com.example.data.repository.RadioRepository
import com.example.player.RadioPlaybackManager
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RadioViewModel
import com.example.ui.screens.RadioMainScreen

class MainActivity : ComponentActivity() {
    private var radioPlaybackManager: RadioPlaybackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = RadioRepository(database.stationDao())
        val playbackManager = RadioPlaybackManager().also { radioPlaybackManager = it }
        val viewModel = RadioViewModel(repository, playbackManager)

        setContent {
            MyApplicationTheme {
                RadioMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        radioPlaybackManager?.release()
    }
}
