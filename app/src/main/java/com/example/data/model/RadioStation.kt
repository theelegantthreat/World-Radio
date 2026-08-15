package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.example.data.db.StationEntity
import java.util.UUID

/**
 * Core data model representing a Radio Station entity in the application.
 *
 * @property name The name of the radio station.
 * @property frequency The broadcast frequency (e.g., "101.1 FM", "Online").
 * @property genre The music or content genre / category.
 * @property country The country of broadcast.
 * @property streamUrl The audio stream URL for playback.
 */
@JsonClass(generateAdapter = true)
data class RadioStation(
    @Json(name = "name") val name: String,
    @Json(name = "frequency") val frequency: String = "Online",
    @Json(name = "tags") val genre: String = "",
    @Json(name = "country") val country: String = "",
    @Json(name = "url") val streamUrl: String = "",
    @Json(name = "stationuuid") val stationuuid: String = UUID.randomUUID().toString(),
    @Json(name = "url_resolved") val urlResolved: String? = null,
    @Json(name = "homepage") val homepage: String? = null,
    @Json(name = "favicon") val favicon: String? = null,
    @Json(name = "countrycode") val countrycode: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "votes") val votes: Int? = 0,
    @Json(name = "bitrate") val bitrate: Int? = 0,
    @Json(name = "codec") val codec: String? = null,
    @Json(name = "clickcount") val clickcount: Int? = 0
) {
    val url: String get() = streamUrl
    val tags: String get() = genre

    fun toEntity(isFavorite: Boolean = false, lastPlayedAt: Long? = null): StationEntity {
        return StationEntity(
            stationuuid = stationuuid,
            name = name.ifBlank { "Unknown Station" },
            url = streamUrl,
            urlResolved = urlResolved ?: streamUrl,
            favicon = favicon ?: "",
            tags = genre,
            country = country,
            countrycode = countrycode ?: "",
            state = state ?: "",
            bitrate = bitrate ?: 0,
            codec = codec ?: "",
            isFavorite = isFavorite,
            lastPlayedAt = lastPlayedAt
        )
    }

    companion object {
        fun fromEntity(entity: StationEntity): RadioStation {
            return RadioStation(
                stationuuid = entity.stationuuid,
                name = entity.name,
                streamUrl = entity.url,
                urlResolved = entity.urlResolved,
                genre = entity.tags,
                country = entity.country,
                countrycode = entity.countrycode,
                state = entity.state,
                favicon = entity.favicon,
                bitrate = entity.bitrate,
                codec = entity.codec
            )
        }
    }
}
