package no.nav.paw.dolly.api.test

import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderRequiredClaims
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

fun MockOAuth2Server.issueAzureM2MToken(
    oid: UUID = UUID.randomUUID(),
    roles: List<String> = listOf("access_as_application"),
): String {
    return issueToken(
        claims = mapOf(
            "oid" to oid.toString(),
            "roles" to roles
        )
    ).serialize()
}

fun MockOAuth2Server.createAuthProviders(): List<AuthProvider> {
    val wellKnownUrl = wellKnownUrl("default").toString()
    return listOf(
        AuthProvider(
            name = AzureAd.name,
            audiences = listOf("default"),
            discoveryUrl = wellKnownUrl,
            requiredClaims = AuthProviderRequiredClaims(listOf("NAVident"))
        )
    )
}