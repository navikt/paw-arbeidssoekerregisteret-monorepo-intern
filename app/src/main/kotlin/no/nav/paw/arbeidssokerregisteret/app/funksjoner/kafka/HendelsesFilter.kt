package no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafka

import no.nav.paw.arbeidssokerregisteret.app.Hendelse
import no.nav.paw.arbeidssokerregisteret.app.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record

fun KStream<Long, InternTilstandOgHendelse>.filtrer(
    filter: (Tilstand?, Hendelse) -> Boolean
): KStream<Long, InternTilstandOgHendelse> {
    val processorSupplier = { FiltrerHendelser(filter) }
    return process(processorSupplier)
}

class FiltrerHendelser(
    private val filter: (Tilstand?, Hendelse) -> Boolean
) : Processor<Long, InternTilstandOgHendelse, Long, InternTilstandOgHendelse> {

    private var context: ProcessorContext<Long, InternTilstandOgHendelse>? = null

    override fun init(context: ProcessorContext<Long, InternTilstandOgHendelse>?) {
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
        ctx: ProcessorContext<Long, InternTilstandOgHendelse>,
        record: Record<Long, InternTilstandOgHendelse>
    ) {
        val inkluder = filter(record.value().tilstand, record.value().hendelse)
        if (inkluder) ctx.forward(record)
    }

    override fun close() {
        super.close()
        context = null
    }
}