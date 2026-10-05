package no.nav.paw.arbeidssokerregisteret.intern.v1

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import io.kotest.matchers.comparables.shouldBeLessThan
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Aarsaksinformasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Annet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvsluttetAarsakType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Helse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonMedDetaljer
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.RegelEvalResultat
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Utdanning
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at hendelser skrevet med Jackson 2 kan leses av gjeldende HendelseSerde.
 * JSON-filene er generert med Jackson 2, se [lagretJson].
 */
class HendelseSerdeKompatibilitetTest : FreeSpec({
    val serde = HendelseSerde()

    eksempler.forEach { (navn, forventet) ->
        "$navn skrevet med Jackson 2 kan leses" {
            val json = lagretJson("interne-hendelser/$navn") {
                serde.serializer().serialize("topic", forventet)
            }
            val faktisk = serde.deserializer().deserialize("topic", json)
            faktisk!!.shouldBeEqualUsingFields(forventet)
        }
    }

    "tidspunkt med nanosekunder skrevet med Jackson 2 leses med under 1 µs avvik" {
        // Deserializeren går via readTree, som leser tidspunktet som double.
        // Det gir tap av presisjon under mikrosekund allerede med Jackson 2.
        val forventet = Instant.parse("2025-03-14T09:26:53.589793238Z")
        val hendelse = (eksempler.first().second as Startet).let {
            it.copy(metadata = it.metadata.copy(tidspunkt = forventet))
        }
        val json = lagretJson("interne-hendelser/startet-nanosekunder") {
            serde.serializer().serialize("topic", hendelse)
        }
        val faktisk = serde.deserializer().deserialize("topic", json)!!.metadata.tidspunkt
        Duration.between(forventet, faktisk).abs() shouldBeLessThan Duration.ofNanos(1_000)
    }
})

private const val IDENTITETSNUMMER = "12345678901"
private const val ANNET_IDENTITETSNUMMER = "10987654321"

private val metadata = Metadata(
    tidspunkt = Instant.parse("2025-03-14T09:26:53.589Z"),
    utfoertAv = Bruker(type = BrukerType.VEILEDER, id = "Z991234", sikkerhetsnivaa = "tokenx:Level4"),
    kilde = "test-kilde",
    aarsak = "test-årsak",
    tidspunktFraKilde = TidspunktFraKilde(
        tidspunkt = Instant.parse("2025-03-13T23:00:00.001Z"),
        avviksType = AvviksType.FORSINKELSE
    )
)

private val metadataUtenValgfrieFelt = Metadata(
    tidspunkt = Instant.parse("2025-03-14T09:26:53Z"),
    utfoertAv = Bruker(type = BrukerType.SLUTTBRUKER, id = IDENTITETSNUMMER, sikkerhetsnivaa = null),
    kilde = "test-kilde",
    aarsak = "test-årsak",
    tidspunktFraKilde = null
)

private val eksempler: List<Pair<String, Hendelse>> = listOf(
    "startet" to Startet(
        hendelseId = UUID.fromString("7db3818c-0bcd-4037-9ce4-ec8c2c4bff07"),
        id = 1001,
        identitetsnummer = IDENTITETSNUMMER,
        metadata = metadata,
        opplysninger = setOf(Opplysning.ER_OVER_18_AAR, Opplysning.BOSATT_ETTER_FREG_LOVEN)
    ),
    "startet-uten-valgfrie-felt" to Startet(
        hendelseId = UUID.fromString("7db3818c-0bcd-4037-9ce4-ec8c2c4bff08"),
        id = 1001,
        identitetsnummer = IDENTITETSNUMMER,
        metadata = metadataUtenValgfrieFelt
    ),
    "avsluttet" to Avsluttet(
        hendelseId = UUID.fromString("2f0c5c1e-8a4c-4b8e-9d6f-1c2b3a4d5e6f"),
        id = 1001,
        identitetsnummer = IDENTITETSNUMMER,
        metadata = metadata,
        opplysninger = setOf(Opplysning.DOED),
        periodeId = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"),
        aarsaksInformasjon = Aarsaksinformasjon(
            aarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST,
            regelEvalResultat = RegelEvalResultat.DOED
        )
    ),
    "avsluttet-uten-valgfrie-felt" to Avsluttet(
        hendelseId = UUID.fromString("2f0c5c1e-8a4c-4b8e-9d6f-1c2b3a4d5e70"),
        id = 1001,
        identitetsnummer = IDENTITETSNUMMER,
        metadata = metadataUtenValgfrieFelt
    ),
    "avvist" to Avvist(
        hendelseId = UUID.fromString("5b6c7d8e-9f0a-4b1c-8d2e-3f4a5b6c7d8e"),
        id = 1002,
        identitetsnummer = IDENTITETSNUMMER,
        metadata = metadata,
        opplysninger = setOf(Opplysning.ER_UNDER_18_AAR, Opplysning.IKKE_BOSATT),
        handling = "/api/v2/arbeidssoker/periode"
    ),
    "avvist-stopp-av-periode" to AvvistStoppAvPeriode(
        hendelseId = UUID.fromString("6c7d8e9f-0a1b-4c2d-8e3f-4a5b6c7d8e9f"),
        id = 1003,
        identitetsnummer = IDENTITETSNUMMER,
        metadata = metadata,
        opplysninger = setOf(Opplysning.ANSATT_IKKE_TILGANG)
    ),
    "opplysninger-om-arbeidssoeker-mottatt" to OpplysningerOmArbeidssoekerMottatt(
        hendelseId = UUID.fromString("7d8e9f0a-1b2c-4d3e-8f4a-5b6c7d8e9f0a"),
        id = 1004,
        identitetsnummer = IDENTITETSNUMMER,
        opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
            id = UUID.fromString("8e9f0a1b-2c3d-4e4f-8a5b-6c7d8e9f0a1b"),
            metadata = metadata,
            utdanning = Utdanning(nus = "4", bestaatt = JaNeiVetIkke.JA, godkjent = JaNeiVetIkke.VET_IKKE),
            helse = Helse(helsetilstandHindrerArbeid = JaNeiVetIkke.NEI),
            jobbsituasjon = Jobbsituasjon(
                beskrivelser = listOf(
                    JobbsituasjonMedDetaljer(
                        beskrivelse = JobbsituasjonBeskrivelse.ER_PERMITTERT,
                        detaljer = mapOf("prosent" to "50", "gjelder_fra_dato_iso8601" to "2025-01-01")
                    ),
                    JobbsituasjonMedDetaljer(
                        beskrivelse = JobbsituasjonBeskrivelse.DELTIDSJOBB_VIL_MER,
                        detaljer = emptyMap()
                    )
                )
            ),
            annet = Annet(andreForholdHindrerArbeid = null)
        )
    ),
    "opplysninger-om-arbeidssoeker-mottatt-uten-valgfrie-felt" to OpplysningerOmArbeidssoekerMottatt(
        hendelseId = UUID.fromString("7d8e9f0a-1b2c-4d3e-8f4a-5b6c7d8e9f0b"),
        id = 1004,
        identitetsnummer = IDENTITETSNUMMER,
        opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
            id = UUID.fromString("8e9f0a1b-2c3d-4e4f-8a5b-6c7d8e9f0a1c"),
            metadata = metadataUtenValgfrieFelt,
            utdanning = null,
            helse = null,
            jobbsituasjon = Jobbsituasjon(beskrivelser = emptyList()),
            annet = null
        )
    ),
    "identitetsnummer-sammenslaatt" to IdentitetsnummerSammenslaatt(
        id = 1005,
        hendelseId = UUID.fromString("9f0a1b2c-3d4e-4f5a-8b6c-7d8e9f0a1b2c"),
        identitetsnummer = IDENTITETSNUMMER,
        metadata = metadata,
        flyttedeIdentitetsnumre = setOf(IDENTITETSNUMMER, ANNET_IDENTITETSNUMMER),
        flyttetTilArbeidssoekerId = 2005
    ),
    "arbeidssoeker-id-flettet-inn" to ArbeidssoekerIdFlettetInn(
        identitetsnummer = IDENTITETSNUMMER,
        id = 1006,
        hendelseId = UUID.fromString("0a1b2c3d-4e5f-4a6b-8c7d-8e9f0a1b2c3d"),
        metadata = metadata,
        kilde = Kilde(arbeidssoekerId = 2006, identitetsnummer = setOf(ANNET_IDENTITETSNUMMER))
    ),
    "automatisk-id-merge-ikke-mulig" to AutomatiskIdMergeIkkeMulig(
        identitetsnummer = IDENTITETSNUMMER,
        id = 1007,
        hendelseId = UUID.fromString("1b2c3d4e-5f6a-4b7c-8d8e-9f0a1b2c3d4e"),
        metadata = metadata,
        gjeldeneIdentitetsnummer = IDENTITETSNUMMER,
        pdlIdentitetsnummer = setOf(IDENTITETSNUMMER, ANNET_IDENTITETSNUMMER),
        lokaleAlias = setOf(
            Alias(identitetsnummer = ANNET_IDENTITETSNUMMER, arbeidsoekerId = 2007, recordKey = -3007, partition = 3)
        ),
        perioder = setOf(
            PeriodeRad(
                periodeId = UUID.fromString("2c3d4e5f-6a7b-4c8d-8e9f-0a1b2c3d4e5f"),
                identitetsnummer = ANNET_IDENTITETSNUMMER,
                fra = Instant.parse("2024-01-01T08:00:00.123Z"),
                til = null
            ),
            PeriodeRad(
                periodeId = UUID.fromString("3d4e5f6a-7b8c-4d9e-8f0a-1b2c3d4e5f6a"),
                identitetsnummer = IDENTITETSNUMMER,
                fra = Instant.parse("2023-01-01T08:00:00Z"),
                til = Instant.parse("2023-06-01T08:00:00Z")
            )
        )
    )
)
