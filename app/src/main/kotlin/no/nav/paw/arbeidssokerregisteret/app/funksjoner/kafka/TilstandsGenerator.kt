package no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafka

import no.nav.paw.arbeidssokerregisteret.app.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.InternTilstandOgHendelse
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record

fun KStream<Long, InternTilstandOgHendelse>.genererTilstander(
    bygger: (Long, InternTilstandOgHendelse) -> InternTilstandOgApiTilstander
): KStream<Long, InternTilstandOgApiTilstander> {
    val processorSupplier = { GenererNyeTilstander(bygger) }
    return process(processorSupplier)
}

class GenererNyeTilstander(
    private val bygger: (Long, InternTilstandOgHendelse) -> InternTilstandOgApiTilstander
) : Processor<Long, InternTilstandOgHendelse, Long, InternTilstandOgApiTilstander> {

    private var context: ProcessorContext<Long, InternTilstandOgApiTilstander>? = null

    override fun init(context: ProcessorContext<Long, InternTilstandOgApiTilstander>?) {
        super.init(context)
        this.context = context
    }

    override fun process(record: Record<Long, InternTilstandOgHendelse>?) {
        if (record == null) return
        process(
            requireNotNull(context) { "Context er ikke initialisert" },
            record
        )
    }

    private fun process(
        ctx: ProcessorContext<Long, InternTilstandOgApiTilstander>,
        record: Record<Long, InternTilstandOgHendelse>
    ) {
        val internTilstandOgApiTilstander = bygger(record.key(), record.value())
        ctx.forward(record.withValue(internTilstandOgApiTilstander))
    }
}