/*
 * Copyright (C) 2026 The Elegant Threat (theelegantthreat)
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations")
    fun getAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 20")
    fun getRecentStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE stationuuid = :uuid")
    suspend fun getStationById(uuid: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Query("UPDATE stations SET isFavorite = :isFavorite WHERE stationuuid = :uuid")
    suspend fun updateFavoriteStatus(uuid: String, isFavorite: Boolean)

    @Query("UPDATE stations SET lastPlayedAt = :lastPlayedAt WHERE stationuuid = :uuid")
    suspend fun updateLastPlayed(uuid: String, lastPlayedAt: Long)

    @Query("UPDATE stations SET lastPlayedAt = :lastPlayedAt, playCount = :playCount WHERE stationuuid = :uuid")
    suspend fun updateLastPlayedAndCount(uuid: String, lastPlayedAt: Long, playCount: Int)

    @Query("SELECT * FROM stations WHERE playCount > 0 ORDER BY playCount DESC LIMIT 5")
    fun getTrendingStations(): Flow<List<StationEntity>>
}
