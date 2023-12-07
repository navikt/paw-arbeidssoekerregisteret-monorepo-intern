package no.nav.paw.arbeidssokerregisteret.app

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.config.KafkaKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.app.helse.Helse
import no.nav.paw.arbeidssokerregisteret.app.helse.initKtor
import no.nav.paw.arbeidssokerregisteret.app.metrics.*
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDbKeyValueBytesStoreSupplier
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

const val kafkaKonfigurasjonsfil = "kafka_konfigurasjon.toml"

typealias StreamHendelse = Hendelse

fun main() {
    val streamLogger = LoggerFactory.getLogger("App")
    streamLogger.info("Starter applikasjon...")
    val kafkaKonfigurasjon = lastKonfigurasjon<KafkaKonfigurasjon>(kafkaKonfigurasjonsfil)
    val tilstandSerde: Serde<Tilstand> = TilstandSerde()
    val dbNavn = kafkaKonfigurasjon.streamKonfigurasjon.tilstandsDatabase
    val strømBygger = StreamsBuilder()
    strømBygger.addStateStore(
        KeyValueStoreBuilder(
            RocksDbKeyValueBytesStoreSupplier(dbNavn, false),
            Serdes.Long(),
            tilstandSerde,
            Time.SYSTEM
        )
    )
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val topology = topology(
        prometheusMeterRegistry = prometheusMeterRegistry,
        builder = strømBygger,
        dbNavn = dbNavn,
        innTopic = kafkaKonfigurasjon.streamKonfigurasjon.eventlogTopic,
        periodeTopic = kafkaKonfigurasjon.streamKonfigurasjon.periodeTopic,
        opplysningerOmArbeidssoekerTopic = kafkaKonfigurasjon.streamKonfigurasjon.opplysningerOmArbeidssoekerTopic
    )

    val kafkaStreams = KafkaStreams(topology, StreamsConfig(kafkaKonfigurasjon.properties))
    fun stateStore(): ReadOnlyKeyValueStore<Long, Tilstand> =  kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            dbNavn,
            QueryableStoreTypes.keyValueStore()
        )
    )
    val keepGoing = AtomicBoolean(true)
    val metricsTask = initStateGaugeTask(
        keepGoing = keepGoing,
        registry = prometheusMeterRegistry,
        streamStateSupplier = kafkaStreams::state,
        contentSupplier = { stateStore().all().asSequence().map { it.value } },
        mapper = ::withMetricsInfoMapper
    )
    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        streamLogger.error("Uventet feil", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }
    kafkaStreams.start()
    val helse = Helse(kafkaStreams)
    initKtor(
        prometheusRegistry = prometheusMeterRegistry,
        helse = helse
    ).start(wait = true)
    keepGoing.set(false)
    metricsTask.cancel(true)
    streamLogger.info("Avsluttet")
}

