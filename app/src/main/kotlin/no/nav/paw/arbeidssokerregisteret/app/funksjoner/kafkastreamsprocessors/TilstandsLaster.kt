package no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors

import no.nav.paw.arbeidssokerregisteret.app.StreamHendelse
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.RecordScope
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import kotlin.jvm.optionals.getOrNull


fun KStream<Long, StreamHendelse>.lastInternTilstand(
    tilstandDbNavn: String
): KStream<Long, InternTilstandOgHendelse> {
    val processorSupplier = { TilstandsLaster(tilstandDbNavn) }
    return process(processorSupplier, Named.`as`("lastInternTilstand"), tilstandDbNavn)
}

class TilstandsLaster(
    private val tilstandDbNavn: String
) : Processor<Long, StreamHendelse, Long, InternTilstandOgHendelse> {

    private var tilstandsDb: KeyValueStore<Long, Tilstand>? = null
    private var context: ProcessorContext<Long, InternTilstandOgHendelse>? = null

    override fun init(context: ProcessorContext<Long, InternTilstandOgHendelse>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandDbNavn)
    }

    override fun process(record: Record<Long, StreamHendelse>?) {
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
        record: Record<Long, StreamHendelse>
    ) {
        val tilstand: Tilstand? = db.get(record.key())
        val recordScope = ctx.recordMetadata().getOrNull()?.let { metadata ->
            RecordScope(
                key = record.key(),
                partition = metadata.partition(),
                offset = metadata.offset()
            )
        } ?: throw  IllegalStateException("Denne prosessoren kan kun brukes i en context hvor record metadata er tilgjengelig")
        ctx.forward(
            record.withValue(
                InternTilstandOgHendelse(
                    recordScope =  recordScope,
                    tilstand = tilstand,
                    hendelse = record.value()
                )
            )
        )
    }

    override fun close() {
        super.close()
        tilstandsDb?.close()
        tilstandsDb = null
        context = null
    }
}