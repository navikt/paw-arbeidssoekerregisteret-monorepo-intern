package no.nav.paw.security.test

import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

fun MockOAuth2Server.issueIDPortenToken(
    acr: String = "idporten-loa-high",
    pid: String = "01017012345"
): String {
    return issueToken(
        claims = mapOf(
            "acr" to acr,
            "pid" to pid
        )
    ).serialize()
}

fun MockOAuth2Server.issueTokenXToken(
    acr: String = "idporten-loa-high",
    pid: String = "01017012345"
): String {
    return issueToken(
        claims = mapOf(
            "acr" to acr,
            "pid" to pid
        )
    ).serialize()
}

fun MockOAuth2Server.issueAzureADToken(
    oid: UUID = UUID.randomUUID(),
    name: String = "Kari Nordmann",
    navIdent: String = "NAV12345"
): String {
    return issueToken(
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
        claims = mapOf(
            "oid" to oid.toString(),
            "roles" to roles
        )
    ).serialize()
}

fun MockOAuth2Server.getAuthProviders(): List<AuthProvider> {
    val issuerId = "default"
    val wellKnownUrl = wellKnownUrl(issuerId).toString()
    return listOf(
        "idporten" to arrayOf("acr=idporten-loa-high"),
        "tokenx" to arrayOf("acr=idporten-loa-high"),
        "azure" to arrayOf("NAVident")
    ).map {
        AuthProvider(
            name = it.first,
            discoveryUrl = wellKnownUrl,
            acceptedAudience = listOf(issuerId),
            claimMap = it.second,
            combineWithOr = true
        )
    }
}

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val acceptedAudience: List<String>,
    val claimMap: Array<String>,
    val combineWithOr: Boolean
)
