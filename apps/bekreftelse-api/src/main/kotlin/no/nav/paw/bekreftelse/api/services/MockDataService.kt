package no.nav.paw.bekreftelse.api.services

import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelse
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.config.hoplite.loadConfigFromProvidedResource
import java.time.Duration
import java.time.Instant
import java.util.*

data class MockBekreftelse(
    val id: UUID
)

data class MockPeriode(
    val id: UUID,
    val bekreftelser: List<MockBekreftelse>
)

data class MockPerson(
    val ident: String,
    val perioder: List<MockPeriode>
)

data class MockData(
    val personer: List<MockPerson>
)

// TODO Fjern når vi har ferdig Kafka-logikk
class MockDataService {

    private var mockData = loadConfigFromProvidedResource<MockData>("/test/mock-data.toml")
    private var tilgjengeligBekreftelser = mutableMapOf<String, TilgjengeligBekreftelserResponse>()

    init {
        mockData.personer.forEach { person ->
            person.perioder.forEach { periode ->
                val bekreftelser = periode.bekreftelser.map { bekreftelse ->
                    val days = Random().nextLong(1, 13)
                    val gjelderFra = Instant.now().minus(Duration.ofDays(days))
                    val gjelserTil = gjelderFra.plus(Duration.ofDays(14))
                    TilgjengeligBekreftelse(periode.id, bekreftelse.id, gjelderFra, gjelserTil)
                }
                tilgjengeligBekreftelser[person.ident] = bekreftelser
            }
        }
    }

    fun finnTilgjengeligBekreftelser(identitetsnummer: String): TilgjengeligBekreftelserResponse {
        return tilgjengeligBekreftelser[identitetsnummer] ?: emptyList()
    }

    fun mottaBekreftelse(
        identitetsnummer: String,
        bekreftelseId: UUID
    ) {
        val eksisterende = tilgjengeligBekreftelser[identitetsnummer]
        if (eksisterende != null) {
            val oppdatert = eksisterende.filter { it.bekreftelseId != bekreftelseId }
            tilgjengeligBekreftelser[identitetsnummer] = oppdatert
        }
    }
}