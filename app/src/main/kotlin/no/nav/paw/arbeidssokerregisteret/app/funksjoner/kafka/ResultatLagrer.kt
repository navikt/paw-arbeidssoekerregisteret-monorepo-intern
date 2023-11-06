package no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafka

import no.nav.paw.arbeidssokerregisteret.app.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore

class ResultatLagrer(
    private val tilstandDbNavn: String
) : Processor<Long, SpecificRecord, Long, InternTilstandOgApiTilstander> {

    private var tilstandsDb: KeyValueStore<Long, Tilstand>? = null
    private var context: ProcessorContext<Long, InternTilstandOgApiTilstander>? = null

    override fun init(context: ProcessorContext<Long, InternTilstandOgApiTilstander>?) {
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
        ctx: ProcessorContext<Long, InternTilstandOgApiTilstander>,
        db: KeyValueStore<Long, Tilstand>,
        record: Record<Long, SpecificRecord>
    ) {

    }

    override fun close() {
        super.close()
        tilstandsDb?.close()
        tilstandsDb = null
        context = null
    }
}