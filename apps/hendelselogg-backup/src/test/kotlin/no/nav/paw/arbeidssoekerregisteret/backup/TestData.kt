package no.nav.paw.arbeidssoekerregisteret.backup

import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.HendelseMetadata
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.HendelseMetadataTidspunktFraKilde
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.HendelseMetadataUtfoertAv
import no.nav.paw.arbeidssoekerregisteret.backup.vo.StoredData
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random
import kotlin.random.Random.Default.nextLong

fun hendelser(): Sequence<Hendelse> {
    return sequence {
        while (true) {
            yieldAll(
                listOf(
                    startet(),
                    opplysninger(),
                    avsluttet()
                )
            )
        }
    }
}


fun startet(
    identitetsnummer: String = nextLong(10000000000, 10002000000).toString(),
    id: Long = nextLong(0, 20),
    timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
): Startet = Startet(
    hendelseId = UUID.randomUUID(),
    id = id,
    identitetsnummer = identitetsnummer,
    metadata = metadata(timestamp)
)

fun avvist(
    identitetsnummer: String = nextLong(10000000000, 10002000000).toString(),
    id: Long = nextLong(0, 20),
    timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
): Avvist = Avvist(
    hendelseId = UUID.randomUUID(),
    id = id,
    identitetsnummer = identitetsnummer,
    metadata = metadata(timestamp)
)

fun avsluttet(
    identitetsnummer: String = nextLong(10000000000, 10002000000).toString(),
    id: Long = nextLong(0, 20),
    timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
): Avsluttet = Avsluttet(
    hendelseId = UUID.randomUUID(),
    id = id,
    identitetsnummer = identitetsnummer,
    metadata = metadata(timestamp)
)

fun opplysninger(
    identitetsnummer: String = nextLong(10000000000, 10002000000).toString(),
    id: Long = nextLong(0, 20),
    timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
): OpplysningerOmArbeidssoekerMottatt = OpplysningerOmArbeidssoekerMottatt(
    hendelseId = UUID.randomUUID(),
    id = id,
    identitetsnummer = identitetsnummer,
    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
        id = UUID.randomUUID(),
        metadata = metadata(timestamp),
        utdanning = Utdanning(
            nus = "4",
            bestaatt = JaNeiVetIkke.JA,
            godkjent = JaNeiVetIkke.NEI
        ),
        helse = Helse(JaNeiVetIkke.VET_IKKE),
        jobbsituasjon = Jobbsituasjon(
            beskrivelser = listOf(
                JobbsituasjonMedDetaljer(
                    beskrivelse = JobbsituasjonBeskrivelse.entries.toTypedArray().random(),
                    detaljer = mapOf("1" to "en", "2" to "to"),
                ),
                JobbsituasjonMedDetaljer(
                    beskrivelse = JobbsituasjonBeskrivelse.entries.toTypedArray().random(),
                    detaljer = mapOf("3" to "tre", "4" to "fire"),
                ),
            ),
        ),
        annet = null
    ),
)

fun metadata(
    timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
): Metadata = Metadata(
    tidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
    utfoertAv = Bruker(
        id = nextLong(0, 20).toString(),
        type = BrukerType.entries.toTypedArray().random()
    ),
    kilde = "test",
    aarsak = "tester",
    tidspunktFraKilde = if (Random.nextBoolean()) null else TidspunktFraKilde(
        tidspunkt = timestamp.minusSeconds(20),
        avviksType = AvviksType.entries.toTypedArray().random()
    )
)

fun Hendelse.storedData(
    partition: Int  = 1,
    offset: Long = 1,
    recordKey: Long = 1,
    arbeidssoekerId: Long = 1,
    traceparent: String = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01",
    merged: Boolean = false
) = StoredData(
    partition = 1,
    offset = 1,
    recordKey = 1,
    arbeidssoekerId = 1,
    traceparent = traceparent,
    data = this,
    merged = merged
)

fun StoredData.apiHendelse(): no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Hendelse =
    no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Hendelse(
        hendelseId = data.hendelseId,
        hendelseType = data.hendelseType,
        merged = merged,
        kafkaPartition = partition,
        metadata = HendelseMetadata(
            tidspunkt = data.metadata.tidspunkt,
            utfoertAv = HendelseMetadataUtfoertAv(
                type = data.metadata.utfoertAv.type.name,
                id = data.metadata.utfoertAv.id
            ),
            kilde = data.metadata.kilde,
            aarsak = data.metadata.aarsak,
            tidspunktFraKilde = data.metadata.tidspunktFraKilde?.let {
                HendelseMetadataTidspunktFraKilde(
                    tidspunkt = it.tidspunkt,
                    avvikstype = it.avviksType.name
                )
            }
        ),
        kafkaOffset = offset,
        data = data,
        api = null
    )
