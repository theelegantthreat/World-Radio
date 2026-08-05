/*
 * Copyright (C) 2026 The Elegant Threat (theelegantthreat)
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val stationuuid: String,
    val name: String,
    val url: String,
    val urlResolved: String,
    val favicon: String,
    val tags: String,
    val country: String,
    val countrycode: String,
    val state: String,
    val bitrate: Int,
    val codec: String,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0
)
