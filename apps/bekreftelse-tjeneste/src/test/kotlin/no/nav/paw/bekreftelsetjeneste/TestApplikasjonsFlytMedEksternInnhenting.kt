package no.nav.paw.bekreftelsetjeneste

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.ValueWithKafkaKeyData
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseMelding
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.startPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.stoppPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloeptEtterEksternInnsamling
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelsetjeneste.testutils.dager
import no.nav.paw.bekreftelsetjeneste.testutils.prettyPrint
import no.nav.paw.bekreftelsetjeneste.testutils.run
import no.nav.paw.bekreftelsetjeneste.testutils.setOppTest
import no.nav.paw.bekreftelsetjeneste.testutils.timestamp
import java.time.Duration
import java.time.Instant

class TestApplikasjonsFlytMedEksternInnsamling : FreeSpec({
    val identietsnummer = "12345678901"
    val periodeStartet = "05.02.2025 15:26".timestamp //Første onsdag i februar 2025
    val interval = 14.dager
    val graceperiode = 7.dager
    val tilgjengeligOffset = 3.dager
    with(
        setOppTest(
            datoOgKlokkeslettVedStart = periodeStartet,
            bekreftelseIntervall = interval,
            tilgjengeligOffset = tilgjengeligOffset,
            innleveringsfrist = graceperiode
        )
    ) {
        val opploesning = Duration.ofSeconds(180)
        val stoppTid = "02.05.2025 00:00".timestamp
        val hendelser = with(KafkaKeyContext(this.kafkaKeysClient)) {
            val periode = periode(
                identitetsnummer = identietsnummer,
                startetMetadata = metadata(tidspunkt = periodeStartet)
            )
            val eksterneHendelser: List<Pair<Instant, ValueWithKafkaKeyData<*>>> = listOf(
                "05.02.2025 15:26".timestamp to periode,
                "22.02.2025 13:34".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = periodeStartet,
                        gjelderTil = "24.02.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = true
                    )
                ),
                "12.03.2025 09:12".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, startPaaVegneAv(
                        periodeId = periode.value.id,
                        bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
                        grace = Duration.ofDays(7),
                        interval = Duration.ofDays(14)
                    )
                ),
                "29.03.2025 12:01".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = "10.03.2025 00:00".timestamp,
                        gjelderTil = "31.03.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = true,
                        bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER
                    )
                ),
                "21.04.2025 05:00".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, stoppPaaVegneAv(
                        periodeId = periode.value.id,
                        bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER
                    )
                )
            )
            run(eksterneHendelser, stoppTid, opploesning).also { hendelser ->
                println("${periodeStartet.prettyPrint} Arbeidssøkerperiode startet")
                println(
                    hendelser
                        .toList().joinToString("\n") { (tidspunkt, hendelse) ->
                            "${tidspunkt.prettyPrint}: hendelse: ${hendelse.prettyPrint()}"
                        })
            }
        }
        val kilde = mutableListOf(*hendelser.toTypedArray())
        println(kilde.joinToString("\n"))
        forventer<BekreftelseTilgjengelig>(
            kilde,
            fra = "21.02.2025 00:00".timestamp,
            til = "21.02.2025 06:00".timestamp
        )
        forventer<BekreftelseMeldingMottatt>(
            kilde,
            fra = "22.02.2025 13:34".timestamp,
            til = "22.02.2025 13:40".timestamp
        )
        forventer<BekreftelseTilgjengelig>(
            kilde,
            fra = "07.03.2025 00:00".timestamp,
            til = "07.03.2025 06:00".timestamp
        )
        forventer<LeveringsfristUtloept>(
            kilde,
            fra = "10.03.2025 00:00".timestamp,
            til = "10.03.2025 03:00".timestamp
        )
        forventer<BekreftelsePaaVegneAvStartet>(
            kilde,
            fra = "12.03.2025 09:12".timestamp,
            til = "12.03.2025 09:18".timestamp
        )
        forventer<RegisterGracePeriodeUtloeptEtterEksternInnsamling>(
            kilde,
            fra = "21.04.2025 05:00".timestamp,
            til = "21.04.2025 05:06".timestamp
        )
        forventer<PeriodeAvsluttet>(
            kilde,
            fra = "21.04.2025 05:00".timestamp,
            til = "21.04.2025 05:06".timestamp
        )
        "Ingen flere hendelser inntraff" {
            kilde.shouldBeEmpty()
        }
    }
})

inline fun <reified A : BekreftelseHendelse> FreeSpec.forventer(
    kilde: MutableList<Pair<Instant, BekreftelseHendelse>>,
    fra: Instant,
    til: Instant
) {
    val kandidater = kilde.toList().filter { (tidspunkt, _) -> tidspunkt == fra || (tidspunkt.isAfter(fra) && tidspunkt.isBefore(til)) }
    val resultat = kandidater.singleOrNull { it.second is A }
    "Forventet en hendelse av type ${A::class.simpleName} i tidsrommet ${fra.prettyPrint} til ${til.prettyPrint}" {
        withClue("Kilde: ${kilde}\nkandidater: ${kandidater}\nTidspunkt for ${A::class.simpleName}:\n\t${
            kilde.filter { it.second is A }.joinToString("\n\t", "\n\t") { it.first.prettyPrint }
        }") {
            resultat.shouldNotBeNull()
            kilde.remove(resultat)
        }
    }

}
