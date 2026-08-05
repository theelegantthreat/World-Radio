package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.example.data.db.StationEntity

@JsonClass(generateAdapter = true)
data class RadioStation(
    @Json(name = "stationuuid") val stationuuid: String,
    @Json(name = "name") val name: String,
    @Json(name = "url") val url: String,
    @Json(name = "url_resolved") val urlResolved: String?,
    @Json(name = "homepage") val homepage: String?,
    @Json(name = "favicon") val favicon: String?,
    @Json(name = "tags") val tags: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "countrycode") val countrycode: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "votes") val votes: Int?,
    @Json(name = "bitrate") val bitrate: Int?,
    @Json(name = "codec") val codec: String?,
    @Json(name = "clickcount") val clickcount: Int?
) {
    fun toEntity(isFavorite: Boolean = false, lastPlayedAt: Long? = null): StationEntity {
        return StationEntity(
            stationuuid = stationuuid,
            name = name.ifBlank { "Unknown Station" },
            url = url,
            urlResolved = urlResolved ?: url,
            favicon = favicon ?: "",
            tags = tags ?: "",
            country = country ?: "",
            countrycode = countrycode ?: "",
            state = state ?: "",
            bitrate = bitrate ?: 0,
            codec = codec ?: "",
            isFavorite = isFavorite,
            lastPlayedAt = lastPlayedAt
        )
    }
}
