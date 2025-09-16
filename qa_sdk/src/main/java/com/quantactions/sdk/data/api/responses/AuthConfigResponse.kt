package com.quantactions.sdk.data.api.responses

import com.google.gson.annotations.SerializedName

data class AuthConfigResponse(
    @SerializedName("force_refresh_after") val forceRefreshAfter: String?,
    @SerializedName("per_key") val perKey: Map<String, PerKeyConfig>?
)

data class PerKeyConfig(
    @SerializedName("min_cap_token_iat") val minCapTokenIat: String?
)
