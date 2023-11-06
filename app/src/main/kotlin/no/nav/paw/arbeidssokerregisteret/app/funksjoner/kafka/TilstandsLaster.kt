package no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafka

import no.nav.paw.arbeidssokerregisteret.app.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore


fun KStream<Long, SpecificRecord>.lastInternTilstand(
    tilstandDbNavn: String
): KStream<Long, InternTilstandOgHendelse> {
    val processorSupplier = { TilstandsLaster(tilstandDbNavn) }
    return process(processorSupplier, Named.`as`("lastInternTilstand"), tilstandDbNavn)
}
class TilstandsLaster(
    private val tilstandDbNavn: String
) : Processor<Long, SpecificRecord, Long, InternTilstandOgHendelse> {

    private var tilstandsDb: KeyValueStore<Long, Tilstand>? = null
    private var context: ProcessorContext<Long, InternTilstandOgHendelse>? = null

    override fun init(context: ProcessorContext<Long, InternTilstandOgHendelse>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandDbNavn)
    }

    override fun process(record: Record<Long, SpecificRecord>?) {
        if (record == null) return
        process(
            requireNotNull(context) { "Context er ikke initialisert" },
            requireNotNull(tilstandsDb) { "TilstandsDb er ikke initialisert" },
            record
        )
    }

    private fun process(
        ctx: ProcessorContext<Long, InternTilstandOgHendelse>,
        db: KeyValueStore<Long, Tilstand>,
        record: Record<Long, SpecificRecord>
    ) {
        val tilstand: Tilstand? = db.get(record.key())
        ctx.forward(record.withValue(InternTilstandOgHendelse(tilstand, record.value())))
    }

    override fun close() {
        super.close()
        tilstandsDb?.close()
        tilstandsDb = null
        context = null
    }
}