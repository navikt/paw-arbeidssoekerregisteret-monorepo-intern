package no.nav.paw.pdl

import kotlinx.coroutines.runBlocking
import no.nav.paw.mockPdlClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PdlClientTest {
    @Test
    fun `Forventer gyldig respons fra hentAktorId`() {
        val respons = readResource("hentIdenter-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking { pdlClient.hentAktorId("2649500819544") }
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
                    pdlClient.hentIdenter("2649500819544")
                }
            }
        )
    }

    @Test
    fun `Forventer gyldig respons fra hentIdenter`() {
        val respons = readResource("hentIdenter-response.json")
        val pdlClient = mockPdlClient(respons)

        val resultat = runBlocking { pdlClient.hentIdenter("2649500819544") }
        val forventet = "09127821914"
        assertEquals(forventet, resultat!!.first().ident)
    }
}

private fun readResource(filename: String) =
    ClassLoader.getSystemResource(filename).readText()
