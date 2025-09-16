package com.quantactions.sdk.data.model

import com.google.gson.annotations.SerializedName

data class Capabilities(
    @SerializedName("iss") val iss: String,
    @SerializedName("sub") val sub: String,
    @SerializedName("aud") val aud: String,
    @SerializedName("iat") val iat: Long,
    @SerializedName("nbf") val nbf: Long,
    @SerializedName("exp") val exp: Long,
    @SerializedName("plan") val plan: String,
    @SerializedName("features") val features: List<String>,
    @SerializedName("ratelimit") val rateLimit: RateLimit,
    @SerializedName("binding") val binding: Binding,
    @SerializedName("jti") val jti: String
)

data class RateLimit(
    @SerializedName("rpm") val rpm: Int,
    @SerializedName("burst") val burst: Int
)

data class Binding(
    @SerializedName("bundle_id") val bundleId: String,
    @SerializedName("sdk_min") val sdkMin: String
)
