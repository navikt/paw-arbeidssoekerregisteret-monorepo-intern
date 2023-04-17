package no.nav.paw.aareg

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AaregClientTest {

    /*
    API Description:
    https://navikt.github.io/aareg/tjenester/integrasjon/api/
    */
    @Test
    fun `Returnerer gyldig objekt når alt er oK`() {
        val response = runBlocking {
            mockAaregClient(MockResponse.arbeidsforhold)
                .hentArbeidsforhold("ident", "call-id")
        }
        assertTrue(response.any { it.arbeidsgiver.organisasjonsnummer == "896929119" })
    }
//
//    @Test
//    fun test_OK_svar_Med_Uventet_JSON() {
//        val response = runBlocking {
//            mockAaregClient(MockResponse.error)
//                .hentArbeidsforhold("hei", "54-56 That's My Number")
//        }
//        val empty = emptyList<Arbeidsforhold>()
//        assertEquals(empty, response)
//    }
//
//    @Test
//    fun test_Server_Error() {
//        val response = runBlocking {
//            mockAaregClient("blablabla", HttpStatusCode.InternalServerError)
//                .hentArbeidsforhold("hei", "123456")
//        }
//        val empty = emptyList<Arbeidsforhold>()
//        assertEquals(empty, response)
//    }
//
//    @Test
//    fun realDeal() {
//        val client = AaregClient(url = "blah") { "tja" }
//        val response = runBlocking { client.hentArbeidsforhold("hei", "Number 2") }
//        assertEquals(emptyList<Arbeidsforhold>(), response)
//    }
}
