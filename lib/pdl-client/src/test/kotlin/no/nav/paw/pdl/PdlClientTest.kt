package no.nav.paw.pdl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import java.util.*

class PdlClientTest : FreeSpec({

    val callId = UUID.randomUUID().toString()
    val navConsumerId = "nav-consumer-id"

    "Forventer gyldig respons fra hentAktorId" {
        val respons = readResource("hentIdenter-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking {
            pdlClient.hentAktorId("2649500819544", callId, navConsumerId, "B123")
        }
        val forventet = "2649500819544"

        resultat shouldBe forventet
    }

    "Forventer feilmelding fra hentIdenter" {
        val respons = readResource("error-response.json")
        val pdlClient = mockPdlClient(respons)

        shouldThrow<PdlException>(
            block = {
                runBlocking {
                    pdlClient.hentIdenter(
                        ident = "2649500819544",
                        callId = callId,
                        navConsumerId = navConsumerId,
                        behandlingsnummer = "B123",
                    )
                }
            },
        )
    }

    "Forventer gyldig respons fra hentIdenter" {
        val respons = readResource("hentIdenter-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking {
            pdlClient.hentIdenter(
                ident = "2649500819544",
                callId = callId,
                navConsumerId = navConsumerId,
                behandlingsnummer = "B123",
            )
        }
        val forventet = "09127821914"

        resultat!!.first().ident shouldBe forventet
    }

    "Forventer gyldig respons fra hentOpphold" {
        val respons = readResource("hentOpphold-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking {
            pdlClient.hentOpphold("2649500819544", callId, navConsumerId, "B123")
        }
        val forventet = Oppholdstillatelse.PERMANENT

        resultat!!.first().type shouldBe forventet
    }

    "Forventer gyldig respons fra hentPerson" {
        val respons = readResource("hentPerson-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking {
            pdlClient.hentPerson(
                "2649500819544",
                callId,
                null,
                navConsumerId,
                behandlingsnummer = "B123"
            )
        }
        val forventet = Oppholdstillatelse.PERMANENT

        resultat!!.opphold.first().type shouldBe forventet
    }

    "Forventer gyldig respons fra hentPersonBolk" {
        val respons = readResource("hentPersonBolk-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking {
            pdlClient.hentPersonBolk(
                listOf("2649500819544"),
                callId,
                null,
                navConsumerId,
                false,
                "B123"
            )
        }
        val forventet = Oppholdstillatelse.PERMANENT

        resultat!!.first().person!!.opphold.first().type shouldBe forventet
    }

    "Forventer gyldig respons fra hentForenkletStatus" {
        val respons = readResource("hentForenkletStatus-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat =
            runBlocking { pdlClient.hentForenkletStatus("2649500819544", callId, null, navConsumerId, "B123") }
        val forventet = "bosattEtterFolkeregisterloven"

        resultat!!.folkeregisterpersonstatus.map { it.forenkletStatus } shouldContain forventet
    }

    "Forventer gyldig respons fra hentForenkletStatusBolk" {
        val respons = readResource("hentForenkletStatusBolk-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking {
            pdlClient.hentForenkletStatusBolk(
                listOf("2649500819544"),
                callId,
                null,
                navConsumerId,
                "B123"
            )
        }
        val forventet = "bosattEtterFolkeregisterloven"

        resultat!!.first().person!!.folkeregisterpersonstatus.map { it.forenkletStatus } shouldContain forventet
    }
})

private fun readResource(filename: String) = ClassLoader.getSystemResource(filename).readText()
