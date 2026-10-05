package no.nav.paw.arbeidssoekerregisteret.kompatibilitet

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.model.InternalState
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselKanal
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.utils.InternalStateSerde
import no.nav.paw.arbeidssoekerregisteret.utils.PeriodeHendelseSerde
import no.nav.paw.arbeidssoekerregisteret.utils.VarselHendelseSerde
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at data skrevet med Jackson 2 kan leses av gjeldende serdes i min-side-varsler.
 * JSON-filene er generert med Jackson 2, se [lagretJson].
 */
class SerdeKompatibilitetTest : FreeSpec({
    val periodeHendelseSerde = PeriodeHendelseSerde()
    val internalStateSerde = InternalStateSerde()
    val varselHendelseSerde = VarselHendelseSerde()

    "PeriodeHendelse skrevet med Jackson 2 kan leses" {
        val json = lagretJson("min-side-varsler/periode-hendelse") {
            periodeHendelseSerde.serializer().serialize("topic", periodeHendelse)
        }
        periodeHendelseSerde.deserializer().deserialize("topic", json) shouldBe periodeHendelse
    }

    "InternalState skrevet med Jackson 2 kan leses" {
        val forventet = InternalState(periode = periodeHendelse.copy(avsluttetTimestamp = null))
        val json = lagretJson("min-side-varsler/internal-state") {
            internalStateSerde.serializer().serialize("topic", forventet)
        }
        internalStateSerde.deserializer().deserialize("topic", json) shouldBe forventet
    }

    varselHendelser.forEach { (navn, forventet) ->
        "VarselHendelse $navn skrevet med Jackson 2 kan leses" {
            val json = lagretJson("min-side-varsler/varsel-hendelse-$navn") {
                varselHendelseSerde.serializer().serialize("topic", forventet)
            }
            varselHendelseSerde.deserializer().deserialize("topic", json) shouldBe forventet
        }
    }
})

private val periodeHendelse = PeriodeHendelse(
    periodeId = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"),
    identitetsnummer = "12345678901",
    startetTimestamp = Instant.parse("2025-03-14T09:26:53.589Z"),
    avsluttetTimestamp = Instant.parse("2025-06-01T08:00:00Z")
)

private val varselHendelser: List<Pair<String, VarselHendelse>> = listOf(
    "ekstern-status-oppdatert" to VarselHendelse(
        eventName = VarselEventName.EKSTERN_STATUS_OPPDATERT,
        status = VarselStatus.SENDT,
        varselId = "7db3818c-0bcd-4037-9ce4-ec8c2c4bff07",
        varseltype = VarselType.OPPGAVE,
        kanal = VarselKanal.BETINGET_SMS,
        renotifikasjon = false,
        sendtSomBatch = true,
        feilmelding = null,
        namespace = "paw",
        appnavn = "paw-arbeidssoekerregisteret-min-side-varsler",
        tidspunkt = Instant.parse("2025-03-14T09:26:53.589Z")
    ),
    "opprettet" to VarselHendelse(
        eventName = VarselEventName.OPPRETTET,
        status = null,
        varselId = "8e9f0a1b-2c3d-4e4f-8a5b-6c7d8e9f0a1b",
        varseltype = VarselType.BESKJED,
        kanal = null,
        renotifikasjon = null,
        sendtSomBatch = null,
        feilmelding = null,
        namespace = "paw",
        appnavn = "paw-arbeidssoekerregisteret-min-side-varsler",
        tidspunkt = Instant.parse("2025-03-14T09:26:53Z")
    )
)
