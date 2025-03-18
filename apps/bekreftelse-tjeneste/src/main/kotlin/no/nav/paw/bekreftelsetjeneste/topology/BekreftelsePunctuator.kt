package no.nav.paw.bekreftelsetjeneste.topology

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.OddetallPartallMap
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import no.nav.paw.collections.toPawNonEmptyListOrNull
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val punctuatorLogger = LoggerFactory.getLogger("bekreftelse.tjeneste.punctuator")

fun bekreftelsePunctuator(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    bekreftelseTilstandStateStoreName: String,
    paaVegneAvTilstandStateStoreName: String,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    oddetallPartallMap: OddetallPartallMap,
    timestamp: Instant,
    ctx: ProcessorContext<Long, BekreftelseHendelse>
) {
    runPunctuator(
        ctx,
        bekreftelseTilstandStateStoreName,
        paaVegneAvTilstandStateStoreName,
        prometheusMeterRegistry,
        bekreftelseKonfigurasjon,
        timestamp,
        oddetallPartallMap
    )
}

//Bruk av 'partially1' på 'fun bekreftelsePunctuator' ser ut til å gjøre at trace koblingen forsvinner,
//derfor er det laget en ny funksjon 'runPunctuator' som 'bekreftelsePunctuator' kaller på.
@WithSpan(
    value = "bekreftelse_punctuator",
    kind = SpanKind.INTERNAL
)
fun runPunctuator(
    ctx: ProcessorContext<Long, BekreftelseHendelse>,
    bekreftelseTilstandStateStoreName: String,
    paaVegneAvTilstandStateStoreName: String,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    timestamp: Instant,
    oddetallPartallMap: OddetallPartallMap
) {
    val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore =
        ctx.getStateStore(bekreftelseTilstandStateStoreName)
    val paaVegneAvTilstandStateStore: PaaVegneAvTilstandStateStore = ctx.getStateStore(paaVegneAvTilstandStateStoreName)
    Span.current().setAttribute(AttributeKey.longKey("partition"), ctx.taskId().partition())
    bekreftelseTilstandStateStore
        .all()
        .use { states ->
            val totalt = AtomicLong(0)
            val antallRegisterHarAnsvar = AtomicLong(0)
            val startTid = Instant.now()
            states
                .asSequence()
                .filter { (_, tilstand) ->
                    val paaVegneAv = paaVegneAvTilstandStateStore.get(tilstand.periode.periodeId)
                    (paaVegneAv == null).also { registeretHarAnsvaret ->
                        totalt.incrementAndGet()
                        if (registeretHarAnsvaret) {
                            antallRegisterHarAnsvar.incrementAndGet()
                            punctuatorLogger.trace("Periode {}, registeret har ansvar", tilstand.periode.periodeId)
                        } else {
                            punctuatorLogger.trace("Periode {}, registeret har ikke ansvar", tilstand.periode.periodeId)
                        }
                    }
                }
                .map { (_, tilstand) ->
                    val context = BekreftelseContext(
                        prometheusMeterRegistry = prometheusMeterRegistry,
                        konfigurasjon = bekreftelseKonfigurasjon,
                        wallClock = WallClock(timestamp),
                        periodeInfo = tilstand.periode,
                        oddetallPartallMap = oddetallPartallMap
                    )
                    context.prosesser(tilstand)
                }
                .forEach { (oppdatertTilstand, bekreftelseHendelser) ->
                    bekreftelseHendelser.forEach {
                        ctx.forward(Record(oppdatertTilstand.periode.recordKey, it, ctx.currentSystemTimeMs()))
                    }
                    bekreftelseTilstandStateStore.put(oppdatertTilstand.periode.periodeId, oppdatertTilstand)
                }.also {
                    punctuatorLogger.info(
                        "[{}ms - partition:{}] Punctator kjørt for {} perioder, {} av disse har registeret ansvar",
                        between(startTid, Instant.now()).toMillis(),
                        ctx.taskId().partition(),
                        totalt.get(),
                        antallRegisterHarAnsvar.get()
                    )
                }
        }
}


private operator fun <K, V> KeyValue<K, V>.component1(): K = key
private operator fun <K, V> KeyValue<K, V>.component2(): V = value
