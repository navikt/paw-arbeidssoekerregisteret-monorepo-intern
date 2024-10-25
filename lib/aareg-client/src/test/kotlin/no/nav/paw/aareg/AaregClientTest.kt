package no.nav.paw.aareg

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import kotlinx.coroutines.runBlocking

class AaregClientTest : FreeSpec({

    /*
    API Description:
    https://navikt.github.io/aareg/tjenester/integrasjon/api/
    */
    "Returnerer gyldig objekt når alt er ok" {
        val response = runBlocking {
            mockAaregClient(MockResponse.arbeidsforhold)
                .hentArbeidsforhold("ident", "call-id")
        }

        response.map { it.arbeidsgiver.organisasjonsnummer } shouldContain "896929119"
    }
})
