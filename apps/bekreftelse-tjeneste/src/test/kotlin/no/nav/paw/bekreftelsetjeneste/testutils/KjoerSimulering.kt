package no.nav.paw.bekreftelsetjeneste.testutils

import no.nav.paw.arbeidssoekerregisteret.testdata.ValueWithKafkaKeyData
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.*
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelsetjeneste.ApplicationTestContext
import java.time.Duration
import java.time.Instant

fun ApplicationTestContext.run(
    input: List<Pair<Instant, ValueWithKafkaKeyData<*>>>,
    stoppTid: Instant,
    opploesning: Duration
): List<Pair<Instant, BekreftelseHendelse>> {
    val perioder = input.map { it.second.value }.filterIsInstance<Periode>()
    val inputStream = input.iterator()
    var neste = if (inputStream.hasNext()) inputStream.next() else null
    var counter = 0
    val hendelser: MutableList<Pair<Instant, BekreftelseHendelse>> = mutableListOf()
    val ventendeBekreftelser = mutableListOf<BekreftelseTilgjengelig>()
    while (wallclock.get() < stoppTid) {
        val immutableNeste = neste
        if (immutableNeste != null && wallclock.get() >= immutableNeste.first) {
            when (val asTypedValue = immutableNeste.second.value) {
                null -> {}
                is Periode -> periodeTopic.pipeInput(immutableNeste.second.key, asTypedValue)
                is Bekreftelse -> {
                    if (ventendeBekreftelser.isNotEmpty()) {
                        with(ventendeBekreftelser.removeFirst()) {
                            bekreftelseTopic.pipeInput(
                                immutableNeste.second.key, Bekreftelse(
                                    asTypedValue.periodeId,
                                    asTypedValue.bekreftelsesloesning,
                                    this.bekreftelseId,
                                    asTypedValue.svar
                                )
                            )
                        }

                    } else {
                        bekreftelseTopic.pipeInput(immutableNeste.second.key, asTypedValue)
                    }
                }
                is PaaVegneAv -> bekreftelsePaaVegneAvTopic.pipeInput(immutableNeste.second.key, asTypedValue)
                is BekreftelseMeldingMottatt -> ventendeBekreftelser.removeIf { it.bekreftelseId == asTypedValue.bekreftelseId }
                else -> throw IllegalArgumentException("Ukjent type: ${asTypedValue::class.simpleName}")
            }
            neste = if (inputStream.hasNext()) inputStream.next() else null
        }
        if (!bekreftelseHendelseloggTopicOut.isEmpty) {
            val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
            when (val hendelse = kv.value) {
                is RegisterGracePeriodeUtloept,
                is RegisterGracePeriodeUtloeptEtterEksternInnsamling -> {
                    perioder
                        .first { it.id == kv.value.periodeId }
                        .let { periode ->
                            Periode(
                                periode.id,
                                periode.identitetsnummer,
                                periode.startet,
                                metadata(tidspunkt = wallclock.get())
                            )
                        }.also { periodeTopic.pipeInput(kv.key, it) }
                }
                is BekreftelseTilgjengelig -> {
                    ventendeBekreftelser.add(hendelse)
                }
                else -> { }
            }
            hendelser.add(wallclock.get() to kv.value)
        }
        still_klokken_frem(opploesning)
        counter++
        if (counter % 1000 == 0) {
            println("Tidspunkt: ${wallclock.get().prettyPrint}")
        }
    }
    return hendelser.toList()
}