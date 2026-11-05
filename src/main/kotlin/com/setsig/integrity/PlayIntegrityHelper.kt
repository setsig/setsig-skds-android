package com.setsig.integrity

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PlayIntegrityHelper {

    /**
     * Requests a Google Play Integrity token bound to a client token and dynamic challenge nonce.
     *
     * @param context Application context
     * @param cloudProjectNumber Google Cloud Project Number linked to Play Integrity
     * @param clientToken Client unique token
     * @param challenge Dynamic TOTP challenge or session ID
     * @return Base64-encoded Play Integrity token
     */
    suspend fun requestIntegrityToken(
        context: Context,
        cloudProjectNumber: Long,
        clientToken: String,
        challenge: String
    ): String = suspendCancellableCoroutine { continuation ->
        try {
            val integrityManager = IntegrityManagerFactory.create(context.applicationContext)
            
            // Build nonce: SHA-256 hash of clientToken + challenge
            val rawNonce = "$clientToken:$challenge"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawNonce.toByteArray(Charsets.UTF_8))
            val nonceString = android.util.Base64.encodeToString(hashBytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)

            val tokenRequest = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .setNonce(nonceString)
                .build()

            integrityManager.requestIntegrityToken(tokenRequest)
                .addOnSuccessListener { response ->
                    continuation.resume(response.token())
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Throwable) {
            continuation.resumeWithException(e)
        }
    }
}

