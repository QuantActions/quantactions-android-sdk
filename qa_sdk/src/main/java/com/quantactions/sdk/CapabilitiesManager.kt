package com.quantactions.sdk

import android.content.Context
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.data.api.TokenApi
import timber.log.Timber
import java.io.InputStreamReader
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class CapabilitiesManager(
    private val context: Context,
    private val preferences: GenericPreferences
) {

    // --- Configuration Constants ---
    private val expectedIssuer = "Quantactions"
    private val expectedAudience = "quantactions-sdk"
    private val publicKeyFilename = "qa_capabilities_public_key"
    // -----------------------------

    private val publicKey: RSAPublicKey by lazy {
        loadPublicKey()
    }

    private var currentDecodedJWT: DecodedJWT? = null

    init {
        loadCapabilitiesFromStorage() // Load existing token on init
    }

    fun hasFeature(feature: String): Boolean {
        val featuresList = currentDecodedJWT?.getClaim("features")?.asList(String::class.java)
        return featuresList?.contains(feature) ?: false
    }

    fun getCapabilities(): List<String>? {
        return currentDecodedJWT?.getClaim("features")?.asList(String::class.java)
    }

    private fun clearCapabilities() {
        preferences.capabilitiesToken = null
        currentDecodedJWT = null
        Timber.d("Cleared stored capabilities token and in-memory data.")
    }

    private fun loadCapabilitiesFromStorage() {
        val tokenString = preferences.capabilitiesToken
        if (tokenString != null) {
            try {
                val decodedToken = verifyAndDecodeToken(tokenString)

                if (decodedToken.expiresAt == null || decodedToken.expiresAt.time < System.currentTimeMillis() - (6 * 60 * 60 * 1000)) {
                    Timber.w("Capabilities token has expired or is about to expire (within 6 hours).")
                    clearCapabilities()
                } else {
                    currentDecodedJWT = decodedToken
                    Timber.d("Capabilities loaded successfully. Features: ${getCapabilities()}")
                }
            } catch (e: SecurityException) {
                Timber.e(e, "Stored capabilities token is invalid.")
                clearCapabilities()
            }
        } else {
            Timber.d("No capabilities token found in preferences.")
            currentDecodedJWT = null
        }
    }

    suspend fun fetchAndStoreCapabilities(tokenApi: TokenApi) {
            Timber.d("Fetching new capabilities token...")
            when(val response = tokenApi.getCapabilities()) {
                is ApiSuccessResponse  -> {
                    val tokenString = response.body!!.token

                    val decodedToken = verifyAndDecodeToken(tokenString)

                    preferences.capabilitiesToken = tokenString
                    currentDecodedJWT = decodedToken
                    Timber.d("New capabilities token fetched, verified, and stored. Features: ${getCapabilities()}")
                }
                is ApiErrorResponse -> {
                    Timber.e("Failed to fetch capabilities token: ${response.errorMessage}")
                }
                is ApiEmptyResponse -> {
                    Timber.e("Failed to fetch capabilities token: Empty response")
                }
            }
    }

    suspend fun refreshCapabilities(tokenApi: TokenApi) {
        Timber.d("Refreshing capabilities...")
        clearCapabilities()
        fetchAndStoreCapabilities(tokenApi)
    }

    private fun verifyAndDecodeToken(token: String): DecodedJWT {
        try {
            val algorithm = Algorithm.RSA256(publicKey)
            val verifier = JWT.require(algorithm)
                .withIssuer(expectedIssuer)
                .withAudience(expectedAudience)
                .acceptLeeway(60)
                .build()

            val decodedJWT = verifier.verify(token)
            Timber.d("Token verified successfully: JTI=${decodedJWT.id}")
            return decodedJWT
        } catch (e: JWTVerificationException) {
            Timber.e(e, "JWT Verification Failed")
            throw SecurityException("Token verification failed: ${e.message}", e)
        } catch (e: Exception) {
            Timber.e(e, "An unexpected error occurred during token verification")
            throw SecurityException(
                "Token verification failed due to an unexpected error: ${e.message}",
                e
            )
        }
    }

    private fun loadPublicKey(): RSAPublicKey {
        try {
            val resourceId = R.raw.qa_capabilities_public_key
            if (resourceId == 0) {
                throw RuntimeException("Public key file not found in res/raw: $publicKeyFilename")
            }
            context.resources.openRawResource(resourceId).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val pemContents = reader.readText()
                    val publicKeyPEM = pemContents
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replace("\\s".toRegex(), "")

                    val encoded = Base64.getDecoder().decode(publicKeyPEM)
                    val keySpec = X509EncodedKeySpec(encoded)
                    val keyFactory = KeyFactory.getInstance("RSA")
                    val generatedKey = keyFactory.generatePublic(keySpec)

                    if (generatedKey !is RSAPublicKey) {
                        throw IllegalArgumentException("Loaded public key is not an RSAPublicKey.")
                    }

                    Timber.d("Public key loaded successfully. Algorithm: ${generatedKey.algorithm}")
                    return generatedKey
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load public key: $publicKeyFilename.pem")
            throw RuntimeException(
                "CRITICAL: Could not initialize CapabilitiesManager. Failed to load public key.",
                e
            )
        }
    }
}
    
