package com.quantactions.sdk

import com.auth0.android.jwt.JWT
import com.quantactions.sdk.data.api.TokenApi
import timber.log.Timber

class CapabilitiesManager(private val preferences: GenericPreferences) {

//    private val publicKeyPem: String by lazy {
//        context.resources.openRawResource(R.raw.qa_capabilities_public_key).use {
//            InputStreamReader(it).readText()
//        }
//    }

    private var features: List<String>? = null

    fun hasFeature(feature: String): Boolean {
        return features?.contains(feature) ?: false
    }

    fun getCapabilities(): List<String>? {
        return features
    }

    fun loadCapabilities() {
        val token = preferences.capabilitiesToken
        if (token != null) {
            try {
                val decodedToken = verifyToken(token)
                if (decodedToken.isExpired(6 * 60 * 60)) {
                    // Token is about to expire, clear it
                    features = null
                    preferences.capabilitiesToken = null
                } else {
                    features = decodedToken.claims["features"]?.asList(String::class.java)
                }
            } catch (e: Exception) {
                // Token is invalid, clear it
                preferences.capabilitiesToken = null
            }
        }
    }

    suspend fun fetchCapabilities(tokenApi: TokenApi) {
        try {
            val response = tokenApi.getCapabilities()
            val token = response.token
            // TODO: needs to be checked
//            val decodedToken = verifyToken(token)
            preferences.capabilitiesToken = token
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch capabilities")
            features = null
            preferences.capabilitiesToken = null
        }
    }

    suspend fun refreshCapabilities(tokenApi: TokenApi) {
        features = null
        preferences.capabilitiesToken = null
        fetchCapabilities(tokenApi)
        loadCapabilities()
    }

    private fun verifyToken(token: String): JWT {
//        val publicKey = getPublicKey()
        val decodedJWT= JWT(token)
//            .withIssuer("https://api.quantactions.com")
//            .withAudience("quantactions-sdk")

//        val claims = Gson().fromJson(String(Base64.getUrlDecoder().decode(decodedJWT.claims)), Capabilities::class.java)
//        val features = decodedJWT.claims["features"]?.asList(String::class.java)
//
//        if (features == null) {
//            throw IllegalArgumentException("Invalid capabilities token: missing features")
//        }

        return decodedJWT
        // Validate binding.bundle_id
//        val currentBundleId = context.packageName
//        if (claims.binding.bundleId != currentBundleId) {
//            throw IllegalArgumentException("Invalid bundle ID in capabilities token")
//        }

    }

//    private fun getPublicKey(): ECPublicKey {
//        val publicKeyPEM = publicKeyPem
//            .replace("-----BEGIN PUBLIC KEY-----", "")
//            .replace("-----END PUBLIC KEY-----", "")
//            .replace("\n", "")
//            .trim()
//        val encoded = Base64.getDecoder().decode(publicKeyPEM)
//        val keyFactory = KeyFactory.getInstance("EC")
//        val keySpec = X509EncodedKeySpec(encoded)
//        return keyFactory.generatePublic(keySpec) as ECPublicKey
//    }
}
