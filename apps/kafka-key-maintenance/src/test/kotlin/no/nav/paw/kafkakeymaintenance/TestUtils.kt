package no.nav.paw.kafkakeymaintenance

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.kafkakeygenerator.client.Alias
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.AktorConfig
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.HendelseRecord
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.metadata
import no.nav.paw.kafkakeymaintenance.pdlprocessor.procesAktorMelding
import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.paw.kafkakeymaintenance.perioder.statiskePerioder
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.streams.KeyValue
import java.time.Duration
import java.time.Instant
import java.time.Instant.parse
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

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

val aktorCfg
    get() = AktorConfig(
        aktorTopic = "aktor_topic",
        hendelseloggTopic = "hendelselogg_topic",
        supressionDelayMS = Duration.ofHours(1).toMillis(),
        intervalMS = Duration.ofMinutes(1).toMillis(),
        batchSize = 1
    )

fun ApplicationTestContext.process(
    aktor: Aktor
): List<HendelseRecord<Hendelse>> {
    val metadata = metadata(
        kilde = aktorConfig.aktorTopic,
        tidspunkt = Instant.now(),
        tidspunktFraKilde = TidspunktFraKilde(
            tidspunkt = Instant.now(),
            avviksType = AvviksType.FORSINKELSE
        )
    )
    return procesAktorMelding(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        aliasTjeneste,
        perioderTjeneste,
        aktor,
        metadata
    )
}

class ApplicationTestContext(
    aktorConfig: AktorConfig = aktorCfg
) {
    private val perioder: ConcurrentHashMap<String, PeriodeRad> = ConcurrentHashMap()
    private val alias: ConcurrentHashMap<String, List<Alias>> = ConcurrentHashMap()

    val aktorConfig = aktorConfig
    val perioderTjeneste: Perioder get() = statiskePerioder(perioder)
    val aliasTjeneste: (List<String>) -> List<LokaleAlias> = { hentAlias(alias, it) }
    val hendelser: BlockingQueue<HendelseRecord<Hendelse>> = LinkedBlockingQueue()
    val receiver: (HendelseRecord<Hendelse>) -> Unit = { hendelser.add(it) }

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
