package no.nav.paw.arbeidssokerregisteret.app

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.config.ApplicationLogicConfig
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Aarsaksinformasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvsluttetAarsakType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.RegelEvalResultat
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Duration
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType as ApiAvsluttetAarsakType

class AvsluttingMedAarsakTest : FreeSpec({

    val testDriver = TopologyTestDriver(
        topology(
            prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            builder = opprettStreamsBuilder(dbNavn, tilstandSerde),
            dbNavn = dbNavn,
            innTopic = eventlogTopicNavn,
            periodeTopic = periodeTopicNavn,
            opplysningerOmArbeidssoekerTopic = opplysningerOmArbeidssoekerTopicNavn,
            applicationLogicConfig = ApplicationLogicConfig(
                inkluderOpplysningerInnenforTidsvindu = Duration.ofSeconds(60)
            )
        ),
        kafkaStreamProperties
    )

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

    fun startPeriode(id: Long, key: Long, identitetsnummer: String) {
        eventlogTopic.pipeInput(
            key,
            Startet(
                hendelseId = UUID.randomUUID(),
                id = id,
                identitetsnummer = identitetsnummer,
                metadata = Metadata(
                    tidspunkt = Instant.now(),
                    utfoertAv = Bruker(BrukerType.SLUTTBRUKER, identitetsnummer, "idporten-loa-high"),
                    kilde = "unit-test",
                    aarsak = "tester"
                )
            )
        )
        periodeTopic.readKeyValue() // konsumer periode-startet-melding
    }

    fun avsluttPeriode(id: Long, key: Long, identitetsnummer: String, aarsak: Aarsaksinformasjon?) {
        eventlogTopic.pipeInput(
            key,
            Avsluttet(
                hendelseId = UUID.randomUUID(),
                id = id,
                identitetsnummer = identitetsnummer,
                metadata = Metadata(
                    tidspunkt = Instant.now(),
                    utfoertAv = Bruker(BrukerType.SYSTEM, "system", null),
                    kilde = "unit-test",
                    aarsak = "tester"
                ),
                aarsaksInformasjon = aarsak
            )
        )
    }

    "Periode.avslutningsInfo skal inneholde riktig årsak for alle avslutningsårsaker" - {

        "SVARTE_NEI_I_BEKREFTELSE skal gi SVARTE_NEI_I_BEKREFTELSE i Periode-melding" {
            val key = 1001L
            startPeriode(1L, key, "11111111111")
            avsluttPeriode(1L, key, "11111111111", Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE,
                regelEvalResultat = RegelEvalResultat.IKKE_RELEVANT
            ))
            val periode = periodeTopic.readKeyValue()
            periode.value.avslutningsInfo.shouldNotBeNull()
            periode.value.avslutningsInfo.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE
        }

        "BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST skal gi BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST i Periode-melding" {
            val key = 1002L
            startPeriode(2L, key, "22222222222")
            avsluttPeriode(2L, key, "22222222222", Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST,
                regelEvalResultat = RegelEvalResultat.IKKE_RELEVANT
            ))
            val periode = periodeTopic.readKeyValue()
            periode.value.avslutningsInfo.shouldNotBeNull()
            periode.value.avslutningsInfo.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
        }

        "FEILREGISTRERING skal gi UDEFINERT i Periode-melding (skjules eksternt)" {
            val key = 1003L
            startPeriode(3L, key, "33333333333")
            avsluttPeriode(3L, key, "33333333333", Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.FEILREGISTRERING,
                regelEvalResultat = RegelEvalResultat.IKKE_RELEVANT
            ))
            val periode = periodeTopic.readKeyValue()
            periode.value.avslutningsInfo.shouldNotBeNull()
            periode.value.avslutningsInfo.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.UDEFINERT
        }

        "UDEFINERT skal gi UDEFINERT i Periode-melding" {
            val key = 1004L
            startPeriode(4L, key, "44444444444")
            avsluttPeriode(4L, key, "44444444444", Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.UDEFINERT,
                regelEvalResultat = RegelEvalResultat.IKKE_RELEVANT
            ))
            val periode = periodeTopic.readKeyValue()
            periode.value.avslutningsInfo.shouldNotBeNull()
            periode.value.avslutningsInfo.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.UDEFINERT
        }

        "UKJENT_VERDI skal gi UKJENT_VERDI i Periode-melding" {
            val key = 1005L
            startPeriode(5L, key, "55555555555")
            avsluttPeriode(5L, key, "55555555555", Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.UKJENT_VERDI,
                regelEvalResultat = RegelEvalResultat.UKJENT_VERDI
            ))
            val periode = periodeTopic.readKeyValue()
            periode.value.avslutningsInfo.shouldNotBeNull()
            periode.value.avslutningsInfo.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.UKJENT_VERDI
        }

        "Null aarsaksInformasjon skal gi null avslutningsInfo i Periode-melding" {
            val key = 1006L
            startPeriode(6L, key, "66666666666")
            avsluttPeriode(6L, key, "66666666666", aarsak = null)
            val periode = periodeTopic.readKeyValue()
            periode.value.avslutningsInfo.shouldBeNull()
        }
    }
})
