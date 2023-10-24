package no.nav.paw.kafkakeygenerator.webserver

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.paw.kafkakeygenerator.Api
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon

fun Routing.konfigurerApi(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    api: Api
) {
    get("/open-hello") {
        call.respondText(api.hello())
    }
    authenticate(autentiseringKonfigurasjon.name) {
        get("/hello") {
            call.respondText(api.hello())
        }
    }
}