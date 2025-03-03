package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import java.util.*

class GenererTilstandTest : FreeSpec({

    "Når en ny periode kommer inn opprettes tilsvarende tilstand" {
        with(kafkaKeyContext()) {
            val periode = periode().value
            periode.asInternTilstand(
                gjeldeneTilstand = null,
            ) shouldBe InternTilstand(
                periodeId = periode.id,
                ident = periode.identitetsnummer,
                bekreftelser = emptyList()
            )
        }
    }

    "Når en periode avsluttes endres ikke tilstanden" {
        with(kafkaKeyContext()) {
            val periode = periode(avsluttetMetadata = metadata()).value
            val gjeldeneTilstand = InternTilstand(
                periodeId = periode.id,
                ident = periode.identitetsnummer,
                bekreftelser = listOf(UUID.randomUUID())
            )
            periode.asInternTilstand(
                gjeldeneTilstand = gjeldeneTilstand,
            ) shouldBe gjeldeneTilstand
        }
    }

    "Når identitetsnummer på en periode endres blir intern tilstand oppdatert" {
        with(kafkaKeyContext()) {
            val periode = periode(identitetsnummer = "00998877654").value
            val gjeldeneTilstand = InternTilstand(
                periodeId = periode.id,
                ident = "12345678909",
                bekreftelser = listOf(UUID.randomUUID(), UUID.randomUUID())
            )
            periode.asInternTilstand(
                gjeldeneTilstand = gjeldeneTilstand,
            ) shouldBe gjeldeneTilstand.copy(
                ident = periode.identitetsnummer
            )
        }
    }
})
