/*
 * Copyright (C) 2026 The Elegant Threat (theelegantthreat)
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.example.data.repository

import com.example.data.api.RadioBrowserApi
import com.example.data.db.StationDao
import com.example.data.db.StationEntity
import com.example.data.model.RadioStation
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class RadioRepository(private val stationDao: StationDao) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://de1.api.radio-browser.info/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(RadioBrowserApi::class.java)

    // DB Operations
    val favoriteStations: Flow<List<StationEntity>> = stationDao.getFavoriteStations()
    val recentStations: Flow<List<StationEntity>> = stationDao.getRecentStations()
    val trendingStations: Flow<List<StationEntity>> = stationDao.getTrendingStations()

    suspend fun getAllStations(): List<StationEntity> {
        return stationDao.getAllStations().first()
    }

    suspend fun insertStations(stations: List<StationEntity>) {
        stations.forEach { station ->
            val existing = stationDao.getStationById(station.stationuuid)
            if (existing != null) {
                val merged = existing.copy(
                    isFavorite = existing.isFavorite || station.isFavorite,
                    lastPlayedAt = maxOf(existing.lastPlayedAt ?: 0L, station.lastPlayedAt ?: 0L).let { if (it == 0L) null else it },
                    playCount = maxOf(existing.playCount, station.playCount)
                )
                stationDao.insertStation(merged)
            } else {
                stationDao.insertStation(station)
            }
        }
    }

    fun exportStationsToJson(stations: List<StationEntity>): String {
        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, StationEntity::class.java)
        val adapter = moshi.adapter<List<StationEntity>>(listType)
        return adapter.toJson(stations)
    }

    fun importStationsFromJson(json: String): List<StationEntity>? {
        return try {
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, StationEntity::class.java)
            val adapter = moshi.adapter<List<StationEntity>>(listType)
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getStationById(uuid: String): StationEntity? = stationDao.getStationById(uuid)

    suspend fun insertStation(station: StationEntity) = stationDao.insertStation(station)

    suspend fun toggleFavorite(station: StationEntity) {
        val existing = stationDao.getStationById(station.stationuuid)
        if (existing == null) {
            stationDao.insertStation(station.copy(isFavorite = true))
        } else {
            stationDao.updateFavoriteStatus(station.stationuuid, !existing.isFavorite)
        }
    }

    suspend fun markAsPlayed(station: StationEntity) {
        val existing = stationDao.getStationById(station.stationuuid)
        if (existing == null) {
