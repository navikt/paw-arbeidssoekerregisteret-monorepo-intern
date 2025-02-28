package no.nav.paw.bekreftelsetjeneste

import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.ValueWithKafkaKeyData
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseMelding
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.startPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.stoppPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import java.time.Duration
import java.time.Instant
import java.util.*

fun main() {
    val identietsnummer = "12345678901"
    val periodeStartet = "05.02.2025 15:26".timestamp //Første onsdag i februar 2025
    val interval = 14.dager
    val graceperiode = 7.dager
    val tilgjengeligOffset = 3.dager
    with(
        setOppTest(
            datoOgKlokkeslettVedStart = periodeStartet,
            bekreftelseIntervall =interval,
            tilgjengeligOffset = tilgjengeligOffset,
            innleveringsfrist = graceperiode
        )
    ) {
        val opploesning = Duration.ofSeconds(60)
        val stoppTid = "01.06.2025 00:00".timestamp
        with(KafkaKeyContext(this.kafkaKeysClient)) {
            val periode = periode(
                identitetsnummer = identietsnummer,
                startetMetadata = metadata(tidspunkt = periodeStartet)
            )
            val eksterneHendelser: List<Pair<Instant, ValueWithKafkaKeyData<*>>> = listOf(
                "05.02.2025 15:26".timestamp to periode,
                "22.02.2025 13:34".timestamp to ValueWithKafkaKeyData(periode.id, periode.key, bekreftelseMelding(
                    periodeId = periode.value.id,
                    gjelderFra = periodeStartet,
                    gjelderTil = "24.02.2025 00:00".timestamp,
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = true
                )),
                "12.03.2025 09:12".timestamp to ValueWithKafkaKeyData(periode.id, periode.key, startPaaVegneAv(
                    periodeId = periode.value.id,
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
                    grace = Duration.ofDays(7),
                    interval = Duration.ofDays(14)
                )),
                "29.03.2025 12:01".timestamp to ValueWithKafkaKeyData(periode.id, periode.key, bekreftelseMelding(
                    periodeId = periode.value.id,
                    gjelderFra = "10.03.2025 00:00".timestamp,
                    gjelderTil = "31.03.2025 00:00".timestamp,
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = true,
                    bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER
                )),
                "21.04.2025 05:00".timestamp to ValueWithKafkaKeyData(periode.id, periode.key, stoppPaaVegneAv(
                    periodeId = periode.value.id,
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER
                ))
            )
            val hendelser = run(eksterneHendelser, stoppTid, opploesning)
            println("${periodeStartet.prettyPrint} Arbeidssøkerperiode startet")
            println(
                hendelser
                    .toList().joinToString("\n") { (tidspunkt, hendelse) ->
                        "${tidspunkt.prettyPrint}: hendelse: ${hendelse.prettyPrint()}"
                    })
        }
    }
}

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
                is RegisterGracePeriodeUtloept -> {
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
        if (counter % 100000 == 0) {
            println("Tidspunkt: ${wallclock.get().prettyPrint}")
        }
    }
    return hendelser.toList()
}

