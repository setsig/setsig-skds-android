package com.setsig.model

import com.google.gson.annotations.SerializedName

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val altitude: Double? = null
)

data class VisualProofPayload(
    @SerializedName("image_hash_sha256")
    val imageHashSha256: String,
    @SerializedName("storage_key")
    val storageKey: String,
    @SerializedName("captured_at")
    val capturedAt: String,
    @SerializedName("liveness_score")
    val livenessScore: Double? = null
)

data class VerifySessionRequest(
    @SerializedName("venue_id")
    val venueId: String,
    @SerializedName("token_id")
    val tokenId: String,
    @SerializedName("client_token")
    val clientToken: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val altitude: Double? = null,
    @SerializedName("attestation_token")
    val attestationToken: String? = null,
    @SerializedName("visual_proof")
    val visualProof: VisualProofPayload? = null
)

data class VerifySessionResponse(
    @SerializedName("session_id")
    val sessionId: String,
    val status: String,
    @SerializedName("distance_meters")
    val distanceMeters: Double,
    @SerializedName("verified_at")
    val verifiedAt: String,
    @SerializedName("proof_signature")
    val proofSignature: String? = null
)

data class KioskTokenResponse(
    @SerializedName("token_id")
    val tokenId: String,
    @SerializedName("qr_payload")
    val qrPayload: String,
    @SerializedName("expires_at")
    val expiresAt: String,
    @SerializedName("ttl_remaining")
    val ttlRemaining: Int
)

data class VerifyIntentResponse(
    @SerializedName("upload_url")
    val uploadUrl: String,
    @SerializedName("storage_key")
    val storageKey: String,
    @SerializedName("expires_in_seconds")
    val expiresInSeconds: Int
)

