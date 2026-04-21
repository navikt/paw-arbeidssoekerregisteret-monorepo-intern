package no.nav.paw.arbeidssoekerregisteret.topology.streams

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.VarselMelding
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.utils.Source
import no.nav.paw.arbeidssoekerregisteret.utils.beskjedVarselCounter
import no.nav.paw.arbeidssoekerregisteret.utils.readPeriodeCounter
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafka.processor.mapKeyAndValue
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import java.util.*

private const val PERIODE_PROCESSOR = "processPerioder"

fun StreamsBuilder.addPeriodeStream(
    applicationConfig: ApplicationConfig,
    meterRegistry: MeterRegistry,
    varselService: VarselService
): StreamsBuilder {
    with(applicationConfig) {
        stream<Long, Periode>(periodeTopic)
            .peek { _, periode -> meterRegistry.readPeriodeCounter(periode) }
            .genericProcess<Long, Periode, UUID, VarselMelding>(PERIODE_PROCESSOR) { record ->
                val periode = record.value()
                val meldinger = varselService.mottaPeriode(periode)
                meldinger.forEach { melding -> forward(record.withKey(melding.varselId).withValue(melding)) }
            }
            .peek { _, melding -> meterRegistry.beskjedVarselCounter(Source.KAFKA, melding) }
            .mapKeyAndValue("mapVarselMelding") { key, melding -> key.toString() to melding.value }
            .to(tmsVarselTopic, Produced.with(Serdes.String(), Serdes.String()))
    }
    return this
}