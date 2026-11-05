package com.setsig

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.setsig.model.KioskTokenResponse
import com.setsig.model.VerifyIntentResponse
import com.setsig.model.VerifySessionRequest
import com.setsig.model.VerifySessionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SetsigConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.setsig.com"
)

class SetsigException(
    val statusCode: Int,
    override val message: String
) : Exception("Setsig API error ($statusCode): $message")

class SetsigClient(
    val config: SetsigConfig,
    private val gson: Gson = Gson()
) {
    constructor(apiKey: String, baseUrl: String = "https://api.setsig.com") : this(SetsigConfig(apiKey, baseUrl))

    /**
     * Verifies physical presence using dynamic TOTP token and device GPS coordinates.
     */
    suspend fun verifyPresence(request: VerifySessionRequest): VerifySessionResponse = withContext(Dispatchers.IO) {
        val endpoint = "${config.baseUrl.trimEnd('/')}/v1/sessions/verify"
        val jsonBody = gson.toJson(request)
        executePost(endpoint, jsonBody, object : TypeToken<VerifySessionResponse>() {})
    }

    /**
     * Fetches a dynamic self-refreshing QR payload for kiosk or terminal displays.
     */
    suspend fun generateKioskToken(venueId: String, ttlSeconds: Int = 60): KioskTokenResponse = withContext(Dispatchers.IO) {
        val endpoint = "${config.baseUrl.trimEnd('/')}/v1/tokens/generate"
        val payload = mapOf(
            "venue_id" to venueId,
            "ttl_seconds" to ttlSeconds
        )
        val jsonBody = gson.toJson(payload)
        executePost(endpoint, jsonBody, object : TypeToken<KioskTokenResponse>() {})
    }

    /**
     * Acquires a presigned URL to upload a visual proof photo directly to the cloud.
     */
    suspend fun acquireVisualProofUploadURL(venueId: String): VerifyIntentResponse = withContext(Dispatchers.IO) {
        val endpoint = "${config.baseUrl.trimEnd('/')}/v1/sessions/verify-intent"
        val payload = mapOf("venue_id" to venueId)
        val jsonBody = gson.toJson(payload)
        executePost(endpoint, jsonBody, object : TypeToken<VerifyIntentResponse>() {})
    }

    private fun <T> executePost(endpoint: String, jsonBody: String, typeToken: TypeToken<T>): T {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val statusCode = conn.responseCode
            val isSuccess = statusCode in 200..299
            val stream = if (isSuccess) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(stream ?: conn.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }

            if (!isSuccess) {
                throw SetsigException(statusCode, responseText.ifEmpty { "HTTP $statusCode error" })
            }

            // Check if response is wrapped in { "data": ... } envelope
            val jsonObject = gson.fromJson(responseText, JsonObject::class.java)
            return if (jsonObject != null && jsonObject.has("data")) {
                gson.fromJson(jsonObject.get("data"), typeToken.type)
            } else {
                gson.fromJson(responseText, typeToken.type)
            }
        } finally {
            conn.disconnect()
        }
    }
}

