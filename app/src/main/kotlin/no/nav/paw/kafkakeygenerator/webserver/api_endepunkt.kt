package no.nav.paw.kafkakeygenerator.webserver

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon

fun Routing.konfigurerApi(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    applikasjon: Applikasjon
) {
    get("/open-hello") {
        call.respondText(applikasjon.hello())
    }
    authenticate(autentiseringKonfigurasjon.name) {
        get("/hello") {
            call.respondText(applikasjon.hello())
        }
        post("/lookup") {

        }
    }

}