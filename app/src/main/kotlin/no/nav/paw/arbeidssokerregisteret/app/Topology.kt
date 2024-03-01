package no.nav.paw.arbeidssokerregisteret.app

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.config.ApplicationLogicConfig
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.genererNyInternTilstandOgNyeApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.ignorerDuplikatStartOgStopp
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.MeteredOutboundTopicNameExtractor
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lagreInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lastInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.tellHendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream

//Endring av denne verdien eller funksjonen 'publicTopicKeyFunction' krever replay av eventlog til nye topics!!
const val PUBLIC_KEY_MODULO_VALUE = 7_500
fun publicTopicKeyFunction(internalKey: Long): Long =
    "internal_key_$internalKey".hashCode().toLong() % PUBLIC_KEY_MODULO_VALUE

fun topology(
    applicationLogicConfig: ApplicationLogicConfig,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    builder: StreamsBuilder,
    dbNavn: String,
    innTopic: String,
    periodeTopic: String,
    opplysningerOmArbeidssoekerTopic: String
): Topology {
    val strøm: KStream<Long, Hendelse> = builder.stream(innTopic, Consumed.with(Serdes.Long(), HendelseSerde()))
    val meteredTopicExtractor =
        MeteredOutboundTopicNameExtractor(periodeTopic, opplysningerOmArbeidssoekerTopic, prometheusMeterRegistry)
    with(prometheusMeterRegistry) {
        strøm
            .peek { _, hendelse -> tellHendelse(innTopic, hendelse) }
            .lastInternTilstand(dbNavn)
            .filter(::ignorerDuplikatStartOgStopp)
            .mapValues { internTilstandOgHendelse ->
                genererNyInternTilstandOgNyeApiTilstander(
                    applicationLogicConfig,
                    internTilstandOgHendelse
                )
            }
            .lagreInternTilstand(dbNavn)
            .flatMap { key, value ->
                val publicKey = publicTopicKeyFunction(key)
                listOfNotNull(
                    value.nyPeriodeTilstand?.let { KeyValue(publicKey, it as SpecificRecord) },
                    value.nyOpplysningerOmArbeidssoekerTilstand?.let { KeyValue(publicKey, it as SpecificRecord) }
                )
            }.to(meteredTopicExtractor)
        return builder.build()
    }
}


