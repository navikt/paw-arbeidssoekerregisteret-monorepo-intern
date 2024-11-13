package no.nav.paw.kafkakeymaintenance

import arrow.core.partially1
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.kafkakeygenerator.client.Alias
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.AktorTopologyConfig
import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad
import no.nav.paw.kafkakeymaintenance.perioder.statiskePerioder
import no.nav.paw.test.kafkaStreamProperties
import no.nav.paw.test.opprettSerde
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import java.time.Duration
import java.time.Instant
import java.time.Instant.parse
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun hentAlias(aliasMap: Map<String, List<Alias>>, identitetsnummer: List<String>): List<LokaleAlias> {
    return identitetsnummer.mapNotNull { aliasMap[it]?.let { alias -> LokaleAlias(it, alias) } }
}

fun alias(
    identitetsnummer: String,
    arbeidsoekerId: Long,
    recordKey: Long = arbeidsoekerId,
    partition: Int = (recordKey % 2).toInt()
): Alias {
    return Alias(identitetsnummer, arbeidsoekerId, recordKey, partition)
}

fun initTopologyTestContext(
    startTid: Instant,
    aktorTopologyCfg: AktorTopologyConfig = aktorTopologyConfig
): TopologyTestContext {
    val periodeMap: ConcurrentHashMap<String, PeriodeRad> = ConcurrentHashMap()
    val aliasMap: ConcurrentHashMap<String, List<Alias>> = ConcurrentHashMap()

    val topology = initTopology(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        stateStoreBuilderFactory = Stores::inMemoryKeyValueStore,
        aktorTopologyConfig = aktorTopologyCfg,
        perioder = statiskePerioder(periodeMap),
        hentAlias = ::hentAlias.partially1(aliasMap),
        aktorSerde = opprettSerde()
    )
    val testDriver = TopologyTestDriver(topology, kafkaStreamProperties, startTid)
    val aktorTopic = testDriver.createInputTopic(
        aktorTopologyConfig.aktorTopic,
        Serdes.StringSerde().serializer(),
        opprettSerde<Aktor>().serializer()
    )
    val hendelseloggTopic = testDriver.createOutputTopic(
        aktorTopologyConfig.hendelseloggTopic,
        Serdes.Long().deserializer(),
        HendelseSerde().deserializer()
    )
    return TopologyTestContext(
        perioder = periodeMap,
        alias = aliasMap,
        aktorTopic = aktorTopic,
        hendelseloggTopic = hendelseloggTopic,
        testDriver = testDriver
    )
}

val aktorTopologyConfig
    get() = AktorTopologyConfig(
        aktorTopic = "aktor_topic",
        hendelseloggTopic = "hendelselogg_topic",
        supressionDelayMS = Duration.ofHours(1).toMillis(),
        intervalMS = Duration.ofMinutes(1).toMillis(),
        stateStoreName = "suppression_store"
    )

class TopologyTestContext(
    private val perioder: ConcurrentHashMap<String, PeriodeRad>,
    private val alias: ConcurrentHashMap<String, List<Alias>>,
    val aktorTopic: TestInputTopic<String, Aktor>,
    val hendelseloggTopic: TestOutputTopic<Long, Hendelse>,
    val testDriver: TopologyTestDriver
) {
    fun clearPerioder() {
        perioder.clear()
    }

    fun clearAlias() {
        alias.clear()
    }

    fun addAlias(identitetsnummer: String, vararg alias: Alias) {
        this.alias.compute(identitetsnummer) { _, value ->
            (value ?: emptyList()) + alias
        }
    }

    fun addPeriode(vararg periode: PeriodeRad) {
        periode.forEach { perioder[it.identitetsnummer] = it }
    }
}

operator fun <K, V> KeyValue<K, V>.component1(): K = key
operator fun <K, V> KeyValue<K, V>.component2(): V = value

fun aktor(
    vararg id: Triple<Type, Boolean, String>
): Aktor = Aktor(
    id.map { Identifikator(it.third, it.first, it.second) }
)

fun id(
    type: Type = Type.FOLKEREGISTERIDENT,
    gjeldende: Boolean = false,
    identifikasjonsnummer: String
): Triple<Type, Boolean, String> = Triple(type, gjeldende, identifikasjonsnummer)

fun testPeriode(
    periodeId: UUID = UUID.randomUUID(),
    identitetsnummer: String,
    fra: String,
    til: String? = null
) = PeriodeRad(
    periodeId = periodeId,
    identitetsnummer = identitetsnummer,
    fra = parse(fra),
    til = til?.let(::parse)
)
