package no.nav.paw.arbeidssokerregisteret.app

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.GJELDER_FRA_DATO
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.STILLING
import no.nav.paw.arbeidssokerregisteret.STILLING_STYRK08
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.config.ApplicationLogicConfig
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import no.nav.paw.test.assertEvent
import no.nav.paw.test.days
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType as AvroAvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse.ER_PERMITTERT as API_ER_PERMITTERT
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType as AvroBrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.JA as ApiJa
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.NEI as ApiNei


class ApplikasjonsTest : FreeSpec({
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val opplysningerTilPeriodeVindu = Duration.ofSeconds(60)
    val topology = topology(
        prometheusMeterRegistry = prometheusMeterRegistry,
        builder = opprettStreamsBuilder(dbNavn, tilstandSerde),
        dbNavn = dbNavn,
        innTopic = eventlogTopicNavn,
        periodeTopic = periodeTopicNavn,
        opplysningerOmArbeidssoekerTopic = opplysningerOmArbeidssoekerTopicNavn,
        applicationLogicConfig = ApplicationLogicConfig(
            inkluderOpplysningerInnenforTidsvindu = opplysningerTilPeriodeVindu
        )
    )

    val testDriver = TopologyTestDriver(topology, kafkaStreamProperties)
    val eventlogTopic = testDriver.createInputTopic(
        eventlogTopicNavn,
        Serdes.Long().serializer(),
        hendelseSerde.serializer()
    )
    val periodeTopic = testDriver.createOutputTopic(
        periodeTopicNavn,
        Serdes.Long().deserializer(),
        periodeSerde.deserializer()
    )
    val opplysningerOmArbeidssoekerTopic = testDriver.createOutputTopic(
        opplysningerOmArbeidssoekerTopicNavn,
        Serdes.Long().deserializer(),
        opplysningerOmArbeidssoekerSerde.deserializer()
    )
    val identitetnummer = "12345678901"
    val key = 5034L
    "Verifiser applikasjonsflyt" - {
        val startet = Startet(
            hendelseId = UUID.randomUUID(),
            id = 1L,
            identitetsnummer = identitetnummer,
            metadata = Metadata(
                Instant.now(),
                Bruker(
                    type = BrukerType.SYSTEM,
                    id = "test",
                    sikkerhetsnivaa = null
                ),
                kilde = "unit-test",
                aarsak = "tester",
                tidspunktFraKilde = TidspunktFraKilde(
                    Instant.parse("2024-03-07T15:41:01Z"),
                    AvviksType.FORSINKELSE
                )
            )
        )
        "Når vi mottater en avvist hendelse skjer det ikke noe" {
            eventlogTopic.pipeInput(key, Avvist(
                hendelseId = UUID.randomUUID(),
                id = 1L,
                identitetsnummer = identitetnummer,
                metadata = startet.metadata
            ))
            periodeTopic.isEmpty shouldBe true
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe true
        }
        "Når vi mottar opplysninger uten aktiv periode skal det ikke skje noe" {
            eventlogTopic.pipeInput(key, opplysningerMottatt(1L, identitetnummer, Instant.now()
                .minus(opplysningerTilPeriodeVindu.multipliedBy(2))))
            periodeTopic.isEmpty shouldBe true
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe true
        }
        "Når vi mottar en 'startet' hendelse etter at vinduet har passert og ikke har noen tidligere tilstand skal vi opprette en ny periode" {
            eventlogTopic.pipeInput(key, startet)
            val periode = periodeTopic.readKeyValue()
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe true
            verifiserPeriodeOppMotStartetOgStoppetHendelser(
                forventetKafkaKey = key,
                startet = startet,
                avsluttet = null,
                mottattRecord = periode
            )
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe true
        }

        "Når vi mottar en 'startet' hendelse for en person med en aktiv periode skal det ikke skje noe" {
            val duplikatStart = Startet(
                hendelseId = UUID.randomUUID(),
                id = 1L,
                identitetsnummer = identitetnummer,
                metadata = Metadata(
                    tidspunkt = Instant.now(),
                    utfoertAv = Bruker(type = BrukerType.SYSTEM, id = "test", sikkerhetsnivaa = null),
                    kilde = "unit-test",
                    aarsak = "tester"
                )
            )
            eventlogTopic.pipeInput(key, duplikatStart)
            periodeTopic.isEmpty shouldBe true
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe true
        }
        val feilrettetStart = Startet(
            hendelseId = UUID.randomUUID(),
            id = 1L,
            identitetsnummer = identitetnummer,
            metadata = Metadata(
                tidspunkt = Instant.now(),
                utfoertAv = Bruker(type = BrukerType.VEILEDER, id = "en_som_fikser_ting", sikkerhetsnivaa = null),
                kilde = "unit-test2",
                aarsak = "teknisk feil i systemet",
                tidspunktFraKilde = TidspunktFraKilde(
                    startet.metadata.tidspunkt - 10.days,
                    AvviksType.TIDSPUNKT_KORRIGERT
                )
            )
        )
        "Når vi mottar en 'startet' med korrigert tidspunkt for en akrive periode skal vi oppdatere perioden" {
            eventlogTopic.pipeInput(key, feilrettetStart)
            periodeTopic.isEmpty shouldBe false
            val periodeKv = periodeTopic.readKeyValue()
            val periode = periodeKv.value
            periode.startet.tidspunkt.truncatedTo(ChronoUnit.MILLIS) shouldBe startet.metadata.tidspunkt.truncatedTo(ChronoUnit.MILLIS)
            periode.startet.tidspunktFraKilde?.avviksType shouldBe AvroAvviksType.TIDSPUNKT_KORRIGERT
            periode.startet.tidspunktFraKilde?.tidspunkt?.truncatedTo(ChronoUnit.MILLIS) shouldBe
                    feilrettetStart.metadata.tidspunktFraKilde?.tidspunkt?.truncatedTo(ChronoUnit.MILLIS)
            periode.id shouldBe startet.hendelseId
            periode.identitetsnummer shouldBe startet.identitetsnummer
            periode.startet.utfoertAv.type shouldBe AvroBrukerType.VEILEDER
            periode.startet.utfoertAv.id shouldBe "en_som_fikser_ting"
            periode.startet.aarsak shouldBe "teknisk feil i systemet"
            periode.startet.kilde shouldBe "unit-test2"
            periodeTopic.isEmpty shouldBe true
        }

        "Når vi mottar en ny situsjon for en person med en aktiv periode skal vi sende ut en ny situasjon" {
            val situsjonMottat = opplysningerMottatt(1L, identitetnummer, Instant.now())
            eventlogTopic.pipeInput(key, situsjonMottat)
            periodeTopic.isEmpty shouldBe true
            val situasjon = opplysningerOmArbeidssoekerTopic.readKeyValue()
            verifiserApiMetadataMotInternMetadata(situsjonMottat.metadata, situasjon.value.sendtInnAv)
            situasjon.key shouldBe key
            situasjon.value.periodeId shouldBe startet.hendelseId
            situasjon.value.utdanning.bestaatt shouldBe ApiJa
            situasjon.value.utdanning.godkjent shouldBe ApiNei
            situasjon.value.utdanning.nus shouldBe "7"
            situasjon.value.jobbsituasjon.beskrivelser.size shouldBe 1
            situasjon.value.annet.andreForholdHindrerArbeid shouldBe ApiJa
            with(situasjon.value.jobbsituasjon) {
                with(beskrivelser.firstOrNull { it.beskrivelse == API_ER_PERMITTERT }) {
                    this.shouldNotBeNull()
                    detaljer?.get(PROSENT) shouldBe "100"
                    detaljer?.get(GJELDER_FRA_DATO) shouldBe "2020-01-02"
                    detaljer?.get(STILLING) shouldBe "Lærer"
                    detaljer?.get(STILLING_STYRK08) shouldBe "2320"
                }
            }
        }

        "Når vi mottat en 'stoppet' hendelse for en person med en aktiv periode, men id i avsluttet er forskjellig fra periodeId skal det ikke skje noe" {
            val stoppet = Avsluttet(
                hendelseId = UUID.randomUUID(),
                id = 1L,
                identitetsnummer = identitetnummer,
                metadata = Metadata(
                    Instant.now(),
                    Bruker(BrukerType.SYSTEM, "test", null),
                    "unit-test",
                    "tester"
                ),
                periodeId = UUID.randomUUID()
            )
            eventlogTopic.pipeInput(key, stoppet)
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe true
            periodeTopic.isEmpty shouldBe true
        }

        "Når vi mottat en 'stoppet' hendelse for en person med en aktiv periode skal vi avslutte perioden" {
            val stoppet = Avsluttet(
                hendelseId = UUID.randomUUID(),
                id = 1L,
                identitetsnummer = identitetnummer,
                metadata = Metadata(
                    Instant.now(),
                    Bruker(BrukerType.SYSTEM, "test", null),
                    "unit-test",
                    "tester"
                )
            )
            eventlogTopic.pipeInput(key, stoppet)
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe true
            periodeTopic.isEmpty shouldBe false
            val avsluttetPeriode = periodeTopic.readKeyValue()
            verifiserPeriodeOppMotStartetOgStoppetHendelser(
                forventetKafkaKey = key,
                startet = feilrettetStart.copy(
                    metadata = feilrettetStart.metadata.copy(
                        tidspunkt = startet.metadata.tidspunkt,
                    ),
                    hendelseId = startet.hendelseId
                ),
                avsluttet = stoppet,
                mottattRecord = avsluttetPeriode
            )
        }
        val opplysningerTidspunkt = Instant.now()
        val situsjonMottat = OpplysningerOmArbeidssoekerMottatt(
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetnummer,
            id = 1L,
            opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
                id = UUID.randomUUID(),
                metadata = Metadata(
                    opplysningerTidspunkt,
                    Bruker(BrukerType.SYSTEM, "test", null),
                    "unit-test",
                    "tester"
                ),
                utdanning = Utdanning(
                    nus = "4",
                    bestaatt = JaNeiVetIkke.JA,
                    godkjent = JaNeiVetIkke.NEI
                ),
                helse = Helse(JaNeiVetIkke.JA),
                jobbsituasjon = Jobbsituasjon(
                    mutableListOf(
                        JobbsituasjonMedDetaljer(
                            beskrivelse = JobbsituasjonBeskrivelse.ER_PERMITTERT,
                            detaljer = mapOf(
                                PROSENT to "100",
                                GJELDER_FRA_DATO to "2020-01-02",
                                STILLING to "Lærer",
                                STILLING_STYRK08 to "2320"
                            )
                        )
                    )
                ),
                annet = Annet(andreForholdHindrerArbeid = JaNeiVetIkke.NEI)
            )
        )
        "Når vi mottar en ny situasjon etter at siste periode er avsluttet skal det ikke skje noe" {
            eventlogTopic.pipeInput(key, situsjonMottat)
            periodeTopic.isEmpty shouldBe true
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe true
        }

        val periodeId2: UUID = UUID.randomUUID()
        "Når vi mottar en 'startet' innenfor tidsvinduet for opplysninger og forrige periode er avsluttet skal vi opprette en ny periode og sende ut opplysningene" {
            val startet2 = Startet(
                hendelseId = periodeId2,
                id = 1L,
                identitetsnummer = identitetnummer,
                metadata = Metadata(
                    tidspunkt = opplysningerTidspunkt.plus(opplysningerTilPeriodeVindu.minusSeconds(1)),
                    utfoertAv = Bruker(BrukerType.SLUTTBRUKER, "123456788901", null),
                    kilde = "unit-test",
                    aarsak = "tester"
                )
            )
            eventlogTopic.pipeInput(key, startet2)
            val periode = periodeTopic.readKeyValue()
            verifiserPeriodeOppMotStartetOgStoppetHendelser(
                forventetKafkaKey = key,
                startet = startet2,
                avsluttet = null,
                mottattRecord = periode
            )
            periode.value.id shouldBe periodeId2
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe false
            val opplysninger = opplysningerOmArbeidssoekerTopic.readKeyValue()
            verifiserApiMetadataMotInternMetadata(situsjonMottat.metadata, opplysninger.value.sendtInnAv)
            opplysninger.key shouldBe key
            opplysninger.value.periodeId shouldBe periodeId2
            opplysninger.value.utdanning.bestaatt shouldBe ApiJa
            opplysninger.value.utdanning.godkjent shouldBe ApiNei
            opplysninger.value.utdanning.nus shouldBe "4"
            opplysninger.value.jobbsituasjon.beskrivelser.size shouldBe 1
            opplysninger.value.annet.andreForholdHindrerArbeid shouldBe ApiNei
            with(opplysninger.value.jobbsituasjon) {
                with(beskrivelser.firstOrNull { it.beskrivelse == API_ER_PERMITTERT }) {
                    this.shouldNotBeNull()
                    detaljer?.get(PROSENT) shouldBe "100"
                    detaljer?.get(GJELDER_FRA_DATO) shouldBe "2020-01-02"
                    detaljer?.get(STILLING) shouldBe "Lærer"
                    detaljer?.get(STILLING_STYRK08) shouldBe "2320"
                }
            }

        }

        "Når vi avsluttet periode med riktig periodeId blir den avsluttet" {
            val stoppet = Avsluttet(
                hendelseId = UUID.randomUUID(),
                id = 1L,
                identitetsnummer = identitetnummer,
                metadata = Metadata(
                    Instant.now(),
                    Bruker(BrukerType.SYSTEM, "test", null),
                    "unit-test",
                    "tester"
                ),
                periodeId = periodeId2
            )
            eventlogTopic.pipeInput(key, stoppet)
            opplysningerOmArbeidssoekerTopic.isEmpty shouldBe true
            periodeTopic.isEmpty shouldBe false
            val avsluttetPeriode = periodeTopic.readKeyValue()
            avsluttetPeriode.value.id shouldBe periodeId2
            verifiserApiMetadataMotInternMetadata(
                forventedeMetadataVerdier = stoppet.metadata,
                mottattApiMetadata = avsluttetPeriode.value.avsluttet
            )
        }

        "Når vi mottar IdentitetsnummerSammenslatt hendelse også en periode startet hendelse skal det ikke skje noe" {
            periodeTopic.isEmpty shouldBe true
            val identitetsnummerSammenslaatt = IdentitetsnummerSammenslaatt(
                id = 1L,
                hendelseId = UUID.randomUUID(),
                identitetsnummer = identitetnummer,
                metadata = Metadata(
                    Instant.now(),
                    Bruker(BrukerType.SYSTEM, "test", null),
                    "unit-test",
                    "tester"
                ),
                flyttedeIdentitetsnumre = setOf(identitetnummer, "12345678902", "12345678903"),
                flyttetTilArbeidssoekerId = 2L
            )
            eventlogTopic.pipeInput(key, identitetsnummerSammenslaatt)
            val periode = Startet(
                hendelseId = periodeId2,
                id = 1L,
                identitetsnummer = identitetnummer,
                metadata = Metadata(
                    tidspunkt = opplysningerTidspunkt.plus(opplysningerTilPeriodeVindu.minusSeconds(1)),
                    utfoertAv = Bruker(BrukerType.SLUTTBRUKER, "123456788901", "idporten-loa-high"),
                    kilde = "unit-test",
                    aarsak = "tester"
                )
            )
            eventlogTopic.pipeInput(key, periode)
            periodeTopic.isEmpty shouldBe true
        }
        "Når vi har en aktiv periode og mottar IdentitetsnummerSammenslatt hendelse skal perioden avsluttes" {
            periodeTopic.isEmpty shouldBe true
            val periode = Startet(
                hendelseId = UUID.randomUUID(),
                id = 5L,
                identitetsnummer = identitetnummer,
                metadata = Metadata(
                    tidspunkt = opplysningerTidspunkt.plus(opplysningerTilPeriodeVindu.minusSeconds(1)),
                    utfoertAv = Bruker(BrukerType.SLUTTBRUKER, "123456788901", "idporten-loa-high"),
                    kilde = "unit-test",
                    aarsak = "tester"
                )
            )
            eventlogTopic.pipeInput(key, periode)
            periodeTopic.assertEvent<Periode>()
            val identitetsnummerSammenslaatt = IdentitetsnummerSammenslaatt(
                id = 5L,
                hendelseId = UUID.randomUUID(),
                identitetsnummer = identitetnummer,
                metadata = Metadata(
                    Instant.now(),
                    Bruker(BrukerType.SYSTEM, "test", null),
                    "unit-test",
                    "tester"
                ),
                flyttedeIdentitetsnumre = setOf(identitetnummer, "12345678902", "12345678903"),
                flyttetTilArbeidssoekerId = 2L
            )
            eventlogTopic.pipeInput(key, identitetsnummerSammenslaatt)
            periodeTopic.isEmpty shouldBe false
        }
    }
})


fun opplysningerMottatt(id: Long, identitetnummer: String, timestamp: Instant) =
    OpplysningerOmArbeidssoekerMottatt(
        hendelseId = UUID.randomUUID(),
        identitetsnummer = identitetnummer,
        id = id,
        opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
            id = UUID.randomUUID(),
            metadata = Metadata(
                timestamp,
                Bruker(BrukerType.SYSTEM, "test", null),
                "unit-test",
                "tester"
            ),
            utdanning = Utdanning(
                nus = "7",
                bestaatt = JaNeiVetIkke.JA,
                godkjent = JaNeiVetIkke.NEI
            ),
            helse = Helse(JaNeiVetIkke.JA),
            jobbsituasjon = Jobbsituasjon(
                mutableListOf(
                    JobbsituasjonMedDetaljer(
                        beskrivelse = JobbsituasjonBeskrivelse.ER_PERMITTERT, detaljer = mutableMapOf(
                            PROSENT to "100",
                            GJELDER_FRA_DATO to "2020-01-02",
                            STILLING to "Lærer",
                            STILLING_STYRK08 to "2320"
                        )
                    )
                )
            ),
            annet = Annet(andreForholdHindrerArbeid = JaNeiVetIkke.JA)
        )
    )