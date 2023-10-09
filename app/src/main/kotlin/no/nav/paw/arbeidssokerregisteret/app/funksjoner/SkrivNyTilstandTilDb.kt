package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.PeriodeTilstandV1
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory



enum class Operasjon{
    SLETT, OPPRETT_ELLER_OPPDATER
}
class SkrivNyTilstandTilDb(
    private val tilstandDbNavn: String,
    private val oppdater: (PeriodeTilstandV1) -> Operasjon
): Processor<String, PeriodeTilstandV1, String, PeriodeTilstandV1> {

    private var tilstandsDb: KeyValueStore<String, PeriodeTilstandV1>? = null
    private var context: ProcessorContext<String, PeriodeTilstandV1>? = null

    override fun init(context: ProcessorContext<String, PeriodeTilstandV1>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandDbNavn)
    }

    override fun process(record: Record<String, PeriodeTilstandV1>?) {
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
        record: Record<String, PeriodeTilstandV1>
    ) {
        LoggerFactory.getLogger(this::class.java).info("Oppdaterer tilstand: ${record.value()}")
        val operasjon = oppdater(record.value())
        when(operasjon){
            Operasjon.OPPRETT_ELLER_OPPDATER -> db.put(record.key(), record.value())
            Operasjon.SLETT -> db.delete(record.key())
        }
        LoggerFactory.getLogger(this::class.java).info("Videresender tilstand: ${record.value()}")
        ctx.forward(record)
    }
}