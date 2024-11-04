package no.nav.paw.bekreftelse.api.test

import com.nimbusds.jwt.SignedJWT
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderClaims
import no.nav.paw.security.authentication.token.AzureAd
import no.nav.paw.security.authentication.token.IdPorten
import no.nav.paw.security.authentication.token.TokenX
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

fun MockOAuth2Server.issueTokenXToken(
    acr: String = "idporten-loa-high",
    pid: String = TestData.fnr1
): SignedJWT {
    return issueToken(
        claims = mapOf(
            "acr" to acr,
            "pid" to pid
        )
    )
}

fun MockOAuth2Server.issueAzureToken(
    oid: UUID = UUID.randomUUID(),
    name: String = "Kari Nordmann",
    navIdent: String = TestData.navIdent1
): SignedJWT {
    return issueToken(
        claims = mapOf(
            "oid" to oid.toString(),
            "name" to name,
            "NAVident" to navIdent
        )
    )
}

fun MockOAuth2Server.issueAzureM2MToken(
    oid: UUID = UUID.randomUUID(),
    roles: List<String> = listOf("access_as_application"),
): SignedJWT {
    return issueToken(
        claims = mapOf(
            "oid" to oid.toString(),
            "roles" to roles
        )
    )
}

fun MockOAuth2Server.createAuthProviders(): List<AuthProvider> {
    val wellKnownUrl = wellKnownUrl("default").toString()
    return listOf(
        AuthProvider(
            name = IdPorten.name,
            clientId = "default",
            discoveryUrl = wellKnownUrl,
            claims = AuthProviderClaims(listOf("acr=idporten-loa-high"))
        ),
        AuthProvider(
            name = TokenX.name,
            clientId = "default",
            discoveryUrl = wellKnownUrl,
            claims = AuthProviderClaims(listOf("acr=Level4", "acr=idporten-loa-high"), true)
        ),
        AuthProvider(
            name = AzureAd.name,
            clientId = "default",
            discoveryUrl = wellKnownUrl,
            claims = AuthProviderClaims(listOf("NAVident"))
        )
    )
}
