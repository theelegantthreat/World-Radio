package com.example.data.api

import android.util.Log
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object IcyMetadataFetcher {
    private const val TAG = "IcyMetadataFetcher"

    fun fetchIcyMetadata(streamUrl: String): String? {
        var currentUrl = streamUrl
        var redirects = 0
        val maxRedirects = 5

        while (redirects < maxRedirects) {
            var connection: HttpURLConnection? = null
            var stream: InputStream? = null
            try {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    setRequestProperty("Icy-Metadata", "1")
                    setRequestProperty("Connection", "close")
                    setRequestProperty("Accept", "*/*")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) RadioApp/1.0")
                    instanceFollowRedirects = false // Follow manually for cross-protocol HTTP -> HTTPS
                    connectTimeout = 4000
                    readTimeout = 4000
                    connect()
                }

                val responseCode = connection.responseCode
                if (responseCode in listOf(301, 302, 303, 307, 308)) {
                    val newUrl = connection.getHeaderField("Location")
                    if (!newUrl.isNullOrBlank()) {
                        currentUrl = newUrl
                        redirects++
                        connection.disconnect()
                        continue
                    }
                }

                if (responseCode != 200) {
                    Log.d(TAG, "Non-200 response: $responseCode")
                    return null
                }

                val metaIntHeader = connection.headerFields.entries.find {
                    it.key?.equals("icy-metaint", ignoreCase = true) == true
                }?.value?.firstOrNull() ?: return null

                val metaint = metaIntHeader.toIntOrNull() ?: return null
                if (metaint <= 0) return null

                stream = connection.inputStream

                // Try to read up to 5 cycles of metadata blocks to guarantee finding a filled title
                for (cycle in 0 until 5) {
                    var bytesRead = 0
                    while (bytesRead < metaint) {
                        val skipped = stream.skip((metaint - bytesRead).toLong())
                        if (skipped <= 0) {
                            val b = stream.read()
                            if (b == -1) return null
                            bytesRead++
                        } else {
                            bytesRead += skipped.toInt()
                        }
                    }

                    val metaLenByte = stream.read()
                    if (metaLenByte == -1) return null
                    val metaLen = metaLenByte * 16

                    if (metaLen > 0) {
                        val metaBytes = ByteArray(metaLen)
                        var offset = 0
                        while (offset < metaLen) {
                            val read = stream.read(metaBytes, offset, metaLen - offset)
                            if (read == -1) return null
                            offset += read
                        }

                        val metaString = String(metaBytes, Charsets.UTF_8)
                        val matcher = Pattern.compile("StreamTitle='(.*?)';").matcher(metaString)
                        if (matcher.find()) {
                            val title = matcher.group(1)?.trim()
                            if (!title.isNullOrEmpty()) {
                                return title
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching ICY metadata for $streamUrl", e)
            } finally {
                try { stream?.close() } catch (e: Exception) {}
                try { connection?.disconnect() } catch (e: Exception) {}
            }
            break
        }
        return null
    }
}
