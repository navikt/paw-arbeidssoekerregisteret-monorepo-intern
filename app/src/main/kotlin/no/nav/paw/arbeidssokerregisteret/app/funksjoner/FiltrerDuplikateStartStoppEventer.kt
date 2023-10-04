package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.PeriodeTilstandV1
import no.nav.paw.arbeidssokerregisteret.app.periodeTilstand
import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import no.nav.paw.arbeidssokerregisteret.intern.StoppV1
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore

val filtrerDuplikateStartStoppEventer: ProcessorSupplier<String, SpecificRecord, String, SpecificRecord> =
    ProcessorSupplier {
        FiltrerDuplikateStartStoppEventer("tilstandsDb")
    }

fun KStream<String, SpecificRecord>.filtererDuplikateStartStoppEventer(dbNavn: String): KStream<String, SpecificRecord> =
    process(
        filtrerDuplikateStartStoppEventer,
        Named.`as`("filtrer-duplikate-start-stopp-eventer"),
        dbNavn
    )

class FiltrerDuplikateStartStoppEventer(private val tilstandDbNavn: String) :
    Processor<String, SpecificRecord, String, SpecificRecord> {

    private var tilstandsDb: KeyValueStore<String, PeriodeTilstandV1>? = null
    private var context: ProcessorContext<String, SpecificRecord>? = null

    override fun init(context: ProcessorContext<String, SpecificRecord>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandDbNavn)
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

    override fun close() {
        super.close()
        tilstandsDb?.close()
        tilstandsDb = null
        context = null
    }
}