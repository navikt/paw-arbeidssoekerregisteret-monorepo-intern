package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.PeriodeTilstandV1
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory

fun KStream<String, SpecificRecord>.opprettEllerOppdaterPeriode(
    tilstandDbNavn: String,
    bygger: Context.() -> PeriodeTilstandV1?
): KStream<String, PeriodeTilstandV1> {
    val processorSupplier = { GenererNyTilstand(tilstandDbNavn, bygger) }
    return process(processorSupplier, tilstandDbNavn)
}

class GenererNyTilstand(
    private val tilstandDbNavn: String,
    private val tilstandsBygger: Context.() -> PeriodeTilstandV1?
) : Processor<String, SpecificRecord, String, PeriodeTilstandV1> {

    private var tilstandsDb: KeyValueStore<String, PeriodeTilstandV1>? = null
    private var context: ProcessorContext<String, PeriodeTilstandV1>? = null

    override fun init(context: ProcessorContext<String, PeriodeTilstandV1>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandDbNavn)
    }

    override fun process(record: Record<String, SpecificRecord>?) {
        if (record == null) return
        process(
            requireNotNull(context) { "Context er ikke initialisert" },
            requireNotNull(tilstandsDb) { "TilstandsDb er ikke initialisert" },
            record
        )
    }

    private fun process(
        ctx: ProcessorContext<String, PeriodeTilstandV1>,
        db: KeyValueStore<String, PeriodeTilstandV1>,
        record: Record<String, SpecificRecord>
    ) {
        db.get(record.key()).also { tilstand ->
            LoggerFactory.getLogger(this::class.java).info("Bygger ny tilstand, endring=${record.value()::class.simpleName}, gjeldene=$tilstand")
            val nyTilstand = with(Context(record.value(), tilstand)) { tilstandsBygger() }
            LoggerFactory.getLogger(this::class.java).info("Ny tilstand (endret=${nyTilstand!=tilstand}): $nyTilstand")
            if (nyTilstand != tilstand) ctx.forward(Record(record.key(), nyTilstand, System.currentTimeMillis()))
        }
    }
}