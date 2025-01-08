package no.nav.paw.security.test

import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderRequiredClaims
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

fun MockOAuth2Server.issueTokenXToken(
    acr: String = "idporten-loa-high",
    pid: String = "01017012345"
): String {
    return issueToken(
        issuerId = "tokenx",
        claims = mapOf(
            "acr" to acr,
            "pid" to pid
        )
    ).serialize()
}

fun MockOAuth2Server.issueAzureADToken(
    oid: UUID = UUID.randomUUID(),
    name: String = "Kari Nordmann",
    navIdent: String = "NAV1234"
): String {
    return issueToken(
        issuerId = "azure",
        claims = mapOf(
            "oid" to oid.toString(),
            "name" to name,
            "NAVident" to navIdent
        )
    ).serialize()
}

fun MockOAuth2Server.issueAzureM2MToken(
    oid: UUID = UUID.randomUUID(),
    roles: List<String> = listOf("access_as_application"),
): String {
    return issueToken(
        issuerId = "azure",
        claims = mapOf(
            "oid" to oid.toString(),
            "roles" to roles
        )
    ).serialize()
}

fun MockOAuth2Server.issueIDPortenToken(
    acr: String = "idporten-loa-high",
    pid: String = "01017012345"
): String {
    return issueToken(
        issuerId = "idporten",
        claims = mapOf(
            "acr" to acr,
            "pid" to pid
        )
    ).serialize()
}

fun MockOAuth2Server.issueMaskinPortenToken(
    scope: String = "nav:arbeid:arbeidssokerregisteret.read"
): String {
    return issueToken(
        issuerId = "maskinporten",
        claims = mapOf(
            "scope" to scope
        )
    ).serialize()
}

fun MockOAuth2Server.getAuthProviders(): List<AuthProvider> {
    return listOf(
        "tokenx" to listOf("acr=idporten-loa-high"),
        "azure" to listOf("NAVident"),
        "idporten" to listOf("acr=idporten-loa-high"),
        "maskinporten" to listOf("scope=nav:arbeid:arbeidssokerregisteret.read")
    ).map {
        AuthProvider(
            name = it.first,
            audiences = listOf("default"),
            discoveryUrl = wellKnownUrl(it.first).toString(),
            requiredClaims = AuthProviderRequiredClaims(it.second)
        )
    }
}
