package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.antallLagredeOpplysningerSumPerPeriode
import no.nav.paw.arbeidssoekerregisteret.config.antallLagredeOpplysningerTotal
import no.nav.paw.arbeidssoekerregisteret.config.buildOpplysningerOmArbeidssoekerAvroSerde
import no.nav.paw.arbeidssoekerregisteret.config.tellMottatteOpplysninger
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.config.kafka.streams.Punctuation
import no.nav.paw.config.kafka.streams.genericProcess
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.state.TimestampedKeyValueStore
import org.apache.kafka.streams.state.ValueAndTimestamp
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong

context(ApplicationContext)
fun buildOpplysningerTopology(
    meterRegistry: MeterRegistry
): Topology = StreamsBuilder().apply {
    addOpplysningerStateStore()
    addOpplysningerKStream(meterRegistry)
}.build()

context(ApplicationContext)
private fun StreamsBuilder.addOpplysningerStateStore() {
    logger.info("Oppretter state store for opplysninger om arbeidssøker")
    val kafkaProperties = properties.kafka
    val kafkaStreamsProperties = properties.kafkaStreams

    this.addStateStore(
        Stores.timestampedKeyValueStoreBuilder(
            Stores.persistentKeyValueStore(kafkaStreamsProperties.opplysningerStore),
            Serdes.String(),
            buildOpplysningerOmArbeidssoekerAvroSerde(kafkaProperties.schemaRegistry)
        )
    )
}

context(ApplicationContext)
fun StreamsBuilder.addOpplysningerKStream(meterRegistry: MeterRegistry) {
    logger.info("Oppretter KStream for opplysninger om arbeidssøker")
    val kafkaStreamsProperties = properties.kafkaStreams

    this
        .stream<Long, OpplysningerOmArbeidssoeker>(kafkaStreamsProperties.opplysningerTopic)
        .peek { key, _ ->
            logger.debug("Mottok event på {} med key {}", kafkaStreamsProperties.opplysningerTopic, key)
            meterRegistry.tellMottatteOpplysninger()
        }.genericProcess<Long, OpplysningerOmArbeidssoeker, Long, OpplysningerOmArbeidssoeker>(
            name = "processOpplysningerOmArbeidssoeker",
            stateStoreNames = arrayOf(kafkaStreamsProperties.opplysningerStore),
            punctuation = buildPunctuation(meterRegistry)
        ) { record ->
            val opplysninger = record.value()
            val stateStore: TimestampedKeyValueStore<String, OpplysningerOmArbeidssoeker> =
                getStateStore(kafkaStreamsProperties.opplysningerStore)
            val lagretTidspunkt = Instant.now()
            logger.info(
                "Lagrer opplysninger {} for periode {} med tidspunkt {}",
                opplysninger.id,
                opplysninger.periodeId,
                lagretTidspunkt
            )
            stateStore.put(
                opplysninger.id.toString(),
                ValueAndTimestamp.make(opplysninger, lagretTidspunkt.toEpochMilli())
            )
        }
}

context(ApplicationContext)
private fun buildPunctuation(meterRegistry: MeterRegistry): Punctuation<Long, OpplysningerOmArbeidssoeker> {
    val kafkaStreamsProperties = properties.kafkaStreams
    logger.info(
        "Oppretter Punctuation for opplysninger om arbeidssøker til å kjøre hvert {}m",
        kafkaStreamsProperties.opplysningerPunctuatorSchedule.toMinutes()
    )

    return Punctuation(
        kafkaStreamsProperties.opplysningerPunctuatorSchedule, PunctuationType.WALL_CLOCK_TIME
    ) { timestamp, context ->
        logger.info("Punctuation kjører for tidspunkt {}", timestamp)

        with(context) {
            val antallTotalt = AtomicLong(0)
            val histogram = mutableMapOf<UUID, AtomicLong>()

            val stateStore: TimestampedKeyValueStore<String, OpplysningerOmArbeidssoeker> =
                getStateStore(kafkaStreamsProperties.opplysningerStore)
            for (keyValue in stateStore.all()) {
                antallTotalt.incrementAndGet()

                val opplysninger = keyValue.value.value()
                val lagretTidspunkt = Instant.ofEpochMilli(keyValue.value.timestamp())
                val utloepTidspunkt = timestamp.minus(kafkaStreamsProperties.opplysningerLagretTidsperiode)
                if (utloepTidspunkt.isAfter(lagretTidspunkt)
                ) {
                    logger.info(
                        "Sletter opplysninger {} for periode {} fordi de har vært lagret mer enn {}m (utløp {} > lagret {})",
                        opplysninger.id,
                        opplysninger.periodeId,
                        kafkaStreamsProperties.opplysningerLagretTidsperiode.toMinutes(),
                        utloepTidspunkt,
                        lagretTidspunkt
                    )
                    stateStore.delete(opplysninger.id.toString())
                    continue
                } else {
                    logger.debug(
                        "Opplysninger {} for periode {} har vært lagret mindre enn {}m (utløp {} < lagret {})",
                        opplysninger.id,
                        opplysninger.periodeId,
                        kafkaStreamsProperties.opplysningerLagretTidsperiode.toMinutes(),
                        utloepTidspunkt,
                        lagretTidspunkt
                    )
                }

                val antall = histogram[opplysninger.periodeId]
                if (antall != null) {
                    antall.incrementAndGet()
                    histogram[opplysninger.periodeId] = antall
                } else {
                    histogram[opplysninger.periodeId] = AtomicLong(1)
                }
            }

            histogram.forEach { (_, antall) -> meterRegistry.antallLagredeOpplysningerSumPerPeriode(timestamp, antall) }
            meterRegistry.antallLagredeOpplysningerTotal(antallTotalt)
        }
    }
}
