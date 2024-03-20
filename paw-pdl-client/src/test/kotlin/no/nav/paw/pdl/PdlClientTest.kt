package no.nav.paw.pdl

import kotlinx.coroutines.runBlocking
import no.nav.paw.mockPdlClient
import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PdlClientTest {
    val callId = UUID.randomUUID().toString()
    val navConsumerId = "nav-consumer-id"

    @Test
    fun `Forventer gyldig respons fra hentAktorId`() {
        val respons = readResource("hentIdenter-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking { pdlClient.hentAktorId("2649500819544", callId, navConsumerId) }
        val forventet = "2649500819544"
        assertEquals(forventet, resultat)
    }

    @Test
    fun `Forventer feilmelding fra hentIdenter`() {
        val respons = readResource("error-response.json")
        val pdlClient = mockPdlClient(respons)
        assertFailsWith<PdlException>(
            block = {
                runBlocking {
                    pdlClient.hentIdenter("2649500819544", callId, navConsumerId)
                }
            },
        )
    }

    @Test
    fun `Forventer gyldig respons fra hentIdenter`() {
        val respons = readResource("hentIdenter-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking { pdlClient.hentIdenter("2649500819544", callId, navConsumerId) }
        val forventet = "09127821914"
        assertEquals(forventet, resultat!!.first().ident)
    }

    @Test
    fun `Forventer gyldig respons fra hentOpphold`() {
        val respons = readResource("hentOpphold-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking { pdlClient.hentOpphold("2649500819544", callId, navConsumerId) }
        val forventet = Oppholdstillatelse.PERMANENT
        assertEquals(forventet, resultat!!.first().type)
    }

    @Test
    fun `Forventer gyldig respons fra hentPerson`() {
        val respons = readResource("hentPerson-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking { pdlClient.hentPerson("2649500819544", callId, null, navConsumerId) }
        val forventet = Oppholdstillatelse.PERMANENT
        assertEquals(forventet, resultat!!.opphold.first().type)
    }

    @Test
    fun `Forventer gyldig respons fra hentForenkletStatus`() {
        val respons = readResource("hentForenkletStatus-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking { pdlClient.hentForenkletStatus("2649500819544", callId, null, navConsumerId) }
        val forventet = "bosattEtterFolkeregisterloven"
        assertTrue { resultat!!.folkeregisterpersonstatus.any { it.forenkletStatus == forventet } }
    }
}

private fun readResource(filename: String) = ClassLoader.getSystemResource(filename).readText()
