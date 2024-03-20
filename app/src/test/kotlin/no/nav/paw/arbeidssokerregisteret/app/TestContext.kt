package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.STILLING_STYRK08
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*

class TestContext(private val producer: KafkaProducer<Long, Hendelse>, private val topic: String) {

    fun start(id: Long, identitetsnummer: String) {
        producer.send(
            ProducerRecord(
                topic,
                identitetsnummer.hashCode().toLong(),
                Startet(
                    hendelseId = UUID.randomUUID(),
                    id = id,
                    identitetsnummer = identitetsnummer,
                    metadata = Metadata(
                        tidspunkt = Instant.now(),
                        utfoertAv = Bruker(BrukerType.SLUTTBRUKER, "test"),
                        kilde = "unit-test",
                        aarsak = "tester"
                    )
                )
            )
        )
            .get()
    }

    fun opplysningerMottattPermitertOgDeltidsJobb(id: Long, identitetsnummer: String) {
        producer.send(
            ProducerRecord(
                topic,
                identitetsnummer.hashCode().toLong(),
                OpplysningerOmArbeidssoekerMottatt(
                    hendelseId = UUID.randomUUID(),
                    id = id,
                    identitetsnummer = identitetsnummer,
                    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
                        metadata = Metadata(
                            tidspunkt = Instant.now(),
                            utfoertAv = Bruker(BrukerType.SLUTTBRUKER, "test"),
                            kilde = "unit-test",
                            aarsak = "tester"
                        ),
                        jobbsituasjon = Jobbsituasjon(
                            beskrivelser = listOf(
                                JobbsituasjonMedDetaljer(
                                    beskrivelse = JobbsituasjonBeskrivelse.ER_PERMITTERT,
                                    detaljer = mapOf(
                                        PROSENT to "100",
                                        STILLING_STYRK08 to "123"
                                    )
                                ),
                                JobbsituasjonMedDetaljer(
                                    beskrivelse = JobbsituasjonBeskrivelse.DELTIDSJOBB_VIL_MER,
                                    detaljer = mapOf(
                                        "prosent" to "25",
                                        "stillingStyrk08" to "124"
                                    )
                                )
                            )
                        ),
                        annet = Annet(JaNeiVetIkke.NEI),
                        utdanning = Utdanning(
                            nus = "7",
                            bestaatt = JaNeiVetIkke.JA,
                            godkjent = JaNeiVetIkke.JA
                        ),
                        helse = Helse(
                            helsetilstandHindrerArbeid = JaNeiVetIkke.NEI
                        ),
                        id = UUID.randomUUID()
                    )
                )
            )
        )
            .get()
    }

    fun opplysningerMottattPermitert(id: Long, identitietsnummer: String) {
        producer.send(
            ProducerRecord(
                topic,
                identitietsnummer.hashCode().toLong(),
                OpplysningerOmArbeidssoekerMottatt(
                    hendelseId = UUID.randomUUID(),
                    id = id,
                    identitetsnummer = identitietsnummer,
                    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
                        metadata = Metadata(
                            tidspunkt = Instant.now(),
                            utfoertAv = Bruker(BrukerType.SLUTTBRUKER, "test"),
                            kilde = "unit-test",
                            aarsak = "tester"
                        ),
                        jobbsituasjon = Jobbsituasjon(
                            beskrivelser = listOf(
                                JobbsituasjonMedDetaljer(
                                    beskrivelse = JobbsituasjonBeskrivelse.ER_PERMITTERT,
                                    detaljer = mapOf(
                                        PROSENT to "100",
                                        STILLING_STYRK08 to "123"
                                    )
                                )
                            )
                        ),
                        annet = Annet(JaNeiVetIkke.NEI),
                        utdanning = Utdanning(
                            nus = "7",
                            bestaatt = JaNeiVetIkke.JA,
                            godkjent = JaNeiVetIkke.JA
                        ),
                        helse = Helse(
                            helsetilstandHindrerArbeid = JaNeiVetIkke.NEI
                        ),
                        id = UUID.randomUUID()
                    )
                )
            )
        )
            .get()
    }

    fun stop(id: Long, identitetsnummer: String) {
        producer.send(
            ProducerRecord(
                topic,
                identitetsnummer.hashCode().toLong(),
                Avsluttet(
                    hendelseId = UUID.randomUUID(),
                    id = id,
                    identitetsnummer = identitetsnummer,
                    metadata = Metadata(
                        tidspunkt = Instant.now(),
                        utfoertAv = Bruker(BrukerType.SYSTEM, "test"),
                        kilde = "unit-test",
                        aarsak = "tester"
                    )
                )
            )
        )
            .get()
    }
}