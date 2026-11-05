package com.setsig

open class SetsigException(
    val statusCode: Int,
    val errorCode: String = "ERR_API_ERROR",
    override val message: String,
    val details: Any? = null
) : Exception("Setsig API error ($statusCode - $errorCode): $message")

class AuthenticationException(message: String = "Invalid or missing setsig API key", details: Any? = null) :
    SetsigException(401, "ERR_UNAUTHORIZED", message, details)

class InsufficientCreditsException(message: String = "Insufficient prepaid balance for presence verification", details: Any? = null) :
    SetsigException(402, "ERR_INSUFFICIENT_CREDITS", message, details)

class PaywallBlockedException(
    val paywallUrl: String? = null,
    val currentBalance: Double? = null,
    val requiredMinTopup: Double? = null,
    message: String = "Verification services locked due to zero or depleted prepaid balance",
    details: Any? = null
) : SetsigException(402, "ERR_PAYWALL_BLOCKED", message, details)

class CrossBorderCreditRequiredException(message: String = "Cross-border verification requires local currency or FX buffer", details: Any? = null) :
    SetsigException(402, "ERR_CROSS_BORDER_CREDIT_REQUIRED", message, details)

class PaywallTierRequiredException(message: String = "Feature requires a higher subscription tier level", details: Any? = null) :
    SetsigException(403, "ERR_PAYWALL_TIER_REQUIRED", message, details)

class AttestationFailedException(message: String = "Hardware attestation failed: untrusted enclave or rooted device", details: Any? = null) :
    SetsigException(403, "ERR_ATTESTATION_FAILED", message, details)

class VisualProofRequiredException(message: String = "Venue policy strictly requires visual photo proof of presence", details: Any? = null) :
    SetsigException(422, "ERR_VISUAL_PROOF_REQUIRED", message, details)

class StaleVisualProofException(message: String = "Visual proof photo expired: must be taken within 10 seconds of scanning", details: Any? = null) :
    SetsigException(400, "ERR_STALE_VISUAL_PROOF", message, details)

class InvalidImageHashException(message: String = "Visual proof SHA-256 hash mismatch: photo integrity compromised", details: Any? = null) :
    SetsigException(400, "ERR_INVALID_IMAGE_HASH", message, details)

class GeofenceOutOfBoundsException(message: String = "Client coordinates outside authorized venue radius", details: Any? = null) :
    SetsigException(400, "ERR_GEOFENCE_OUT_OF_BOUNDS", message, details)

class ReplayAttackException(message: String = "Single-use token already consumed (replay attack detected)", details: Any? = null) :
    SetsigException(409, "ERR_REPLAY_DETECTED", message, details)
