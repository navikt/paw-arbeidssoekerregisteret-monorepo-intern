package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.processors

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentPerson
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.scheduleAvsluttPerioder
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.HendelseState
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.metrics.tellAvsluttetPeriode
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.application.InngangsReglerV3
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Duration
import java.util.UUID

fun KStream<Long, Periode>.oppdaterHendelseState(
    hendelseStateStoreName: String,
    sisteKjoeringStateStoreName: String,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    pdlHentPerson: PdlHentPerson
): KStream<Long, Hendelse> {
    val processor = {
        PeriodeProcessor(
            hendelseStateStoreName,
            sisteKjoeringStateStoreName,
            prometheusMeterRegistry,
            pdlHentPerson
        )
    }
    return process(processor, Named.`as`("periodeProsessor"), hendelseStateStoreName, sisteKjoeringStateStoreName)
}

class PeriodeProcessor(
    private val hendelseStateStoreName: String,
    private val sistKjoeringStateStoreName: String,
    private val prometheusMeterRegistry: PrometheusMeterRegistry,
    private val pdlHentPersonBolk: PdlHentPerson,
) : Processor<Long, Periode, Long, Hendelse> {
    private var hendelseStateStore: KeyValueStore<UUID, HendelseState>? = null
    private var sisteKjoeringStateStore: KeyValueStore<Int, Long>? = null
    private var context: ProcessorContext<Long, Hendelse>? = null

    override fun init(context: ProcessorContext<Long, Hendelse>?) {
        super.init(context)
        this.context = context
        hendelseStateStore = context?.getStateStore(hendelseStateStoreName)
        sisteKjoeringStateStore = context?.getStateStore(sistKjoeringStateStoreName)
        scheduleAvsluttPerioder(
            ctx = requireNotNull(context),
            hendelseStateStore = requireNotNull(hendelseStateStore),
            sisteKjoeringStateStore = requireNotNull(sisteKjoeringStateStore),
            schduledInterval = Duration.ofMinutes(10),
            interval = Duration.ofDays(1),
            pdlHentPersonBolk = pdlHentPersonBolk,
            prometheusMeterRegistry = prometheusMeterRegistry,
            regler = InngangsReglerV3
        )
    }

    override fun process(record: Record<Long, Periode>?) {
        if (record == null) return
        val hendelseStore = requireNotNull(hendelseStateStore) { "State store is not initialized" }
        val periode = record.value()
        if (periode.avsluttet != null) {
            val tilstand = hendelseStore.get(periode.id)
            prometheusMeterRegistry.tellAvsluttetPeriode(periode.avsluttet, tilstand)
            hendelseStore.delete(periode.id)
            return
        }
        val hendelseState = hendelseStore.get(periode.id)
        if (hendelseState == null) {
            hendelseStore.put(
                periode.id, HendelseState(
                    brukerId = null,
                    periodeId = periode.id,
                    recordKey = record.key(),
                    identitetsnummer = periode.identitetsnummer,
                    opplysninger = emptySet(),
                    startetTidspunkt = periode.startet.tidspunkt,
                    harTilhoerendePeriode = true
                )
            )
        } else {
            hendelseState.harTilhoerendePeriode = true
            hendelseStore.put(periode.id, hendelseState)
        }
    }
}
