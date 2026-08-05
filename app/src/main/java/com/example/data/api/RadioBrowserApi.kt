/*
 * Copyright (C)2026 The Elegant Threat (theelegantthreat)
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.example.data.api

import com.example.data.model.RadioStation
import retrofit2.http.GET
import retrofit2.http.Query

interface RadioBrowserApi {

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("country") country: String? = null,
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int = 40,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<RadioStation>

    @GET("json/stations/byclickcount")
    suspend fun getPopularStations(
        @Query("limit") limit: Int = 40,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<RadioStation>
}
