package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.PeriodeTilstandV1
import no.nav.paw.arbeidssokerregisteret.app.periodeTilstand
import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import no.nav.paw.arbeidssokerregisteret.intern.StoppV1
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore

val ignorerDuplikateStartStoppEventer: ProcessorSupplier<String, SpecificRecord, String, SpecificRecord> =
    ProcessorSupplier {
        IgnorerDuplikateStartStoppEventer("tilstandsDb")
    }

class IgnorerDuplikateStartStoppEventer(private val tilstandsDbNavn: String) :
    Processor<String, SpecificRecord, String, SpecificRecord> {

    private var tilstandsDb: KeyValueStore<String, PeriodeTilstandV1>? = null
    private var context: ProcessorContext<String, SpecificRecord>? = null

    override fun init(context: ProcessorContext<String, SpecificRecord>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandsDbNavn)
    }

    override fun process(record: Record<String, SpecificRecord>?) {
        requireNotNull(tilstandsDb) { "TilstandsDb er ikke initialisert" }
        requireNotNull(context) { "Context er ikke initialisert" }
        when (val value = record?.value()) {
            null -> return
            is StartV1 -> tilstandsDb
                ?.putIfAbsent(value.personNummer, value.periodeTilstand())
                .also { if (it == null) context?.forward(record) }

            is StoppV1 -> tilstandsDb?.delete(value.personNummer).also {
                if (it != null) context?.forward(record)
            }

            else -> context?.forward(record)
        }
    }
}