package no.nav.paw.arbeidssokerregisteret

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssokerregisteret.TestData
import no.nav.paw.arbeidssokerregisteret.requestScope
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import no.nav.security.mock.oauth2.MockOAuth2Server

class InngangSomBrukerTest : FunSpec({
    val oauth = MockOAuth2Server()
    val testAuthUrl = "/testAuthTokenx"

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }


})
