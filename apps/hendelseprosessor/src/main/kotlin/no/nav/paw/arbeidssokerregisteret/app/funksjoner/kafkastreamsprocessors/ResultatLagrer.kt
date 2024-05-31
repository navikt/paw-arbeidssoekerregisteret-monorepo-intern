package no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors

import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore

fun KStream<Long, InternTilstandOgApiTilstander>.lagreInternTilstand(
    tilstandDbNavn: String
): KStream<Long, InternTilstandOgApiTilstander> {
    val processBuilder = { ResultatLagrer(tilstandDbNavn) }
    return process(processBuilder, Named.`as`("lagreInternTilstand"), tilstandDbNavn)
}

class ResultatLagrer(
    private val tilstandDbNavn: String
) : Processor<Long, InternTilstandOgApiTilstander, Long, InternTilstandOgApiTilstander> {

    private var tilstandsDb: KeyValueStore<Long, TilstandV1>? = null
    private var context: ProcessorContext<Long, InternTilstandOgApiTilstander>? = null

    override fun init(context: ProcessorContext<Long, InternTilstandOgApiTilstander>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandDbNavn)
    }

    override fun process(record: Record<Long, InternTilstandOgApiTilstander>?) {
        if (record == null) return
        process(
            requireNotNull(context) { "Context er ikke initialisert" },
            requireNotNull(tilstandsDb) { "TilstandsDb er ikke initialisert" },
            record
        )
    }

    private fun process(
        ctx: ProcessorContext<Long, InternTilstandOgApiTilstander>,
        db: KeyValueStore<Long, TilstandV1>,
        record: Record<Long, InternTilstandOgApiTilstander>
    ) {
        val tilstandMedMetadata = record.value().tilstand
        db.put(record.value().id, tilstandMedMetadata)
        ctx.forward(record)
    }

    override fun close() {
        super.close()
        tilstandsDb?.close()
        tilstandsDb = null
        context = null
    }
}