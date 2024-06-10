package no.nav.paw.arbeidssoekerregisteret.backup

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
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


fun startet(): Startet = Startet(
    hendelseId = UUID.randomUUID(),
    id = nextLong(0, 20),
    identitetsnummer = nextLong(10000000000, 10002000000).toString(),
    metadata = metadata()
)

fun avsluttet(): Avsluttet = Avsluttet(
    hendelseId = UUID.randomUUID(),
    id = nextLong(0, 20),
    identitetsnummer = nextLong(10000000000, 10002000000).toString(),
    metadata = metadata()
)

fun opplysninger(): OpplysningerOmArbeidssoekerMottatt = OpplysningerOmArbeidssoekerMottatt(
    hendelseId = UUID.randomUUID(),
    id = nextLong(0, 20),
    identitetsnummer = nextLong(10000000000, 10002000000).toString(),
    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
        id = UUID.randomUUID(),
        metadata = metadata(),
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

fun metadata(): Metadata = Metadata(
    tidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
    utfoertAv = Bruker(
        id = nextLong(0, 20).toString(),
        type = BrukerType.entries.toTypedArray().random()
    ),
    kilde = "test",
    aarsak = "tester",
    tidspunktFraKilde = if (Random.nextBoolean()) null else TidspunktFraKilde(
        tidspunkt = Instant.now().minusSeconds(300).truncatedTo(ChronoUnit.MILLIS),
        avviksType = AvviksType.entries.toTypedArray().random()
    )
)