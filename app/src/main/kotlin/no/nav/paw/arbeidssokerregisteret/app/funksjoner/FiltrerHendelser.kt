package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.PeriodeTilstandV1
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore

class Context(
    val hendelse: Hendelse,
    val gjeldeneTilstand: PeriodeTilstandV1?
)

fun KStream<String, Hendelse>.filtrer(
    tilstandDbNavn: String,
    filter: Context.() -> Boolean
): KStream<String, Hendelse> {
    val processorSupplier = { FiltrerHendelser(tilstandDbNavn, filter) }
    return process(processorSupplier, tilstandDbNavn)
}

class FiltrerHendelser(
    private val tilstandDbNavn: String,
    private val filter: Context.() -> Boolean
) : Processor<String, Hendelse, String, Hendelse> {

    private var tilstandsDb: KeyValueStore<String, PeriodeTilstandV1>? = null
    private var context: ProcessorContext<String, Hendelse>? = null

    override fun init(context: ProcessorContext<String, Hendelse>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandDbNavn)
    }

    override fun process(record: Record<String, Hendelse>?) {
        if (record == null) return
        process(
            requireNotNull(context) { "Context er ikke initialisert" },
            requireNotNull(tilstandsDb) { "TilstandsDb er ikke initialisert" },
            record
        )
    }

    private fun process(
        ctx: ProcessorContext<String, Hendelse>,
        db: KeyValueStore<String, PeriodeTilstandV1>,
        record: Record<String, Hendelse>
    ) {
        val tilstand = db.get(record.key())
        val inkluder = with(Context(record.value(), tilstand)) { filter() }
        if (inkluder) ctx.forward(record)
    }

    override fun close() {
        super.close()
        tilstandsDb?.close()
        tilstandsDb = null
        context = null
    }
}