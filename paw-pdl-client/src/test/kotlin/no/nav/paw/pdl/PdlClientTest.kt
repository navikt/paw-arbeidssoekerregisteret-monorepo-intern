package no.nav.paw.pdl

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PdlClientTest {
    @Test
    fun `Kaster PdlException ved feilrespons fra PDL`() {
        assertThrows<PdlException> {
            runBlocking {
                mockPdlClient(MockResponse.error).hentIdenter(MOCK_FNR)
            }
        }
    }

    @Test
    fun `HentIdenter returnerer en identer`() {
        val response = runBlocking {
            mockPdlClient(MockResponse.hentIdenter).hentIdenter(MOCK_FNR)
        }
        assertEquals("2649500819544", response)
    }
}
