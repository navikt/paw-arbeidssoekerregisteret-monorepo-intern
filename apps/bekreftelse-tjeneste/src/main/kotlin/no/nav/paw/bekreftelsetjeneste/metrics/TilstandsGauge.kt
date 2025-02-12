package no.nav.paw.bekreftelsetjeneste.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.paavegneav.InternPaaVegneAv
import no.nav.paw.bekreftelsetjeneste.paavegneav.PaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstandStatus
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeVarselet
import no.nav.paw.bekreftelsetjeneste.tilstand.IkkeKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.InternBekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelsetjeneste.tilstand.KlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.Levert
import no.nav.paw.bekreftelsetjeneste.tilstand.VenterSvar
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

const val bekreftelseTilstandMetric = "bekreftelse_tilstand_v2"
class Labels {
    companion object {
        const val graceperiode_dager = "graceperiode_dager"
        const val interval_dager = "interval_dager"
        const val dager_siden_siste_leverte_periode_avsluttet = "dager_siden_siste_leverte_periode_avsluttet"
        const val ansvarlig = "ansvarlig"
        const val antall_ansvarlige = "antall_ansvarlige"
        const val dager_siden_forfall = "dager_siden_forfall"
        const val dager_til_siste_frist = "dager_til_siste_frist"
    }
}

fun <T, E> T.asCloseableSequence(): Sequence<E> where T : Closeable, T : Iterator<E> =
    generateSequence {
        if (this.hasNext()) {
            this.next()
        } else {
            this.close()
            null
        }
    }

class TilstandsGauge(
    private val kafkaStreams: KafkaStreams,
    private val paaVegneAvStoreName: String,
    private val tilstandStoreName: String,
    keepGoing: AtomicBoolean,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
) {

    val stateGaugeTask = initStateGaugeTask(
        keepGoing = keepGoing,
        registry = prometheusMeterRegistry,
        streamStateSupplier = { kafkaStreams.state() },
        contentSupplier = ::contentSupplier,
        mapper = { (tilstand, ansvarlige) ->
            val now = Instant.now()
            listOf(map(now, bekreftelseKonfigurasjon, tilstand, ansvarlige))
        }
    )

    private fun contentSupplier(): Sequence<Pair<BekreftelseTilstand, List<InternPaaVegneAv>>> {
        val paaVegneAvStateStore = kafkaStreams.keyValueStateStore<UUID, PaaVegneAvTilstand>(paaVegneAvStoreName)
        val tilstandStateStore = kafkaStreams.keyValueStateStore<UUID, BekreftelseTilstand>(tilstandStoreName)
        return tilstandStateStore.all()
            .asCloseableSequence()
            .map { (periodeId, tilstand) ->
                tilstand to (paaVegneAvStateStore[periodeId]?.paaVegneAvList ?: emptyList())
            }
    }
}

fun map(
    naaTid: Instant,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    tilstand: BekreftelseTilstand,
    ansvarlige: Collection<InternPaaVegneAv>
): WithMetricsInfo {
    val gracePeriode =
        ansvarlige.maxOfOrNull { it.gracePeriode } ?: bekreftelseKonfigurasjon.graceperiode
    val intervall = ansvarlige.maxOfOrNull { it.intervall } ?: bekreftelseKonfigurasjon.interval
    val (antallAnsvarlige, ansvarlig) = ansvar(ansvarlige)
    val dagerSidenSistLeverteAvsluttet = dagerSidenForfallSistLeverte(naaTid, tilstand)
    val dagerSidenForfallIkkeLevert = dagerSidenFrist(naaTid, tilstand)
    val dagerTilSisteFrist = with(gracePeriode.toDays()) {
        when {
            dagerSidenForfallIkkeLevert != null -> this - dagerSidenForfallIkkeLevert
            dagerSidenSistLeverteAvsluttet != null -> this - dagerSidenSistLeverteAvsluttet
            else -> null
        }
    }
    val tags = listOf(
        Tag.of(Labels.graceperiode_dager, gracePeriode.toDays().toString()),
        Tag.of(Labels.interval_dager, intervall.toDays().toString()),
        Tag.of(Labels.dager_siden_siste_leverte_periode_avsluttet, dagerSidenSistLeverteAvsluttet.dagerSidenSistLeverteAvsluttetString()),
        Tag.of(Labels.ansvarlig, ansvarlig),
        Tag.of(Labels.antall_ansvarlige, antallAnsvarlige.toString()),
        Tag.of(Labels.dager_siden_forfall, dagerSidenForfallIkkeLevert.dagerSidenForfallString()),
        Tag.of(Labels.dager_til_siste_frist, dagerTilSisteFrist.dagerTilSisteFristString())
    )
    return WithMetricsInfo(
        partition = tilstand.kafkaPartition,
        name = bekreftelseTilstandMetric,
        labels = tags
    )
}


operator fun <K, V> KeyValue<K, V>.component1(): K = key
operator fun <K, V> KeyValue<K, V>.component2(): V = value

fun <K, V> KafkaStreams.keyValueStateStore(name: String): ReadOnlyKeyValueStore<K, V> = store(
    StoreQueryParameters.fromNameAndType(
        name,
        QueryableStoreTypes.keyValueStore()
    )
)

fun dagerSidenFrist(naaTid: Instant, tilstand: BekreftelseTilstand): Long? =
    tilstand.bekreftelser
        .filter { it.sisteTilstand().utestaaende() }
        .minByOrNull { it.gjelderTil }
        ?.gjelderTil
        ?.let { Duration.between(naaTid, it).toDays() }

fun dagerSidenForfallSistLeverte(naaTid: Instant, tilstand: BekreftelseTilstand): Long? =
    tilstand.bekreftelser
        .filter { it.sisteTilstand() is Levert }
        .maxByOrNull { it.gjelderTil }
        ?.gjelderTil
        ?.let { Duration.between(it, naaTid).toDays() }

fun BekreftelseTilstandStatus.utestaaende(): Boolean = when (this) {
    is GracePeriodeUtloept -> true
    is GracePeriodeVarselet -> true
    is IkkeKlarForUtfylling -> false
    is InternBekreftelsePaaVegneAvStartet -> false
    is KlarForUtfylling -> true
    is Levert -> false
    is VenterSvar -> true
}

fun ansvar(ansvarlige: Collection<InternPaaVegneAv>): Pair<Int, String> = when {
    ansvarlige.isEmpty() -> 0 to "registeret"
    ansvarlige.size == 1 -> 1 to ansvarlige.first().loesning.name
    ansvarlige.size <= 3 -> ansvarlige.size to ansvarlige.map { it.loesning.name }.sorted().joinToString("_")
    else -> ansvarlige.size to "flere"
}

fun Long?.dagerSidenForfallString(): String = when {
    this == null -> "levert"
    this < 0 -> "ikke forfalt"
    this in (0..7) -> this.toString()
    this in (8..14) -> "[8-14]"
    else -> "14+"
}

fun Long?.dagerTilSisteFristString(): String = when {
    this == null -> "levert"
    this < -3 -> "passert"
    this in (-3..7) -> this.toString()
    this in (8..14) -> "[8-14]"
    else -> "14+"
}

fun Long?.dagerSidenSistLeverteAvsluttetString(): String = when {
    this == null -> "ingen levert"
    this < 0 -> "ikke avsluttet"
    this in (0..7) -> this.toString()
    this in (8..14) -> "[8-14]"
    else -> "14+"
}