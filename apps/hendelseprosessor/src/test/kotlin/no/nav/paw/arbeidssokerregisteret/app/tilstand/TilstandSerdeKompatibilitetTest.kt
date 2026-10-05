package no.nav.paw.arbeidssokerregisteret.app.tilstand

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.HendelseScope
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Annet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Helse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonMedDetaljer
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Utdanning
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at tilstand i state store skrevet med Jackson 2 kan leses av gjeldende
 * TilstandSerde. JSON-filene er generert med Jackson 2, se [lagretJson].
 */
class TilstandSerdeKompatibilitetTest : FreeSpec({
    val serde = TilstandSerde()

    eksempler.forEach { (navn, forventet) ->
        "$navn skrevet med Jackson 2 kan leses" {
            val json = lagretJson("hendelseprosessor/$navn") {
                serde.serializer().serialize("topic", forventet)!!
            }
            serde.deserializer().deserialize("topic", json) shouldBe forventet
        }
    }
})

private const val IDENTITETSNUMMER = "12345678901"

private fun metadata(tidspunkt: String, brukerType: BrukerType = BrukerType.SLUTTBRUKER) = Metadata(
    tidspunkt = Instant.parse(tidspunkt),
    utfoertAv = Bruker(type = brukerType, id = IDENTITETSNUMMER, sikkerhetsnivaa = "idporten-loa-high"),
    kilde = "test-kilde",
    aarsak = "test-årsak",
    tidspunktFraKilde = null
)

private val opplysninger = OpplysningerOmArbeidssoeker(
    id = UUID.fromString("8e9f0a1b-2c3d-4e4f-8a5b-6c7d8e9f0a1b"),
    metadata = metadata("2025-03-14T09:30:00.123Z"),
    utdanning = Utdanning(nus = "4", bestaatt = JaNeiVetIkke.JA, godkjent = null),
    helse = Helse(helsetilstandHindrerArbeid = JaNeiVetIkke.NEI),
    jobbsituasjon = Jobbsituasjon(
        beskrivelser = listOf(
            JobbsituasjonMedDetaljer(
                beskrivelse = JobbsituasjonBeskrivelse.HAR_BLITT_SAGT_OPP,
                detaljer = mapOf("siste_dag_med_loenn_iso8601" to "2025-02-28")
            )
        )
    ),
    annet = Annet(andreForholdHindrerArbeid = JaNeiVetIkke.VET_IKKE)
)

private val eksempler: List<Pair<String, TilstandV1>> = listOf(
    "tilstand-startet" to TilstandV1(
        hendelseScope = HendelseScope(key = -1001L, id = 1001L, partition = 3, offset = 4711L),
        gjeldeneTilstand = GjeldeneTilstand.STARTET,
        gjeldeneIdentitetsnummer = IDENTITETSNUMMER,
        alleIdentitetsnummer = setOf(IDENTITETSNUMMER, "10987654321"),
        gjeldenePeriode = Periode(
            id = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"),
            identitetsnummer = IDENTITETSNUMMER,
            startet = metadata("2025-03-14T09:26:53.589Z"),
            startetVedOffset = 4700L,
            avsluttet = null,
            avsluttetVedOffset = null
        ),
        forrigePeriode = Periode(
            id = UUID.fromString("4b2c3d4e-5f6a-4b7c-8d8e-9f0a1b2c3d4e"),
            identitetsnummer = IDENTITETSNUMMER,
            startet = metadata("2024-01-01T08:00:00Z"),
            startetVedOffset = 100L,
            avsluttet = metadata("2024-06-01T08:00:00.5Z", BrukerType.SYSTEM).copy(
                tidspunktFraKilde = TidspunktFraKilde(
                    tidspunkt = Instant.parse("2024-05-31T22:00:00Z"),
                    avviksType = AvviksType.TIDSPUNKT_KORRIGERT
                )
            ),
            avsluttetVedOffset = 200L
        ),
        sisteOpplysningerOmArbeidssoeker = opplysninger,
        forrigeOpplysningerOmArbeidssoeker = null
    ),
    "tilstand-avsluttet-uten-perioder" to TilstandV1(
        hendelseScope = HendelseScope(key = -1002L, id = 1002L, partition = 0, offset = 0L),
        gjeldeneTilstand = GjeldeneTilstand.AVSLUTTET,
        gjeldeneIdentitetsnummer = IDENTITETSNUMMER,
        alleIdentitetsnummer = setOf(IDENTITETSNUMMER),
        gjeldenePeriode = null,
        forrigePeriode = null,
        sisteOpplysningerOmArbeidssoeker = null,
        forrigeOpplysningerOmArbeidssoeker = null
    )
)
