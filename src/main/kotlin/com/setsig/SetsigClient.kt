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
                var errorCode = "ERR_API_ERROR"
                var errorMsg = responseText.ifEmpty { "HTTP $statusCode error" }
                var paywallUrl: String? = null
                var curBal: Double? = null
                var reqDeposit: Double? = null

                try {
                    val errJson = gson.fromJson(responseText, JsonObject::class.java)
                    if (errJson != null) {
                        if (errJson.has("error_code")) errorCode = errJson.get("error_code").asString
                        else if (errJson.has("code")) errorCode = errJson.get("code").asString
                        if (errJson.has("message")) errorMsg = errJson.get("message").asString

                        if (errJson.has("details") && errJson.get("details").isJsonObject) {
                            val det = errJson.getAsJsonObject("details")
                            if (det.has("paywall_url")) paywallUrl = det.get("paywall_url").asString
                            if (det.has("current_balance")) curBal = det.get("current_balance").asDouble
                            if (det.has("required_min_topup")) reqDeposit = det.get("required_min_topup").asDouble
                        }
                    }
                } catch (_: Throwable) {}

                when (errorCode) {
                    "ERR_PAYWALL_BLOCKED" -> throw PaywallBlockedException(paywallUrl, curBal, reqDeposit, errorMsg)
                    "ERR_CROSS_BORDER_CREDIT_REQUIRED" -> throw CrossBorderCreditRequiredException(errorMsg)
                    "ERR_PAYWALL_TIER_REQUIRED" -> throw PaywallTierRequiredException(errorMsg)
                    "ERR_ATTESTATION_FAILED" -> throw AttestationFailedException(errorMsg)
                    "ERR_VISUAL_PROOF_REQUIRED" -> throw VisualProofRequiredException(errorMsg)
                    "ERR_STALE_VISUAL_PROOF" -> throw StaleVisualProofException(errorMsg)
                    "ERR_INVALID_IMAGE_HASH" -> throw InvalidImageHashException(errorMsg)
                    "ERR_GEOFENCE_OUT_OF_BOUNDS" -> throw GeofenceOutOfBoundsException(errorMsg)
                    "ERR_REPLAY_DETECTED" -> throw ReplayAttackException(errorMsg)
                    else -> {
                        when (statusCode) {
                            401, 403 -> throw AuthenticationException(errorMsg)
                            402 -> throw InsufficientCreditsException(errorMsg)
                            409 -> throw ReplayAttackException(errorMsg)
                            else -> throw SetsigException(statusCode, errorCode, errorMsg)
                        }
                    }
                }
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

