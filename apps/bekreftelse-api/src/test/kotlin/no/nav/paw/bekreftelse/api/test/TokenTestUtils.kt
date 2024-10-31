package no.nav.paw.bekreftelse.api.test

import com.nimbusds.jwt.SignedJWT
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
