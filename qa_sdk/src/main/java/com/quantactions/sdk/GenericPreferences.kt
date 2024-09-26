package com.quantactions.sdk

interface GenericPreferences {

    var gender: QA.Gender

    var yearOfBirth: Int

    var selfDeclaredHealthy: Boolean

    var identityId: String

    var password: String?

    val accessToken: String?

    val refreshToken: String?

    var areCredentialsRegistered: Boolean

    var isOauthActivated: Boolean

    fun saveAccessTokens(accessToken: String? = null, refreshToken: String? = null)

}